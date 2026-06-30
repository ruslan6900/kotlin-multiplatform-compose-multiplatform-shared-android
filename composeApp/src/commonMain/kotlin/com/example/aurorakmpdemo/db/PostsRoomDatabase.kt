package com.example.aurorakmpdemo.db

import androidx.room.ConstructedBy
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.Upsert
import com.example.aurorakmpdemo.data.Post

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: Int,
    val userId: Int,
    val title: String,
    val body: String,
) {
    fun toPost(): Post = Post(
        userId = userId,
        id = id,
        title = title,
        body = body,
    )

    companion object {
        fun fromPost(post: Post): PostEntity = PostEntity(
            id = post.id,
            userId = post.userId,
            title = post.title,
            body = post.body,
        )
    }
}

@Entity(tableName = "saved_note")
data class SavedNoteEntity(
    @PrimaryKey val key: Int = DEFAULT_KEY,
    val value: String,
) {
    companion object {
        const val DEFAULT_KEY: Int = 1
    }
}

@Dao
interface PostsDao {
    @Upsert
    suspend fun upsert(post: PostEntity)

    @Upsert
    suspend fun upsertLatestNote(note: SavedNoteEntity)

    @Query("SELECT * FROM posts WHERE id = :id LIMIT 1")
    suspend fun selectById(id: Int): PostEntity?

    @Query("SELECT * FROM saved_note WHERE key = :key LIMIT 1")
    suspend fun selectLatestNote(key: Int = SavedNoteEntity.DEFAULT_KEY): SavedNoteEntity?
}

@Database(
    entities = [PostEntity::class, SavedNoteEntity::class],
    version = 2,
    exportSchema = true,
)
@ConstructedBy(PostsRoomDatabaseConstructor::class)
abstract class PostsRoomDatabase : RoomDatabase() {
    abstract fun postsDao(): PostsDao
}

@Suppress("KotlinNoActualForExpect")
expect object PostsRoomDatabaseConstructor : RoomDatabaseConstructor<PostsRoomDatabase> {
    override fun initialize(): PostsRoomDatabase
}
