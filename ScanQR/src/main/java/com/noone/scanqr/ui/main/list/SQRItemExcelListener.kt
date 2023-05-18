package com.noone.scanqr.ui.main.list

/**
 * Copyright (c) 2023 NoOne. All rights reserved.
 * Created by NoOne. on 15/05/2023.
 */

import com.noone.scanqr.data.SQRExcel

interface SQRItemExcelListener {

    fun onItemCLick(position: Int, item: SQRExcel)
    fun onItemLongCLick(position: Int, item: SQRExcel)
}