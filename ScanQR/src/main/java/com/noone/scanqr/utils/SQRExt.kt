package com.noone.scanqr.utils

/**
 * Copyright (c) 2023 NoOne. All rights reserved.
 * Created by NoOne. on 15/05/2023.
 */

import android.content.Context
import android.content.Intent
import android.content.Intent.CATEGORY_DEFAULT
import android.content.Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.Intent.FLAG_ACTIVITY_NO_HISTORY
import android.net.Uri
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import com.google.android.material.snackbar.Snackbar
import com.noone.scanqr.utils.SQRUtils.showLog
import java.util.Locale

/**
 * Prevent duplicate click
 */
fun View.setOnSingleClickListener(action: (View) -> Unit) {
    setOnClickListener { view ->
        view.isClickable = false
        action(view)
        view.postDelayed(
            {
                view.isClickable = true
            },
            300L
        )
    }
}

fun View?.makeVisibility(isShow: Boolean) {
    this?.visibility = if (isShow) View.VISIBLE else View.GONE
}

fun View.hide() {
    visibility = View.GONE
}

fun View.show() {
    visibility = View.VISIBLE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun Snackbar.gravityTop() {
    this.view.layoutParams = (this.view.layoutParams as FrameLayout.LayoutParams).apply {
        gravity = Gravity.TOP
    }
}

private fun removeUTF8BOM(str: String): String {
    if (str.isEmpty()) return ""

    return if (str.startsWith(SQRConstants.UTF8_BOM)) {
        str.substring(1)
    } else {
        str
    }
}

fun String.convertToNormalIndex(): String {
    val index: String
    val textRemovedBom = if (this.isNotEmpty()) removeUTF8BOM(this) else ""

    index = try {
        if (textRemovedBom.trim().lowercase(Locale.ROOT).contains(".")) {
            textRemovedBom.trim().lowercase(Locale.ROOT).substring(0, textRemovedBom.indexOf(".")).replace("\\..*", "")
        } else {
            textRemovedBom.trim().lowercase(Locale.ROOT).replace("\\..*", "")
        }
    } catch (ex: Exception) {
        showLog("convertToNormalIndex ex: $ex")
        ""
    }

    return index
}

private fun characterCompare(lhs: String, rhs: String): Int {
    // return -1 is the same
    var i = 0
    while (i < lhs.length && i < rhs.length) {
        val lchar = lhs[i]
        val rchar = rhs[i]

        if (lchar != rchar) {
            return i // I set a breakpoint here to take a look at the values
        }
        i++
    }
    return if (i < lhs.length || i < rhs.length) i else -1
}

fun Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    with(intent) {
        data = Uri.fromParts("package", packageName, null)
        addCategory(CATEGORY_DEFAULT)
        addFlags(FLAG_ACTIVITY_NEW_TASK)
        addFlags(FLAG_ACTIVITY_NO_HISTORY)
        addFlags(FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
    }

    startActivity(intent)
}