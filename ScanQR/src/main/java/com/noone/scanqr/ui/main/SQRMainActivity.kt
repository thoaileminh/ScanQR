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
import com.noone.scanqr.ui.main.list.SQRExcelListAdapter
import com.noone.scanqr.ui.main.list.SQRItemExcelListener
import com.noone.scanqr.ui.scanqr.SQRScanActivity
import com.noone.scanqr.utils.SQRConstants.SQRSCAN_BUNDLE_DATA
import com.noone.scanqr.utils.SQRPermissionUtils
import com.noone.scanqr.utils.SQRUtils
import com.noone.scanqr.utils.SQRUtils.showLog
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
    private val version = 1.0
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

            tvVersion.text = getString(R.string.scan_qr_scan_version, version.toString())
        }
    }

    private fun setUpViewModel() {
        viewModel.run {
            excelData.observe(this@SQRMainActivity) { data ->
                if (data != null && data.isNotEmpty()) {
                    viewBinding.tvProgress.text = getString(R.string.scan_qr_reading_file_success)
                    viewBinding.tvIndexScanned.text = ""
                    viewBinding.tvListNotScan.visibility = View.VISIBLE
                    viewBinding.tvListNotScan.text = getString(R.string.scan_qr_list_not_scan, data.size.toString())
                    adapter?.setList(ArrayList(data))
                } else {
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
            val excelResponse = data?.getParcelableExtra(SQRSCAN_BUNDLE_DATA) as SQRExcel?
            viewBinding.tvIndexScanned.text = getString(R.string.scan_qr_index_scanned, excelResponse?.index.toString())
            if (excelResponse != null) {
                setItemScanned(excelResponse)
            } else {
                viewBinding.tvIndexScanned.setTextColor(ContextCompat.getColor(this, R.color.sqr_red))
                showSnackbarError(getString(R.string.scan_qr_scan_error_data))
            }
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
                SQRUtils.saveFilePath(context = this@SQRMainActivity, filePath = path) {}
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

    private fun setItemScanned(item: SQRExcel) {
        showLog("setItemScanned item: $item")
        val index = item.index ?: -1

        compositeDisposable.add(
            viewModel.database.excelDao().deleteItemScanned(index)
                .subscribeOn(Schedulers.io())
                .subscribe({
                    lifecycleScope.launch(Dispatchers.Main) {
                        adapter?.let { adapter ->
                            val position = adapter.findPositionByItemIndex(item.index)
                            val itemUpdate = adapter.findItemByItemIndex(item.index)

                            when {
                                position == -1 -> {
                                    viewBinding.tvIndexScanned.setTextColor(ContextCompat.getColor(this@SQRMainActivity, R.color.sqr_yellow))
                                    showSnackbarWarning(getString(R.string.scan_qr_scan_not_exist, item.index.toString()))
                                }
                                itemUpdate?.isScanned == true -> {
                                    viewBinding.tvIndexScanned.setTextColor(ContextCompat.getColor(this@SQRMainActivity, R.color.sqr_yellow))
                                    showSnackbarWarning(getString(R.string.scan_qr_scan_is_scanned, item.index.toString()))
                                }
                                else -> {
                                    adapter.setItemScanned(position)
                                    viewBinding.tvIndexScanned.setTextColor(ContextCompat.getColor(this@SQRMainActivity, R.color.sqr_green))
                                    showSnackbarSuccess(getString(R.string.scan_qr_scan_success, item.index.toString()))
                                }
                            }
                        }
                    }
                }, { throwable ->
                    showLog("setItemScanned throwable: $throwable")
                    viewBinding.tvIndexScanned.setTextColor(ContextCompat.getColor(this, R.color.sqr_red))
                    showSnackbarError(getString(R.string.scan_qr_scan_error, item.index.toString()))
                })
        )
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

    private fun showSnackbarSuccess(message: String) {
        lifecycleScope.launch {
            val snack = Snackbar.make(viewBinding.root, message, Snackbar.LENGTH_INDEFINITE)
            snack.setTextColor(ContextCompat.getColor(this@SQRMainActivity, R.color.sqr_white))
            snack.setBackgroundTint(ContextCompat.getColor(this@SQRMainActivity, R.color.sqr_green))
            snack.setAction(getString(R.string.scan_qr_scan_close)) {
                snack.dismiss()
            }
            snack.show()
        }
    }

    private fun showSnackbarWarning(message: String) {
        lifecycleScope.launch {
            val snack = Snackbar.make(viewBinding.root, message, Snackbar.LENGTH_INDEFINITE)
            snack.setTextColor(ContextCompat.getColor(this@SQRMainActivity, R.color.sqr_black))
            snack.setBackgroundTint(ContextCompat.getColor(this@SQRMainActivity, R.color.sqr_yellow))
            snack.setAction(getString(R.string.scan_qr_scan_close)) {
                snack.dismiss()
            }
            snack.show()
        }
    }

    private fun showSnackbarError(message: String) {
        lifecycleScope.launch {
            val snack = Snackbar.make(viewBinding.root, message, Snackbar.LENGTH_INDEFINITE)
            snack.setTextColor(ContextCompat.getColor(this@SQRMainActivity, R.color.sqr_white))
            snack.setBackgroundTint(ContextCompat.getColor(this@SQRMainActivity, R.color.sqr_red))
            snack.setAction(getString(R.string.scan_qr_scan_close)) {
                snack.dismiss()
            }
            snack.show()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        exitProcess(0)
    }
}