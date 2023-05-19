package com.noone.scanqr.ui.main.list

/**
 * Copyright (c) 2023 NoOne. All rights reserved.
 * Created by NoOne. on 15/05/2023.
 */

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.noone.scanqr.data.SQRExcel
import com.noone.scanqr.data.SQRExcel.Companion.loadMoreItem
import com.noone.scanqr.databinding.SqrItemExcelBinding
import com.noone.scanqr.databinding.SqrItemProgressLoadMoreBinding
import com.noone.scanqr.utils.SQRConstants.VIEW_TYPE_EXCEL_ITEM
import com.noone.scanqr.utils.SQRUtils.showLog
import java.util.Locale

class SQRExcelListAdapter(
    private val mListener: SQRItemExcelListener,
) : ListAdapter<SQRExcel, RecyclerView.ViewHolder>(callback) {

    private var listData: ArrayList<SQRExcel> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_EXCEL_ITEM -> SQRItemExcelViewHolder(
                SqrItemExcelBinding.inflate(inflater, parent, false),
                mListener
            )

            // show progress load more later
            else -> SQRItemLoadMoreViewHolder(
                SqrItemProgressLoadMoreBinding.inflate(inflater, parent, false)
            )
        }
    }

    override fun getItemCount(): Int = listData.size

    override fun getItemViewType(position: Int): Int {
        return listData[position].viewType
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SQRItemExcelViewHolder -> {
                val item = listData[position]
                holder.bind(position, item)
            }

            is SQRItemLoadMoreViewHolder -> {
                holder.bind(position)
            }

            else -> Unit
        }
    }

    fun setList(newData: ArrayList<SQRExcel>) {
        try {
            listData.addAll(newData)
            notifyDataSetChanged()
        } catch (ex: Exception) {
            showLog("adapter setList ex: $ex")
        }
    }

    fun getList(): ArrayList<SQRExcel> = listData

    fun findPositionByItemIndex(index: String?): Int {
        if (index.isNullOrEmpty()) return -1
        return try {
            getList().indexOfFirst { item ->
                index.lowercase(Locale.ROOT) == item.index.lowercase(Locale.ROOT)
            }
        } catch (ex: Exception) {
            -1
        }
    }

    fun findItemByItemIndex(index: String?): SQRExcel? {
        if (index.isNullOrEmpty()) return null

        return try {
            getList().find { item ->
                (index.equals(item.index, false))
            }
        } catch (ex: Exception) {
            null
        }
    }

    fun setItemScanned(position: Int) {
        getList()[position].isScanned = true
        notifyItemChanged(position)
    }

    fun removeExcelItem(position: Int) {
        getList().removeAt(position)
        notifyItemRemoved(position)
    }

    fun getTotalItems(): Int = getList().filter { it.viewType == VIEW_TYPE_EXCEL_ITEM }.size
    fun getTotalItemsNotScan(): Int = getList().filter { !it.isScanned }.size

    fun addLoadMoreItem() {
        hideLoadMoreItem()
        if (listData.contains(loadMoreItem).not()) {
            listData.add(loadMoreItem)
            notifyDataSetChanged()
        }
    }

    fun hideLoadMoreItem() {
        listData.remove(loadMoreItem)
        notifyDataSetChanged()
    }

    fun clear() {
        listData = arrayListOf()
        notifyDataSetChanged()
    }

    companion object {

        private val callback = object : DiffUtil.ItemCallback<SQRExcel>() {
            override fun areItemsTheSame(oldItem: SQRExcel, newItem: SQRExcel): Boolean {
                return oldItem.index == newItem.index &&
                        oldItem.label == newItem.label &&
                        oldItem.isScanned == newItem.isScanned

            }

            override fun areContentsTheSame(oldItem: SQRExcel, newItem: SQRExcel): Boolean {
                return oldItem == newItem
            }
        }
    }
}