package com.jomiro.app.orobest_sign.screen.signature.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints

/** Gira solo la hoja, no la Activity. Compose también transforma las coordenadas táctiles. */
@Composable
internal fun SidewaysSignatureLayout(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val turn = height > width
        val child = measurables.single().measure(Constraints.fixed(if (turn) height else width, if (turn) width else height))
        layout(width, height) {
            child.placeWithLayer((width - child.width) / 2, (height - child.height) / 2) {
                rotationZ = if (turn) 90f else 0f
                transformOrigin = TransformOrigin.Center
            }
        }
    }
}

/** Mide el diálogo en el espacio horizontal antes de girarlo para evitar recortes. */
internal fun Modifier.sidewaysDialog(turned: Boolean): Modifier = if (!turned) this else layout { measurable, constraints ->
    val child = measurable.measure(Constraints(
        minWidth = constraints.minHeight, maxWidth = constraints.maxHeight,
        minHeight = constraints.minWidth, maxHeight = constraints.maxWidth,
    ))
    layout(child.height, child.width) {
        child.placeWithLayer((child.height - child.width) / 2, (child.width - child.height) / 2) {
            rotationZ = 90f
            transformOrigin = TransformOrigin.Center
        }
    }
}
