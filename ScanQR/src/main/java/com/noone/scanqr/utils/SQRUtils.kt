package com.noone.scanqr.utils

/**
 * Copyright (c) 2023 NoOne. All rights reserved.
 * Created by NoOne. on 15/05/2023.
 */

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.noone.scanqr.utils.SQRConstants.SQR_PREF
import com.noone.scanqr.utils.SQRConstants.SQR_PREF_FILE_PATH
import java.io.File

/**
 * Excel Worksheet Utility Methods
 *
 *
 * Created by: Ranit Raj Ganguly on 16/04/21.
 */
object SQRUtils {

    fun showLog(message: String) {
        Log.e("NoOne", message)
    }

    @SuppressLint("Range")
    fun getFileName(uri: Uri?, activity: Activity): String? {
        if (uri == null) return null

        var result: String? = null
        if (uri.scheme == "content") {
            val cursor: Cursor? = activity.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result!!.lastIndexOf('/')
            if (cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        return result
    }

    fun getFileName(filePath: String?): String {
        if (filePath.isNullOrEmpty()) return ""

        val file = File(filePath)
        return file.name
    }

    fun saveFilePath(
        context: Context?,
        filePath: String,
        onCompleteCallback: (isSaved: Boolean) -> Unit,
    ) {
        try {
            context?.getSharedPreferences(SQR_PREF, Context.MODE_PRIVATE)?.edit()?.putString(SQR_PREF_FILE_PATH, filePath)?.apply()
            onCompleteCallback(true)
        } catch (ex: Exception) {
            showLog("saveToPreferenceString ex: $ex")
            onCompleteCallback(false)
        }
    }

    fun getFilePathSaved(
        context: Context?,
    ): String? {
        return try {
            context?.getSharedPreferences(SQR_PREF, Context.MODE_PRIVATE)?.getString(SQR_PREF_FILE_PATH, null)
        } catch (ex: Exception) {
            showLog("saveToPreferenceString ex: $ex")
            null
        }
    }

    fun clearPref(context: Context?) {
        try {
            context?.getSharedPreferences(SQR_PREF, Context.MODE_PRIVATE)?.edit()?.clear()?.apply()
        } catch (ex: Exception) {
            showLog("clearPref ex: $ex")
        }
    }
}
