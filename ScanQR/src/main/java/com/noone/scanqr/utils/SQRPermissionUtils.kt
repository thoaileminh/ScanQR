package com.noone.scanqr.utils

/**
 * Copyright (c) 2023 NoOne. All rights reserved.
 * Created by NoOne. on 15/05/2023.
 */

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

open class SQRPermissionUtils {
    private val arrPermissions = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA,
    )

    fun requestPermissions(activity: Activity) {
        ActivityCompat.requestPermissions(activity, arrPermissions, SQRConstants.REQUEST_PERMISSION_ALL)
    }

    fun checkPermissionsAtRuntime(activity: Activity): Boolean {
        // SQRUtils.showLog("checkPermissionsAtRuntime SDK_INT: ${Build.VERSION.SDK_INT}")
        val readStorage: Int = ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
        // val write: Int = ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val camera: Int = ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)

        return readStorage == PackageManager.PERMISSION_GRANTED
                // && write == PackageManager.PERMISSION_GRANTED
                && camera == PackageManager.PERMISSION_GRANTED
    }
}