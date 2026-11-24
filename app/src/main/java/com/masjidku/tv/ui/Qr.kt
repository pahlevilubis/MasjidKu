package com.masjidku.tv.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter

@Composable
fun KodeQr(teks: String, ukuran: Dp, modifier: Modifier = Modifier) {
    val matriks: BitMatrix? = remember(teks) {
        try {
            QRCodeWriter().encode(teks, BarcodeFormat.QR_CODE, 0, 0, mapOf(EncodeHintType.MARGIN to 1))
        } catch (e: Exception) {
            null
        }
    }
    Canvas(modifier.size(ukuran).background(Color.White)) {
        val m = matriks ?: return@Canvas
        val sel = size.width / m.width
        for (y in 0 until m.height) {
            for (x in 0 until m.width) {
                if (m.get(x, y)) {
                    drawRect(Color.Black, topLeft = Offset(x * sel, y * sel), size = Size(sel + 0.6f, sel + 0.6f))
                }
            }
        }
    }
}
