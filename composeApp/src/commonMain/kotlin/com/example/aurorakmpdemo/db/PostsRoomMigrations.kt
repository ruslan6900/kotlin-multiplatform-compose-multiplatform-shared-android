package com.example.aurorakmpdemo.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Сохраняет имеющиеся тестовые посты и заметки при добавлении сущностей проверки библиотек. */
val postsMigration2To3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        // В v3 к существующей таблице posts добавлен этот индекс; Room строго его проверяет.
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_posts_userId ON posts(userId)")
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS library_probe (id INTEGER NOT NULL, title TEXT NOT NULL, stage TEXT NOT NULL, tags TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))",
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS library_probe_event (id INTEGER NOT NULL, probeId INTEGER NOT NULL, message TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id), FOREIGN KEY(probeId) REFERENCES library_probe(id) ON UPDATE NO ACTION ON DELETE CASCADE)",
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_library_probe_event_probeId ON library_probe_event(probeId)")
    }
}
