package com.example.aurorakmpdemo.data

import com.example.aurorakmpdemo.db.PostEntity
import com.example.aurorakmpdemo.db.PostsDao
import com.example.aurorakmpdemo.db.SavedNoteEntity
import kotlin.time.Clock

/**
 * Small repository intentionally backed by a DAO directly.
 * It is a runnable example of the same dependency direction used by Continent:
 * Room database -> DAO -> repository -> UI.
 */
class RoomVerificationRepository(
    private val dao: PostsDao,
) {
    suspend fun saveAndRead(note: String): RoomVerificationResult {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        val normalizedNote = note.trim()
        val post = PostEntity(
            id = VERIFICATION_POST_ID,
            userId = VERIFICATION_USER_ID,
            title = normalizedNote,
            body = "Room/Koin verification written at $timestamp",
        )
        dao.upsert(post)
        dao.upsertLatestNote(SavedNoteEntity(value = normalizedNote))
        return RoomVerificationResult(
            post = dao.selectById(VERIFICATION_POST_ID),
            note = dao.selectLatestNote()?.value,
            timestamp = timestamp,
        )
    }

    suspend fun read(): RoomVerificationResult = RoomVerificationResult(
        post = dao.selectById(VERIFICATION_POST_ID),
        note = dao.selectLatestNote()?.value,
        timestamp = null,
    )

    companion object {
        const val VERIFICATION_POST_ID = 777
        const val VERIFICATION_USER_ID = 42
    }
}

data class RoomVerificationResult(
    val post: PostEntity?,
    val note: String?,
    val timestamp: Long?,
) {
    val isPersisted: Boolean get() = post != null && note != null
}
