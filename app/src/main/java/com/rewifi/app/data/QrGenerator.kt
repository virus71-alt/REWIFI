package com.rewifi.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.rewifi.app.R

object QrGenerator {

    private const val BLACK = 0xFF000000.toInt()
    private const val WHITE = 0xFFFFFFFF.toInt()

    /** Standard Android WIFI QR payload — scan with the camera to join instantly. */
    private fun payload(ssid: String, password: String): String {
        fun esc(s: String) = s.replace("\\", "\\\\").replace(";", "\\;")
            .replace(",", "\\,").replace(":", "\\:").replace("\"", "\\\"")
        return "WIFI:T:WPA;S:${esc(ssid)};P:${esc(password)};;"
    }

    fun build(context: Context, ssid: String, password: String, size: Int = 600): ImageBitmap =
        buildBitmap(context, ssid, password, size).asImageBitmap()

    /** Raw [Bitmap] of the WiFi QR with the REWIFI logo stamped in the center. */
    fun buildBitmap(context: Context, ssid: String, password: String, size: Int = 600): Bitmap {
        val hints = mapOf(
            // High recovery (~30%) so the center logo never breaks scanning.
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val matrix = QRCodeWriter().encode(payload(ssid, password), BarcodeFormat.QR_CODE, size, size, hints)
        val w = matrix.width
        val h = matrix.height
        val pixels = IntArray(w * h)
        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                pixels[row + x] = if (matrix[x, y]) BLACK else WHITE
            }
        }
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
        drawCenterLogo(context, bmp)
        return bmp
    }

    /**
     * Stamps a brutalist logo badge over the center of the QR. The badge covers a
     * small square (~24% of the side ≈ 6% of the area), well inside the ~30% that
     * ErrorCorrectionLevel.H can recover — so the code still scans cleanly.
     */
    private fun drawCenterLogo(context: Context, bmp: Bitmap) {
        val canvas = Canvas(bmp)
        val w = bmp.width.toFloat()
        val cx = w / 2f
        val cy = w / 2f
        val half = w * 0.24f / 2f          // badge half-side
        val radius = w * 0.05f             // brutalist rounded square

        // White badge with a hard black border.
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = WHITE }
        canvas.drawRoundRect(cx - half, cy - half, cx + half, cy + half, radius, radius, fill)
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = w * 0.012f
            color = BLACK
        }
        canvas.drawRoundRect(cx - half, cy - half, cx + half, cy + half, radius, radius, border)

        // App icon, centered inside the badge.
        val logo = ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground)
        if (logo != null) {
            val ls = (half * 1.45f).toInt()   // ~72% of the badge
            logo.setBounds(
                (cx - ls / 2f).toInt(), (cy - ls / 2f).toInt(),
                (cx + ls / 2f).toInt(), (cy + ls / 2f).toInt()
            )
            logo.draw(canvas)
        }
    }
}
