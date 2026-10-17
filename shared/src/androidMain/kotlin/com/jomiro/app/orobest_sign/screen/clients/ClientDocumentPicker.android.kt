package com.jomiro.app.orobest_sign.screen.clients

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientFile
import java.io.ByteArrayOutputStream
import java.io.File

@Composable
actual fun rememberClientDocumentPicker(onSelected: (List<ClientFile>) -> Unit): (ClientDocumentSource) -> Unit {
    val context = LocalContext.current
    val currentCallback by rememberUpdatedState(onSelected)
    var captureFile by remember { mutableStateOf<File?>(null) }
    var captureUri by remember { mutableStateOf<Uri?>(null) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val file = captureFile
        val uri = captureUri
        captureFile = null
        captureUri = null
        if (saved && file != null && uri != null) {
            val picked = context.contentResolver.openInputStream(uri)?.use { compressImage(it, "cedula.jpg") }
            currentCallback(listOf(picked ?: invalidFile()))
        } else currentCallback(emptyList())
        file?.delete()
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        currentCallback(uris.take(11).map { uri -> runCatching { readImage(context, uri) }.getOrNull() ?: invalidFile() })
    }
    val pdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        currentCallback(uris.take(11).map { uri -> runCatching { readDocument(context, uri) }.getOrNull() ?: invalidFile() })
    }

    return remember(context, camera, gallery, pdf) {
        { source ->
            when (source) {
                ClientDocumentSource.Camera -> {
                    val directory = File(context.cacheDir, "client-captures").apply { mkdirs() }
                    val output = File.createTempFile("client-document-", ".jpg", directory)
                    val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", output)
                    captureFile = output
                    captureUri = uri
                    camera.launch(uri)
                }
                ClientDocumentSource.Gallery -> gallery.launch("image/*")
                ClientDocumentSource.Pdf -> pdf.launch(arrayOf("application/pdf", "image/jpeg", "image/png", "text/plain", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
            }
        }
    }
}

private fun readImage(context: Context, uri: Uri): ClientFile? {
    val name = displayName(context, uri) ?: "foto-cliente.jpg"
    return context.contentResolver.openInputStream(uri)?.use { compressImage(it, name) }
}

private fun invalidFile() = ClientFile(byteArrayOf(), "Archivo no disponible", "application/octet-stream")

private fun readDocument(context: Context, uri: Uri): ClientFile? {
    val name = displayName(context, uri) ?: "documento.pdf"
    val mime = when (name.substringAfterLast('.').lowercase()) {
        "pdf" -> "application/pdf"
        "jpg", "jpeg" -> return readImage(context, uri)
        "png" -> return readImage(context, uri)
        "txt" -> "text/plain"
        "doc" -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        else -> return null
    }
    val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            if (total > 10 * 1024 * 1024) return null
            output.write(buffer, 0, count)
        }
        output.toByteArray()
    } ?: return null
    if (bytes.isEmpty()) return null
    if (mime == "application/pdf" && (bytes.size < 5 || !bytes.decodeToString(endIndex = 5).startsWith("%PDF-"))) return null
    return ClientFile(bytes, name, mime)
}

private fun displayName(context: Context, uri: Uri): String? = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
    if (!cursor.moveToFirst()) null else cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
}

private fun compressImage(input: java.io.InputStream, filename: String): ClientFile? {
    val encoded = input.readBytes()
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(encoded, 0, encoded.size, bounds)
    if (bounds.outWidth < 1 || bounds.outHeight < 1) return null
    val sample = maxOf(1, maxOf(bounds.outWidth, bounds.outHeight) / 2400)
    val bitmap = BitmapFactory.decodeByteArray(encoded, 0, encoded.size, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
    val scaled = if (maxOf(bitmap.width, bitmap.height) > 2400) {
        val scale = 2400f / maxOf(bitmap.width, bitmap.height)
        Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true).also { if (it !== bitmap) bitmap.recycle() }
    } else bitmap
    val output = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, 84, output)
    scaled.recycle()
    val cleanName = filename.substringBeforeLast('.', filename).take(150).ifBlank { "foto-cliente" } + ".jpg"
    return ClientFile(output.toByteArray(), cleanName, "image/jpeg")
}
