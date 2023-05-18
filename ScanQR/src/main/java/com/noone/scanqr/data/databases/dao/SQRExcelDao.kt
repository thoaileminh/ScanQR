package com.noone.scanqr.data.databases.dao

/**
 * Copyright (c) 2023 NoOne. All rights reserved.
 * Created by NoOne. on 15/05/2023.
 */

import androidx.room.*
import com.noone.scanqr.data.SQRExcel
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

@Dao
interface SQRExcelDao : SQRBaseDao<SQRExcel> {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSQRExcelData(data: List<SQRExcel>): Completable

    @Query("SELECT * FROM Excel WHERE fileName=:fileName")
    fun getExcelData(fileName: String): Single<List<SQRExcel>>

    @Query("UPDATE Excel SET `isScanned`= 1 WHERE `index`=:index")
    fun updateItemScanned(
        index: String
    ): Single<Int>

    @Query("DELETE FROM Excel")
    fun deleteAll(): Single<Int>
}