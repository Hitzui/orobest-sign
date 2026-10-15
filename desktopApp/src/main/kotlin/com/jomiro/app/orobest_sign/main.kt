package com.jomiro.app.orobest_sign

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "orobest-sign",
    ) {
        App()
    }
}