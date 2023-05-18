package com.noone.scanqr.ui.info

/**
 * Copyright (c) 2023 NoOne. All rights reserved.
 * Created by NoOne. on 18/05/2023.
 */

import android.app.Application
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SQRInfoViewModel @Inject constructor(
    private val app: Application,
) : ViewModel() {
}