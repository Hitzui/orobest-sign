package com.jomiro.app.orobest_sign.feature.signature.data.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream

internal actual fun ImageBitmap.encodePng(): ByteArray = ByteArrayOutputStream().use { stream ->
    check(asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream))
    stream.toByteArray()
}

internal actual fun decodeSignaturePng(bytes: ByteArray): ImageBitmap =
    requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size)).asImageBitmap()
