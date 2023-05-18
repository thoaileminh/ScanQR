package com.noone.scanqr.ui.main

/**
 * Copyright (c) 2023 NoOne. All rights reserved.
 * Created by NoOne. on 15/05/2023.
 */

import android.app.Application
import android.content.Context
import android.os.Environment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noone.scanqr.R
import com.noone.scanqr.data.SQRExcel
import com.noone.scanqr.data.databases.SQRDatabases
import com.noone.scanqr.utils.SQRUtils.showLog
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFCell
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class SQRMainViewModel @Inject constructor(
    private val app: Application,
) : ViewModel() {

    internal val database = SQRDatabases.getInstance(app.applicationContext)
    internal val excelData: MutableLiveData<List<SQRExcel>> = MutableLiveData()
    private val compositeDisposable = CompositeDisposable()
    val errorData: MutableLiveData<String> = MutableLiveData()

    private var cell: Cell? = null
    private var sheet: Sheet? = null
    private var workbook: Workbook? = null

    internal var fileName: String = "NoOne.xlsx"
    internal var filePath: String? = null

    private val testPath = "/storage/self/primary/Download/testexcel.xlsx"

    /**
     * Export Data into Excel Workbook
     *
     * @param context  - Pass the application context
     * @param fileName - Pass the desired fileName for the output excel Workbook
     * @param dataList - Contains the actual data to be displayed in excel
     */
    fun exportDataIntoWorkbook(
        context: Context,
        dataList: List<SQRExcel>
    ): Boolean {

        // Check if available and not read only
        if (!isExternalStorageAvailable() || isExternalStorageReadOnly()) {
            showLog("Storage not available or read only")
            return false
        }

        // Creating a New HSSF Workbook (.xls format)
        workbook = HSSFWorkbook()
        // Creating a New Sheet and Setting width for each column
        sheet = workbook?.createSheet(fileName)
        sheet?.setColumnWidth(0, 15 * 400)
        sheet?.setColumnWidth(1, 15 * 400)
        fillDataIntoExcel(dataList)
        return storeExcelInStorage(context)
    }

    /**
     * Checks if Storage is READ-ONLY
     *
     * @return boolean
     */
    private fun isExternalStorageReadOnly(): Boolean {
        val externalStorageState = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED_READ_ONLY == externalStorageState
    }

    /**
     * Checks if Storage is Available
     *
     * @return boolean
     */
    private fun isExternalStorageAvailable(): Boolean {
        val externalStorageState = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == externalStorageState
    }

    /**
     * Fills Data into Excel Sheet
     *
     *
     * NOTE: Set row index as i+1 since 0th index belongs to header row
     *
     * @param dataList - List containing data to be filled into excel
     */
    private fun fillDataIntoExcel(dataList: List<SQRExcel>) {
        for (i in dataList.indices) {
            // Create a New Row for every new entry in list
            val rowData: Row? = sheet?.createRow(i + 1)

            // Create Cells for each row
            cell = rowData?.createCell(0)
            cell?.apply {
                setCellValue(dataList[i].index.toString())
            }

            cell = rowData?.createCell(12)
            cell?.apply {
                setCellValue(dataList[i].label)
            }
        }
    }

    /**
     * Store Excel Workbook in external storage
     *
     * @param context  - application context
     * @param fileName - name of workbook which will be stored in device
     * @return boolean - returns state whether workbook is written into storage or not
     */
    private fun storeExcelInStorage(context: Context): Boolean {
        var isSuccess: Boolean
        val file = File(context.getExternalFilesDir(null), fileName)
        var fileOutputStream: FileOutputStream? = null
        try {
            fileOutputStream = FileOutputStream(file)
            workbook?.write(fileOutputStream)
            showLog("Writing file: $file")
            isSuccess = true
        } catch (e: IOException) {
            showLog("Error storeExcelInStorage Exception: $e")
            errorData.postValue("storeExcelInStorage IOException: $e")
            isSuccess = false
        } catch (e: Exception) {
            showLog("Error storeExcelInStorage Exception: $e")
            errorData.postValue("storeExcelInStorage Exception: $e")
            isSuccess = false
        } finally {
            try {
                fileOutputStream?.close()
            } catch (ex: Exception) {
                showLog("Error storeExcelInStorage Exception: $ex")
                errorData.postValue("storeExcelInStorage finally: $ex")
            }
        }
        return isSuccess
    }

    fun readFileSelected() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (filePath.isNullOrEmpty()) return@launch

                excelData.postValue(arrayListOf()) // reset data

                clearExcelData()

                // Don't forget to Change to your assets folder excel sheet
                /* val myInput: InputStream = assets.open("TestExcel.xlsx")
                 val workbook = XSSFWorkbook(myInput)*/

                val excelFile = FileInputStream(File(filePath.orEmpty()))
                workbook = XSSFWorkbook(excelFile)
                sheet = workbook?.getSheetAt(0) // 0 = first sheet

                if (workbook == null || sheet == null) {
                    errorData.postValue("readFileSelected: " + "sheet is null")
                    return@launch
                }

                val rowIterator = sheet!!.rowIterator()
                val dataList: ArrayList<SQRExcel> = arrayListOf()

                while (rowIterator.hasNext()) {
                    val myRow = rowIterator.next() as Row
                    val cellIterator = myRow.cellIterator()
                    val item = SQRExcel()

                    while (cellIterator.hasNext()) {
                        val myCell = cellIterator.next() as XSSFCell
                        if (myCell.columnIndex == 0 && myCell.rawValue.isNotEmpty()) {
                            item.index = myCell.rawValue.toIntOrNull()
                        }
                        if (myCell.columnIndex == 12 && myCell.rawValue.isNotEmpty()) {
                            item.label = myCell.toString()
                        }

                        item.fileName = fileName
                    }

                    if (item.index != null && item.index != -1) {
                        dataList.add(item)
                    }
                }
                insertExcelData(dataList)
            } catch (ex: Exception) {
                showLog("readXLSXFileFromStorage ex: $ex")
                errorData.postValue("readFileSelected: " + ex.message)
                excelData.postValue(arrayListOf())
            }
        }
    }

    private fun insertExcelData(data: ArrayList<SQRExcel>) {
        showLog("insertExcelData data.size: ${data.size}")
        try {
            compositeDisposable.add(
                database.excelDao().insertSQRExcelData(data)
                    .subscribeOn(Schedulers.io())
                    .subscribe({
                    }, {
                        errorData.postValue("insertExcelData: " + it.message)
                        showLog("insertExcelData error: $it")
                    })
            )
            excelData.postValue(data)
        } catch (ex: Exception) {
            errorData.postValue(ex.message)
            showLog("insertExcelData ex: ${"insertExcelData: " + ex.message}")
        }
    }

    fun getExcelData() {
        if (fileName.isEmpty()) return

        compositeDisposable.add(
            database.excelDao().getExcelData(fileName = fileName)
                .subscribeOn(Schedulers.io())
                .subscribe({ list ->
                    showLog("getExcelData list: ${list.size}")
                    if (list.isNotEmpty()) {
                        excelData.postValue(list)
                    } else {
                        errorData.postValue(app.applicationContext.getString(R.string.scan_qr_data_empty))
                    }
                }, { throwable ->
                    excelData.postValue(arrayListOf())
                    errorData.postValue("getExcelData: " + throwable.message)
                    showLog("getExcelData throwable: $throwable")
                })
        )
    }

    private fun clearExcelData() {
        compositeDisposable.add(
            database.excelDao().deleteAll()
                .subscribeOn(Schedulers.io())
                .subscribe({
                }, { throwable ->
                    excelData.postValue(arrayListOf())
                    errorData.postValue("clearExcelData: " + throwable.message)
                    showLog("getExcelData throwable: $throwable")
                })
        )
    }

    override fun onCleared() {
        compositeDisposable.dispose()
        super.onCleared()
    }
}