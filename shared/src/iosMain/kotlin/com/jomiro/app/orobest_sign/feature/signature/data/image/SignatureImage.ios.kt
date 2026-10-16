package com.jomiro.app.orobest_sign.feature.signature.data.image

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

internal actual fun ImageBitmap.encodePng(): ByteArray = Image.makeFromBitmap(asSkiaBitmap()).use { image ->
    requireNotNull(image.encodeToData(EncodedImageFormat.PNG)).use { it.bytes }
}

internal actual fun decodeSignaturePng(bytes: ByteArray): ImageBitmap = Image.makeFromEncoded(bytes).toComposeImageBitmap()
