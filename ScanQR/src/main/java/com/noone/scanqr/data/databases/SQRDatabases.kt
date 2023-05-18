package com.noone.scanqr.data.databases

/**
 * Copyright (c) 2023 NoOne. All rights reserved.
 * Created by NoOne. on 15/05/2023.
 */

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.noone.scanqr.data.SQRExcel
import com.noone.scanqr.data.databases.dao.SQRExcelDao
import com.noone.scanqr.utils.SQRConstants

@Database(
    entities = [
        SQRExcel::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class SQRDatabases : RoomDatabase() {

    internal abstract fun excelDao(): SQRExcelDao

    companion object {
        private lateinit var INSTANCE: SQRDatabases

        /* private val MIGRATION_1_2: Migration = object : Migration(1, 2) {
             override fun migrate(database: SupportSQLiteDatabase) {}
         }*/

        @Synchronized
        fun getInstance(context: Context): SQRDatabases {
            if (!Companion::INSTANCE.isInitialized) {
                synchronized(SQRDatabases::class) {
                    INSTANCE = Room
                        .databaseBuilder(
                            context.applicationContext,
                            SQRDatabases::class.java,
                            SQRConstants.SQR_DB_NAME
                        )
                        /*.addMigrations(
                            MIGRATION_1_2,
                        )*/
                        .fallbackToDestructiveMigration()
                        .build()
                }
            }
            return INSTANCE
        }

        fun destroyInstance() {
            INSTANCE.close()
        }
    }
}