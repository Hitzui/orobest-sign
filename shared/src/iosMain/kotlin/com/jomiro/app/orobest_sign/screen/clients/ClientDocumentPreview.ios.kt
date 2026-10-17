@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.jomiro.app.orobest_sign.screen.clients

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.interop.UIKitView
import com.composables.ui.components.Text
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Image
import platform.Foundation.NSData
import platform.Foundation.create
import platform.PDFKit.PDFDocument
import platform.PDFKit.PDFView
import platform.Foundation.NSURL
import platform.WebKit.WKWebView
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientFile

internal actual fun decodeClientDocumentImage(bytes: ByteArray): ImageBitmap =
    Image.makeFromEncoded(bytes).use { it.toComposeImageBitmap() }

@Composable
internal actual fun ClientDocumentPreviewBackHandler(onClose: () -> Unit) = Unit

@Composable
internal actual fun ClientOfficePreview(file: ClientFile, modifier: Modifier) {
    val data = remember(file) {
        file.bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = file.bytes.size.toULong()) }
    }
    UIKitView(
        factory = { WKWebView().apply { loadData(data, file.mimeType, "UTF-8", NSURL.fileURLWithPath("/")) } },
        modifier = modifier,
        onRelease = { it.stopLoading() },
    )
}

@Composable
internal actual fun ClientPdfPreview(bytes: ByteArray, modifier: Modifier) {
    val document = remember(bytes) {
        if (bytes.isEmpty()) null else bytes.usePinned {
            PDFDocument(data = NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()))
        }
    }
    if (document == null || document.isLocked || document.pageCount == 0uL) {
        Text("No se pudo mostrar el PDF. Puede estar dañado o protegido con contraseña.", modifier)
    } else {
        UIKitView(
            factory = { PDFView().apply { autoScales = true; this.document = document } },
            modifier = modifier,
            update = { it.document = document },
            onRelease = { it.document = null },
        )
    }
}
