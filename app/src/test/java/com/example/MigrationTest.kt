package com.example

import android.content.Context
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {

    @Test
    fun testMigrationFromV4ToV5Succeeds() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("dt_timesheet_database")

        // 1. Create a database at version 4 without driverName
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                .name("dt_timesheet_database")
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(4) {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL("""
                            CREATE TABLE IF NOT EXISTS `shifts` (
                                `id` TEXT NOT NULL,
                                `unitId` TEXT NOT NULL,
                                `date` TEXT NOT NULL,
                                `hmAwal` REAL NOT NULL,
                                `hmAkhir` REAL,
                                `kmAwal` REAL NOT NULL,
                                `kmAkhir` REAL,
                                `totalHm` REAL NOT NULL,
                                `totalKm` REAL NOT NULL,
                                `totalRitase` INTEGER NOT NULL,
                                `totalFuel` REAL NOT NULL,
                                `status` TEXT NOT NULL,
                                `ownerId` TEXT NOT NULL,
                                `createdAt` INTEGER NOT NULL,
                                `updatedAt` INTEGER NOT NULL,
                                `syncedAt` INTEGER NOT NULL,
                                `deletedAt` INTEGER,
                                PRIMARY KEY(`id`)
                            )
                        """.trimIndent())
                        db.execSQL("""
                            CREATE TABLE IF NOT EXISTS `ritase_events` (
                                `id` TEXT NOT NULL,
                                `shiftId` TEXT NOT NULL,
                                `unitId` TEXT NOT NULL,
                                `timestamp` INTEGER NOT NULL,
                                `timeFormatted` TEXT NOT NULL,
                                `latitude` REAL NOT NULL,
                                `longitude` REAL NOT NULL,
                                `accuracy` REAL NOT NULL,
                                `trigger_type` TEXT NOT NULL,
                                `status` TEXT NOT NULL,
                                `updatedAt` INTEGER NOT NULL,
                                `syncedAt` INTEGER NOT NULL,
                                `deletedAt` INTEGER,
                                PRIMARY KEY(`id`)
                            )
                        """.trimIndent())
                        db.execSQL("""
                            CREATE TABLE IF NOT EXISTS `fuel_entries` (
                                `id` TEXT NOT NULL,
                                `shiftId` TEXT NOT NULL,
                                `unitId` TEXT NOT NULL,
                                `timestamp` INTEGER NOT NULL,
                                `timeFormatted` TEXT NOT NULL,
                                `hmIsiFuel` REAL NOT NULL,
                                `liter` REAL NOT NULL,
                                `updatedAt` INTEGER NOT NULL,
                                `syncedAt` INTEGER NOT NULL,
                                `deletedAt` INTEGER,
                                PRIMARY KEY(`id`)
                            )
                        """.trimIndent())
                        db.execSQL("""
                            CREATE TABLE IF NOT EXISTS `units` (
                                `cn` TEXT NOT NULL,
                                `displayName` TEXT NOT NULL,
                                `lastUsedAt` INTEGER NOT NULL,
                                PRIMARY KEY(`cn`)
                            )
                        """.trimIndent())
                        db.execSQL("""
                            CREATE TABLE IF NOT EXISTS `loading_areas` (
                                `id` TEXT NOT NULL,
                                `name` TEXT NOT NULL,
                                `latitude` REAL NOT NULL,
                                `longitude` REAL NOT NULL,
                                `radiusMeters` REAL NOT NULL,
                                `enabled` INTEGER NOT NULL,
                                PRIMARY KEY(`id`)
                            )
                        """.trimIndent())
                        db.execSQL("""
                            CREATE TABLE IF NOT EXISTS `user_accounts` (
                                `id` TEXT NOT NULL,
                                `fullName` TEXT NOT NULL,
                                `phone` TEXT NOT NULL,
                                `username` TEXT NOT NULL,
                                `passwordHash` TEXT NOT NULL,
                                `salt` TEXT NOT NULL,
                                `createdAt` INTEGER NOT NULL,
                                `lastLoginAt` INTEGER NOT NULL,
                                PRIMARY KEY(`id`)
                            )
                        """.trimIndent())
                        db.execSQL("""
                            CREATE TABLE IF NOT EXISTS `rosters` (
                                `id` TEXT NOT NULL,
                                `driverName` TEXT NOT NULL,
                                `vehicleId` TEXT NOT NULL,
                                `shiftsCommaSeparated` TEXT NOT NULL,
                                `updatedAt` INTEGER NOT NULL,
                                PRIMARY KEY(`id`)
                            )
                        """.trimIndent())
                    }
                    override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )
        val writableDb = helper.writableDatabase
        writableDb.close()

        // 2. Open via AppDatabase v5 with MIGRATION_4_5
        val appDb = AppDatabase.getInstance(context)
        val list = runBlocking { appDb.shiftDao().getAllCompletedShiftsDirect() }
        assertNotNull(list)
    }
}
