package com.jomiro.app.orobest_sign.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.composables.icons.lucide.ClipboardList
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Users
import com.composables.ui.components.Button
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.Icon
import com.composables.ui.components.Text
import com.composables.ui.theme.backgroundColor
import com.composables.ui.theme.borderColor
import com.composables.ui.theme.colors
import com.composables.ui.theme.panelColor
import com.composeunstyled.theme.Theme
import com.jomiro.app.orobest_sign.feature.auth.domain.model.SessionProfile
import com.jomiro.app.orobest_sign.screen.clients.ClientsRoute
import com.jomiro.app.orobest_sign.screen.purchases.PurchasesRoute

@Composable
fun HomeRoute(
    profile: SessionProfile,
    signingOut: Boolean,
    sessionError: String?,
    onLogout: () -> Unit,
    onSessionCheck: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableStateOf("purchases") }
    var fullscreen by remember { mutableStateOf(false) }
    val savedState = rememberSaveableStateHolder()
    // Ambos alcances viven durante la sesión, también al cambiar de pestaña.
    val owners = remember {
        listOf("purchases", "clients").associateWith {
            object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() }
        }
    }
    DisposableEffect(owners) { onDispose { owners.values.forEach { it.viewModelStore.clear() } } }

    Column(
        Modifier.fillMaxSize().background(Theme[colors][backgroundColor]).then(
            if (fullscreen) Modifier else Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                .consumeWindowInsets(WindowInsets.safeDrawing).imePadding()
        ),
    ) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            savedState.SaveableStateProvider(selectedTab) {
                when (selectedTab) {
                    "purchases" -> PurchasesRoute(
                        profile, signingOut, sessionError, onLogout, onSessionCheck,
                        owner = owners.getValue("purchases"),
                        onFullscreenChange = { fullscreen = it },
                    )
                    "clients" -> ClientsRoute(
                        onSessionCheck = onSessionCheck,
                        onLogout = onLogout,
                        signingOut = signingOut,
                        owner = owners.getValue("clients"),
                    )
                }
            }
        }
        if (!fullscreen) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Theme[colors][borderColor]))
            Box(Modifier.fillMaxWidth().background(Theme[colors][panelColor]), contentAlignment = Alignment.Center) {
                Row(
                    Modifier.widthIn(max = 960.dp).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    listOf("purchases" to "Compras", "clients" to "Clientes").forEach { (tab, label) ->
                        val active = selectedTab == tab
                        Button(
                            onClick = { selectedTab = tab },
                            enabled = !signingOut,
                            style = if (active) ButtonStyle.Primary else ButtonStyle.Ghost,
                            modifier = Modifier.weight(1f).height(64.dp).semantics {
                                selected = active
                                role = Role.Tab
                            },
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(if (tab == "purchases") Lucide.ClipboardList else Lucide.Users, Modifier.size(22.dp))
                                Text(label, fontSize = 13.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }
        }
    }
}
