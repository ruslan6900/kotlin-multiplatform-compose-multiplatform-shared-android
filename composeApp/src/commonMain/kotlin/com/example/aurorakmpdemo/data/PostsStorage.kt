package com.example.aurorakmpdemo.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface PostsStorage {
    suspend fun savePost(post: Post)
    suspend fun getPost(id: Int): Post?
    suspend fun saveLatestNote(text: String)
    suspend fun getLatestNote(): String?
}

class InMemoryPostsStorage : PostsStorage {
    private val mutex = Mutex()
    private val posts = mutableMapOf<Int, Post>()

    override suspend fun savePost(post: Post) {
        mutex.withLock {
            posts[post.id] = post
        }
    }

    override suspend fun getPost(id: Int): Post? = mutex.withLock { posts[id] }

    private var latestNote: String? = null

    override suspend fun saveLatestNote(text: String) {
        mutex.withLock {
            latestNote = text
        }
    }

    override suspend fun getLatestNote(): String? = mutex.withLock { latestNote }
}
