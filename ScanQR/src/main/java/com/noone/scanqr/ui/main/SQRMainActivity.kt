package com.noone.scanqr.ui.main

/**
 * Copyright (c) 2023 NoOne. All rights reserved.
 * Created by NoOne. on 15/05/2023.
 */

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.budiyev.android.codescanner.ScanMode
import com.google.android.material.snackbar.Snackbar
import com.hbisoft.pickit.PickiT
import com.hbisoft.pickit.PickiTCallbacks
import com.noone.scanqr.BuildConfig
import com.noone.scanqr.R
import com.noone.scanqr.data.SQRExcel
import com.noone.scanqr.data.databases.SQRDatabases
import com.noone.scanqr.databinding.SqrActivityMainBinding
import com.noone.scanqr.ui.info.SQRInfoActivity
import com.noone.scanqr.ui.main.list.SQRExcelListAdapter
import com.noone.scanqr.ui.main.list.SQRItemExcelListener
import com.noone.scanqr.utils.SQRPermissionUtils
import com.noone.scanqr.utils.SQRUtils
import com.noone.scanqr.utils.SQRUtils.showLog
import com.noone.scanqr.utils.convertToNormalIndex
import com.noone.scanqr.utils.gravityTop
import com.noone.scanqr.utils.hide
import com.noone.scanqr.utils.setOnSingleClickListener
import com.noone.scanqr.utils.show
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SQRMainActivity : AppCompatActivity(), PickiTCallbacks {

    private val viewModel: SQRMainViewModel by viewModels()

    private lateinit var viewBinding: SqrActivityMainBinding

    private var pickit: PickiT? = null
    private lateinit var codeScanner: CodeScanner

    private var adapter: SQRExcelListAdapter? = null

    private var sqrDatabases: SQRDatabases? = null
    private val compositeDisposable = CompositeDisposable()

    private var currentResult: com.google.zxing.Result? = null
    private var currentMessage: String = ""
    private lateinit var currentSnackBar: Snackbar
    private var isShowingScanQR: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = SqrActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        setUpUI()
        setUpSanQR()
        setUpViewModel()
    }

    private fun setUpUI() {
        setUpSnackBar()

        sqrDatabases = SQRDatabases.getInstance(this)
        pickit = PickiT(this, this, this)

        SQRPermissionUtils().requestPermissions(this)
        setupAdapter()
        loadLocalBDIfExist()

        viewBinding.run {
            tvInfo.apply {
                text = getString(R.string.scan_qr_scan_version, BuildConfig.VERSION_NAME)
                setOnSingleClickListener {
                    gotoActivityInfo()
                }
            }

            btnSelectExcelFile.setOnSingleClickListener {
                gotoSelectExcelFile()
            }

            btnScanQR.setOnSingleClickListener {
                gotoScanQRActivity()
            }

            btnCloseScanView.setOnSingleClickListener {
                isShowingScanQR = false
                viewBinding.layoutScannerView.hide()
            }
        }
    }

    private fun setUpViewModel() {
        viewModel.run {
            excelData.observe(this@SQRMainActivity) { data ->
                if (data != null && data.isNotEmpty()) {
                    viewBinding.tvProgress.text = getString(R.string.scan_qr_reading_file_success)
                    adapter?.let { adapter ->
                        adapter.setList(ArrayList(data))
                        showTotalNumbers()
                    }

                    SQRUtils.saveFilePath(context = this@SQRMainActivity, filePath = viewModel.filePath.orEmpty()) {}
                } else {
                    hideTotalNumbers()
                }
            }

            errorData.observe(this@SQRMainActivity) { error ->
                viewBinding.tvProgress.text = getString(R.string.scan_qr_name_error, error)
            }
        }
    }

    private fun showTotalNumbers() {
        viewBinding.run {
            // total items of list
            tvListTotal.visibility = View.VISIBLE
            tvListTotalNumber.visibility = View.VISIBLE
            tvListTotalNumber.text = adapter?.getTotalItems().toString()

            // total items not scan of list
            tvListTotalNotScan.visibility = View.VISIBLE
            tvListTotalNotScanNumber.visibility = View.VISIBLE
            tvListTotalNotScanNumber.text = adapter?.getTotalItemsNotScan().toString()
        }
    }

    private fun hideTotalNumbers() {
        viewBinding.run {
            tvListTotal.text = ""
            tvListTotalNotScan.text = ""
            tvListTotalNumber.text = ""
            tvListTotalNotScanNumber.text = ""
            tvIndexScanned.text = ""
        }
        adapter?.clear()
    }

    private fun loadLocalBDIfExist() {
        viewModel.run {
            filePath = SQRUtils.getFilePathSaved(this@SQRMainActivity)
            showLog("loadLocalBDIfExist filePath: $filePath")
            if (filePath?.isNotEmpty() == true) {
                fileName = SQRUtils.getFileName(filePath = filePath)
                viewBinding.tvNameFileSelected.text = getString(R.string.scan_qr_name_selected_excel_file, fileName)
                viewBinding.tvProgress.text = getString(R.string.scan_qr_reading_file_selected_before)
                viewModel.getExcelData()
            } else {
                viewBinding.tvNameFileSelected.text = getString(R.string.scan_qr_name_select_excel_file)
            }
        }
    }

    private fun gotoSelectExcelFile() {
        if (SQRPermissionUtils().checkPermissionsAtRuntime(this@SQRMainActivity)) {
            selectExcelFile()
        } else {
            SQRPermissionUtils().requestPermissions(this@SQRMainActivity)
        }
    }

    private fun gotoScanQRActivity() {
        if (SQRPermissionUtils().checkPermissionsAtRuntime(this@SQRMainActivity)) {
            isShowingScanQR = true
            viewBinding.layoutScannerView.show()
            codeScanner.startPreview()
        } else {
            SQRPermissionUtils().requestPermissions(this@SQRMainActivity)
        }
    }

    private fun getDataSelectExcelFile(data: Intent?) {
        val uri = data?.data ?: return
        pickit?.getPath(data.data, Build.VERSION.SDK_INT)

        viewModel.fileName = SQRUtils.getFileName(uri = uri, activity = this).orEmpty()
        viewBinding.tvNameFileSelected.text = getString(R.string.scan_qr_name_selected_excel_file, viewModel.fileName)
    }

    private fun selectExcelFile() {
        val chooseFile = Intent(Intent.ACTION_GET_CONTENT)
        chooseFile.addCategory(Intent.CATEGORY_OPENABLE)
        chooseFile.type = "*/*"
        val intent: Intent = Intent.createChooser(chooseFile, getString(R.string.scan_qr_select_excel_file))
        resultLauncherSelectExcelFile.launch(intent)
    }

    override fun PickiTonUriReturned() = Unit
    override fun PickiTonStartListener() = Unit

    override fun PickiTonProgressUpdate(progress: Int) {
        viewBinding.tvProgress.text = getString(R.string.scan_qr_progressing, progress.toString().plus("%"))
    }

    override fun PickiTonCompleteListener(
        path: String,
        wasDriveFile: Boolean,
        wasUnknownProvider: Boolean,
        wasSuccessful: Boolean,
        Reason: String?
    ) {
        showLog("PickiTonCompleteListener wasSuccessful: $wasSuccessful path: $path")

        viewModel.run {
            if (wasSuccessful && path.isNotEmpty()) {
                filePath = path
                readFileSelected()
            } else {
                errorData.postValue("PickiTonCompleteListener Reason: $Reason")
            }
        }
    }

    override fun PickiTonMultipleCompleteListener(paths: ArrayList<String>?, wasSuccessful: Boolean, Reason: String?) {
        showLog("PickiTonMultipleCompleteListener wasSuccessful: $wasSuccessful paths: $paths")
    }

    private fun setupAdapter() {
        if (adapter == null) {
            adapter = SQRExcelListAdapter(
                object : SQRItemExcelListener {
                    override fun onItemCLick(position: Int, item: SQRExcel) {
                        // gotoScanQRActivity()
                    }

                    override fun onItemLongCLick(position: Int, item: SQRExcel) {
                    }
                })
        }

        adapter?.let { adapter ->
            val lm = LinearLayoutManager(this)
            viewBinding.rvData.apply {
                setAdapter(adapter)
                layoutManager = lm
            }
        }
    }

    private fun setUpSanQR() {
        codeScanner = CodeScanner(this, viewBinding.viewScanner)

        // Parameters (default values)
        codeScanner.camera = CodeScanner.CAMERA_BACK // or CAMERA_FRONT or specific camera id
        codeScanner.formats = CodeScanner.ALL_FORMATS // list of type BarcodeFormat, ex. listOf(BarcodeFormat.QR_CODE)
        codeScanner.autoFocusMode = AutoFocusMode.SAFE // or CONTINUOUS
        codeScanner.scanMode = ScanMode.CONTINUOUS // or CONTINUOUS or PREVIEW
        codeScanner.isAutoFocusEnabled = true // Whether to enable auto focus or not
        codeScanner.isFlashEnabled = false // Whether to enable flash or not

        // Callbacks
        codeScanner.decodeCallback = DecodeCallback { result ->
            lifecycleScope.launch(Dispatchers.IO) {
                if (currentResult?.text == result.text && currentSnackBar.isShown) return@launch

                showLog("DecodeCallback Scan result: $result")
                val index: String = result.text.convertToNormalIndex()
                currentResult = result
                checkDataItemScanned(index)
            }
        }

        codeScanner.errorCallback = ErrorCallback { throwable -> // or ErrorCallback.SUPPRESS
            lifecycleScope.launch(Dispatchers.IO) {
                showLog("DecodeCallback Scan throwable: $throwable")
                showStateScanError(throwable.message.orEmpty())
            }
        }

        viewBinding.viewScanner.setOnSingleClickListener {
            codeScanner.startPreview()
        }
    }

    private fun setUpSnackBar() {
        viewBinding.run {
            currentSnackBar = Snackbar.make(viewBinding.root, currentMessage, Snackbar.LENGTH_INDEFINITE).apply {
                gravityTop()
                setAction(getString(R.string.scan_qr_scan_close)) {
                    dismiss()
                }
            }
        }
    }

    private fun checkDataItemScanned(index: String?) {
        lifecycleScope.launch(Dispatchers.Main) {
            adapter?.let { adapter ->
                val position = adapter.findPositionByItemIndex(index)
                val itemUpdate = adapter.findItemByItemIndex(index)

                when {
                    index.isNullOrEmpty() -> {
                        showStateScanError(getString(R.string.scan_qr_scan_error_data))
                    }
                    position < 0 -> {
                        showStateScanError(getString(R.string.scan_qr_scan_not_exist, index))
                    }
                    itemUpdate?.isScanned == true -> {
                        showStateScanError(getString(R.string.scan_qr_scan_is_scanned, index))
                    }
                    else -> {
                        compositeDisposable.add(
                            viewModel.database.excelDao().updateItemScanned(index)
                                .subscribeOn(Schedulers.io())
                                .subscribe({
                                    showStateScanSuccess(position, getString(R.string.scan_qr_scan_success, index))
                                }, { throwable ->
                                    showLog("setItemScanned throwable: $throwable")
                                    showStateScanError(getString(R.string.scan_qr_scan_error, index))
                                })
                        )
                    }
                }
            }
        }
    }

    private val resultLauncherSelectExcelFile = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        showLog("resultLauncherSelectExcelFile result: $result")
        if (result.resultCode == RESULT_OK) {
            getDataSelectExcelFile(result.data)
        }
    }

    private fun showStateScanSuccess(position: Int, message: String) {
        lifecycleScope.launch {
            if (currentMessage == message) return@launch

            adapter?.let { adapter ->
                adapter.setItemScanned(position)
                showTotalNumbers()
            }

            viewBinding.tvIndexScanned.text = currentMessage
            viewBinding.tvIndexScanned.setTextColor(ContextCompat.getColor(this@SQRMainActivity, R.color.sqr_green))

            currentSnackBar.apply {
                setText(message)
                setTextColor(ContextCompat.getColor(this@SQRMainActivity, R.color.sqr_white))
                setBackgroundTint(ContextCompat.getColor(this@SQRMainActivity, R.color.sqr_green))
                show()
            }
        }
    }

    private fun showStateScanError(message: String) {
        lifecycleScope.launch {
            if (currentMessage == message) return@launch

            currentMessage = message
            viewBinding.tvIndexScanned.text = currentMessage
            viewBinding.tvIndexScanned.setTextColor(ContextCompat.getColor(this@SQRMainActivity, R.color.sqr_red))

            currentSnackBar.apply {
                setText(message)
                setTextColor(ContextCompat.getColor(this@SQRMainActivity, R.color.sqr_white))
                setBackgroundTint(ContextCompat.getColor(this@SQRMainActivity, R.color.sqr_red))
                show()
            }
        }
    }

    private fun gotoActivityInfo() {
        val intent = Intent(this, SQRInfoActivity::class.java)
        startActivity(intent)
    }

    override fun onBackPressed() {
        if (viewBinding.layoutScannerView.isVisible || isShowingScanQR) {
            isShowingScanQR = false
            viewBinding.layoutScannerView.hide()
        } else {
            super.onBackPressed()
        }
    }
}