package com.jomiro.app.orobest_sign.screen.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicSecureTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Building2
import com.composables.icons.lucide.Eye
import com.composables.icons.lucide.EyeOff
import com.composables.icons.lucide.LockKeyhole
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.PenTool
import com.composables.icons.lucide.UserRound
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
import com.composables.ui.theme.destructiveColor
import com.composables.ui.theme.fieldColor
import com.composables.ui.theme.onFieldColor
import com.composables.ui.theme.primaryColor
import com.composables.ui.theme.secondaryColor
import com.composeunstyled.theme.Theme
import com.jomiro.app.orobest_sign.screen.login.uistate.LoginPhase
import com.jomiro.app.orobest_sign.screen.login.uistate.LoginUiState

@Composable
fun LoginScreen(
    state: LoginUiState,
    username: TextFieldState,
    password: TextFieldState,
    onLogin: () -> Unit,
    onRetryRestore: () -> Unit,
    onUseAnotherAccount: () -> Unit,
    onLogout: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize()
            .background(Theme[colors][backgroundColor])
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.size(80.dp).background(Theme[colors][secondaryColor], RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Lucide.PenTool, Modifier.size(36.dp), tint = Theme[colors][primaryColor])
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("OroBest Sign", fontSize = 30.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(
                    if (state.profile == null) "Firma de compras, desde tu agencia." else "Tu sesión está activa.",
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                )
            }

            when (state.phase) {
                LoginPhase.RestoringSession -> LoadingSession()
                LoginPhase.RestoreFailed -> {
                    state.errorMessage?.let { LoginMessage(it, isError = true) }
                    Button(onClick = onRetryRestore, modifier = Modifier.fillMaxWidth(), style = ButtonStyle.Primary) {
                        Text("Reintentar")
                    }
                    Button(onClick = onUseAnotherAccount, modifier = Modifier.fillMaxWidth(), style = ButtonStyle.Outlined) {
                        Text("Usar otra cuenta")
                    }
                }
                LoginPhase.SignIn, LoginPhase.SigningIn -> LoginForm(state, username, password, onLogin)
                LoginPhase.Authenticated, LoginPhase.SigningOut -> {
                    state.profile?.let { profile ->
                        Column(
                            modifier = Modifier.fillMaxWidth().background(Theme[colors][secondaryColor], RoundedCornerShape(20.dp)).padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("Bienvenido", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                            Text(profile.name?.takeIf { it.isNotBlank() } ?: profile.username, fontSize = 18.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Lucide.Building2, Modifier.size(20.dp))
                                Text(profile.agency.name, fontSize = 16.sp)
                            }
                        }
                    }
                    state.errorMessage?.let { LoginMessage(it, isError = true) }
                    Button(
                        onClick = onLogout,
                        enabled = state.phase != LoginPhase.SigningOut,
                        modifier = Modifier.fillMaxWidth(),
                        style = ButtonStyle.Outlined,
                    ) {
                        Text(if (state.phase == LoginPhase.SigningOut) "Cerrando sesión…" else "Cerrar sesión")
                    }
                    if (state.phase == LoginPhase.SigningOut) IndeterminateProgressIndicator(Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun LoadingSession() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IndeterminateProgressIndicator(Modifier.fillMaxWidth().semantics { contentDescription = "Verificando sesión" })
        Text("Verificando tu sesión…", modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    }
}

@Composable
private fun LoginForm(state: LoginUiState, username: TextFieldState, password: TextFieldState, onLogin: () -> Unit) {
    val enabled = state.phase == LoginPhase.SignIn
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val submit = {
        if (enabled) {
            keyboard?.hide()
            focusManager.clearFocus()
            onLogin()
        }
    }
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("Iniciar sesión", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Usuario", fontWeight = FontWeight.Medium)
            TextField(
                state = username,
                modifier = Modifier.fillMaxWidth().semantics {
                    contentType = ContentType.Username
                    state.usernameError?.let { error(it) }
                },
                enabled = enabled,
                accessibilityLabel = "Usuario",
                placeholder = { Text("Tu usuario de OroBest") },
                leading = { Icon(Lucide.UserRound, Modifier.size(20.dp)) },
                borderColor = Theme[colors][if (state.usernameError == null) borderColor else destructiveColor],
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Next),
                onKeyboardAction = { focusManager.moveFocus(FocusDirection.Next) },
            )
            state.usernameError?.let { FieldError(it) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Contraseña", fontWeight = FontWeight.Medium)
            PasswordField(password, enabled, state.passwordError, submit)
            state.passwordError?.let { FieldError(it) }
        }
        state.errorMessage?.let { LoginMessage(it, isError = true) }
        state.notice?.let { LoginMessage(it) }
        Button(onClick = submit, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), style = ButtonStyle.Primary) {
            Text(if (enabled) "Iniciar sesión" else "Iniciando sesión…", fontSize = 16.sp)
        }
        if (!enabled) IndeterminateProgressIndicator(Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Lucide.LockKeyhole, Modifier.size(18.dp))
            Text("Tu sesión se mantendrá abierta en este dispositivo.", fontSize = 13.sp, lineHeight = 19.sp)
        }
    }
}

/** Campo seguro nativo de Foundation, decorado con los tokens de Composables UI. */
@Composable
private fun PasswordField(state: TextFieldState, enabled: Boolean, errorMessage: String?, onSubmit: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val foreground = Theme[colors][onFieldColor]
    val outline = Theme[colors][if (errorMessage == null) borderColor else destructiveColor]
    BasicSecureTextField(
        state = state,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().semantics {
            contentType = ContentType.Password
            contentDescription = "Contraseña"
            errorMessage?.let { error(it) }
        },
        textStyle = TextStyle(color = foreground, fontSize = 16.sp),
        cursorBrush = SolidColor(foreground),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done, autoCorrectEnabled = false),
        onKeyboardAction = { onSubmit() },
        textObfuscationMode = if (visible) TextObfuscationMode.Visible else TextObfuscationMode.Hidden,
        decorator = { innerTextField ->
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                    .background(Theme[colors][fieldColor], RoundedCornerShape(10.dp))
                    .border(1.dp, outline, RoundedCornerShape(10.dp))
                    .padding(start = 12.dp, end = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Lucide.LockKeyhole, Modifier.size(20.dp), tint = foreground)
                Box(Modifier.weight(1f).padding(vertical = 12.dp)) { innerTextField() }
                IconButton(onClick = { visible = !visible }, enabled = enabled, modifier = Modifier.size(48.dp), style = ButtonStyle.Ghost) {
                    Icon(if (visible) Lucide.EyeOff else Lucide.Eye, contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña")
                }
            }
        },
    )
}

@Composable
private fun FieldError(message: String) {
    Text(message, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = Theme[colors][destructiveColor], fontSize = 13.sp)
}

@Composable
private fun LoginMessage(message: String, isError: Boolean = false) {
    Text(
        message,
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }
            .background(Theme[colors][secondaryColor], RoundedCornerShape(12.dp)).padding(16.dp),
        color = Theme[colors][if (isError) destructiveColor else primaryColor],
        fontSize = 14.sp,
        lineHeight = 21.sp,
    )
}
