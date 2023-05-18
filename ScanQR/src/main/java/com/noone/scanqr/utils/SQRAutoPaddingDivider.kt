package com.noone.scanqr.utils

/**
 * Copyright (c) 2023 NoOne. All rights reserved.
 * Created by NoOne. on 15/05/2023.
 */

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.noone.scanqr.R

class SQRAutoPaddingDivider @JvmOverloads constructor(
    context: Context,
    @ColorRes color: Int = R.color.sqr_gray,
    @DimenRes height: Int = R.dimen.dp_1,
    @DimenRes marginLeft: Int = R.dimen.dp_0,
) : RecyclerView.ItemDecoration() {

    private var mDivider: Drawable? =
        if (color != 0) ColorDrawable(ContextCompat.getColor(context, color))
        else null
    private var mHeight: Int = context.resources.getDimension(height).toInt()
    private var marginLeft: Int = context.resources.getDimension(marginLeft).toInt()

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        mDivider?.let {
            val childCount = parent.childCount
            var dividerLeft: Int
            var dividerRight: Int
            for (i in 0..childCount - 2) {
                val child = parent.getChildAt(i)
                dividerLeft = if (marginLeft > 0) marginLeft else child.paddingLeft
                dividerRight = child.width - child.paddingRight

                val params = child.layoutParams as RecyclerView.LayoutParams

                val dividerTop = child.bottom + params.bottomMargin
                val dividerBottom = dividerTop + mHeight

                it.setBounds(dividerLeft, dividerTop, dividerRight, dividerBottom)
                it.draw(canvas)
            }
        }
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        with(outRect) {
            if (parent.getChildAdapterPosition(view) != 0) {
                top = mHeight
            }
        }
    }
}