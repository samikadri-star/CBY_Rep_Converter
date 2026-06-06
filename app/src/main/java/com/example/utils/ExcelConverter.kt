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
     * Optimized for low memory usage and stream-based reading to prevent OutOfMemory crashes on large reports.
     */
    fun convertTextUriToExcel(
        context: Context,
        inputUri: Uri,
        originalName: String,
        charsetOption: String = "ISO-8859-6"
    ): File {
        val contentResolver = context.contentResolver

        // Decode based on the selected Charset
        val charset = try {
            Charset.forName(charsetOption)
        } catch (e: Exception) {
            Charset.forName("ISO-8859-6") // default fallback
        }

        // First pass: scan first 100 lines for filename extraction to keep memory footprint minimal
        val baseName = try {
            val nameStream = contentResolver.openInputStream(inputUri)
            if (nameStream != null) {
                val reader = nameStream.bufferedReader(charset)
                val firstLines = mutableListOf<String>()
                var count = 0
                while (count < 100) {
                    val line = reader.readLine() ?: break
                    firstLines.add(line)
                    count++
                }
                nameStream.close()
                extractFilenameFromContent(firstLines)
            } else {
                "تقرير"
            }
        } catch (e: Exception) {
            "تقرير"
        }

        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("تقرير")
        sheet.setRightToLeft(true) // RTL layout for Arabic excel sheets

        val lowercaseName = originalName.lowercase(Locale.US)
        val isTargetReport = lowercaseName.startsWith("glrfdj") || 
                             lowercaseName.startsWith("cir1sa") || 
                             lowercaseName.startsWith("glrddj") || 
                             lowercaseName.startsWith("cir1samo")

        var excelRowIndex = 0
        var hasAtLeastOneRow = false

        // Second pass: Read line-by-line, parse, and write to Excel on-the-fly
        val processingInputStream = contentResolver.openInputStream(inputUri)
            ?: throw IllegalArgumentException("Could not open input stream for selected file.")

        try {
            processingInputStream.bufferedReader(charset).forEachLine { line ->
                val cleaned = cleanLine(line)
                if (cleaned.isNotEmpty()) {
                    var rowSplit = if (cleaned.contains('\t')) {
                        cleaned.split('\t')
                    } else {
                        cleaned.split('|')
                    }
                    rowSplit = rowSplit.map { it.trim() }

                    // Apply dynamic merging for special banking reports
                    if (isTargetReport && rowSplit.size > 1) {
                        val newRow = mutableListOf<String>()
                        val mergeTargets = setOf("999", "005", "001")
                        for (cell in rowSplit) {
                            if (mergeTargets.contains(cell) && newRow.isNotEmpty()) {
                                val lastIdx = newRow.size - 1
                                val lastVal = newRow[lastIdx]
                                newRow[lastIdx] = if (lastVal.isEmpty()) cell else "$lastVal $cell"
                            } else {
                                newRow.add(cell)
                            }
                        }
                        rowSplit = newRow
                    }

                    val excelRow = sheet.createRow(excelRowIndex)
                    for ((colIndex, cellVal) in rowSplit.withIndex()) {
                        val excelCell = excelRow.createCell(colIndex)
                        
                        // Attempt numeric parsing if it's purely decimal, to preserve Excel numeric formulas
                        val numericVal = cellVal.replace(",", "").toDoubleOrNull()
                        if (numericVal != null && cellVal.matches(Regex("^[+-]?[0-9,.\\s]+$"))) {
                            excelCell.setCellValue(numericVal)
                        } else {
                            excelCell.setCellValue(cellVal)
                        }
                    }
                    excelRowIndex++
                    hasAtLeastOneRow = true
                }
            }
        } finally {
            try {
                processingInputStream.close()
            } catch (ignored: Exception) {}
        }

        if (!hasAtLeastOneRow) {
            workbook.close()
            throw IllegalArgumentException("الملف المختار فارغ ولا يحتوي على بيانات صالحة بعد تصفية السطور.")
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val safeExcelName = "${baseName}_$timestamp"

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
