package com.example.aurorakmpdemo.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class PostsRepository(
    private val client: HttpClient,
    private val storage: PostsStorage,
) {
    suspend fun fetchAndPersistPost(id: Int): FetchResult {
        val remote = client.get("https://jsonplaceholder.typicode.com/posts/$id").body<Post>()
        storage.savePost(remote)
        return FetchResult(remote = remote, cached = storage.getPost(id))
    }
}
