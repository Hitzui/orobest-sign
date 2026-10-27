package com.jomiro.app.orobest_sign.feature.clients.data.scanner

import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientFile
import com.jomiro.app.orobest_sign.feature.clients.domain.scanner.ScannerException
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.stream.MemoryCacheImageOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/** Bound memory/dimensions and upload size even when a driver ignores requested DPI/format. */
internal fun normalizeScannedImage(file: File): ClientFile {
    if (!file.isFile || file.length() !in 1..(256L * 1024 * 1024)) {
        throw ScannerException("El escáner no devolvió una imagen válida o es demasiado grande. Revisa la resolución del controlador.")
    }
    val source = ImageIO.createImageInputStream(file)?.use { input ->
        val reader = ImageIO.getImageReaders(input).asSequence().firstOrNull()
            ?: throw ScannerException("No se pudo leer la imagen devuelta por el escáner.")
        try {
            reader.input = input
            val largest = max(reader.getWidth(0), reader.getHeight(0))
            val parameters = reader.defaultReadParam
            val sample = max(1, largest / 4000)
            parameters.setSourceSubsampling(sample, sample, 0, 0)
            reader.read(0, parameters)
        } finally {
            reader.dispose()
        }
    } ?: throw ScannerException("No se pudo leer la imagen devuelta por el escáner.")
    try {
        val scale = minOf(1.0, 4000.0 / max(source.width, source.height))
        val image = BufferedImage(max(1, (source.width * scale).roundToInt()), max(1, (source.height * scale).roundToInt()), BufferedImage.TYPE_INT_RGB)
        try {
            val graphics = image.createGraphics()
            try {
                graphics.color = Color.WHITE
                graphics.fillRect(0, 0, image.width, image.height)
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
                graphics.drawImage(source, 0, 0, image.width, image.height, null)
            } finally {
                graphics.dispose()
            }
            for (quality in listOf(0.9f, 0.75f)) {
                val bytes = encodeJpeg(image, quality)
                if (bytes.size <= 10 * 1024 * 1024) {
                    return ClientFile(bytes, "escaneo-${System.currentTimeMillis()}.jpg", "image/jpeg")
                }
            }
            throw ScannerException("La imagen escaneada supera los 10 MiB. Reduce la resolución en el controlador del escáner.")
        } finally {
            image.flush()
        }
    } finally {
        source.flush()
    }
}

private fun encodeJpeg(image: BufferedImage, quality: Float): ByteArray {
    val writer = ImageIO.getImageWritersByFormatName("jpeg").next()
    try {
        return ByteArrayOutputStream().use { bytes ->
            MemoryCacheImageOutputStream(bytes).use { output ->
                writer.output = output
                val parameters = writer.defaultWriteParam.apply {
                    compressionMode = ImageWriteParam.MODE_EXPLICIT
                    compressionQuality = quality
                }
                writer.write(null, IIOImage(image, null, null), parameters)
            }
            bytes.toByteArray()
        }
    } finally {
        writer.dispose()
    }
}
