package com.example.aurorakmpdemo.db

import androidx.room.ConstructedBy
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.RawQuery
import androidx.room.RoomRawQuery
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.room.Upsert
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Transaction
import com.example.aurorakmpdemo.data.Post
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "posts", indices = [Index(value = ["userId"])])
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

@Entity(tableName = "library_probe")
data class LibraryProbeEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val stage: String,
    val tags: List<String>,
    val createdAt: Long,
)

@Entity(
    tableName = "library_probe_event",
    foreignKeys = [
        ForeignKey(
            entity = LibraryProbeEntity::class,
            parentColumns = ["id"],
            childColumns = ["probeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["probeId"])],
)
data class LibraryProbeEventEntity(
    @PrimaryKey val id: Int,
    val probeId: Int,
    val message: String,
    val createdAt: Long,
)

data class LibraryProbeWithEvents(
    @Embedded val probe: LibraryProbeEntity,
    @Relation(parentColumn = "id", entityColumn = "probeId")
    val events: List<LibraryProbeEventEntity>,
)

class StringListConverter {
    @TypeConverter
    fun fromStored(value: String): List<String> = if (value.isBlank()) emptyList() else value.split('|')

    @TypeConverter
    fun toStored(value: List<String>): String = value.joinToString("|")
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLibraryProbe(probe: LibraryProbeEntity)

    @Update
    suspend fun updateLibraryProbe(probe: LibraryProbeEntity): Int

    @Delete
    suspend fun deleteLibraryProbe(probe: LibraryProbeEntity): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLibraryProbeEvent(event: LibraryProbeEventEntity)

    /** A real write transaction: either both rows are committed or neither is. */
    @Transaction
    suspend fun insertLibraryProbeWithEvent(
        probe: LibraryProbeEntity,
        event: LibraryProbeEventEntity,
    ) {
        insertLibraryProbe(probe)
        insertLibraryProbeEvent(event)
    }

    @Transaction
    @Query("SELECT * FROM library_probe WHERE id = :id")
    suspend fun selectLibraryProbeWithEvents(id: Int): LibraryProbeWithEvents?

    @Query("SELECT * FROM library_probe WHERE id = :id")
    fun observeLibraryProbe(id: Int): Flow<LibraryProbeEntity?>

    @RawQuery
    suspend fun selectLibraryProbesRaw(query: RoomRawQuery): List<LibraryProbeEntity>

    @Query("SELECT COUNT(*) FROM library_probe")
    suspend fun libraryProbeCount(): Int
}

@Database(
    entities = [PostEntity::class, SavedNoteEntity::class, LibraryProbeEntity::class, LibraryProbeEventEntity::class],
    version = 3,
    exportSchema = true,
)
@ConstructedBy(PostsRoomDatabaseConstructor::class)
@TypeConverters(StringListConverter::class)
abstract class PostsRoomDatabase : RoomDatabase() {
    abstract fun postsDao(): PostsDao
}

@Suppress("KotlinNoActualForExpect")
expect object PostsRoomDatabaseConstructor : RoomDatabaseConstructor<PostsRoomDatabase> {
    override fun initialize(): PostsRoomDatabase
}
