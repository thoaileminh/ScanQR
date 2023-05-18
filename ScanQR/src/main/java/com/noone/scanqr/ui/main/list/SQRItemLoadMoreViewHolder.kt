package com.noone.scanqr.ui.main.list

/**
 * Copyright (c) 2023 NoOne. All rights reserved.
 * Created by NoOne. on 15/05/2023.
 */

import androidx.recyclerview.widget.RecyclerView
import com.noone.scanqr.databinding.SqrItemProgressLoadMoreBinding

class SQRItemLoadMoreViewHolder(
    private val binding: SqrItemProgressLoadMoreBinding,
) : RecyclerView.ViewHolder(binding.root) {

    private var viewBinding = SqrItemProgressLoadMoreBinding.bind(itemView)

    fun bind(position: Int) {
    }
}