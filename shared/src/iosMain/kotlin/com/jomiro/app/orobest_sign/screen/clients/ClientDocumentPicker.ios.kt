package com.jomiro.app.orobest_sign.screen.clients

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.window.LocalUIViewController
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientFile
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSObject
import platform.Foundation.NSURL
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceTypeCamera
import platform.UIKit.UIImagePickerControllerSourceTypePhotoLibrary
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerModeImport
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage.drawInRect
import platform.UIKit.UIViewController.dismissViewControllerAnimated
import platform.UIKit.UIViewController.presentViewController
import platform.UIKit.UIImagePickerController.allowsEditing
import platform.UIKit.UIImagePickerController.delegate
import platform.UIKit.UIImagePickerController.mediaTypes
import platform.UIKit.UIImagePickerController.sourceType
import platform.UIKit.UIDocumentPickerViewController.delegate
import platform.UIKit.UIDocumentPickerViewController.allowsMultipleSelection
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.dataWithContentsOfURL
import platform.UIKit.UIDocumentPickerMode
import platform.UIKit.UIImagePickerControllerSourceType
import platform.darwin.NSObject
import platform.posix.memcpy
import kotlin.math.min

private var activePickerDelegate: NSObject? = null

@Composable
actual fun rememberClientDocumentPicker(onSelected: (List<ClientFile>) -> Unit): (ClientDocumentSource) -> Unit {
    val host = LocalUIViewController.current
    val callback = rememberUpdatedState(onSelected)
    return { source ->
        when (source) {
            ClientDocumentSource.Camera, ClientDocumentSource.Gallery -> {
                val picker = UIImagePickerController()
                val pickerSource = if (source == ClientDocumentSource.Camera && UIImagePickerController.isSourceTypeAvailable(
                        UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
                    )) {
                    UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
                } else {
                    UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
                }
                picker.sourceType = pickerSource
                picker.allowsEditing = false
                picker.mediaTypes = listOf("public.image")
                val delegate = ClientImagePickerDelegate(host) { image -> callback.value(listOfNotNull(image)) }
                activePickerDelegate = delegate
                picker.delegate = delegate
                host.presentViewController(picker, animated = true, completion = null)
            }
            ClientDocumentSource.Pdf -> {
                val picker = UIDocumentPickerViewController(documentTypes = listOf("com.adobe.pdf", "public.image", "public.plain-text", "com.microsoft.word.doc", "org.openxmlformats.wordprocessingml.document"), inMode = UIDocumentPickerMode.UIDocumentPickerModeImport)
                picker.allowsMultipleSelection = true
                val delegate = ClientPdfPickerDelegate(host) { file -> callback.value(file) }
                activePickerDelegate = delegate
                picker.delegate = delegate
                host.presentViewController(picker, animated = true, completion = null)
            }
        }
    }
}

private class ClientImagePickerDelegate(
    private val host: UIViewController,
    private val onSelected: (ClientFile?) -> Unit,
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
    override fun imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        onSelected(image?.toClientJpeg())
        host.dismissViewControllerAnimated(true, completion = null)
        activePickerDelegate = null
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        onSelected(null)
        host.dismissViewControllerAnimated(true, completion = null)
        activePickerDelegate = null
    }
}

private class ClientPdfPickerDelegate(
    private val host: UIViewController,
    private val onSelected: (List<ClientFile>) -> Unit,
) : NSObject(), UIDocumentPickerDelegateProtocol {
    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        onSelected(didPickDocumentsAtURLs.take(11).map { item ->
            val invalid = ClientFile(byteArrayOf(), "Archivo no disponible", "application/octet-stream")
            val url = item as? NSURL ?: return@map invalid
            val fileName = url.lastPathComponent ?: "documento.pdf"
            val mime = when (fileName.substringAfterLast('.').lowercase()) {
                "pdf" -> "application/pdf"
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "txt" -> "text/plain"
                "doc" -> "application/msword"
                "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                else -> return@map invalid
            }
            val data = NSData.dataWithContentsOfURL(url) ?: return@map invalid
            if (data.length == 0uL || data.length > 10uL * 1024uL * 1024uL) return@map invalid
            ClientFile(data.toByteArray(), fileName, mime)
        })
        host.dismissViewControllerAnimated(true, completion = null)
        activePickerDelegate = null
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onSelected(emptyList())
        host.dismissViewControllerAnimated(true, completion = null)
        activePickerDelegate = null
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun UIImage.toClientJpeg(): ClientFile {
    val maxSide = 2400.0
    val ratio = min(1.0, maxSide / maxOf(size.width, size.height))
    val target = CGSizeMake(size.width * ratio, size.height * ratio)
    UIGraphicsBeginImageContextWithOptions(target, true, 1.0)
    drawInRect(CGRectMake(0.0, 0.0, target.width, target.height))
    val resized = UIGraphicsGetImageFromCurrentImageContext() ?: this
    UIGraphicsEndImageContext()
    val data = UIImageJPEGRepresentation(resized, 0.84) ?: return ClientFile(byteArrayOf(), "cedula.jpg", "image/jpeg")
    return ClientFile(data.toByteArray(), "foto-cliente.jpg", "image/jpeg")
}

private fun NSData.toByteArray(): ByteArray {
    val count = length.toInt()
    if (count == 0) return byteArrayOf()
    return ByteArray(count).also { bytes -> bytes.usePinned { memcpy(it.addressOf(0), this@toByteArray.bytes, length) } }
}
