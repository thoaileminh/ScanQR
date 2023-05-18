package com.noone.scanqr.utils

/**
 * Copyright (c) 2023 NoOne. All rights reserved.
 * Created by NoOne. on 15/05/2023.
 */

import android.view.View

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
