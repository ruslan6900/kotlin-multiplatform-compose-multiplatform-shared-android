package com.example.aurorakmpdemo.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.SystemClock
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.*

actual fun elapsedRealtimeMillis(): Long = SystemClock.elapsedRealtime()
private fun manager() = AndroidAppContext.require().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

actual suspend fun queryNetworkSnapshot(): NetworkSnapshot {
    return try {
        val cm = manager()
        val network = cm.activeNetwork ?: return NetworkSnapshot(NetworkAvailability.Offline, "activeNetwork=null")
        val capabilities = cm.getNetworkCapabilities(network)
            ?: return NetworkSnapshot(NetworkAvailability.Unknown, "capabilities=null", detail = "Активная сеть не предоставляет свойства")
        val internet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val validated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val transport = listOf(
            NetworkCapabilities.TRANSPORT_WIFI to "Wi-Fi",
            NetworkCapabilities.TRANSPORT_CELLULAR to "мобильная",
            NetworkCapabilities.TRANSPORT_ETHERNET to "Ethernet",
            NetworkCapabilities.TRANSPORT_VPN to "VPN",
            NetworkCapabilities.TRANSPORT_BLUETOOTH to "Bluetooth",
        ).filter { capabilities.hasTransport(it.first) }.joinToString(" + ") { it.second }.ifBlank { "другой" }
        val status = when {
            internet && validated -> NetworkAvailability.Online
            else -> NetworkAvailability.ConnectedUnverified
        }
        NetworkSnapshot(status, if (validated && internet) "validated" else "unvalidated", transport,
            "INTERNET=$internet; VALIDATED=$validated; METERED=${!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)}")
    } catch (e: Exception) { NetworkSnapshot(NetworkAvailability.Unknown, "ошибка Android", detail = e.message ?: "нет ответа") }
}

actual fun observeNetworkSnapshots(): Flow<NetworkSnapshot> = callbackFlow {
    val cm = manager()
    // The callback only invalidates the snapshot: query the current default network,
    // because a late onLost callback may belong to the previous default network.
    val invalidations = kotlinx.coroutines.channels.Channel<Unit>(kotlinx.coroutines.channels.Channel.CONFLATED)
    val forwarding = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { invalidations.trySend(Unit) }
        override fun onLost(network: Network) { invalidations.trySend(Unit) }
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) { invalidations.trySend(Unit) }
        override fun onUnavailable() { invalidations.trySend(Unit) }
    }
    cm.registerDefaultNetworkCallback(forwarding)
    val job = launch {
        trySend(queryNetworkSnapshot())
        for (ignored in invalidations) trySend(queryNetworkSnapshot())
    }
    awaitClose { cm.unregisterNetworkCallback(forwarding); invalidations.close(); job.cancel() }
}.distinctUntilChanged()

actual fun observeSystemTimeSignals(): Flow<SystemTimeSignal> = callbackFlow {
    val context = AndroidAppContext.require()
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_TIME_CHANGED -> trySend(SystemTimeSignal.ClockChanged)
                Intent.ACTION_TIMEZONE_CHANGED -> trySend(SystemTimeSignal.TimeZoneChanged)
                Intent.ACTION_TIME_TICK -> trySend(SystemTimeSignal.MinuteTick)
            }
        }
    }
    val filter = IntentFilter().apply {
        addAction(Intent.ACTION_TIME_CHANGED); addAction(Intent.ACTION_TIMEZONE_CHANGED); addAction(Intent.ACTION_TIME_TICK)
    }
    if (Build.VERSION.SDK_INT >= 33) context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
    else @Suppress("DEPRECATION") context.registerReceiver(receiver, filter)
    awaitClose { context.unregisterReceiver(receiver) }
}
