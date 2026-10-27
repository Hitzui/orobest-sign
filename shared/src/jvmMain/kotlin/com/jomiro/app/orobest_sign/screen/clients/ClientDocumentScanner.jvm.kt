package com.jomiro.app.orobest_sign.screen.clients

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ScanLine
import com.composables.ui.components.AlertDialog
import com.composables.ui.components.Button
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.Icon
import com.composables.ui.components.IndeterminateProgressIndicator
import com.composables.ui.components.Text
import com.composables.ui.theme.borderColor
import com.composables.ui.theme.colors
import com.composables.ui.theme.panelColor
import com.composeunstyled.theme.Theme
import com.jomiro.app.orobest_sign.feature.clients.data.scanner.isWindowsDesktop
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientDocumentCategory
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientFile
import com.jomiro.app.orobest_sign.screen.clients.viewmodel.DesktopScannerViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal actual fun ClientDocumentScanner(
    clientId: Int,
    enabled: Boolean,
    onBusyChange: (Boolean) -> Unit,
    onSelected: (ClientDocumentCategory, ClientFile) -> Unit,
) {
    if (!remember { isWindowsDesktop() }) return
    // Leaving this client's documents cancels acquisition and drops its preview.
    val owner = remember(clientId) { object : ViewModelStoreOwner {
        override val viewModelStore = ViewModelStore()
    } }
    val busyCallback = rememberUpdatedState(onBusyChange)
    DisposableEffect(owner) {
        onDispose {
            owner.viewModelStore.clear()
            busyCallback.value(false)
        }
    }
    val viewModel = koinViewModel<DesktopScannerViewModel>(viewModelStoreOwner = owner)
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.busy) { busyCallback.value(state.busy) }

    Column(
        Modifier.fillMaxWidth().background(Theme[colors][panelColor], RoundedCornerShape(14.dp))
            .border(1.dp, Theme[colors][borderColor], RoundedCornerShape(14.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Lucide.ScanLine, Modifier.size(22.dp))
            Text("Escáner de documentos", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(state.selected?.let { "Escáner guardado: ${it.name}" } ?: "Elige un escáner la primera vez; se recordará en esta PC.", fontSize = 13.sp)
        Text("Escáneres USB o de red mediante WIA/TWAIN. Una cara por escaneo, preferentemente desde el cristal, en color a 300 ppp. El controlador puede abrir su propia ventana.", fontSize = 12.sp, lineHeight = 18.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                ClientDocumentCategory.IdentityFront to "Escanear frente",
                ClientDocumentCategory.IdentityBack to "Escanear reverso",
                ClientDocumentCategory.Other to "Escanear otro",
            ).forEach { (category, label) ->
                Button(onClick = { viewModel.scan(category) }, enabled = enabled && !state.busy, style = ButtonStyle.Primary) { Text(label) }
            }
            Button(onClick = viewModel::changeScanner, enabled = enabled && !state.busy, style = ButtonStyle.Outlined) {
                Text(if (state.selected == null) "Elegir escáner" else "Cambiar escáner")
            }
        }
        if (state.working) {
            IndeterminateProgressIndicator(Modifier.fillMaxWidth())
            Text("Comunicando con el escáner…", fontSize = 13.sp)
            Button(onClick = viewModel::cancel, style = ButtonStyle.Outlined) { Text("Cancelar") }
        }
        if (!state.selecting) state.error?.let { error ->
            Text(error, fontSize = 13.sp, lineHeight = 18.sp)
            if (state.lastCategory != null) Button(onClick = viewModel::retry, enabled = enabled && !state.busy, style = ButtonStyle.Outlined) { Text("Reintentar escaneo") }
        }
    }

    AlertDialog(
        visible = state.selecting,
        onDismissRequest = viewModel::cancel,
        title = { Text("Elegir escáner") },
        text = {
            Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Se consultan WIA y TWAIN de 32/64 bits. Para una Epson de red que funciona en Epson Scan 2, prueba la entrada TWAIN. La selección se guarda en esta PC.", fontSize = 13.sp)
                if (state.working) IndeterminateProgressIndicator(Modifier.fillMaxWidth())
                state.error?.let { Text(it, fontSize = 13.sp) }
                state.devices.forEach { device ->
                    Button(onClick = { viewModel.select(device) }, enabled = !state.working, style = ButtonStyle.Outlined, modifier = Modifier.fillMaxWidth()) {
                        Text(device.name + if (state.selected?.id == device.id) " · guardado" else "")
                    }
                }
            }
        },
        positiveButton = { Button(onClick = viewModel::refreshDevices, enabled = !state.working, style = ButtonStyle.Outlined) { Text("Actualizar") } },
        negativeButton = { Button(onClick = viewModel::cancel, style = ButtonStyle.Ghost) { Text("Cancelar") } },
    )
    val preview = state.preview
    AlertDialog(
        visible = preview != null,
        onDismissRequest = viewModel::cancel,
        title = { Text("Revisar escaneo · " + when (preview?.category) {
            ClientDocumentCategory.IdentityFront -> "Frente"
            ClientDocumentCategory.IdentityBack -> "Reverso"
            else -> "Otro documento"
        }) },
        text = {
            if (preview != null) Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ClientImagePreview(preview.file.bytes, "Documento escaneado", Modifier.fillMaxWidth().height(380.dp))
                Text("Usar imagen la deja seleccionada. Después pulsa Subir documentos para guardarla en el cliente.", fontSize = 13.sp)
            }
        },
        positiveButton = {
            Button(onClick = { viewModel.takePreview()?.let { onSelected(it.category, it.file) } }, enabled = enabled && preview != null, style = ButtonStyle.Primary) { Text("Usar imagen") }
        },
        negativeButton = { Button(onClick = viewModel::cancel, style = ButtonStyle.Outlined) { Text("Descartar") } },
    )
}
