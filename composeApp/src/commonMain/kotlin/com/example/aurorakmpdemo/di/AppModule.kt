package com.example.aurorakmpdemo.di

import com.example.aurorakmpdemo.data.PostsRepository
import com.example.aurorakmpdemo.data.PostsStorage
import com.example.aurorakmpdemo.data.buildHttpClient
import com.example.aurorakmpdemo.platform.PlatformDiagnosticsProvider
import com.example.aurorakmpdemo.platform.createPostsStorage
import org.koin.dsl.module

fun appModule() = module {
    single { buildHttpClient() }
    single<PostsStorage> { createPostsStorage() }
    single { PostsRepository(get(), get()) }
    single { PlatformDiagnosticsProvider() }
}
