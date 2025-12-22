package com.example.tabelahisabapp.ui.settings

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tabelahisabapp.data.repository.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class ExportUiState(
    val isExporting: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val repository: MainRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    fun exportToExcel(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            
            withContext(Dispatchers.IO) {
                try {
                    val exportDir = getExportDirectory()
                    if (!exportDir.exists()) exportDir.mkdirs()

                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    
                    // Export customers
                    val customersFile = File(exportDir, "customers_$timestamp.csv")
                    val customers = repository.getAllCustomersWithBalance().first()
                    FileWriter(customersFile).use { writer ->
                        writer.append("Name,Phone,Balance,Created At\n")
                        customers.forEach { result ->
                            val customer = result.customer
                            writer.append("${customer.name},${customer.phone ?: ""},${result.balance},${formatDate(customer.createdAt)}\n")
                        }
                    }

                    // Export transactions
                    val transactionsFile = File(exportDir, "transactions_$timestamp.csv")
                    FileWriter(transactionsFile).use { writer ->
                        writer.append("Customer,Type,Amount,Date,Note\n")
                        customers.forEach { result ->
                            val transactions = repository.getTransactionsForCustomer(result.customer.id).first()
                            transactions.forEach { txn ->
                                writer.append("${result.customer.name},${txn.type},${txn.amount},${formatDate(txn.date)},${txn.note ?: ""}\n")
                            }
                        }
                    }

                    // Export daily balances
                    val dailyFile = File(exportDir, "daily_balances_$timestamp.csv")
                    val dailyBalances = repository.getAllDailyBalances().first()
                    FileWriter(dailyFile).use { writer ->
                        writer.append("Date,Opening Cash,Opening Bank,Closing Cash,Closing Bank,Note\n")
                        dailyBalances.forEach { balance ->
                            writer.append("${formatDate(balance.date)},${balance.openingCash},${balance.openingBank},${balance.closingCash},${balance.closingBank},${balance.note ?: ""}\n")
                        }
                    }

                    // Export trades
                    val tradesFile = File(exportDir, "trades_$timestamp.csv")
                    val trades = repository.getAllTrades().first()
                    FileWriter(tradesFile).use { writer ->
                        writer.append("Date,Type,Item,Quantity,Price Per Unit,Total,Note\n")
                        trades.forEach { trade ->
                            writer.append("${formatDate(trade.date)},${trade.type},${trade.itemName},${trade.quantity},${trade.pricePerUnit},${trade.totalAmount},${trade.note ?: ""}\n")
                        }
                    }

                    _uiState.update { 
                        it.copy(
                            isExporting = false,
                            message = "Exported to: Download/UdhaarLedger/Exports/"
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    _uiState.update { 
                        it.copy(
                            isExporting = false,
                            message = "Export failed: ${e.message}"
                        )
                    }
                }
            }
        }
    }

    fun exportToCsv(context: Context) {
        // Same as exportToExcel for now (CSV format)
        exportToExcel(context)
    }

    fun printReport(context: Context, reportType: String, settings: PrintSettings) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val pdfDocument = PdfDocument()
                    val pageInfo = PdfDocument.PageInfo.Builder(
                        if (settings.paperSize == "A4") 595 else 612,
                        if (settings.paperSize == "A4") 842 else 792,
                        1
                    ).create()
                    
                    val page = pdfDocument.startPage(pageInfo)
                    val canvas = page.canvas
                    
                    val titlePaint = Paint().apply {
                        color = Color.BLACK
                        textSize = 24f
                        isFakeBoldText = true
                    }
                    
                    val headerPaint = Paint().apply {
                        color = Color.DKGRAY
                        textSize = 16f
                        isFakeBoldText = true
                    }
                    
                    val bodyPaint = Paint().apply {
                        color = Color.BLACK
                        textSize = 12f
                    }

                    var yPos = 50f

                    // Business header
                    if (settings.businessName.isNotBlank()) {
                        canvas.drawText(settings.businessName, 50f, yPos, titlePaint)
                        yPos += 30f
                    }

                    // Report title
                    val title = when (reportType) {
                        "customer" -> "Customer Ledger Report"
                        "daily" -> "Daily Register Report"
                        "trading" -> "Trading Report"
                        else -> "Report"
                    }
                    canvas.drawText(title, 50f, yPos, headerPaint)
                    yPos += 20f

                    // Generated date
                    val dateFormat = SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault())
                    canvas.drawText("Generated: ${dateFormat.format(Date())}", 50f, yPos, bodyPaint)
                    yPos += 40f

                    // Content based on report type
                    when (reportType) {
                        "customer" -> {
                            val customers = repository.getAllCustomersWithBalance().first()
                            customers.take(30).forEach { result ->
                                canvas.drawText(
                                    "${result.customer.name}: ₹${result.balance}",
                                    50f, yPos, bodyPaint
                                )
                                yPos += 20f
                            }
                        }
                        "daily" -> {
                            val balances = repository.getAllDailyBalances().first()
                            balances.take(30).forEach { balance ->
                                val total = balance.closingCash + balance.closingBank
                                canvas.drawText(
                                    "${formatDate(balance.date)}: ₹$total",
                                    50f, yPos, bodyPaint
                                )
                                yPos += 20f
                            }
                        }
                        "trading" -> {
                            val trades = repository.getAllTrades().first()
                            trades.take(30).forEach { trade ->
                                canvas.drawText(
                                    "${formatDate(trade.date)} - ${trade.itemName}: ₹${trade.totalAmount}",
                                    50f, yPos, bodyPaint
                                )
                                yPos += 20f
                            }
                        }
                    }

                    pdfDocument.finishPage(page)

                    // Save PDF
                    val exportDir = getExportDirectory()
                    if (!exportDir.exists()) exportDir.mkdirs()
                    
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    val pdfFile = File(exportDir, "${reportType}_report_$timestamp.pdf")
                    
                    FileOutputStream(pdfFile).use { output ->
                        pdfDocument.writeTo(output)
                    }
                    pdfDocument.close()

                    // Open print dialog
                    withContext(Dispatchers.Main) {
                        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                        _uiState.update { it.copy(message = "PDF saved to: ${pdfFile.path}") }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    _uiState.update { it.copy(message = "Print failed: ${e.message}") }
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun getExportDirectory(): File {
        return File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "UdhaarLedger/Exports"
        )
    }

    private fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

