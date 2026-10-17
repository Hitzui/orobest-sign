package com.jomiro.app.orobest_sign.screen.clients

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientFile
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
actual fun rememberClientDocumentPicker(onSelected: (List<ClientFile>) -> Unit): (ClientDocumentSource) -> Unit {
    val callback = rememberUpdatedState(onSelected)
    return { source ->
        val chooser = JFileChooser()
        chooser.dialogTitle = when (source) {
            ClientDocumentSource.Pdf -> "Seleccionar documentos del cliente"
            else -> "Seleccionar foto del cliente"
        }
        chooser.isAcceptAllFileFilterUsed = false
        chooser.isMultiSelectionEnabled = source != ClientDocumentSource.Camera
        chooser.fileFilter = if (source == ClientDocumentSource.Pdf) {
            FileNameExtensionFilter("Documentos del cliente", "pdf", "doc", "docx", "txt", "jpg", "jpeg", "png")
        } else {
            FileNameExtensionFilter("Imágenes JPG o PNG", "jpg", "jpeg", "png")
        }
        if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
            callback.value(emptyList())
        } else {
            val selected = if (chooser.isMultiSelectionEnabled) chooser.selectedFiles.toList() else listOf(chooser.selectedFile)
            callback.value(selected.take(11).map { file ->
                val ext = file.extension.lowercase()
                val invalid = ClientFile(byteArrayOf(), file.name, "application/octet-stream")
                if (file.length() !in 1..(10L * 1024 * 1024)) return@map invalid
                val mime = when (ext) {
                    "jpg", "jpeg" -> "image/jpeg"
                    "png" -> "image/png"
                    "pdf" -> "application/pdf"
                    "txt" -> "text/plain"
                    "doc" -> "application/msword"
                    "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    else -> return@map invalid
                }
                runCatching { ClientFile(file.readBytes(), file.name, mime) }.getOrDefault(invalid)
            })
        }
    }
}
