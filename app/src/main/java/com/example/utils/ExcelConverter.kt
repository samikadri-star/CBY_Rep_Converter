package com.example.utils

import android.content.Context
import android.net.Uri
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*

object ExcelConverter {

    fun cleanLine(line: String): String {
        // Correct regex for removing ANSI escape colors and terminal codes (PyQt6: r'\x1B(?:[@-Z\\-_]|\[[0-?]*[ -/]*[@-~])')
        val ansiEscape = Regex("\u001B(?:[@-Z\\\\-_]|\\[[0-?]*[ -/]*[@-~])")
        return ansiEscape.replace(line, "").trim()
    }

    fun extractFilenameFromContent(lines: List<String>): String {
        val keywords = listOf("كشف", "تقرير", "ميزان", "حساب", "ملخص")

        for (line in lines) {
            val cleaned = cleanLine(line)
            if (cleaned.isEmpty()) continue

            for (key in keywords) {
                if (cleaned.startsWith(key)) {
                    var namePart = cleaned.substring(key.length).trim()
                    if (namePart.isNotEmpty()) {
                        // Remove characters not allowed in file names
                        namePart = Regex("[\\\\/*?:\"<>|]").replace(namePart, "")
                        namePart = Regex("\\s+").replace(namePart, "_")
                            .trim { it == '_' }
                        
                        val limit = if (namePart.length > 50) 50 else namePart.length
                        return "${key}_${namePart.substring(0, limit)}"
                    }
                    return key
                }
            }
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "تقرير_$timestamp"
    }

    /**
     * Converts a single text file URI to a polished Excel workbook file saved locally in cache.
     * Supports dynamic charsets like ISO-8859-6, CP1256, or UTF-8.
     */
    fun convertTextUriToExcel(
        context: Context,
        inputUri: Uri,
        originalName: String,
        charsetOption: String = "ISO-8859-6"
    ): File {
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(inputUri) 
            ?: throw IllegalArgumentException("Could not open input stream for selected file.")

        // Read all bytes
        val bytes = inputStream.use { it.readBytes() }
        
        // Decode based on the selected Charset
        val charset = try {
            Charset.forName(charsetOption)
        } catch (e: Exception) {
            Charset.forName("ISO-8859-6") // default fallback
        }
        
        val content = String(bytes, charset)
        val rawLines = content.lineSequence().toList()

        if (rawLines.isEmpty()) {
            throw IllegalArgumentException("الملف المختار فارغ ولا يحتوي على بيانات.")
        }

        // Process filename
        val baseName = extractFilenameFromContent(rawLines)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val safeExcelName = "${baseName}_$timestamp"

        // Parse rows & cells
        val parsedData = mutableListOf<List<String>>()
        for (line in rawLines) {
            val cleaned = cleanLine(line)
            if (cleaned.isNotEmpty()) {
                val rowSplit = if (cleaned.contains('\t')) {
                    cleaned.split('\t')
                } else if (cleaned.contains('|')) {
                    cleaned.split('|')
                } else {
                    cleaned.split('|')
                }
                parsedData.add(rowSplit.map { it.trim() })
            }
        }

        if (parsedData.isEmpty()) {
            throw IllegalArgumentException("لم يتم العثور على أي بيانات صالحة بعد تصفية السطور.")
        }

        // Create Excel Workbook
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("تقرير")
        sheet.setRightToLeft(true) // RTL layout for Arabic excel sheets

        // Populate cells
        for ((rowIndex, rowItems) in parsedData.withIndex()) {
            val excelRow = sheet.createRow(rowIndex)
            for ((colIndex, cellValue) in rowItems.withIndex()) {
                val excelCell = excelRow.createCell(colIndex)
                
                // Attempt numeric parsing if it's purely decimal, to preserve Excel numeric formulas
                val numericVal = cellValue.replace(",", "").toDoubleOrNull()
                if (numericVal != null && cellValue.matches(Regex("^[+-]?[0-9,.\\s]+$"))) {
                    excelCell.setCellValue(numericVal)
                } else {
                    excelCell.setCellValue(cellValue)
                }
            }
        }

        // Save workbook to cache directory
        val outputDir = File(context.cacheDir, "converted_xlsx")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        val outputFile = File(outputDir, "$safeExcelName.xlsx")
        FileOutputStream(outputFile).use { fos ->
            workbook.write(fos)
        }
        workbook.close()

        return outputFile
    }
}
