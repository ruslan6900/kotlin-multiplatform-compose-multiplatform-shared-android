package com.example.aurorakmpdemo.data

import com.example.aurorakmpdemo.db.PostEntity
import com.example.aurorakmpdemo.db.PostsDao
import com.example.aurorakmpdemo.db.SavedNoteEntity

class RoomPostsStorage(
    private val dao: PostsDao,
) : PostsStorage {
    override suspend fun savePost(post: Post) {
        dao.upsert(PostEntity.fromPost(post))
    }

    override suspend fun getPost(id: Int): Post? = dao.selectById(id)?.toPost()

    override suspend fun saveLatestNote(text: String) {
        dao.upsertLatestNote(
            SavedNoteEntity(
                key = SavedNoteEntity.DEFAULT_KEY,
                value = text,
            ),
        )
    }

    override suspend fun getLatestNote(): String? = dao.selectLatestNote()?.value
}
