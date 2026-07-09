package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [QuranCard::class], version = 3, exportSchema = false)
abstract class QuranDatabase : RoomDatabase() {
    abstract fun quranCardDao(): QuranCardDao

    companion object {
        @Volatile
        private var INSTANCE: QuranDatabase? = null

        fun getDatabase(context: Context): QuranDatabase {
            val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
                override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) { migrateTable(db) }
            }
            val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
                override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) { migrateTable(db) }
            }
            val MIGRATION_1_3 = object : androidx.room.migration.Migration(1, 3) {
                override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) { migrateTable(db) }
            }
            
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    QuranDatabase::class.java,
                    "quran_launcher_db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_1_3).build()
                INSTANCE = instance
                instance
            }
        }
        
        private fun migrateTable(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `quran_cards_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `clipboardText` TEXT NOT NULL, `imageUri` TEXT, `presetResName` TEXT, `reciterIdentifier` TEXT, `notificationTriggerWord` TEXT, `sortOrder` INTEGER NOT NULL DEFAULT 0, `timestamp` INTEGER NOT NULL DEFAULT 0)")
            
            val cursor = db.query("PRAGMA table_info(quran_cards)")
            val columns = mutableListOf<String>()
            while (cursor.moveToNext()) {
                columns.add(cursor.getString(1))
            }
            cursor.close()
            
            val validCols = setOf("id", "title", "clipboardText", "imageUri", "presetResName", "reciterIdentifier", "notificationTriggerWord", "sortOrder", "timestamp")
            val commonColumns = columns.filter { validCols.contains(it) }.joinToString(", ")
            
            if (commonColumns.isNotEmpty()) {
                // Verify if quran_cards exists before attempting to copy
                val tableExistsCursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='quran_cards'")
                val tableExists = tableExistsCursor.count > 0
                tableExistsCursor.close()
                
                if (tableExists) {
                    db.execSQL("INSERT INTO `quran_cards_new` ($commonColumns) SELECT $commonColumns FROM `quran_cards`")
                    
                    // Optional: Verify count matches
                    val oldCountCursor = db.query("SELECT COUNT(*) FROM quran_cards")
                    oldCountCursor.moveToFirst()
                    val oldCount = oldCountCursor.getInt(0)
                    oldCountCursor.close()
                    
                    val newCountCursor = db.query("SELECT COUNT(*) FROM quran_cards_new")
                    newCountCursor.moveToFirst()
                    val newCount = newCountCursor.getInt(0)
                    newCountCursor.close()
                    
                    if (oldCount != newCount) {
                        throw IllegalStateException("Migration failed: Count mismatch (Expected $oldCount, got $newCount)")
                    }
                    
                    db.execSQL("DROP TABLE `quran_cards`")
                }
            }
            
            db.execSQL("ALTER TABLE `quran_cards_new` RENAME TO `quran_cards`")
        }
    }
}
