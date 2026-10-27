package com.jomiro.app.orobest_sign.screen.signature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
    var capturing by remember { mutableStateOf(false) }
    Button(
        enabled = enabled && !capturing,
        style = ButtonStyle.Outlined,
        onClick = {
            capturing = true
            scope.launch {
                try {
                    val image = withContext(Dispatchers.IO) { captureUareUFingerprintPng() }
                    onCaptured(image)
                } catch (error: Exception) {
                    val rootCause = generateSequence(error as Throwable?) { it.cause }.lastOrNull()
                    val reason = rootCause?.message?.takeIf(String::isNotBlank)
                        ?: error.message?.takeIf(String::isNotBlank)
                        ?: "Error del SDK DigitalPersona"
                    onError("La huella no se capturó ni se guardó: $reason")
                } finally {
                    capturing = false
                }
            }
        },
    ) { Text(if (capturing) "Capturando…" else "Capturar huella") }
}
