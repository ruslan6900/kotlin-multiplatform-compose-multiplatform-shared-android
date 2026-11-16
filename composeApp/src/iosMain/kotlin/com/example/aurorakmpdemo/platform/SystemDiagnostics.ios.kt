package com.example.aurorakmpdemo.platform

import kotlinx.cinterop.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.channels.awaitClose
import platform.CoreFoundation.CFRelease
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSSystemTimeZoneDidChangeNotification
import platform.UIKit.UIApplicationSignificantTimeChangeNotification
import platform.SystemConfiguration.*

actual fun elapsedRealtimeMillis(): Long = (NSProcessInfo.processInfo.systemUptime * 1000.0).toLong()
@OptIn(ExperimentalForeignApi::class)
actual suspend fun queryNetworkSnapshot(): NetworkSnapshot = memScoped {
    val reachability = SCNetworkReachabilityCreateWithName(null, "0.0.0.0")
        ?: return@memScoped NetworkSnapshot(NetworkAvailability.Unknown, "ошибка reachability")
    try {
        val flags = alloc<UIntVar>()
        if (!SCNetworkReachabilityGetFlags(reachability, flags.ptr))
            return@memScoped NetworkSnapshot(NetworkAvailability.Unknown, "ошибка reachability")
        val value = flags.value
        val reachable = value and kSCNetworkReachabilityFlagsReachable != 0u
        val requiresConnection = value and kSCNetworkReachabilityFlagsConnectionRequired != 0u
        if (reachable && !requiresConnection)
            NetworkSnapshot(NetworkAvailability.ConnectedUnverified, "маршрут доступен", detail = "iOS не подтвердила доступ в интернет")
        else NetworkSnapshot(NetworkAvailability.Offline, "маршрут недоступен", detail = "reachable=$reachable; connectionRequired=$requiresConnection")
    } finally { CFRelease(reachability) }
}
actual fun observeNetworkSnapshots(): Flow<NetworkSnapshot> = flow {
    while (currentCoroutineContext().isActive) { emit(queryNetworkSnapshot()); delay(2000) }
}.distinctUntilChanged()
@OptIn(ExperimentalForeignApi::class)
actual fun observeSystemTimeSignals(): Flow<SystemTimeSignal> = callbackFlow {
    val center = NSNotificationCenter.defaultCenter
    val zoneObserver = center.addObserverForName(NSSystemTimeZoneDidChangeNotification, null, null) {
        trySend(SystemTimeSignal.TimeZoneChanged)
    }
    // iOS reports significant time changes (including midnight and time-zone changes),
    // so the shared clock comparison decides whether wall time actually jumped.
    val timeObserver = center.addObserverForName(UIApplicationSignificantTimeChangeNotification, null, null) {
        trySend(SystemTimeSignal.SettingsChanged)
    }
    awaitClose { center.removeObserver(zoneObserver); center.removeObserver(timeObserver) }
}
