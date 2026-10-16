package com.jomiro.app.orobest_sign.screen.signature

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

@Composable
internal actual fun SignaturePlatformEffect(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val view = LocalView.current
    DisposableEffect(view) {
        val previous = view.keepScreenOn
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = previous }
    }
}
