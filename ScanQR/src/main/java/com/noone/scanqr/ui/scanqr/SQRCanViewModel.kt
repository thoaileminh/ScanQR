package com.noone.scanqr.ui.scanqr

/**
 * Copyright (c) 2023 NoOne. All rights reserved.
 * Created by NoOne. on 15/05/2023.
 */

import android.app.Application
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SQRCanViewModel @Inject constructor(
    private val app: Application
) : ViewModel() {

}