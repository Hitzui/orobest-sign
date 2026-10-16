package com.jomiro.app.orobest_sign.screen.signature.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.ui.components.Text
import com.composables.ui.theme.borderColor
import com.composables.ui.theme.colors
import com.composables.ui.theme.secondaryColor
import com.composeunstyled.theme.Theme
import com.jomiro.app.orobest_sign.feature.signature.data.image.drawSignatureStrokes
import com.jomiro.app.orobest_sign.feature.signature.domain.model.SignaturePoint
import com.jomiro.app.orobest_sign.feature.signature.domain.model.SignatureStroke

@Composable
internal fun SignatureCanvas(
    strokes: List<SignatureStroke>, enabled: Boolean, modifier: Modifier,
    onBegin: (SignaturePoint) -> Unit, onPoint: (SignaturePoint) -> Unit, onEnd: () -> Unit,
) {
    val begin = rememberUpdatedState(onBegin)
    val point = rememberUpdatedState(onPoint)
    val end = rememberUpdatedState(onEnd)
    val guide = Theme[colors][secondaryColor]
    Box(modifier.background(Color.White, RoundedCornerShape(12.dp)).border(1.dp, Theme[colors][borderColor], RoundedCornerShape(12.dp))) {
        Canvas(
            Modifier.fillMaxSize().semantics { contentDescription = "Lienzo para dibujar la firma con el dedo o lápiz" }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        fun normalize(position: Offset) = SignaturePoint(position.x / size.width, position.y / size.height)
                        down.consume()
                        begin.value(normalize(down.position))
                        try {
                            do {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                point.value(normalize(change.position))
                                change.consume()
                            } while (change.pressed)
                        } finally {
                            end.value()
                        }
                    }
                },
        ) {
            drawLine(guide, Offset(size.width * 0.06f, size.height * 0.82f), Offset(size.width * 0.94f, size.height * 0.82f), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
            drawSignatureStrokes(strokes)
        }
        if (strokes.isEmpty()) Text("Firme aquí", Modifier.align(Alignment.Center).padding(8.dp), fontSize = 20.sp)
    }
}
