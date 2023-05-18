package com.noone.scanqr.ui.main.list

/**
 * Copyright (c) 2023 NoOne. All rights reserved.
 * Created by NoOne. on 15/05/2023.
 */

import androidx.recyclerview.widget.RecyclerView
import com.noone.scanqr.R
import com.noone.scanqr.data.SQRExcel
import com.noone.scanqr.databinding.SqrItemExcelBinding
import com.noone.scanqr.utils.hide
import com.noone.scanqr.utils.setOnSingleClickListener
import com.noone.scanqr.utils.show

class SQRItemExcelViewHolder(
    binding: SqrItemExcelBinding,
    private val listener: SQRItemExcelListener,
) : RecyclerView.ViewHolder(binding.root) {

    private var viewBinding = SqrItemExcelBinding.bind(itemView)
    private val context = itemView.context
    private val itemContent = viewBinding.itemContent
    private val tvItemIndex = viewBinding.tvItemIndex
    private val tvItemLabel = viewBinding.tvItemLabel

    fun bind(position: Int, item: SQRExcel) {
        itemContent.apply {
            if (item.isScanned) {
                this.hide()
            } else {
                tvItemIndex.text = context.getString(R.string.scan_qr_item_index, item.index)
                tvItemLabel.text = context.getString(R.string.scan_qr_item_label, item.label.orEmpty())
                show()
                setOnSingleClickListener {
                    listener.onItemCLick(position = position, item = item)
                }
            }
        }
    }
}