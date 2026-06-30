package com.example.aurorakmpdemo.data

import kotlinx.serialization.Serializable

@Serializable
data class Post(
    val userId: Int,
    val id: Int,
    val title: String,
    val body: String,
)

data class FetchResult(
    val remote: Post,
    val cached: Post?,
)
