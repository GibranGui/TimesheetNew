package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ShiftEntity::class,
        RitaseEntity::class,
        FuelEntryEntity::class,
        UnitEntity::class,
        LoadingAreaEntity::class,
        UserAccountEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shiftDao(): ShiftDao
    abstract fun ritaseDao(): RitaseDao
    abstract fun fuelDao(): FuelDao
    abstract fun unitDao(): UnitDao
    abstract fun loadingAreaDao(): LoadingAreaDao
    abstract fun userAccountDao(): UserAccountDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Penambahan kolom sinkronisasi (v1 -> v2). Data lama tetap utuh.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shifts ADD COLUMN syncedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE shifts ADD COLUMN deletedAt INTEGER")
                db.execSQL("ALTER TABLE ritase_events ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE ritase_events SET updatedAt = timestamp WHERE updatedAt = 0")
                db.execSQL("ALTER TABLE ritase_events ADD COLUMN syncedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE ritase_events ADD COLUMN deletedAt INTEGER")
                db.execSQL("ALTER TABLE fuel_entries ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE fuel_entries SET updatedAt = timestamp WHERE updatedAt = 0")
                db.execSQL("ALTER TABLE fuel_entries ADD COLUMN syncedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE fuel_entries ADD COLUMN deletedAt INTEGER")
            }
        }

        // Tabel akun + kolom pemilik data online (v2 -> v3). Data lama tetap utuh.
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS user_accounts (
                        id TEXT NOT NULL PRIMARY KEY,
                        fullName TEXT NOT NULL,
                        phone TEXT NOT NULL,
                        username TEXT NOT NULL UNIQUE,
                        passwordHash TEXT NOT NULL,
                        salt TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        lastLoginAt INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL("ALTER TABLE shifts ADD COLUMN ownerId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE ritase_events ADD COLUMN owner_id TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE fuel_entries ADD COLUMN owner_id TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE ritase_events RENAME COLUMN trigger TO trigger_type")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dt_timesheet_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
