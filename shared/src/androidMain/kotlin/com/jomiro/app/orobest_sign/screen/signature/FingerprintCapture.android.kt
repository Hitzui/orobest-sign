package com.jomiro.app.orobest_sign.screen.signature

import androidx.compose.runtime.Composable

@Composable
internal actual fun FingerprintCaptureControl(enabled: Boolean, onCaptured: (ByteArray) -> Unit, onError: (String) -> Unit) = Unit
