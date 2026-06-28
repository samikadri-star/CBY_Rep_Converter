package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.utils.ExcelConverter
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("محول التقارير", appName)
  }

  @Test
  fun `test glr2dj report conversion and alignment`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    
    // Create a temporary daily journal file starting with glr2dj
    val tempFile = File.createTempFile("glr2dj_daily_journal", ".txt")
    val fileContent = """
        2026-06-28|قيد تحويل بسيط|1000.00|0.00|1000.00
        2026-06-28|الرصيد المدور|1000.00|0.00|1000.00
        الاجمالي|2500.00|0.00|2500.00
    """.trimIndent()
    
    tempFile.writeText(fileContent, java.nio.charset.Charset.forName("ISO-8859-6"))
    val inputUri = Uri.fromFile(tempFile)
    
    // Convert
    val outputFile = ExcelConverter.convertTextUriToExcel(
      context = context,
      inputUri = inputUri,
      originalName = "glr2dj_report_sample.txt",
      charsetOption = "ISO-8859-6"
    )
    
    assertTrue(outputFile.exists())
    
    // Read and verify the sheets cells using Apache POI
    FileInputStream(outputFile).use { fis ->
      val workbook = XSSFWorkbook(fis)
      val sheet = workbook.getSheetAt(0)
      
      // Row 0: "2026-06-28|قيد تحويل بسيط|1000.00|0.00|1000.00"
      // Split elements: ["2026-06-28", "قيد تحويل بسيط", "1000.00", "0.00", "1000.00"] -> size = 5
      // Aligns to: ["2026-06-28", "", "قيد تحويل بسيط", "1000.00", "0.00", "1000.00"] -> size = 6
      val row0 = sheet.getRow(0)
      assertEquals("2026-06-28", row0.getCell(0).stringCellValue)
      assertEquals("", row0.getCell(1).stringCellValue)
      assertEquals("قيد تحويل بسيط", row0.getCell(2).stringCellValue)
      assertEquals(1000.00, row0.getCell(3).numericCellValue, 0.01)
      assertEquals(0.00, row0.getCell(4).numericCellValue, 0.01)
      assertEquals(1000.00, row0.getCell(5).numericCellValue, 0.01)
      
      // Row 1: "2026-06-28|الرصيد المدور|1000.00|0.00|1000.00" -> size = 5, isSpecialSummaryRow
      // Aligns to: ["2026-06-28", "", "الرصيد المدور", "1000.00", "0.00", "1000.00"] -> size = 6
      val row1 = sheet.getRow(1)
      assertEquals("2026-06-28", row1.getCell(0).stringCellValue)
      assertEquals("", row1.getCell(1).stringCellValue)
      assertEquals("الرصيد المدور", row1.getCell(2).stringCellValue)
      assertEquals(1000.00, row1.getCell(3).numericCellValue, 0.01)
      assertEquals(0.00, row1.getCell(4).numericCellValue, 0.01)
      assertEquals(1000.00, row1.getCell(5).numericCellValue, 0.01)
      
      // Row 2: "الاجمالي|2500.00|0.00|2500.00" -> size = 4, isSpecialSummaryRow
      // Aligns to: ["", "", "الاجمالي", "2500.00", "0.00", "2500.00"] -> size = 6
      val row2 = sheet.getRow(2)
      assertEquals("", row2.getCell(0).stringCellValue)
      assertEquals("", row2.getCell(1).stringCellValue)
      assertEquals("الاجمالي", row2.getCell(2).stringCellValue)
      assertEquals(2500.00, row2.getCell(3).numericCellValue, 0.01)
      assertEquals(0.00, row2.getCell(4).numericCellValue, 0.01)
      assertEquals(2500.00, row2.getCell(5).numericCellValue, 0.01)
      
      workbook.close()
    }
    
    // Clean up
    tempFile.delete()
    outputFile.delete()
  }
}
