package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.data.db.LectureWithTags
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    fun exportLectureToPdf(context: Context, lectureWithTags: LectureWithTags): File? {
        val lecture = lectureWithTags.lecture
        val pdfDocument = PdfDocument()

        val pageWidth = 595 // A4 width in points (72 dpi)
        val pageHeight = 842 // A4 height in points
        val margin = 40f
        val contentWidth = (pageWidth - margin * 2).toInt()

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas: Canvas = page.canvas

        val titlePaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val metaPaint = TextPaint().apply {
            color = Color.DKGRAY
            textSize = 11f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val tagPaint = TextPaint().apply {
            color = Color.rgb(63, 81, 181) // Indigo
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val bodyPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 12f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val dividerPaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        var currentY = margin

        // Header: App Title
        canvas.drawText("Умный конспект лекции", margin, currentY + 14f, metaPaint)
        currentY += 24f

        // Lecture Title
        val titleLayout = StaticLayout.Builder.obtain(lecture.title, 0, lecture.title.length, titlePaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.2f)
            .build()
        titleLayout.draw(canvas)
        canvas.translate(0f, titleLayout.height.toFloat())
        currentY += titleLayout.height + 10f

        // Date and Metadata
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val dateStr = "Дата записи: ${dateFormat.format(Date(lecture.dateTimestamp))}"
        val durationStr = "Длительность: ${lecture.durationSeconds / 60} мин ${lecture.durationSeconds % 60} сек"
        val tagsStr = if (lectureWithTags.tags.isNotEmpty()) {
            "Предметы: " + lectureWithTags.tags.joinToString { it.name }
        } else {
            "Предметы: Не указаны"
        }

        canvas.drawText(dateStr, margin, currentY, metaPaint)
        currentY += 16f
        canvas.drawText(durationStr, margin, currentY, metaPaint)
        currentY += 16f
        canvas.drawText(tagsStr, margin, currentY, tagPaint)
        currentY += 24f

        // Divider
        canvas.drawLine(margin, currentY, pageWidth - margin, currentY, dividerPaint)
        currentY += 16f

        // Text Content
        val textContent = if (lecture.transcriptionText.isNotBlank()) {
            lecture.transcriptionText
        } else {
            "Текст конспекта пока не расшифрован."
        }

        val paragraphs = textContent.split("\n")

        for (paragraph in paragraphs) {
            val trimmed = paragraph.trim()
            if (trimmed.isEmpty()) continue

            val pLayout = StaticLayout.Builder.obtain(trimmed, 0, trimmed.length, bodyPaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(2f, 1.25f)
                .build()

            if (currentY + pLayout.height > pageHeight - margin) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                currentY = margin
            }

            pLayout.draw(canvas)
            canvas.translate(0f, pLayout.height.toFloat() + 12f)
            currentY += pLayout.height + 12f
        }

        pdfDocument.finishPage(page)

        val exportsDir = File(context.cacheDir, "exports")
        if (!exportsDir.exists()) exportsDir.mkdirs()

        val safeTitle = lecture.title.replace(Regex("[^a-zA-Z0-9а-яА-Я_]"), "_")
        val pdfFile = File(exportsDir, "Конспект_${safeTitle}_${lecture.id}.pdf")

        return try {
            val fos = FileOutputStream(pdfFile)
            pdfDocument.writeTo(fos)
            fos.close()
            pdfDocument.close()
            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    fun shareLectureText(context: Context, lectureWithTags: LectureWithTags) {
        val lecture = lectureWithTags.lecture
        val text = StringBuilder().apply {
            append("📖 ${lecture.title}\n")
            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            append("📅 ${dateFormat.format(Date(lecture.dateTimestamp))}\n")
            if (lectureWithTags.tags.isNotEmpty()) {
                append("🏷️ ${lectureWithTags.tags.joinToString { it.name }}\n")
            }
            append("\n-----------------------------------\n\n")
            append(lecture.transcriptionText)
        }.toString()

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Поделиться конспектом")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }
}
