package com.jomiro.app.orobest_sign.screen.signature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import platform.UIKit.UIApplication

@Composable
internal actual fun SignaturePlatformEffect(onBack: () -> Unit) {
    DisposableEffect(Unit) {
        val application = UIApplication.sharedApplication
        val previous = application.idleTimerDisabled
        application.idleTimerDisabled = true
        onDispose { application.idleTimerDisabled = previous }
    }
}
