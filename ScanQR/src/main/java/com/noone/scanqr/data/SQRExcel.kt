package com.noone.scanqr.data

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Entity
import com.google.gson.annotations.SerializedName
import com.noone.scanqr.utils.SQRConstants
import kotlinx.parcelize.Parcelize

/**
 * Copyright (c) 2023 NoOne. All rights reserved.
 * Created by NoOne. on 15/05/2023.
 */

@Keep
@Parcelize
@Entity(tableName = "Excel", primaryKeys = ["index"])
data class SQRExcel(
    @SerializedName("index")
    var index: Int? = null,

    @SerializedName("label")
    var label: String? = null,

    @SerializedName("isScanned")
    var isScanned: Boolean = false,

    @SerializedName("fileName")
    var fileName: String? = null,

    var viewType: Int = SQRConstants.VIEW_TYPE_EXCEL_ITEM, // type of view holder
) : Parcelable {

    companion object {
        val loadMoreItem = SQRExcel(
            index = -1,
            label = "load more",
            viewType = SQRConstants.VIEW_TYPE_EXCEL_ITEM_LOAD_MORE
        )
    }
}