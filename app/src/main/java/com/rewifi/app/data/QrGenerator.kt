package com.rewifi.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.rewifi.app.R

data class QrConfig(
    val isDark: Boolean = false,
    val showLogo: Boolean = true,
    val showSsid: Boolean = true,
    val showSecurity: Boolean = true,
    val showBranding: Boolean = true
)

enum class PrintCardSize(val label: String, val qrScale: Float) {
    SMALL("Small Card", 0.55f),
    MEDIUM("Medium", 0.72f),
    LARGE("Large Poster", 0.86f)
}

object QrGenerator {

    private const val BLACK = 0xFF000000.toInt()
    private const val WHITE = 0xFFFFFFFF.toInt()
    private const val DARK_BG = 0xFF121212.toInt()
    private const val DARK_FG = 0xFFEEEEEE.toInt()

    /** Standard Android WIFI QR payload — scan with camera to join instantly. */
    fun payload(ssid: String, password: String): String {
        fun esc(s: String) = s.replace("\\", "\\\\").replace(";", "\\;")
            .replace(",", "\\,").replace(":", "\\:").replace("\"", "\\\"")
        return if (password.isBlank()) {
            "WIFI:T:nopass;S:${esc(ssid)};;"
        } else {
            "WIFI:T:WPA;S:${esc(ssid)};P:${esc(password)};;"
        }
    }

    fun build(context: Context, ssid: String, password: String, size: Int = 600): ImageBitmap =
        buildCustomBitmap(context, ssid, password, QrConfig(), size).asImageBitmap()

    fun buildBitmap(context: Context, ssid: String, password: String, size: Int = 600): Bitmap =
        buildCustomBitmap(context, ssid, password, QrConfig(), size)

    /** Generate raw QR bitmap respecting theme & center logo configuration. */
    fun buildCustomBitmap(
        context: Context,
        ssid: String,
        password: String,
        config: QrConfig,
        size: Int = 600
    ): Bitmap {
        val hints = mapOf(
            // High error correction (~30%) ensures center logo doesn't break reading
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val matrix = QRCodeWriter().encode(payload(ssid, password), BarcodeFormat.QR_CODE, size, size, hints)
        val w = matrix.width
        val h = matrix.height

        val fgColor = if (config.isDark) DARK_FG else BLACK
        val bgColor = if (config.isDark) DARK_BG else WHITE

        val pixels = IntArray(w * h)
        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                pixels[row + x] = if (matrix[x, y]) fgColor else bgColor
            }
        }
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp.setPixels(pixels, 0, w, 0, 0, w, h)

        if (config.showLogo) {
            drawCenterLogo(context, bmp, config.isDark)
        }
        return bmp
    }

    /**
     * Build high-resolution export card / poster bitmap (e.g. 2048 x 2560 px).
     * Rendered directly on Canvas without blurry bitmap upscaling.
     */
    fun buildCardBitmap(
        context: Context,
        ssid: String,
        password: String,
        config: QrConfig,
        canvasWidth: Int = 2048,
        printSize: PrintCardSize = PrintCardSize.MEDIUM
    ): Bitmap {
        val width = canvasWidth.coerceAtLeast(1200)
        val height = (width * 1.25f).toInt()

        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val bgColor = if (config.isDark) DARK_BG else WHITE
        val textColor = if (config.isDark) WHITE else BLACK
        val textSecondaryColor = if (config.isDark) 0xFFAAAAAA.toInt() else 0xFF666666.toInt()
        val borderColor = if (config.isDark) 0xFF333333.toInt() else 0xFFCCCCCC.toInt()

        // Background
        canvas.drawColor(bgColor)

        // Outer brutalist border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = width * 0.012f
            color = borderColor
        }
        val margin = width * 0.04f
        canvas.drawRect(margin, margin, width - margin, height - margin, borderPaint)

        var currentY = margin + (width * 0.06f)

        // 1. Branding Header
        if (config.showBranding) {
            val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textColor
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                textSize = width * 0.045f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("REWIFI", width / 2f, currentY + brandPaint.textSize, brandPaint)

            val subBrandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textSecondaryColor
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = width * 0.022f
                textAlign = Paint.Align.CENTER
            }
            currentY += brandPaint.textSize + (width * 0.015f)
            canvas.drawText("SECURE WIFI ACCESS", width / 2f, currentY + subBrandPaint.textSize, subBrandPaint)
            currentY += subBrandPaint.textSize + (width * 0.04f)
        } else {
            currentY += width * 0.05f
        }

        // 2. Large QR Code
        val availableQrWidth = (width * printSize.qrScale).toInt()
        val rawQr = buildCustomBitmap(context, ssid, password, config, availableQrWidth)

        val qrLeft = (width - availableQrWidth) / 2f
        val qrTop = currentY
        val qrDst = RectF(qrLeft, qrTop, qrLeft + availableQrWidth, qrTop + availableQrWidth)

        val qrPaint = Paint().apply { isFilterBitmap = false } // Keep crisp edges
        canvas.drawBitmap(rawQr, null, qrDst, qrPaint)

        currentY = qrTop + availableQrWidth + (width * 0.06f)

        // 3. SSID Label
        if (config.showSsid) {
            val ssidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textColor
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = width * 0.052f
                textAlign = Paint.Align.CENTER
            }
            // Truncate if extremely long
            val displaySsid = if (ssid.length > 24) ssid.take(22) + "…" else ssid
            canvas.drawText(displaySsid, width / 2f, currentY + ssidPaint.textSize, ssidPaint)
            currentY += ssidPaint.textSize + (width * 0.02f)
        }

        // 4. Security Label
        if (config.showSecurity) {
            val secText = if (password.isBlank()) "SECURITY: OPEN" else "SECURITY: WPA2 / WPA3"
            val secPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textSecondaryColor
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                textSize = width * 0.025f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(secText, width / 2f, currentY + secPaint.textSize, secPaint)
            currentY += secPaint.textSize + (width * 0.025f)
        }

        // 5. Instruction subtitle
        val instructPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = width * 0.028f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("SCAN TO CONNECT", width / 2f, currentY + instructPaint.textSize, instructPaint)

        return bmp
    }

    /** Center logo badge inside QR code. */
    private fun drawCenterLogo(context: Context, bmp: Bitmap, isDark: Boolean) {
        val canvas = Canvas(bmp)
        val w = bmp.width.toFloat()
        val cx = w / 2f
        val cy = w / 2f
        val half = w * 0.22f / 2f          // badge half-side (~22% of side)
        val radius = w * 0.045f

        val badgeBg = if (isDark) DARK_BG else WHITE
        val badgeBorder = if (isDark) WHITE else BLACK

        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = badgeBg }
        canvas.drawRoundRect(cx - half, cy - half, cx + half, cy + half, radius, radius, fill)

        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = w * 0.015f
            color = badgeBorder
        }
        canvas.drawRoundRect(cx - half, cy - half, cx + half, cy + half, radius, radius, border)

        val logo = ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground)
        if (logo != null) {
            val ls = (half * 1.45f).toInt()
            logo.setBounds(
                (cx - ls / 2f).toInt(), (cy - ls / 2f).toInt(),
                (cx + ls / 2f).toInt(), (cy + ls / 2f).toInt()
            )
            logo.draw(canvas)
        }
    }
}
