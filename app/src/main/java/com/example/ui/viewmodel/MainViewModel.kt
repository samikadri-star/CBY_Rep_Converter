package com.example.ui.viewmodel

import android.app.Application
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.ConversionItem
import com.example.data.repository.ConversionRepository
import com.example.utils.ExcelConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ConversionRepository
    val historyItems: StateFlow<List<ConversionItem>>

    private val _isConverting = MutableStateFlow(false)
    val isConverting = _isConverting.asStateFlow()

    private val _progressValue = MutableStateFlow(0f)
    val progressValue = _progressValue.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage = _statusMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ConversionRepository(database.conversionDao())
        historyItems = repository.allConversions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun convertFiles(uris: List<Uri>, charsetOption: String) {
        if (uris.isEmpty()) return

        viewModelScope.launch {
            _isConverting.value = true
            _progressValue.value = 0f
            _errorMessage.value = null
            _statusMessage.value = null

            var successCount = 0
            val totalFiles = uris.size
            val localErrors = mutableListOf<String>()

            withContext(Dispatchers.IO) {
                for ((index, uri) in uris.withIndex()) {
                    var originalName = "تقرير"
                    try {
                        // Retrieve file display name via ContentResolver SAF
                        getApplication<Application>().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                if (nameIndex != -1) {
                                    originalName = cursor.getString(nameIndex)
                                }
                            }
                        }

                        // Perform Excel Conversion
                        val outputFile = ExcelConverter.convertTextUriToExcel(
                            context = getApplication(),
                            inputUri = uri,
                            originalName = originalName,
                            charsetOption = charsetOption
                        )

                        // Save details to database
                        val item = ConversionItem(
                            originalFileName = originalName,
                            convertedFileName = outputFile.name,
                            fileUriString = Uri.fromFile(outputFile).toString(),
                            status = "SUCCESS",
                            fileSize = outputFile.length(),
                            errorMessage = null
                        )
                        repository.insert(item)
                        successCount++

                    } catch (t: Throwable) {
                        t.printStackTrace()
                        val errMsg = if (t is OutOfMemoryError) {
                            "الملف كبير جداً وتجاوز حد الذاكرة المتاحة للتطبيق. يُرجى تجزئة الملف ومحاولة تحويله."
                        } else {
                            t.localizedMessage ?: "فشل في عملية القراءة أو التحويل."
                        }
                        localErrors.add("$originalName: $errMsg")

                        val failedItem = ConversionItem(
                            originalFileName = originalName,
                            convertedFileName = "تقرير_فاشل.xlsx",
                            fileUriString = "",
                            status = "FAILED",
                            fileSize = 0L,
                            errorMessage = errMsg
                        )
                        repository.insert(failedItem)
                    }

                    _progressValue.value = (index + 1).toFloat() / totalFiles
                }

                if (localErrors.isNotEmpty()) {
                    _errorMessage.value = localErrors.joinToString("\n\n")
                }
                
                if (successCount > 0) {
                    _statusMessage.value = "✅ تم تحويل $successCount من $totalFiles ملفات بنجاح!"
                } else if (localErrors.isNotEmpty()) {
                    _statusMessage.value = "❌ فشلت عملية تحويل الملفات"
                }
            }

            _isConverting.value = false
        }
    }

    /**
     * Share converted Excel file with external apps.
     */
    fun shareExcelFile(item: ConversionItem) {
        val context = getApplication<Application>()
        val fileUri = Uri.parse(item.fileUriString)
        val file = fileUri.path?.let { File(it) } ?: return

        if (!file.exists()) {
            _errorMessage.value = "عذراً، الملف غير موجود في وحدة التخزين المؤقت، قد يكون قد تم حذفه."
            return
        }

        try {
            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooser = Intent.createChooser(shareIntent, "مشاركة تقرير الاكسل عبر:").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            _errorMessage.value = "حدث خطأ أثناء محاولة المشاركة: ${e.localizedMessage}"
        }
    }

    /**
     * Open converted Excel file in system viewer (Google Sheets, MS Excel).
     */
    fun openExcelFile(item: ConversionItem) {
        val context = getApplication<Application>()
        val fileUri = Uri.parse(item.fileUriString)
        val file = fileUri.path?.let { File(it) } ?: return

        if (!file.exists()) {
            _errorMessage.value = "عذراً، الملف غير موجود في وحدة التخزين المؤقت."
            return
        }

        try {
            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, file)

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(viewIntent)
        } catch (e: Exception) {
            _errorMessage.value = "لا يوجد تطبيق مناسب لفتح ملفات Excel على هذا الجهاز مجهّز حالياً، يُوصى بتحميل Excel أو Google Sheets."
        }
    }

    /**
     * Save converted Excel file from internal cache to public Downloads folder.
     */
    fun saveToDownloads(item: ConversionItem) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val fileUri = Uri.parse(item.fileUriString)
            val file = fileUri.path?.let { File(it) }

            if (file == null || !file.exists()) {
                _errorMessage.value = "الرمز الداخلي للملف تالف أو تم مسحه."
                return@launch
            }

            val saved = withContext(Dispatchers.IO) {
                val resolver = context.contentResolver
                val displayName = item.convertedFileName

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val targetUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (targetUri == null) {
                        false
                    } else {
                        try {
                            resolver.openOutputStream(targetUri)?.use { outStream ->
                                file.inputStream().use { inStream ->
                                    inStream.copyTo(outStream)
                                }
                            }
                            true
                        } catch (e: Exception) {
                            e.printStackTrace()
                            false
                        }
                    }
                } else {
                    val targetDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!targetDir.exists()) {
                        targetDir.mkdirs()
                    }
                    val targetFile = File(targetDir, displayName)
                    try {
                        file.copyTo(targetFile, overwrite = true)
                        true
                    } catch (e: Exception) {
                        e.printStackTrace()
                        false
                    }
                }
            }

            if (saved) {
                _statusMessage.value = "📥 تم حفظ الملف في مجلد التنزيلات (Downloads) بنجاح!"
            } else {
                _errorMessage.value = "فشل حفظ الملف في مجدل التنزيلات."
            }
        }
    }

    fun deleteHistoryItem(item: ConversionItem) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                // Remove local physical file if it exists
                if (item.fileUriString.isNotEmpty()) {
                    val fileUri = Uri.parse(item.fileUriString)
                    fileUri.path?.let { path ->
                        val file = File(path)
                        if (file.exists()) {
                            file.delete()
                        }
                    }
                }
                repository.delete(item)
            }
            _statusMessage.value = "تمت إزالة السجل وحذف الملف المؤقت الخاص به."
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                // Delete physical files
                val cacheDirForExcel = File(getApplication<Application>().cacheDir, "converted_xlsx")
                if (cacheDirForExcel.exists()) {
                    cacheDirForExcel.deleteRecursively()
                }
                repository.clearAll()
            }
            _statusMessage.value = "تم إفراغ سجل التحويل والملفات المؤقتة تماماً."
        }
    }

    fun dismissStatus() {
        _statusMessage.value = null
    }

    fun dismissError() {
        _errorMessage.value = null
    }
}
