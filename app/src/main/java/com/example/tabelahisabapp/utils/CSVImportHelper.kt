package com.example.tabelahisabapp.utils

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

/**
 * CSV Import Helper - Parses CSV files for bulk import
 * Supports multiple date formats commonly used in India
 */
object CSVImportHelper {
    
    // Supported date formats
    private val dateFormats = listOf(
        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()),
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
        SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()),
        SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
    )
    
    /**
     * Parse a date string trying multiple formats
     * Returns midnight timestamp for the date
     */
    fun parseDate(dateStr: String): Long? {
        for (format in dateFormats) {
            try {
                format.isLenient = false
                val date = format.parse(dateStr.trim())
                if (date != null) {
                    // Normalize to midnight
                    val calendar = Calendar.getInstance().apply {
                        time = date
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    return calendar.timeInMillis
                }
            } catch (e: Exception) {
                // Try next format
            }
        }
        return null
    }
    
    /**
     * Read CSV file from URI and return list of rows (each row is a map of column->value)
     */
    fun readCSV(context: Context, uri: Uri): Result<List<Map<String, String>>> {
        return try {
            val rows = mutableListOf<Map<String, String>>()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    // Read header row
                    val headerLine = reader.readLine() ?: return Result.failure(
                        Exception("Empty CSV file")
                    )
                    val headers = parseCSVLine(headerLine).map { it.trim().lowercase() }
                    
                    // Read data rows
                    var lineNumber = 1
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        lineNumber++
                        val values = parseCSVLine(line!!)
                        if (values.isNotEmpty() && values.any { it.isNotBlank() }) {
                            val row = mutableMapOf<String, String>()
                            headers.forEachIndexed { index, header ->
                                row[header] = values.getOrElse(index) { "" }.trim()
                            }
                            row["_line"] = lineNumber.toString()
                            rows.add(row)
                        }
                    }
                }
            } ?: return Result.failure(Exception("Could not open file"))
            
            Result.success(rows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Parse a single CSV line handling quoted values
     */
    private fun parseCSVLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        
        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString())
        
        return result
    }
    
    /**
     * Validate required columns exist in CSV
     */
    fun validateColumns(
        rows: List<Map<String, String>>,
        requiredColumns: List<String>
    ): ValidationResult {
        if (rows.isEmpty()) {
            return ValidationResult(false, "CSV file is empty", emptyList())
        }
        
        val firstRow = rows.first()
        val missingColumns = requiredColumns.filter { col -> 
            !firstRow.keys.any { it.equals(col, ignoreCase = true) }
        }
        
        return if (missingColumns.isEmpty()) {
            ValidationResult(true, "All required columns present", emptyList())
        } else {
            ValidationResult(
                false, 
                "Missing required columns: ${missingColumns.joinToString(", ")}", 
                missingColumns
            )
        }
    }
    
    /**
     * Get column value case-insensitively
     */
    fun Map<String, String>.getColumn(name: String): String {
        return this.entries.find { it.key.equals(name, ignoreCase = true) }?.value ?: ""
    }
    
    data class ValidationResult(
        val isValid: Boolean,
        val message: String,
        val missingColumns: List<String>
    )
}

/**
 * Import result for a single import type
 */
data class ImportResult(
    val success: Boolean,
    val importedCount: Int,
    val skippedCount: Int,
    val errorCount: Int,
    val errors: List<ImportError>,
    val message: String
)

data class ImportError(
    val lineNumber: Int,
    val field: String,
    val value: String,
    val error: String
)
