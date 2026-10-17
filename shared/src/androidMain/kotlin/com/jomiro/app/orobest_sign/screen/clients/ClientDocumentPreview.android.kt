package com.jomiro.app.orobest_sign.screen.clients

import android.graphics.Bitmap
import android.content.Intent
import androidx.core.content.FileProvider
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.ui.components.Button
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.IndeterminateProgressIndicator
import com.composables.ui.components.Text
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientFile
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

internal actual fun decodeClientDocumentImage(bytes: ByteArray): ImageBitmap {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    require(bounds.outWidth > 0 && bounds.outHeight > 0)
    val options = BitmapFactory.Options().apply {
        inSampleSize = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / inSampleSize > 3200) inSampleSize *= 2
    }
    return requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)).asImageBitmap()
}

@Composable
internal actual fun ClientDocumentPreviewBackHandler(onClose: () -> Unit) {
    BackHandler(onBack = onClose)
}

@Composable
internal actual fun ClientOfficePreview(file: ClientFile, modifier: Modifier) {
    val context = LocalContext.current
    var error by remember(file) { mutableStateOf<String?>(null) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(error ?: "Abre este documento con una aplicación compatible instalada en el dispositivo.", fontSize = 14.sp)
        Button(onClick = {
            try {
                val directory = File(context.cacheDir, "client-captures").apply { mkdirs() }
                val suffix = if (file.mimeType == "application/msword") ".doc" else ".docx"
                val output = File.createTempFile("document-", suffix, directory)
                output.writeBytes(file.bytes)
                val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", output)
                val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, file.mimeType)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(intent)
                } catch (failure: Exception) {
                    output.delete()
                    throw failure
                }
            } catch (_: Exception) {
                error = "No se pudo abrir el documento. Comprueba que tengas un lector DOC/DOCX instalado."
            }
        }, style = ButtonStyle.Primary) { Text("Abrir documento") }
    }
}

@Composable
internal actual fun ClientPdfPreview(bytes: ByteArray, modifier: Modifier) {
    val context = LocalContext.current
    var page by remember(bytes) { mutableIntStateOf(0) }
    var pages by remember(bytes) { mutableIntStateOf(0) }
    var image by remember(bytes) { mutableStateOf<ImageBitmap?>(null) }
    var error by remember(bytes) { mutableStateOf<String?>(null) }
    var attempt by remember(bytes) { mutableIntStateOf(0) }
    LaunchedEffect(bytes, page, attempt) {
        image = null
        error = null
        try {
            val result = withContext(Dispatchers.IO) {
                val file = File.createTempFile("client-preview-", ".pdf", context.cacheDir)
                try {
                    file.writeBytes(bytes)
                    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                        PdfRenderer(descriptor).use { renderer ->
                            renderer.openPage(page).use { pdfPage ->
                                val ratio = minOf(1800f / pdfPage.width, 2400f / pdfPage.height)
                                val bitmap = Bitmap.createBitmap(
                                    (pdfPage.width * ratio).roundToInt().coerceAtLeast(1),
                                    (pdfPage.height * ratio).roundToInt().coerceAtLeast(1),
                                    Bitmap.Config.ARGB_8888,
                                )
                                bitmap.eraseColor(Color.WHITE)
                                try {
                                    pdfPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                    bitmap.asImageBitmap() to renderer.pageCount
                                } catch (failure: Exception) {
                                    bitmap.recycle()
                                    throw failure
                                }
                            }
                        }
                    }
                } finally {
                    file.delete()
                }
            }
            image = result.first
            pages = result.second
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            error = "No se pudo mostrar el PDF. Puede estar dañado o protegido con contraseña."
        }
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            when {
                image != null -> ZoomableDocumentImage(requireNotNull(image), "Página ${page + 1} del documento PDF", Modifier.fillMaxSize())
                error != null -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(requireNotNull(error), fontSize = 14.sp)
                    Button(onClick = { attempt++ }, style = ButtonStyle.Outlined) { Text("Reintentar") }
                }
                else -> IndeterminateProgressIndicator(Modifier.fillMaxWidth())
            }
        }
        if (pages > 0) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { page-- }, enabled = page > 0 && image != null, style = ButtonStyle.Outlined) { Text("Anterior") }
            Text("${page + 1} / $pages", fontSize = 13.sp)
            Button(onClick = { page++ }, enabled = page + 1 < pages && image != null, style = ButtonStyle.Outlined) { Text("Siguiente") }
        }
    }
}
