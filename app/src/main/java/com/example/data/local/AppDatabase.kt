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
        UserAccountEntity::class,
        RosterEntity::class,
        DriverTemplateEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shiftDao(): ShiftDao
    abstract fun ritaseDao(): RitaseDao
    abstract fun fuelDao(): FuelDao
    abstract fun unitDao(): UnitDao
    abstract fun loadingAreaDao(): LoadingAreaDao
    abstract fun userAccountDao(): UserAccountDao
    abstract fun rosterDao(): RosterDao
    abstract fun driverTemplateDao(): DriverTemplateDao

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

        // Tabel jadwal kerja driver dari Google Sheets (v3 -> v4). Data lama tetap utuh.
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS rosters (
                        id TEXT NOT NULL PRIMARY KEY,
                        driverName TEXT NOT NULL,
                        vehicleId TEXT NOT NULL,
                        shiftsCommaSeparated TEXT NOT NULL,
                        updatedAt INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        // Tabel daftar nama driver (template saran saat membuat akun) (v4 -> v5). Data lama tetap utuh.
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS driver_templates (
                        name TEXT NOT NULL PRIMARY KEY,
                        lastSeenAt INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dt_timesheet_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
