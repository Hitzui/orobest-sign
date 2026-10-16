package com.jomiro.app.orobest_sign

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import javax.imageio.ImageIO

fun main() = application {
    val appIcon = remember {
        val resource = checkNotNull(Thread.currentThread().contextClassLoader.getResourceAsStream("orobest-sign.png"))
        resource.use { BitmapPainter(ImageIO.read(it).toComposeImageBitmap()) }
    }
    Window(
        onCloseRequest = ::exitApplication,
        title = "OroBest Sign",
        icon = appIcon,
    ) {
        App()
    }
}
