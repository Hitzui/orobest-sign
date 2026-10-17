package com.jomiro.app.orobest_sign.screen.clients

import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.Lucide
import com.composables.ui.components.Button
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.Icon
import com.composables.ui.components.IconButton
import com.composables.ui.components.IndeterminateProgressIndicator
import com.composables.ui.components.Text
import com.composables.ui.theme.backgroundColor
import com.composables.ui.theme.colors
import com.composables.ui.theme.panelColor
import com.composeunstyled.theme.Theme
import com.jomiro.app.orobest_sign.screen.clients.uistate.ClientsUiState
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun ClientDocumentPreviewScreen(state: ClientsUiState, onClose: () -> Unit, onRetry: () -> Unit) {
    val document = state.previewDocument ?: return
    ClientDocumentPreviewBackHandler(onClose)
    Column(
        Modifier.fillMaxSize().background(Theme[colors][backgroundColor]).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconButton(onClick = onClose, style = ButtonStyle.Outlined, modifier = Modifier.size(46.dp)) {
                Icon(Lucide.ChevronLeft, contentDescription = "Volver a documentos")
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(document.name, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(state.selectedClient?.displayName.orEmpty(), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            when {
                state.previewLoading -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    IndeterminateProgressIndicator(Modifier.fillMaxWidth())
                    Text("Descargando documento…", fontSize = 14.sp)
                }
                state.previewError != null -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(state.previewError, fontSize = 14.sp)
                    Button(onClick = onRetry, style = ButtonStyle.Primary) { Text("Reintentar") }
                }
                state.previewFile != null -> {
                    val file = state.previewFile
                    when (file.mimeType) {
                        "application/pdf" -> ClientPdfPreview(file.bytes, Modifier.fillMaxSize())
                        "image/jpeg", "image/png" -> ClientImagePreview(file.bytes, file.fileName, Modifier.fillMaxSize())
                        "text/plain" -> Text(remember(file) { file.bytes.decodeToString() }, Modifier.fillMaxSize().verticalScroll(rememberScrollState()), fontSize = 14.sp)
                        else -> ClientOfficePreview(file, Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}

@Composable
private fun ClientImagePreview(bytes: ByteArray, name: String, modifier: Modifier) {
    var failed by remember(bytes) { mutableStateOf(false) }
    val image by produceState<ImageBitmap?>(null, bytes) {
        try {
            value = withContext(Dispatchers.Default) { decodeClientDocumentImage(bytes) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            failed = true
        }
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        when {
            image != null -> ZoomableDocumentImage(requireNotNull(image), name, Modifier.fillMaxSize())
            failed -> Text("No se pudo mostrar la imagen. El archivo puede estar dañado.", fontSize = 14.sp)
            else -> IndeterminateProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}

@Composable
internal fun ZoomableDocumentImage(image: ImageBitmap, description: String, modifier: Modifier = Modifier) {
    var scale by remember(image) { mutableStateOf(1f) }
    var offset by remember(image) { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    fun clamp(position: Offset): Offset = Offset(
        position.x.coerceIn(-viewport.width * (scale - 1) / 2, viewport.width * (scale - 1) / 2),
        position.y.coerceIn(-viewport.height * (scale - 1) / 2, viewport.height * (scale - 1) / 2),
    )
    val transform = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(1f, 5f)
        offset = clamp(offset + pan)
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier.weight(1f).fillMaxWidth().background(Theme[colors][panelColor])
                .clipToBounds().onSizeChanged { viewport = it }.transformable(transform),
        ) {
            Image(
                image, description,
                Modifier.fillMaxSize().graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
                contentScale = ContentScale.Fit,
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { scale = (scale - 0.5f).coerceAtLeast(1f); offset = clamp(offset) }, enabled = scale > 1f, style = ButtonStyle.Outlined) { Text("Alejar") }
            Button(onClick = { scale = 1f; offset = Offset.Zero }, style = ButtonStyle.Ghost, modifier = Modifier.weight(1f)) { Text("Restablecer", fontSize = 12.sp) }
            Button(onClick = { scale = (scale + 0.5f).coerceAtMost(5f) }, enabled = scale < 5f, style = ButtonStyle.Outlined) { Text("Ampliar") }
        }
    }
}

internal expect fun decodeClientDocumentImage(bytes: ByteArray): ImageBitmap

@Composable
internal expect fun ClientPdfPreview(bytes: ByteArray, modifier: Modifier = Modifier)

@Composable
internal expect fun ClientOfficePreview(file: ClientFile, modifier: Modifier = Modifier)

@Composable
internal expect fun ClientDocumentPreviewBackHandler(onClose: () -> Unit)
