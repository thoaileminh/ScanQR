package com.noone.scanqr.ui.scanqr

/**
 * Copyright (c) 2023 NoOne. All rights reserved.
 * Created by NoOne. on 15/05/2023.
 */

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.budiyev.android.codescanner.ScanMode
import com.noone.scanqr.R
import com.noone.scanqr.databinding.SqrActivityScanQrBinding
import com.noone.scanqr.utils.SQRConstants.SQRSCAN_BUNDLE_DATA
import com.noone.scanqr.utils.SQRUtils.showLog
import com.noone.scanqr.utils.convertToNormalIndex
import com.noone.scanqr.utils.setOnSingleClickListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SQRScanActivity : AppCompatActivity() {

    private val viewModel: SQRCanViewModel by viewModels()

    private lateinit var viewBinding: SqrActivityScanQrBinding

    private lateinit var codeScanner: CodeScanner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = SqrActivityScanQrBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        setUpUI()
        setUpViewModel()
    }

    private fun setUpUI() {
        codeScanner = CodeScanner(this, viewBinding.viewScanner)

        // Parameters (default values)
        codeScanner.camera = CodeScanner.CAMERA_BACK // or CAMERA_FRONT or specific camera id
        codeScanner.formats = CodeScanner.ALL_FORMATS // list of type BarcodeFormat, ex. listOf(BarcodeFormat.QR_CODE)
        codeScanner.autoFocusMode = AutoFocusMode.SAFE // or CONTINUOUS
        codeScanner.scanMode = ScanMode.SINGLE // or CONTINUOUS or PREVIEW
        codeScanner.isAutoFocusEnabled = true // Whether to enable auto focus or not
        codeScanner.isFlashEnabled = false // Whether to enable flash or not

        // Callbacks
        codeScanner.decodeCallback = DecodeCallback { result ->
            lifecycleScope.launch(Dispatchers.IO) {
                showLog("DecodeCallback Scan result: $result")
                val index: String = result.text.convertToNormalIndex()
                sendBackDataToMain(index = index)
            }
        }
        codeScanner.errorCallback = ErrorCallback { throwable -> // or ErrorCallback.SUPPRESS
            lifecycleScope.launch(Dispatchers.IO) {
                showLog("DecodeCallback Scan throwable: $throwable")
                Toast.makeText(this@SQRScanActivity, getString(R.string.scan_qr_error, throwable.message), Toast.LENGTH_LONG).show()
            }
        }

        viewBinding.viewScanner.setOnSingleClickListener {
            codeScanner.startPreview()
        }
    }

    private fun setUpViewModel() {
    }

    override fun onResume() {
        super.onResume()
        codeScanner.startPreview()
    }

    override fun onPause() {
        codeScanner.releaseResources()
        super.onPause()
    }

    private fun sendBackDataToMain(index: String) {
        showLog("sendBackDataToMain index: $index")
        val resultIntent = Intent()
        val bundle = Bundle().apply {
            putString(SQRSCAN_BUNDLE_DATA, index)
        }

        resultIntent.putExtras(bundle)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}