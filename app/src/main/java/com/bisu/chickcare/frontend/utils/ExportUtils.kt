package com.bisu.chickcare.frontend.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import com.bisu.chickcare.backend.repository.DetectionEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportUtils {
    
    /**
     * Export detection history as CSV file
     * @param context The context to use
     * @param detections List of detection entries to export
     * @param onSuccess Callback when export succeeds (provides file URI)
     * @param onError Callback when export fails
     */
    suspend fun exportDetectionHistoryAsCSV(
        context: Context,
        detections: List<DetectionEntry>,
        onSuccess: (Uri) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
            val fileName = "chickcare_detections_${dateFormat.format(Date())}.csv"
            val file = File(context.getExternalFilesDir(null), fileName)
            
            FileOutputStream(file).use { outputStream ->
                // Write CSV header
                outputStream.write("Date,Time,Location,Result,Health Status,Confidence,Has Image,Has Audio,Treatment,Treatment Date,Next Dose Date,Treatment Notes,Recommendations\n".toByteArray())
                
                // Write data rows
                detections.forEach { detection ->
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(detection.timestamp))
                    val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(detection.timestamp))
                    val location = detection.location ?: "N/A"
                    val result = detection.result.replace(",", ";") // Replace commas to avoid CSV issues
                    val healthStatus = if (detection.isHealthy) "Healthy" else "Unhealthy"
                    val confidence = "${(detection.confidence * 100).toInt()}%"
                    val hasImage = if (!detection.imageUri.isNullOrEmpty()) "Yes" else "No"
                    val hasAudio = if (!detection.audioUri.isNullOrEmpty()) "Yes" else "No"
                    val treatment = detection.treatment?.replace(",", ";") ?: "N/A"
                    val treatmentDate = if (detection.treatmentDate != null) SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(detection.treatmentDate)) else "N/A"
                    val nextDoseDate = if (detection.nextDoseDate != null) SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(detection.nextDoseDate)) else "N/A"
                    val treatmentNotes = detection.treatmentNotes?.replace(",", ";") ?: "N/A"
                    val recommendations = detection.recommendations.joinToString("; ") { it.replace(",", ";") }
                    
                    val row = "$date,$time,$location,$result,$healthStatus,$confidence,$hasImage,$hasAudio,\"$treatment\",$treatmentDate,$nextDoseDate,\"$treatmentNotes\",\"$recommendations\"\n"
                    outputStream.write(row.toByteArray())
                }
            }
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            
            withContext(Dispatchers.Main) {
                onSuccess(uri)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError("Failed to export CSV: ${e.message}")
            }
        }
    }
    
    /**
     * Export detection history as PDF file
     * @param context The context to use
     * @param detections List of detection entries to export
     * @param onSuccess Callback when export succeeds (provides file URI)
     * @param onError Callback when export fails
     */
    suspend fun exportDetectionHistoryAsPDF(
        context: Context,
        detections: List<DetectionEntry>,
        onSuccess: (Uri) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
            val fileName = "chickcare_detections_${dateFormat.format(Date())}.pdf"
            val file = File(context.getExternalFilesDir(null), fileName)
            
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size in points
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas
            var yPosition = 50f
            val paint = Paint().apply {
                isAntiAlias = true
                textSize = 12f
            }
            val titlePaint = Paint().apply {
                isAntiAlias = true
                textSize = 18f
                isFakeBoldText = true
            }
            val headerPaint = Paint().apply {
                isAntiAlias = true
                textSize = 10f
                isFakeBoldText = true
            }
            
            val pageHeight = 842f
            val margin = 50f
            val lineHeight = 20f
            var currentPage = 1
            
            // Write title
            canvas.drawText("ChickCare Detection History Report", margin, yPosition, titlePaint)
            yPosition += 30f
            canvas.drawText("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}", margin, yPosition, paint)
            yPosition += 40f
            
            // Write header row
            val headers = listOf("Date", "Time", "Result", "Status", "Confidence", "Treatment")
            val columnWidths = listOf(90f, 70f, 120f, 70f, 70f, 100f)
            var xPos = margin
            headers.forEachIndexed { index, header ->
                canvas.drawText(header, xPos, yPosition, headerPaint)
                xPos += columnWidths[index]
            }
            yPosition += 30f
            canvas.drawLine(margin, yPosition, 545f, yPosition, paint)
            yPosition += 20f
            
            // Write data rows
            detections.forEach { detection ->
                // Check if we need a new page
                if (yPosition > pageHeight - 100f) {
                    pdfDocument.finishPage(page)
                    currentPage++
                    val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, currentPage).create()
                    page = pdfDocument.startPage(newPageInfo)
                    canvas = page.canvas
                    yPosition = 50f
                }
                
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(detection.timestamp))
                val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(detection.timestamp))
                val result = detection.result.take(15) // Truncate long results
                val status = if (detection.isHealthy) "Healthy" else "Unhealthy"
                val confidence = "${(detection.confidence * 100).toInt()}%"
                val treatment = detection.treatment?.take(12) ?: "N/A"
                
                xPos = margin
                val rowData = listOf(date, time, result, status, confidence, treatment)
                rowData.forEachIndexed { index, data ->
                    canvas.drawText(data, xPos, yPosition, paint)
                    xPos += columnWidths[index]
                }
                yPosition += lineHeight
                
                // Add treatment details below if available
                if (detection.treatment != null) {
                    if (yPosition > pageHeight - 100f) {
                        pdfDocument.finishPage(page)
                        currentPage++
                        val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, currentPage).create()
                        page = pdfDocument.startPage(newPageInfo)
                        canvas = page.canvas
                        yPosition = 50f
                    }
                    val treatmentInfo = StringBuilder()
                    if (detection.treatmentDate != null) {
                        treatmentInfo.append("Given: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(detection.treatmentDate))}")
                    }
                    if (detection.nextDoseDate != null) {
                        if (treatmentInfo.isNotEmpty()) treatmentInfo.append(", ")
                        treatmentInfo.append("Next: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(detection.nextDoseDate))}")
                    }
                    if (treatmentInfo.isNotEmpty()) {
                        canvas.drawText("  └─ $treatmentInfo", margin + 20f, yPosition, paint.apply { textSize = 10f })
                        yPosition += lineHeight
                    }
                }
            }
            
            pdfDocument.finishPage(page)
            
            FileOutputStream(file).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            pdfDocument.close()
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            
            withContext(Dispatchers.Main) {
                onSuccess(uri)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError("Failed to export PDF: ${e.message}")
            }
        }
    }
    
    /**
     * Share a file via Intent
     * @param context The context to use
     * @param uri The URI of the file to share
     * @param mimeType The MIME type of the file
     * @param fileName The name of the file
     */
    fun shareFile(
        context: Context,
        uri: Uri,
        mimeType: String,
        fileName: String
    ) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, fileName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(Intent.createChooser(shareIntent, "Share $fileName"))
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to share file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Export dashboard stats as image (bitmap)
     * Note: This requires the dashboard to be rendered as a bitmap first
     * @param context The context to use
     * @param bitmap The bitmap to export
     * @param onSuccess Callback when export succeeds (provides file URI)
     * @param onError Callback when export fails
     */
    @Suppress("unused")
    suspend fun exportDashboardStatsAsImage(
        context: Context,
        bitmap: Bitmap,
        onSuccess: (Uri) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
            val fileName = "chickcare_dashboard_${dateFormat.format(Date())}.png"
            val file = File(context.getExternalFilesDir(null), fileName)
            
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            
            withContext(Dispatchers.Main) {
                onSuccess(uri)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError("Failed to export image: ${e.message}")
            }
        }
    }
    
    /**
     * Capture a Compose view as a bitmap
     * This is a helper function that can be used with Compose's ViewCompositionStrategy
     */
    @Suppress("unused")
    fun captureBitmapFromView(view: android.view.View): Bitmap? {
        return try {
            val bitmap = createBitmap(view.width, view.height)
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            bitmap
        } catch (_: Exception) {
            null
        }
    }
    
    /**
     * Export report data as CSV based on report category
     */
    suspend fun exportReportAsCSV(
        context: Context,
        reportTitle: String,
        reportCategory: com.bisu.chickcare.backend.repository.ReportCategory,
        reportData: com.bisu.chickcare.backend.viewmodels.ReportData,
        onSuccess: (Uri) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
            val fileName = "chickcare_${reportTitle.lowercase().replace(" ", "_")}_${dateFormat.format(Date())}.csv"
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            
            FileOutputStream(file).use { outputStream ->
                when (reportCategory) {
                    com.bisu.chickcare.backend.repository.ReportCategory.HEALTH -> {
                        // Health Report: Health Records + Vaccinations + Medications + Detections
                        outputStream.write("HEALTH SUMMARY REPORT\n".toByteArray())
                        outputStream.write("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n\n".toByteArray())
                        
                        // Health Records
                        outputStream.write("HEALTH RECORDS\n".toByteArray())
                        outputStream.write("Date,Chicken Name,Condition,Status,Symptoms,Treatment,Veterinarian\n".toByteArray())
                        reportData.healthRecords.forEach { record ->
                            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(record.date))
                            outputStream.write("$date,${record.chickenName},${record.condition},${record.status},${record.symptoms.replace(",", ";")},${record.treatment.replace(",", ";")},${record.veterinarian ?: "N/A"}\n".toByteArray())
                        }
                        outputStream.write("\n".toByteArray())
                        
                        // Vaccinations
                        outputStream.write("VACCINATIONS\n".toByteArray())
                        outputStream.write("Date,Vaccine Name,Next Due Date,Batch Number,Administered By,Notes\n".toByteArray())
                        reportData.vaccinations.forEach { vac ->
                            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(vac.date))
                            val nextDue = if (vac.nextDueDate > 0) SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(vac.nextDueDate)) else "N/A"
                            outputStream.write("$date,${vac.vaccineName},$nextDue,${vac.batchNumber},${vac.administeredBy},${vac.notes.replace(",", ";")}\n".toByteArray())
                        }
                        outputStream.write("\n".toByteArray())
                        
                        // Medications
                        outputStream.write("MEDICATIONS\n".toByteArray())
                        outputStream.write("Date,Medication Name,Dosage,Frequency,Duration,Administered By,Notes\n".toByteArray())
                        reportData.medications.forEach { med ->
                            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(med.scheduledDate))
                            outputStream.write("$date,${med.medicationName},${med.dosage},${med.frequency},${med.duration},${med.administeredBy},${med.notes.replace(",", ";")}\n".toByteArray())
                        }
                        outputStream.write("\n".toByteArray())
                        
                        // Detection History
                        outputStream.write("DETECTION HISTORY\n".toByteArray())
                        outputStream.write("Date,Time,Result,Health Status,Confidence,Location\n".toByteArray())
                        reportData.detections.forEach { det ->
                            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(det.timestamp))
                            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(det.timestamp))
                            val status = if (det.isHealthy) "Healthy" else "Unhealthy"
                            outputStream.write("$date,$time,${det.result.replace(",", ";")},$status,${(det.confidence * 100).toInt()}%,${det.location ?: "N/A"}\n".toByteArray())
                        }
                    }
                    com.bisu.chickcare.backend.repository.ReportCategory.PRODUCTION -> {
                        // Production Report: Egg Production + Feeding Schedules
                        outputStream.write("PRODUCTION REPORT\n".toByteArray())
                        outputStream.write("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n\n".toByteArray())
                        
                        // Egg Production
                        outputStream.write("EGG PRODUCTION\n".toByteArray())
                        outputStream.write("Date,Total Eggs,Healthy Eggs,Broken Eggs,Coop Location,Notes\n".toByteArray())
                        reportData.eggProduction.forEach { egg ->
                            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(egg.date))
                            outputStream.write("$date,${egg.totalEggs},${egg.healthyEggs},${egg.brokenEggs},${egg.coopLocation},${egg.notes.replace(",", ";")}\n".toByteArray())
                        }
                        outputStream.write("\n".toByteArray())
                        
                        // Feeding Schedules
                        outputStream.write("FEEDING SCHEDULES\n".toByteArray())
                        outputStream.write("Date,Feed Type,Quantity,Target Group,Frequency,Status,Notes\n".toByteArray())
                        reportData.feedingSchedules.forEach { feed ->
                            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(feed.scheduledAt))
                            val status = if (feed.isCompleted) "Completed" else "Pending"
                            outputStream.write("$date,${feed.feedType},${feed.quantity},${feed.targetGroup},${feed.frequency},$status,${feed.notes.replace(",", ";")}\n".toByteArray())
                        }
                    }
                    com.bisu.chickcare.backend.repository.ReportCategory.FINANCIAL -> {
                        // Financial Report: Expenses
                        outputStream.write("FINANCIAL REPORT\n".toByteArray())
                        outputStream.write("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n\n".toByteArray())
                        
                        outputStream.write("EXPENSES\n".toByteArray())
                        outputStream.write("Date,Category,Amount,Description,Payment Method\n".toByteArray())
                        var totalAmount = 0.0
                        reportData.expenses.forEach { exp ->
                            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(exp.date))
                            totalAmount += exp.amount
                            outputStream.write("$date,${exp.category},${exp.amount},${exp.description.replace(",", ";")},${exp.paymentMethod}\n".toByteArray())
                        }
                        outputStream.write("\n".toByteArray())
                        outputStream.write("TOTAL EXPENSES,$totalAmount\n".toByteArray())
                    }
                    com.bisu.chickcare.backend.repository.ReportCategory.COMPREHENSIVE -> {
                        // Comprehensive Report: All data
                        outputStream.write("COMPREHENSIVE FARM REPORT\n".toByteArray())
                        outputStream.write("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n\n".toByteArray())
                        
                        // Include all sections from above
                        // Health Records
                        outputStream.write("HEALTH RECORDS\n".toByteArray())
                        outputStream.write("Date,Chicken Name,Condition,Status,Symptoms,Treatment\n".toByteArray())
                        reportData.healthRecords.forEach { record ->
                            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(record.date))
                            outputStream.write("$date,${record.chickenName},${record.condition},${record.status},${record.symptoms.replace(",", ";")},${record.treatment.replace(",", ";")}\n".toByteArray())
                        }
                        outputStream.write("\n".toByteArray())
                        
                        // Vaccinations
                        outputStream.write("VACCINATIONS\n".toByteArray())
                        outputStream.write("Date,Vaccine Name,Next Due Date,Administered By\n".toByteArray())
                        reportData.vaccinations.forEach { vac ->
                            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(vac.date))
                            val nextDue = if (vac.nextDueDate > 0) SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(vac.nextDueDate)) else "N/A"
                            outputStream.write("$date,${vac.vaccineName},$nextDue,${vac.administeredBy}\n".toByteArray())
                        }
                        outputStream.write("\n".toByteArray())
                        
                        // Medications
                        outputStream.write("MEDICATIONS\n".toByteArray())
                        outputStream.write("Date,Medication Name,Dosage,Frequency\n".toByteArray())
                        reportData.medications.forEach { med ->
                            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(med.scheduledDate))
                            outputStream.write("$date,${med.medicationName},${med.dosage},${med.frequency}\n".toByteArray())
                        }
                        outputStream.write("\n".toByteArray())
                        
                        // Egg Production
                        outputStream.write("EGG PRODUCTION\n".toByteArray())
                        outputStream.write("Date,Total Eggs,Healthy Eggs,Broken Eggs\n".toByteArray())
                        reportData.eggProduction.forEach { egg ->
                            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(egg.date))
                            outputStream.write("$date,${egg.totalEggs},${egg.healthyEggs},${egg.brokenEggs}\n".toByteArray())
                        }
                        outputStream.write("\n".toByteArray())
                        
                        // Feeding Schedules
                        outputStream.write("FEEDING SCHEDULES\n".toByteArray())
                        outputStream.write("Date,Feed Type,Quantity,Target Group\n".toByteArray())
                        reportData.feedingSchedules.forEach { feed ->
                            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(feed.scheduledAt))
                            outputStream.write("$date,${feed.feedType},${feed.quantity},${feed.targetGroup}\n".toByteArray())
                        }
                        outputStream.write("\n".toByteArray())
                        
                        // Expenses
                        outputStream.write("EXPENSES\n".toByteArray())
                        outputStream.write("Date,Category,Amount,Description\n".toByteArray())
                        var totalAmount = 0.0
                        reportData.expenses.forEach { exp ->
                            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(exp.date))
                            totalAmount += exp.amount
                            outputStream.write("$date,${exp.category},${exp.amount},${exp.description.replace(",", ";")}\n".toByteArray())
                        }
                        outputStream.write("\n".toByteArray())
                        outputStream.write("TOTAL EXPENSES,$totalAmount\n".toByteArray())
                        
                        // Detections
                        outputStream.write("\nDETECTION HISTORY\n".toByteArray())
                        outputStream.write("Date,Result,Health Status,Confidence\n".toByteArray())
                        reportData.detections.forEach { det ->
                            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(det.timestamp))
                            val status = if (det.isHealthy) "Healthy" else "Unhealthy"
                            outputStream.write("$date,${det.result.replace(",", ";")},$status,${(det.confidence * 100).toInt()}%\n".toByteArray())
                        }
                    }
                }
            }
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            
            withContext(Dispatchers.Main) {
                onSuccess(uri)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError("Failed to export CSV: ${e.message}")
            }
        }
    }
    
    /**
     * Export report data as PDF based on report category
     */
    suspend fun exportReportAsPDF(
        context: Context,
        reportTitle: String,
        reportCategory: com.bisu.chickcare.backend.repository.ReportCategory,
        reportData: com.bisu.chickcare.backend.viewmodels.ReportData,
        onSuccess: (Uri) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
            val fileName = "chickcare_${reportTitle.lowercase().replace(" ", "_")}_${dateFormat.format(Date())}.pdf"
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas
            var yPosition = 50f
            val margin = 50f
            val lineHeight = 20f
            val pageHeight = 842f
            var currentPage = 1
            
            val paint = Paint().apply {
                isAntiAlias = true
                textSize = 12f
            }
            val titlePaint = Paint().apply {
                isAntiAlias = true
                textSize = 18f
                isFakeBoldText = true
            }
            val headerPaint = Paint().apply {
                isAntiAlias = true
                textSize = 10f
                isFakeBoldText = true
            }
            
            // Write title
            canvas.drawText(reportTitle, margin, yPosition, titlePaint)
            yPosition += 30f
            canvas.drawText("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}", margin, yPosition, paint)
            yPosition += 40f
            
            // Write content based on category
            when (reportCategory) {
                com.bisu.chickcare.backend.repository.ReportCategory.HEALTH -> {
                    // Health Records
                    if (reportData.healthRecords.isNotEmpty()) {
                        if (yPosition > pageHeight - 100f) {
                            pdfDocument.finishPage(page)
                            currentPage++
                            val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, currentPage).create()
                            page = pdfDocument.startPage(newPageInfo)
                            canvas = page.canvas
                            yPosition = 50f
                        }
                        canvas.drawText("HEALTH RECORDS", margin, yPosition, headerPaint)
                        yPosition += 25f
                        reportData.healthRecords.take(20).forEach { record ->
                            if (yPosition > pageHeight - 100f) {
                                pdfDocument.finishPage(page)
                                currentPage++
                                val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, currentPage).create()
                                page = pdfDocument.startPage(newPageInfo)
                                canvas = page.canvas
                                yPosition = 50f
                            }
                            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(record.date))
                            canvas.drawText("$date - ${record.chickenName}: ${record.condition} (${record.status})", margin, yPosition, paint)
                            yPosition += lineHeight
                        }
                        yPosition += 20f
                    }
                    
                    // Vaccinations
                    if (reportData.vaccinations.isNotEmpty()) {
                        if (yPosition > pageHeight - 100f) {
                            pdfDocument.finishPage(page)
                            currentPage++
                            val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, currentPage).create()
                            page = pdfDocument.startPage(newPageInfo)
                            canvas = page.canvas
                            yPosition = 50f
                        }
                        canvas.drawText("VACCINATIONS", margin, yPosition, headerPaint)
                        yPosition += 25f
                        reportData.vaccinations.take(20).forEach { vac ->
                            if (yPosition > pageHeight - 100f) {
                                pdfDocument.finishPage(page)
                                currentPage++
                                val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, currentPage).create()
                                page = pdfDocument.startPage(newPageInfo)
                                canvas = page.canvas
                                yPosition = 50f
                            }
                            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(vac.date))
                            canvas.drawText("$date - ${vac.vaccineName} (Next: ${if (vac.nextDueDate > 0) SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(vac.nextDueDate)) else "N/A"})", margin, yPosition, paint)
                            yPosition += lineHeight
                        }
                        yPosition += 20f
                    }
                    
                    // Medications
                    if (reportData.medications.isNotEmpty()) {
                        if (yPosition > pageHeight - 100f) {
                            pdfDocument.finishPage(page)
                            currentPage++
                            val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, currentPage).create()
                            page = pdfDocument.startPage(newPageInfo)
                            canvas = page.canvas
                            yPosition = 50f
                        }
                        canvas.drawText("MEDICATIONS", margin, yPosition, headerPaint)
                        yPosition += 25f
                        reportData.medications.take(20).forEach { med ->
                            if (yPosition > pageHeight - 100f) {
                                pdfDocument.finishPage(page)
                                currentPage++
                                val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, currentPage).create()
                                page = pdfDocument.startPage(newPageInfo)
                                canvas = page.canvas
                                yPosition = 50f
                            }
                            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(med.scheduledDate))
                            canvas.drawText("$date - ${med.medicationName} (${med.dosage}, ${med.frequency})", margin, yPosition, paint)
                            yPosition += lineHeight
                        }
                        yPosition += 20f
                    }
                    
                    // Detections
                    if (reportData.detections.isNotEmpty()) {
                        if (yPosition > pageHeight - 100f) {
                            pdfDocument.finishPage(page)
                            currentPage++
                            val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, currentPage).create()
                            page = pdfDocument.startPage(newPageInfo)
                            canvas = page.canvas
                            yPosition = 50f
                        }
                        canvas.drawText("DETECTION HISTORY", margin, yPosition, headerPaint)
                        yPosition += 25f
                        reportData.detections.take(20).forEach { det ->
                            if (yPosition > pageHeight - 100f) {
                                pdfDocument.finishPage(page)
                                currentPage++
                                val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, currentPage).create()
                                page = pdfDocument.startPage(newPageInfo)
                                canvas = page.canvas
                                yPosition = 50f
                            }
                            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(det.timestamp))
                            val status = if (det.isHealthy) "Healthy" else "Unhealthy"
                            canvas.drawText("$date - ${det.result} ($status, ${(det.confidence * 100).toInt()}%)", margin, yPosition, paint)
                            yPosition += lineHeight
                        }
                    }
                }
                com.bisu.chickcare.backend.repository.ReportCategory.PRODUCTION -> {
                    // Egg Production
                    if (reportData.eggProduction.isNotEmpty()) {
                        if (yPosition > pageHeight - 100f) {
                            pdfDocument.finishPage(page)
                            currentPage++
                            val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, currentPage).create()
                            page = pdfDocument.startPage(newPageInfo)
                            canvas = page.canvas
                            yPosition = 50f
                        }
                        canvas.drawText("EGG PRODUCTION", margin, yPosition, headerPaint)
                        yPosition += 25f
                        reportData.eggProduction.take(30).forEach { egg ->
                            if (yPosition > pageHeight - 100f) {
                                pdfDocument.finishPage(page)
                                currentPage++
                                val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, currentPage).create()
                                page = pdfDocument.startPage(newPageInfo)
                                canvas = page.canvas
                                yPosition = 50f
                            }
                            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(egg.date))
                            canvas.drawText("$date - Total: ${egg.totalEggs}, Healthy: ${egg.healthyEggs}, Broken: ${egg.brokenEggs}", margin, yPosition, paint)
                            yPosition += lineHeight
                        }
                        yPosition += 20f
                    }
                    
                    // Feeding Schedules
                    if (reportData.feedingSchedules.isNotEmpty()) {
                        if (yPosition > pageHeight - 100f) {
                            pdfDocument.finishPage(page)
                            currentPage++
                            val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, currentPage).create()
                            page = pdfDocument.startPage(newPageInfo)
                            canvas = page.canvas
                            yPosition = 50f
                        }
                        canvas.drawText("FEEDING SCHEDULES", margin, yPosition, headerPaint)
                        yPosition += 25f
                        reportData.feedingSchedules.take(30).forEach { feed ->
                            if (yPosition > pageHeight - 100f) {
                                pdfDocument.finishPage(page)
                                currentPage++
                                val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, currentPage).create()
                                page = pdfDocument.startPage(newPageInfo)
                                canvas = page.canvas
                                yPosition = 50f
                            }
                            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(feed.scheduledAt))
                            val status = if (feed.isCompleted) "Completed" else "Pending"
                            canvas.drawText("$date - ${feed.feedType} (${feed.quantity}) for ${feed.targetGroup} - $status", margin, yPosition, paint)
                            yPosition += lineHeight
                        }
                    }
                }
                com.bisu.chickcare.backend.repository.ReportCategory.FINANCIAL -> {
                    // Expenses
                    if (reportData.expenses.isNotEmpty()) {
                        if (yPosition > pageHeight - 100f) {
                            pdfDocument.finishPage(page)
                            currentPage++
                            val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, currentPage).create()
                            page = pdfDocument.startPage(newPageInfo)
                            canvas = page.canvas
                            yPosition = 50f
                        }
                        canvas.drawText("EXPENSES", margin, yPosition, headerPaint)
                        yPosition += 25f
                        var totalAmount = 0.0
                        reportData.expenses.take(50).forEach { exp ->
                            if (yPosition > pageHeight - 100f) {
                                pdfDocument.finishPage(page)
                                currentPage++
                                val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, currentPage).create()
                                page = pdfDocument.startPage(newPageInfo)
                                canvas = page.canvas
                                yPosition = 50f
                            }
                            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(exp.date))
                            totalAmount += exp.amount
                            canvas.drawText("$date - ${exp.category}: ${exp.amount} (${exp.paymentMethod})", margin, yPosition, paint)
                            yPosition += lineHeight
                        }
                        yPosition += 20f
                        canvas.drawText("TOTAL EXPENSES: $totalAmount", margin, yPosition, headerPaint)
                    }
                }
                com.bisu.chickcare.backend.repository.ReportCategory.COMPREHENSIVE -> {
                    // All data sections (similar to CSV but formatted for PDF)
                    // Health Records
                    if (reportData.healthRecords.isNotEmpty()) {
                        if (yPosition > pageHeight - 100f) {
                            pdfDocument.finishPage(page)
                            currentPage++
                            val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, currentPage).create()
                            page = pdfDocument.startPage(newPageInfo)
                            canvas = page.canvas
                            yPosition = 50f
                        }
                        canvas.drawText("HEALTH RECORDS", margin, yPosition, headerPaint)
                        yPosition += 25f
                        reportData.healthRecords.take(15).forEach { record ->
                            if (yPosition > pageHeight - 100f) {
                                pdfDocument.finishPage(page)
                                currentPage++
                                val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, currentPage).create()
                                page = pdfDocument.startPage(newPageInfo)
                                canvas = page.canvas
                                yPosition = 50f
                            }
                            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(record.date))
                            canvas.drawText("$date - ${record.chickenName}: ${record.condition}", margin, yPosition, paint)
                            yPosition += lineHeight
                        }
                        yPosition += 20f
                    }
                    
                    // Add other sections similarly...
                    // (Egg Production, Expenses, etc.)
                }
            }
            
            pdfDocument.finishPage(page)
            
            FileOutputStream(file).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            pdfDocument.close()
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            
            withContext(Dispatchers.Main) {
                onSuccess(uri)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError("Failed to export PDF: ${e.message}")
            }
        }
    }
    
    /**
     * Export a single detection entry as PDF with photos and treatment info
     * @param context The context to use
     * @param detection The detection entry to export
     * @param onSuccess Callback when export succeeds (provides file URI)
     * @param onError Callback when export fails
     */
    suspend fun exportSingleDetectionAsPDF(
        context: Context,
        detection: DetectionEntry,
        onSuccess: (Uri) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
            val fileName = "chickcare_detection_${dateFormat.format(Date(detection.timestamp))}.pdf"
            val file = File(context.getExternalFilesDir(null), fileName)
            
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas
            var yPosition = 50f
            val margin = 50f
            val pageHeight = 842f
            var currentPage = 1
            
            val paint = Paint().apply {
                isAntiAlias = true
                textSize = 12f
            }
            val titlePaint = Paint().apply {
                isAntiAlias = true
                textSize = 20f
                isFakeBoldText = true
            }
            val headerPaint = Paint().apply {
                isAntiAlias = true
                textSize = 14f
                isFakeBoldText = true
            }
            
            // Title
            canvas.drawText("ChickCare Detection Report", margin, yPosition, titlePaint)
            yPosition += 30f
            canvas.drawText("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}", margin, yPosition, paint)
            yPosition += 40f
            
            // Detection Information
            canvas.drawText("Detection Information", margin, yPosition, headerPaint)
            yPosition += 25f
            
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(detection.timestamp))
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(detection.timestamp))
            val status = if (detection.isHealthy) "Healthy" else "Infected"
            val confidence = "${(detection.confidence * 100).toInt()}%"
            
            canvas.drawText("Date: $date", margin, yPosition, paint)
            yPosition += 20f
            canvas.drawText("Time: $time", margin, yPosition, paint)
            yPosition += 20f
            canvas.drawText("Result: ${detection.result}", margin, yPosition, paint)
            yPosition += 20f
            canvas.drawText("Status: $status", margin, yPosition, paint)
            yPosition += 20f
            canvas.drawText("Confidence: $confidence", margin, yPosition, paint)
            yPosition += 20f
            
            if (!detection.location.isNullOrEmpty()) {
                canvas.drawText("Location: ${detection.location}", margin, yPosition, paint)
                yPosition += 20f
            }
            
            yPosition += 20f
            
            // Treatment Information
            if (detection.treatment != null) {
                if (yPosition > pageHeight - 150f) {
                    pdfDocument.finishPage(page)
                    currentPage++
                    val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, currentPage).create()
                    page = pdfDocument.startPage(newPageInfo)
                    canvas = page.canvas
                    yPosition = 50f
                }
                
                canvas.drawText("Treatment Information", margin, yPosition, headerPaint)
                yPosition += 25f
                canvas.drawText("Treatment: ${detection.treatment}", margin, yPosition, paint)
                yPosition += 20f
                
                if (detection.treatmentDate != null) {
                    val treatmentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(detection.treatmentDate))
                    canvas.drawText("Date Given: $treatmentDate", margin, yPosition, paint)
                    yPosition += 20f
                }
                
                if (detection.nextDoseDate != null) {
                    val nextDoseDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(detection.nextDoseDate))
                    val isOverdue = detection.nextDoseDate < System.currentTimeMillis()
                    canvas.drawText("Next Dose: $nextDoseDate${if (isOverdue) " (OVERDUE)" else ""}", margin, yPosition, paint)
                    yPosition += 20f
                }
                
                if (!detection.treatmentNotes.isNullOrEmpty()) {
                    canvas.drawText("Notes: ${detection.treatmentNotes}", margin, yPosition, paint)
                    yPosition += 20f
                }
                
                yPosition += 20f
            }
            
            // Recommendations
            if (detection.recommendations.isNotEmpty()) {
                if (yPosition > pageHeight - 100f) {
                    pdfDocument.finishPage(page)
                    currentPage++
                    val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, currentPage).create()
                    page = pdfDocument.startPage(newPageInfo)
                    canvas = page.canvas
                    yPosition = 50f
                }
                
                canvas.drawText("Recommendations", margin, yPosition, headerPaint)
                yPosition += 25f
                detection.recommendations.forEach { recommendation ->
                    if (yPosition > pageHeight - 50f) {
                        pdfDocument.finishPage(page)
                        currentPage++
                        val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, currentPage).create()
                        page = pdfDocument.startPage(newPageInfo)
                        canvas = page.canvas
                        yPosition = 50f
                    }
                    canvas.drawText("• $recommendation", margin + 10f, yPosition, paint)
                    yPosition += 20f
                }
            }
            
            pdfDocument.finishPage(page)
            
            FileOutputStream(file).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            pdfDocument.close()
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            
            withContext(Dispatchers.Main) {
                onSuccess(uri)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError("Failed to export PDF: ${e.message}")
            }
        }
    }
}

