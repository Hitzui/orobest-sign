package com.jomiro.app.orobest_sign.screen.clients

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.ui.components.Button
import com.composables.ui.components.AlertDialog
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.Icon
import com.composables.ui.components.IconButton
import com.composables.ui.components.IndeterminateProgressIndicator
import com.composables.ui.components.Text
import com.composables.ui.components.TextField
import com.composables.ui.theme.backgroundColor
import com.composables.ui.theme.borderColor
import com.composables.ui.theme.colors
import com.composables.ui.theme.onPrimaryColor
import com.composables.ui.theme.panelColor
import com.composables.ui.theme.primaryColor
import com.composables.ui.theme.secondaryColor
import com.composeunstyled.theme.Theme
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.Camera
import com.composables.icons.lucide.Eye
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.LogOut
import com.composables.icons.lucide.RefreshCw
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.Trash2
import com.jomiro.app.orobest_sign.feature.clients.domain.model.Client
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientDocument
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientDocumentCategory
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientFile
import com.jomiro.app.orobest_sign.screen.clients.uistate.ClientsUiState
import com.jomiro.app.orobest_sign.screen.clients.viewmodel.ClientsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ClientsRoute(onSessionCheck: () -> Unit, onLogout: () -> Unit, signingOut: Boolean, owner: ViewModelStoreOwner) {
    val viewModel = koinViewModel<ClientsViewModel>(viewModelStoreOwner = owner)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val search = rememberTextFieldState()
    LaunchedEffect(state.requiresSessionCheck) {
        if (state.requiresSessionCheck) onSessionCheck()
    }
    if (state.previewDocument != null) {
        ClientDocumentPreviewScreen(state, onClose = viewModel::closePreview, onRetry = viewModel::retryPreview)
    } else if (state.selectedClient == null) {
        ClientsListScreen(
            state = state,
            search = search,
            onLogout = onLogout,
            signingOut = signingOut,
            onSearch = { viewModel.search(search.text.toString()) },
            onRefresh = viewModel::refresh,
            onPage = viewModel::page,
            onSelect = viewModel::select,
        )
    } else {
        ClientDocumentsScreen(
            state = state,
            onBack = viewModel::backToClients,
            onRefresh = viewModel::refreshDocuments,
            onUpload = viewModel::uploadMany,
            onRetryUpload = viewModel::retryUpload,
            onCancelUpload = viewModel::dismissPendingUpload,
            onOpenDocument = viewModel::openDocument,
            onDeleteDocument = viewModel::deleteDocument,
        )
    }
}

@Composable
private fun ClientsListScreen(
    state: ClientsUiState,
    search: androidx.compose.foundation.text.input.TextFieldState,
    onLogout: () -> Unit,
    signingOut: Boolean,
    onSearch: () -> Unit,
    onRefresh: () -> Unit,
    onPage: (Int) -> Unit,
    onSelect: (Client) -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Theme[colors][backgroundColor]), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            Modifier.widthIn(max = 960.dp).fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "header") {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Clientes", Modifier.semantics { heading() }, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                        Text("Consulta documentos e identificaciones.", fontSize = 13.sp)
                    }
                    IconButton(onClick = onRefresh, enabled = !state.loading, style = ButtonStyle.Outlined, modifier = Modifier.size(46.dp)) {
                        Icon(Lucide.RefreshCw, contentDescription = "Actualizar clientes")
                    }
                    IconButton(onClick = onLogout, enabled = !signingOut, style = ButtonStyle.Outlined, modifier = Modifier.size(46.dp)) {
                        Icon(Lucide.LogOut, contentDescription = "Cerrar sesión")
                    }
                }
            }
            item(key = "search") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        state = search, modifier = Modifier.weight(1f), accessibilityLabel = "Buscar cliente",
                        placeholder = { Text("Nombre, cédula o número de cliente", fontSize = 13.sp) },
                        borderColor = Theme[colors][borderColor],
                        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Search),
                        onKeyboardAction = { onSearch() },
                    )
                    IconButton(onClick = onSearch, style = ButtonStyle.Primary, modifier = Modifier.size(48.dp)) {
                        Icon(Lucide.Search, contentDescription = "Buscar clientes")
                    }
                }
            }
            item(key = "count") { Text("${state.total} clientes", fontSize = 13.sp) }
            if (state.loading) {
                item(key = "loading") { IndeterminateProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 16.dp)) }
            } else if (state.error != null) {
                item(key = "error") {
                    NoticeCard(state.error) { Button(onClick = onRefresh, style = ButtonStyle.Outlined) { Text("Reintentar") } }
                }
            } else if (state.clients.isEmpty()) {
                item(key = "empty") { NoticeCard("No hay clientes que coincidan con la búsqueda.") {} }
            } else {
                items(state.clients, key = { it.id }) { client ->
                    ClientCard(client, onClick = { onSelect(client) })
                }
                item(key = "pagination") {
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = { onPage(state.page - 1) }, enabled = state.page > 1, style = ButtonStyle.Outlined) { Text("Anterior") }
                        Text("${state.page} / ${state.totalPages}", fontSize = 13.sp)
                        Button(onClick = { onPage(state.page + 1) }, enabled = state.page < state.totalPages, style = ButtonStyle.Outlined) { Text("Siguiente") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClientCard(client: Client, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Theme[colors][panelColor], RoundedCornerShape(16.dp))
            .border(1.dp, Theme[colors][borderColor], RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(client.displayName.ifBlank { "Cliente ${client.id}" }, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text("Cédula: ${client.identityNumber?.takeIf(String::isNotBlank) ?: "No registrada"}", fontSize = 13.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Cliente #${client.id}", fontSize = 12.sp)
            Button(onClick = onClick, style = ButtonStyle.Primary, modifier = Modifier.heightIn(min = 44.dp)) { Text("Documentos") }
        }
    }
}

@Composable
private fun ClientDocumentsScreen(
    state: ClientsUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onUpload: (ClientDocumentCategory, String, List<ClientFile>) -> Unit,
    onRetryUpload: () -> Unit,
    onCancelUpload: () -> Unit,
    onOpenDocument: (ClientDocument) -> Unit,
    onDeleteDocument: (ClientDocument) -> Unit,
) {
    val client = state.selectedClient ?: return
    var categoryName by rememberSaveable { mutableStateOf(ClientDocumentCategory.IdentityFront.name) }
    var pickedFiles by remember { mutableStateOf<List<ClientFile>>(emptyList()) }
    var pickerError by remember { mutableStateOf<String?>(null) }
    var deleteCandidate by remember { mutableStateOf<ClientDocument?>(null) }
    val enabled = !state.uploading && !state.deleting && state.pendingUpload == null
    val otherName = rememberTextFieldState()
    val picker = rememberClientDocumentPicker { files ->
        if (files.size > 10) {
            pickedFiles = emptyList()
            pickerError = "Selecciona como máximo 10 archivos a la vez."
        } else if (files.any { it.bytes.isEmpty() || it.bytes.size > 10 * 1024 * 1024 }) {
            pickedFiles = emptyList()
            pickerError = "No se pudo leer uno de los archivos, su formato no es compatible o supera los 10 MiB. Vuelve a seleccionarlos."
        } else {
            pickedFiles = files
            pickerError = null
        }
    }

    Column(
        Modifier.fillMaxSize().background(Theme[colors][backgroundColor]).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconButton(onClick = onBack, enabled = !state.uploading && !state.deleting, style = ButtonStyle.Outlined, modifier = Modifier.size(46.dp)) {
                Icon(Lucide.ChevronLeft, contentDescription = "Volver a clientes")
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(client.displayName.ifBlank { "Cliente ${client.id}" }, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("Cédula: ${client.identityNumber?.takeIf(String::isNotBlank) ?: "No registrada"}", fontSize = 13.sp)
            }
            IconButton(onClick = onRefresh, enabled = !state.documentsLoading && !state.uploading && !state.deleting, style = ButtonStyle.Outlined, modifier = Modifier.size(46.dp)) {
                Icon(Lucide.RefreshCw, contentDescription = "Actualizar documentos")
            }
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.documentsLoading) {
                IndeterminateProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 16.dp))
            } else {
                state.documentsError?.let { NoticeCard(it) { Button(onClick = onRefresh, enabled = !state.uploading && !state.deleting, style = ButtonStyle.Outlined) { Text("Actualizar") } } }
                DocumentSection("Cédula · Frente", ClientDocumentCategory.IdentityFront, state.documents, enabled, onOpenDocument, { deleteCandidate = it }) { selected ->
                    categoryName = selected.name
                    picker(ClientDocumentSource.Camera)
                }
                DocumentSection("Cédula · Reverso", ClientDocumentCategory.IdentityBack, state.documents, enabled, onOpenDocument, { deleteCandidate = it }) { selected ->
                    categoryName = selected.name
                    picker(ClientDocumentSource.Camera)
                }
                DocumentSection("Otros documentos", ClientDocumentCategory.Other, state.documents, enabled, onOpenDocument, { deleteCandidate = it }) { selected ->
                    categoryName = selected.name
                    picker(ClientDocumentSource.Camera)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { categoryName = ClientDocumentCategory.IdentityFront.name; picker(ClientDocumentSource.Gallery) }, enabled = enabled, style = ButtonStyle.Outlined, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Galería · Frente") }
                    Button(onClick = { categoryName = ClientDocumentCategory.IdentityBack.name; picker(ClientDocumentSource.Gallery) }, enabled = enabled, style = ButtonStyle.Outlined, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Galería · Reverso") }
                }
                TextField(
                    state = otherName,
                    modifier = Modifier.fillMaxWidth(), accessibilityLabel = "Nombre del otro documento",
                    placeholder = { Text("Nombre del otro documento (opcional)", fontSize = 13.sp) },
                    borderColor = Theme[colors][borderColor],
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { categoryName = ClientDocumentCategory.Other.name; picker(ClientDocumentSource.Gallery) }, enabled = enabled, style = ButtonStyle.Outlined, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Elegir fotos") }
                    Button(onClick = { categoryName = ClientDocumentCategory.Other.name; picker(ClientDocumentSource.Pdf) }, enabled = enabled, style = ButtonStyle.Outlined, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Elegir archivos") }
                }
                Text("Hasta 10 archivos por selección, de 10 MiB cada uno: PDF, DOC, DOCX, JPG, PNG y TXT. Se añaden sin reemplazar los existentes y se comparten con la web.", fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
        pickerError?.let { NoticeCard(it) {} }
        if (pickedFiles.isNotEmpty()) {
            NoticeCard("${pickedFiles.size} archivo(s): " + pickedFiles.joinToString { it.fileName }) {
                Button(onClick = {
                    val selectedCategory = ClientDocumentCategory.valueOf(categoryName)
                    onUpload(selectedCategory, otherName.text.toString(), pickedFiles)
                    pickedFiles = emptyList()
                }, enabled = enabled, style = ButtonStyle.Primary) { Text("Subir documentos") }
                Button(onClick = { pickedFiles = emptyList() }, enabled = enabled, style = ButtonStyle.Ghost) { Text("Quitar selección") }
            }
        }
        state.notice?.let { NoticeCard(it) {} }
        state.pendingUpload?.let { pending ->
            NoticeCard("${state.uploadedCount} guardado(s), ${state.queuedUploads.size + 1} pendiente(s). " + if (state.uploading) "Subiendo ${pending.file.fileName}…" else "No se pudo confirmar la subida de ${pending.file.fileName}.") {
                if (!state.uploading) {
                    Button(onClick = onRetryUpload, style = ButtonStyle.Primary) { Text("Reintentar subida") }
                    Button(onClick = onCancelUpload, style = ButtonStyle.Ghost) { Text("Descartar") }
                }
            }
        }
        if (state.deleting) Text("Eliminando documento…", fontSize = 13.sp)
        AlertDialog(
            visible = deleteCandidate != null,
            onDismissRequest = { deleteCandidate = null },
            title = { Text("¿Eliminar documento?") },
            text = { Text("Se eliminará ${deleteCandidate?.name.orEmpty()} del cliente, tanto en la app como en la web.") },
            positiveButton = { Button(onClick = { deleteCandidate?.let(onDeleteDocument); deleteCandidate = null }, style = ButtonStyle.Primary) { Text("Eliminar") } },
            negativeButton = { Button(onClick = { deleteCandidate = null }, style = ButtonStyle.Outlined) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun DocumentSection(
    title: String,
    category: ClientDocumentCategory,
    documents: List<ClientDocument>,
    enabled: Boolean,
    onOpen: (ClientDocument) -> Unit,
    onDelete: (ClientDocument) -> Unit,
    onCamera: (ClientDocumentCategory) -> Unit,
) {
    val files = documents.filter { it.category == category }
    Column(
        Modifier.fillMaxWidth().background(Theme[colors][panelColor], RoundedCornerShape(14.dp))
            .border(1.dp, Theme[colors][borderColor], RoundedCornerShape(14.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(if (files.isEmpty()) "Todavía no hay archivo" else "${files.size} archivo(s)", fontSize = 12.sp)
            }
            Button(onClick = { onCamera(category) }, enabled = enabled, style = ButtonStyle.Primary, modifier = Modifier.heightIn(min = 44.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Lucide.Camera, Modifier.size(18.dp))
                    Text("Tomar foto")
                }
            }
        }
        files.forEach { file ->
            Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(file.name, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (file.available) formatFileSize(file.sizeBytes) else "Archivo no disponible en el servidor",
                        fontSize = 12.sp,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onOpen(file) }, enabled = enabled && file.available, style = ButtonStyle.Outlined) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Lucide.Eye, Modifier.size(18.dp))
                            Text("Ver")
                        }
                    }
                    Button(onClick = { onDelete(file) }, enabled = enabled, style = ButtonStyle.Ghost) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Lucide.Trash2, Modifier.size(18.dp))
                            Text("Eliminar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoticeCard(message: String, actions: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Theme[colors][secondaryColor], RoundedCornerShape(12.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(message, fontSize = 13.sp, lineHeight = 19.sp)
        actions()
    }
}

private fun formatFileSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
    bytes >= 1024 -> "${bytes / 1024} KB"
    else -> "$bytes B"
}
