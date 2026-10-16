package com.jomiro.app.orobest_sign.screen.purchases

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Building2
import com.composables.icons.lucide.CalendarDays
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.ClipboardList
import com.composables.icons.lucide.LogOut
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.PenLine
import com.composables.icons.lucide.RefreshCw
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.SlidersHorizontal
import com.composables.icons.lucide.SquarePen
import com.composables.icons.lucide.UserRound
import com.composables.ui.components.AlertDialog
import com.composables.ui.components.Button
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
import com.jomiro.app.orobest_sign.feature.auth.domain.model.SessionProfile
import com.jomiro.app.orobest_sign.feature.purchases.domain.model.Purchase
import com.jomiro.app.orobest_sign.feature.purchases.domain.model.PurchaseFilters
import com.jomiro.app.orobest_sign.feature.purchases.domain.model.SignatureFilter
import com.jomiro.app.orobest_sign.screen.purchases.uistate.PurchasesUiState

@Composable
fun PurchasesScreen(
    profile: SessionProfile,
    state: PurchasesUiState,
    query: TextFieldState,
    from: TextFieldState,
    until: TextFieldState,
    signature: SignatureFilter,
    onSignatureFilter: (SignatureFilter) -> Unit,
    onApply: () -> Unit,
    onClear: () -> Unit,
    onRefresh: () -> Unit,
    onPage: (Int) -> Unit,
    onSignature: (Purchase) -> Unit,
    onDismissSelection: () -> Unit,
    signingOut: Boolean,
    sessionError: String?,
    onLogout: () -> Unit,
) {
    val enabled = !signingOut && !state.requiresSessionCheck
    val listState = rememberLazyListState()
    LaunchedEffect(state.page, state.filters) { listState.scrollToItem(0) }
    Box(
        Modifier.fillMaxSize().background(Theme[colors][backgroundColor])
            .windowInsetsPadding(WindowInsets.safeDrawing).imePadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.widthIn(max = 960.dp).fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "header") {
                PurchasesHeader(profile, enabled, state.loading, onRefresh, onLogout)
            }
            item(key = "agency") {
                Row(
                    modifier = Modifier.fillMaxWidth().background(Theme[colors][secondaryColor], RoundedCornerShape(16.dp)).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Lucide.Building2, Modifier.size(24.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(profile.agency.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text("Compras vigentes · Todas las cajas de tu agencia", fontSize = 12.sp, lineHeight = 18.sp)
                    }
                }
            }
            item(key = "filters") {
                PurchaseFiltersPanel(query, from, until, signature, onSignatureFilter, enabled, onApply, onClear, state.filters, state.filterError)
            }
            item(key = "results") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Resultados", modifier = Modifier.semantics { heading() }, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (state.loading) "Consultando compras…" else "${state.total} compras encontradas",
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }, fontSize = 13.sp,
                        )
                    }
                    if (state.filters != PurchaseFilters()) {
                        Text("Filtrado", Modifier.background(Theme[colors][secondaryColor], RoundedCornerShape(8.dp)).padding(8.dp), fontSize = 12.sp)
                    }
                }
            }
            if (signingOut) item(key = "signingOut") { MessagePanel("Cerrando sesión…") }
            sessionError?.let { message -> item(key = "sessionError") { MessagePanel(message) } }
            when {
                state.loading -> item(key = "loading") {
                    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        IndeterminateProgressIndicator(Modifier.fillMaxWidth().semantics { contentDescription = "Cargando compras" })
                        Text("Obteniendo la información de tu agencia.", fontSize = 14.sp)
                    }
                }
                state.error != null -> item(key = "error") {
                    MessagePanel(state.error) {
                        if (enabled) Button(onClick = onRefresh, style = ButtonStyle.Outlined) { Text("Reintentar") }
                    }
                }
                state.items.isEmpty() -> item(key = "empty") {
                    MessagePanel(when {
                        state.page > 1 -> "Ya no hay compras en esta página. Vuelve al inicio para consultar el listado actualizado."
                        state.filters == PurchaseFilters() -> "Todavía no hay compras vigentes en esta agencia."
                        else -> "No hay compras que coincidan con los filtros aplicados."
                    }) {
                        Icon(Lucide.ClipboardList, Modifier.size(32.dp))
                        if (state.page > 1) Button(onClick = { onPage(1) }, enabled = enabled, style = ButtonStyle.Primary) { Text("Volver al inicio") }
                        if (state.filters != PurchaseFilters()) Button(onClick = onClear, enabled = enabled, style = ButtonStyle.Outlined) { Text("Limpiar filtros") }
                    }
                }
                else -> {
                    items(state.items, key = { it.id }) { purchase ->
                        PurchaseCard(purchase, enabled, onSignature)
                    }
                    item(key = "pagination") {
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = { onPage(state.page - 1) }, enabled = enabled && state.page > 1, style = ButtonStyle.Outlined, modifier = Modifier.size(48.dp)) {
                                Icon(Lucide.ChevronLeft, contentDescription = "Página anterior")
                            }
                            Text("Página ${state.page} de ${state.totalPages}", fontSize = 14.sp)
                            IconButton(onClick = { onPage(state.page + 1) }, enabled = enabled && state.page < state.totalPages, style = ButtonStyle.Outlined, modifier = Modifier.size(48.dp)) {
                                Icon(Lucide.ChevronRight, contentDescription = "Página siguiente")
                            }
                        }
                    }
                }
            }
        }
    }
    val purchase = state.selectedPurchase
    AlertDialog(
        visible = purchase != null && enabled,
        onDismissRequest = onDismissSelection,
        title = { Text(if (purchase?.signatureVersion == null) "Agregar firma" else "Editar firma") },
        icon = { Icon(Lucide.PenLine) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Compra ${purchase?.number.orEmpty()}", fontWeight = FontWeight.SemiBold)
                Text(purchase?.customerName?.takeIf { it.isNotBlank() } ?: "Cliente no registrado")
                Text("La captura y edición de la firma se habilitarán en la siguiente etapa.", fontSize = 14.sp, lineHeight = 21.sp)
            }
        },
        positiveButton = { Button(onClick = onDismissSelection, modifier = Modifier.fillMaxWidth(), style = ButtonStyle.Primary) { Text("Volver a compras") } },
    )
}

@Composable
private fun PurchasesHeader(profile: SessionProfile, enabled: Boolean, loading: Boolean, onRefresh: () -> Unit, onLogout: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(44.dp).background(Theme[colors][primaryColor], RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(Lucide.PenLine, Modifier.size(24.dp), tint = Theme[colors][onPrimaryColor])
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("OroBest Sign", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(profile.name?.takeIf { it.isNotBlank() } ?: profile.username, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onRefresh, enabled = enabled && !loading, style = ButtonStyle.Outlined, modifier = Modifier.size(48.dp)) {
                Icon(Lucide.RefreshCw, Modifier.size(20.dp), contentDescription = "Actualizar compras")
            }
            IconButton(onClick = onLogout, enabled = enabled, style = ButtonStyle.Outlined, modifier = Modifier.size(48.dp)) {
                Icon(Lucide.LogOut, Modifier.size(20.dp), contentDescription = "Cerrar sesión")
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Compras", Modifier.semantics { heading() }, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("Consulta y gestiona las firmas de tus compras.", fontSize = 14.sp, lineHeight = 21.sp)
        }
    }
}

@Composable
private fun PurchaseFiltersPanel(
    query: TextFieldState, from: TextFieldState, until: TextFieldState, signature: SignatureFilter,
    onSignatureFilter: (SignatureFilter) -> Unit, enabled: Boolean, onApply: () -> Unit, onClear: () -> Unit,
    appliedFilters: PurchaseFilters, error: String?,
) {
    var showDates by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val apply = { focus.clearFocus(); onApply() }
    Column(
        Modifier.fillMaxWidth().border(1.dp, Theme[colors][borderColor], RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextField(
                state = query, enabled = enabled, accessibilityLabel = "Número de compra", modifier = Modifier.weight(1f),
                placeholder = { Text("Número de compra", fontSize = 14.sp) },
                leading = { Icon(Lucide.Search, Modifier.size(20.dp)) },
                borderColor = Theme[colors][borderColor],
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Search),
                onKeyboardAction = { apply() },
            )
            IconButton(onClick = apply, enabled = enabled, style = ButtonStyle.Primary, modifier = Modifier.size(48.dp)) {
                Icon(Lucide.Search, Modifier.size(20.dp), contentDescription = "Buscar compras y aplicar filtros")
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SignatureFilter.entries.forEach { filter ->
                Button(
                    onClick = { onSignatureFilter(filter) }, enabled = enabled,
                    style = if (signature == filter) ButtonStyle.Primary else ButtonStyle.Outlined,
                    modifier = Modifier.heightIn(min = 48.dp).semantics { selected = signature == filter },
                ) { Text(when (filter) { SignatureFilter.All -> "Todas"; SignatureFilter.Pending -> "Pendientes"; SignatureFilter.Signed -> "Firmadas" }, fontSize = 13.sp) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { showDates = !showDates }, enabled = enabled, style = ButtonStyle.Ghost) {
                Icon(Lucide.SlidersHorizontal, Modifier.size(18.dp))
                Text(if (showDates) "Ocultar fechas" else "Rango de fechas", fontSize = 13.sp)
            }
            Button(onClick = { focus.clearFocus(); onClear() }, enabled = enabled, style = ButtonStyle.Ghost) { Text("Limpiar", fontSize = 13.sp) }
        }
        if (showDates) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DateFilterField("Desde", from, enabled, Modifier.weight(1f))
                DateFilterField("Hasta", until, enabled, Modifier.weight(1f))
            }
            Text("Formato AAAA-MM-DD · Días incluidos", fontSize = 12.sp)
            Button(onClick = apply, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), style = ButtonStyle.Primary) { Text("Aplicar fechas", fontSize = 14.sp) }
        } else if (appliedFilters.from.isNotBlank() || appliedFilters.until.isNotBlank()) {
            Text("Fechas: ${appliedFilters.from.ifBlank { "Sin inicio" }} → ${appliedFilters.until.ifBlank { "Sin límite" }}", fontSize = 12.sp)
        }
        if (PurchaseFilters(query.text.toString(), from.text.toString(), until.text.toString(), signature).normalized() != appliedFilters) {
            Text("Cambios pendientes de aplicar. Pulsa buscar.", fontSize = 12.sp)
        }
        error?.let { MessagePanel(it) }
    }
}

@Composable
private fun DateFilterField(label: String, state: TextFieldState, enabled: Boolean, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        TextField(state = state, enabled = enabled, modifier = Modifier.fillMaxWidth(), accessibilityLabel = "Fecha $label",
            placeholder = { Text("AAAA-MM-DD", fontSize = 13.sp) }, borderColor = Theme[colors][borderColor],
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, autoCorrectEnabled = false))
    }
}

@Composable
private fun PurchaseCard(purchase: Purchase, enabled: Boolean, onSignature: (Purchase) -> Unit) {
    val signed = purchase.signatureVersion != null
    Column(
        Modifier.fillMaxWidth().background(Theme[colors][panelColor], RoundedCornerShape(16.dp))
            .border(1.dp, Theme[colors][borderColor], RoundedCornerShape(16.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("COMPRA", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                Text(purchase.number, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            }
            Row(
                Modifier.background(Theme[colors][if (signed) primaryColor else secondaryColor], RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically,
            ) {
                val color = Theme[colors][if (signed) onPrimaryColor else primaryColor]
                if (signed) Icon(Lucide.Check, Modifier.size(14.dp), tint = color)
                Text(if (signed) "Firmada" else "Pendiente", color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PurchaseInfo(Lucide.UserRound, purchase.customerName?.takeIf { it.isNotBlank() } ?: "Cliente no registrado")
            PurchaseInfo(Lucide.CalendarDays, formatPurchaseDate(purchase.date))
        }
        Box(Modifier.fillMaxWidth().heightIn(min = 1.dp).background(Theme[colors][secondaryColor]))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Total de compra", fontSize = 13.sp)
                Text(formatPurchaseTotal(purchase.total), fontSize = 21.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { onSignature(purchase) }, enabled = enabled, style = if (signed) ButtonStyle.Outlined else ButtonStyle.Primary,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics {
                    contentDescription = "${if (signed) "Editar" else "Agregar"} firma de la compra ${purchase.number}"
                },
            ) {
                Icon(if (signed) Lucide.SquarePen else Lucide.PenLine, Modifier.size(18.dp))
                Text(if (signed) "Editar firma" else "Agregar firma", fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun PurchaseInfo(icon: ImageVector, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, Modifier.size(18.dp))
        Text(value, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun MessagePanel(message: String, content: @Composable () -> Unit = {}) {
    Column(
        Modifier.fillMaxWidth().background(Theme[colors][secondaryColor], RoundedCornerShape(14.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(message, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, fontSize = 14.sp, lineHeight = 21.sp)
        content()
    }
}
