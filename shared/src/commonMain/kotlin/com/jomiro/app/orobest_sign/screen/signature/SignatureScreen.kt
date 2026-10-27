package com.jomiro.app.orobest_sign.screen.signature

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Eraser
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Save
import com.composables.ui.components.AlertDialog
import com.composables.ui.components.Button
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.Icon
import com.composables.ui.components.IndeterminateProgressIndicator
import com.composables.ui.components.Text
import com.composables.ui.theme.backgroundColor
import com.composables.ui.theme.colors
import com.composables.ui.theme.secondaryColor
import com.composeunstyled.theme.Theme
import com.jomiro.app.orobest_sign.feature.signature.data.image.decodeSignaturePng
import com.jomiro.app.orobest_sign.feature.signature.domain.model.SignaturePoint
import com.jomiro.app.orobest_sign.screen.signature.components.SidewaysSignatureLayout
import com.jomiro.app.orobest_sign.screen.signature.components.SignatureCanvas
import com.jomiro.app.orobest_sign.screen.signature.components.sidewaysDialog
import com.jomiro.app.orobest_sign.screen.signature.uistate.SignatureConfirmation
import com.jomiro.app.orobest_sign.screen.signature.uistate.SignatureUiState

@Composable
fun SignatureScreen(
    state: SignatureUiState,
    onBegin: (SignaturePoint) -> Unit, onPoint: (SignaturePoint) -> Unit, onEnd: () -> Unit,
    onClear: () -> Unit, onTogglePrevious: () -> Unit, onSave: () -> Unit, onExit: () -> Unit,
    onReload: () -> Unit, onDismissConfirmation: () -> Unit, onConfirmDiscard: () -> Unit,
    onFingerprintCaptured: (ByteArray) -> Unit,
    onFingerprintError: (String) -> Unit,
    onClearFingerprint: () -> Unit,
) {
    val image =
        remember(state.previousPng) { state.previousPng?.let { runCatching { decodeSignaturePng(it) }.getOrNull() } }
    BoxWithConstraints(
        Modifier.fillMaxSize().background(Theme[colors][backgroundColor])
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        val turned = maxHeight > maxWidth
        SidewaysSignatureLayout(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "${if ((state.purchase?.baseVersion ?: 0) > 0) "Editar" else "Agregar"} firma · ${state.purchase?.number ?: "Compra"}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(state.purchase?.customer?.takeIf { it.isNotBlank() }
                            ?: "Cliente no registrado",
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                    }
                    if (state.previousPng != null) Button(
                        onClick = onTogglePrevious,
                        enabled = !state.saving && !state.retryPending,
                        style = ButtonStyle.Outlined
                    ) {
                        Text(
                            if (state.showPrevious) "Capturar nueva" else "Ver anterior",
                            fontSize = 12.sp
                        )
                    }
                }
                if (state.loading || state.saving) IndeterminateProgressIndicator(Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FingerprintCaptureControl(
                        enabled = !state.loading && !state.saving && !state.retryPending && !state.showPrevious,
                        onCaptured = onFingerprintCaptured,
                        onError = onFingerprintError,
                    )
                    if (state.fingerprintPng != null) {
                        Text("Huella capturada", fontSize = 12.sp)
                        Button(onClick = onClearFingerprint, enabled = !state.saving && !state.retryPending, style = ButtonStyle.Outlined) {
                            Text("Descartar captura", fontSize = 12.sp)
                        }
                    }
                }
                val message = when {
                    state.saving -> "Guardando PNG… Espera la confirmación."
                    state.loading -> "Consultando la compra y su firma actual…"
                    state.error != null -> state.error
                    state.showPrevious -> "Firma guardada · versión ${state.purchase?.baseVersion}. Para reemplazarla, pulsa Capturar nueva."
                    state.previousError != null -> state.previousError
                    state.purchase?.purchaseModified == true -> "La compra cambió desde la firma anterior. Se guardará una nueva versión."
                    else -> "Teléfono de lado, parte superior hacia la izquierda · Firma dentro del recuadro."
                }
                Text(
                    message,
                    Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (state.showPrevious && image != null) {
                        Image(
                            image,
                            "Firma anterior de esta compra",
                            Modifier.fillMaxSize().background(Color.White),
                            contentScale = ContentScale.Fit
                        )
                    } else if (state.showPrevious) {
                        Text(
                            "No se pudo mostrar la firma anterior. Pulsa Capturar nueva para firmar.",
                            fontSize = 14.sp
                        )
                    } else {
                        SignatureCanvas(
                            state.strokes,
                            state.canDraw,
                            Modifier.widthIn(max = 900.dp)
                                .heightIn(max = 450.dp)
                                .fillMaxWidth()
                                .aspectRatio(2f, matchHeightConstraintsFirst = true),
                            onBegin,
                            onPoint,
                            onEnd
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onExit,
                        enabled = !state.saving,
                        style = ButtonStyle.Outlined
                    ) { Text("Cancelar") }
                    if (state.needsReload || (state.purchase == null && !state.loading)) {
                        Button(
                            onClick = onReload,
                            enabled = !state.saving && !state.loading && !state.requiresSessionCheck,
                            modifier = Modifier.weight(1f),
                            style = ButtonStyle.Primary
                        ) { Text("Recargar compra") }
                    } else {
                        Button(
                            onClick = onClear,
                            enabled = state.canDraw && state.hasInk && !state.strokeActive,
                            style = ButtonStyle.Outlined
                        ) {
                            Icon(Lucide.Eraser, Modifier.size(18.dp))
                            Text("Limpiar")
                        }
                        Button(
                            onClick = onSave,
                            enabled = state.canSave,
                            modifier = Modifier.weight(1f),
                            style = ButtonStyle.Primary
                        ) {
                            Icon(Lucide.Save, Modifier.size(18.dp))
                            Text(if (state.saving) "Guardando…" else if (state.retryPending) "Reintentar guardado" else "Guardar firma")
                        }
                    }
                }
            }
        }
        AlertDialog(
            visible = state.confirmation != null,
            onDismissRequest = onDismissConfirmation,
            modifier = Modifier.sidewaysDialog(turned),
            title = { Text(if (state.confirmation == SignatureConfirmation.Reload) "¿Recargar la compra?" else "¿Salir de la firma?") },
            text = {
                Text(
                    when {
                        state.retryPending -> "El envío no está confirmado. Puedes reintentarlo sin duplicados antes de salir. Si sales, consulta la compra para comprobar si se guardó."
                        state.confirmation == SignatureConfirmation.Reload -> "Se descartarán estos trazos y se consultarán los datos y la versión actuales antes de volver a firmar."
                        else -> "Los trazos y la huella capturada que aún no hayas guardado se perderán."
                    }, fontSize = 14.sp
                )
            },
            positiveButton = {
                Button(
                    onClick = onDismissConfirmation,
                    modifier = Modifier.fillMaxWidth(),
                    style = ButtonStyle.Primary
                ) { Text("Seguir aquí") }
            },
            negativeButton = {
                Button(
                    onClick = onConfirmDiscard,
                    modifier = Modifier.fillMaxWidth(),
                    style = ButtonStyle.Outlined
                ) { Text(if (state.confirmation == SignatureConfirmation.Reload) "Recargar y descartar trazos" else "Salir") }
            },
        )
    }
}
