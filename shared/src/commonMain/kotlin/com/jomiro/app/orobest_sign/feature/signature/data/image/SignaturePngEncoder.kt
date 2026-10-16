package com.jomiro.app.orobest_sign.feature.signature.data.image

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.jomiro.app.orobest_sign.core.designsystem.theme.OroBestPalette
import com.jomiro.app.orobest_sign.feature.signature.domain.model.SignaturePngEncoder
import com.jomiro.app.orobest_sign.feature.signature.domain.model.SignatureStroke

internal expect fun ImageBitmap.encodePng(): ByteArray
internal expect fun decodeSignaturePng(bytes: ByteArray): ImageBitmap

internal class DefaultSignaturePngEncoder : SignaturePngEncoder {
    override fun encode(strokes: List<SignatureStroke>): ByteArray {
        require(strokes.any { it.isNotEmpty() })
        val bitmap = ImageBitmap(1600, 800)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap), Size(1600f, 800f)) {
            drawRect(Color.White)
            drawSignatureStrokes(strokes)
        }
        return bitmap.encodePng().also { require(it.size <= 2 * 1024 * 1024) }
    }
}

/** El lienzo y el PNG usan la misma geometría; las guías y controles no se exportan. */
internal fun DrawScope.drawSignatureStrokes(strokes: List<SignatureStroke>) {
    val width = size.width / 400f
    strokes.forEach { points ->
        if (points.isEmpty()) return@forEach
        fun point(index: Int) = Offset(points[index].x * size.width, points[index].y * size.height)
        if (points.size == 1) {
            drawCircle(OroBestPalette.BrandBlue, width / 2f, point(0))
        } else {
            val path = Path().apply {
                val first = point(0)
                moveTo(first.x, first.y)
                for (index in 1..points.lastIndex) {
                    val previous = point(index - 1)
                    val current = point(index)
                    quadraticTo(previous.x, previous.y, (previous.x + current.x) / 2f, (previous.y + current.y) / 2f)
                }
                val last = point(points.lastIndex)
                lineTo(last.x, last.y)
            }
            drawPath(path, OroBestPalette.BrandBlue, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}
