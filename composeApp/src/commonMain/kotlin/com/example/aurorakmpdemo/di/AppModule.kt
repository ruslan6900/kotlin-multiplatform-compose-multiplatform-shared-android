package com.example.aurorakmpdemo.di

import com.example.aurorakmpdemo.data.PostsRepository
import com.example.aurorakmpdemo.data.PostsStorage
import com.example.aurorakmpdemo.data.RoomPostsStorage
import com.example.aurorakmpdemo.data.RoomVerificationRepository
import com.example.aurorakmpdemo.data.LibraryCheckRequest
import com.example.aurorakmpdemo.data.LibraryChecksRunner
import com.example.aurorakmpdemo.data.LibraryChecksViewModel
import com.example.aurorakmpdemo.data.buildHttpClient
import com.example.aurorakmpdemo.db.PostsRoomDatabase
import com.example.aurorakmpdemo.platform.PlatformDiagnosticsProvider
import com.example.aurorakmpdemo.platform.createPostsDatabase
import org.koin.dsl.module
import org.koin.core.qualifier.named

private fun libraryChecksModule() = module {
    single { buildHttpClient() }
    single(named("library-check-prefix")) { "koin" }
    factory { parameters -> LibraryCheckRequest(parameters.get(), get(named("library-check-prefix"))) }
    factory { LibraryChecksRunner(get(), get(), get(), get(named("library-check-prefix"))) }
    factory { LibraryChecksViewModel(get()) }
}

fun appModule() = module {
    includes(libraryChecksModule())
    // Same shape as the Continent graph: database -> DAO -> repository.
    single<PostsRoomDatabase> { createPostsDatabase() }
    single { get<PostsRoomDatabase>().postsDao() }
    single<PostsStorage> { RoomPostsStorage(get()) }
    single { PostsRepository(get(), get()) }
    single { RoomVerificationRepository(get()) }
    single { PlatformDiagnosticsProvider() }
}
