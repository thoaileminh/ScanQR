package com.noone.scanqr.ui.info

/**
 * Copyright (c) 2023 NoOne. All rights reserved.
 * Created by NoOne. on 18/05/2023.
 */

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.noone.scanqr.databinding.SqrActivityInfoBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SQRInfoActivity : AppCompatActivity() {

    private val viewModel: SQRInfoViewModel by viewModels()

    private lateinit var viewBinding: SqrActivityInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = SqrActivityInfoBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        setUpUI()
        setUpViewModel()
    }

    private fun setUpUI() {
    }

    private fun setUpViewModel() {
    }
}