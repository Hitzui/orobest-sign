package com.jomiro.app.orobest_sign.screen.clients

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.ui.components.Button
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.Text
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientFile
import java.awt.Desktop
import java.nio.file.Files
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image

internal actual fun decodeClientDocumentImage(bytes: ByteArray): ImageBitmap =
    Image.makeFromEncoded(bytes).use { it.toComposeImageBitmap() }

@Composable
internal actual fun ClientDocumentPreviewBackHandler(onClose: () -> Unit) = Unit

@Composable
internal actual fun ClientPdfPreview(bytes: ByteArray, modifier: Modifier) {
    ExternalDocumentPreview(bytes, ".pdf", modifier)
}

@Composable
internal actual fun ClientOfficePreview(file: ClientFile, modifier: Modifier) {
    ExternalDocumentPreview(file.bytes, if (file.mimeType == "application/msword") ".doc" else ".docx", modifier)
}

@Composable
private fun ExternalDocumentPreview(bytes: ByteArray, suffix: String, modifier: Modifier) {
    val scope = rememberCoroutineScope()
    var opening by remember(bytes) { mutableStateOf(false) }
    var message by remember(bytes) { mutableStateOf("El documento está listo para abrirse con el visor de esta computadora.") }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(message, fontSize = 14.sp)
        Button(onClick = {
            opening = true
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        check(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN))
                        val file = Files.createTempFile("orobest-document-", suffix).toFile()
                        file.deleteOnExit()
                        file.writeBytes(bytes)
                        try {
                            Desktop.getDesktop().open(file)
                        } catch (error: Exception) {
                            file.delete()
                            throw error
                        }
                    }
                    message = "Documento abierto en el visor de la computadora. Puedes volver a OroBest Sign cuando termines."
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    message = "No se pudo abrir el documento. Verifica que tengas un lector compatible predeterminado."
                } finally {
                    opening = false
                }
            }
        }, enabled = !opening, style = ButtonStyle.Primary) {
            Text(if (opening) "Abriendo…" else "Abrir documento")
        }
    }
}
