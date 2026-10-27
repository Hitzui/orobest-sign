package com.jomiro.app.orobest_sign.screen.signature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.composables.ui.components.Button
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal actual fun FingerprintCaptureControl(
    enabled: Boolean,
    onCaptured: (ByteArray) -> Unit,
    onError: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    Button(
        enabled = enabled,
        style = ButtonStyle.Outlined,
        onClick = {
            scope.launch {
                try {
                    val image = withContext(Dispatchers.IO) { captureUareUFingerprintPng() }
                    onCaptured(image)
                } catch (error: Exception) {
                    val rootCause = generateSequence(error as Throwable?) { it.cause }.lastOrNull()
                    val reason = rootCause?.message?.takeIf(String::isNotBlank)
                        ?: error.message?.takeIf(String::isNotBlank)
                        ?: "Error del SDK DigitalPersona"
                    onError("No se pudo capturar la huella: $reason")
                }
            }
        },
    ) { Text("Capturar huella") }
}
