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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.hbisoft.pickit.PickiT
import com.hbisoft.pickit.PickiTCallbacks
import com.noone.scanqr.R
import com.noone.scanqr.data.SQRExcel
import com.noone.scanqr.data.databases.SQRDatabases
import com.noone.scanqr.databinding.SqrActivityMainBinding
import com.noone.scanqr.ui.info.SQRInfoActivity
import com.noone.scanqr.ui.main.list.SQRExcelListAdapter
import com.noone.scanqr.ui.main.list.SQRItemExcelListener
import com.noone.scanqr.ui.scanqr.SQRScanActivity
import com.noone.scanqr.utils.SQRConstants.SQRSCAN_BUNDLE_DATA
import com.noone.scanqr.utils.SQRPermissionUtils
import com.noone.scanqr.utils.SQRUtils
import com.noone.scanqr.utils.SQRUtils.showLog
import com.noone.scanqr.utils.gravityTop
import com.noone.scanqr.utils.setOnSingleClickListener
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

@AndroidEntryPoint
class SQRMainActivity : AppCompatActivity(), PickiTCallbacks {

    private val viewModel: SQRMainViewModel by viewModels()

    private lateinit var viewBinding: SqrActivityMainBinding

    private var pickit: PickiT? = null

    private var adapter: SQRExcelListAdapter? = null

    private var sqrDatabases: SQRDatabases? = null
    private val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = SqrActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        setUpUI()
        setUpViewModel()
    }

    private fun setUpUI() {
        sqrDatabases = SQRDatabases.getInstance(this)
        pickit = PickiT(this, this, this)

        SQRPermissionUtils().requestPermissions(this)
        setupAdapter()
        loadLocalBDIfExist()

        viewBinding.run {
            btnSelectExcelFile.setOnSingleClickListener {
                gotoSelectExcelFile()
            }

            btnScanQR.setOnSingleClickListener {
                gotoScanQRActivity()
            }

            tvInfo.apply {
                text = getString(R.string.scan_qr_scan_version)
                setOnSingleClickListener {
                    gotoActivityInfo()
                }
            }
        }
    }

    private fun setUpViewModel() {
        viewModel.run {
            excelData.observe(this@SQRMainActivity) { data ->
                if (data != null && data.isNotEmpty()) {
                    viewBinding.tvProgress.text = getString(R.string.scan_qr_reading_file_success)
                    viewBinding.tvListNotScan.visibility = View.VISIBLE
                    viewBinding.tvListNotScan.text = getString(R.string.scan_qr_list_not_scan, data.size.toString())
                    adapter?.setList(ArrayList(data))
                    SQRUtils.saveFilePath(context = this@SQRMainActivity, filePath = viewModel.filePath.orEmpty()) {}
                } else {
                    viewBinding.tvListNotScan.text = ""
                    viewBinding.tvIndexScanned.text = ""
                    adapter?.clear()
                }
            }

            errorData.observe(this@SQRMainActivity) { error ->
                viewBinding.tvProgress.text = getString(R.string.scan_qr_name_error, error)
            }
        }
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
            val intent = Intent(this@SQRMainActivity, SQRScanActivity::class.java)
            resultLauncherScanQR.launch(intent)
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

    private fun getDataScanQR(data: Intent?) {
        data.let {
            val index = data?.getStringExtra(SQRSCAN_BUNDLE_DATA)
            checkDataItemScanned(index)
        }
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
                                    showStateScanSuccess(position, index)
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

    private val resultLauncherScanQR = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        showLog("resultLauncherScanQR result: $result")
        if (result.resultCode == RESULT_OK) {
            getDataScanQR(result.data)
        }
    }

    private fun showStateScanSuccess(position: Int, index: String) {
        lifecycleScope.launch {
            val message = getString(R.string.scan_qr_scan_success, index)
            adapter?.setItemScanned(position)
            viewBinding.tvIndexScanned.text = message
            viewBinding.tvIndexScanned.setTextColor(ContextCompat.getColor(this@SQRMainActivity, R.color.sqr_green))

            val snack = Snackbar.make(viewBinding.root, message, Snackbar.LENGTH_SHORT)
            snack.setTextColor(ContextCompat.getColor(this@SQRMainActivity, R.color.sqr_white))
            snack.setBackgroundTint(ContextCompat.getColor(this@SQRMainActivity, R.color.sqr_green))
            snack.gravityTop()
            snack.setAction(getString(R.string.scan_qr_scan_close)) {
                snack.dismiss()
            }
            snack.show()
        }
    }


    private fun showStateScanError(message: String) {
        lifecycleScope.launch {
            viewBinding.tvIndexScanned.text = message
            viewBinding.tvIndexScanned.setTextColor(ContextCompat.getColor(this@SQRMainActivity, R.color.sqr_red))

            val snack = Snackbar.make(viewBinding.root, message, Snackbar.LENGTH_SHORT)
            snack.setTextColor(ContextCompat.getColor(this@SQRMainActivity, R.color.sqr_white))
            snack.setBackgroundTint(ContextCompat.getColor(this@SQRMainActivity, R.color.sqr_red))
            snack.gravityTop()
            snack.setAction(getString(R.string.scan_qr_scan_close)) {
                snack.dismiss()
            }
            snack.show()
        }
    }

    private fun gotoActivityInfo() {
        val intent = Intent(this, SQRInfoActivity::class.java)
        startActivity(intent)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        exitProcess(0)
    }
}