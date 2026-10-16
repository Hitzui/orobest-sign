package com.jomiro.app.orobest_sign.screen.login

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jomiro.app.orobest_sign.screen.login.viewmodel.LoginViewModel
import kotlinx.coroutines.flow.drop
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginRoute(viewModel: LoginViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val username = rememberTextFieldState()
    // No usar rememberSaveable: la contraseña nunca se escribe en el estado guardado del SO.
    val password = remember { TextFieldState() }

    LaunchedEffect(username) {
        snapshotFlow { username.text.toString() }.drop(1).collect { viewModel.onUsernameEdited() }
    }
    LaunchedEffect(password) {
        snapshotFlow { password.text.toString() }.drop(1).collect { viewModel.onPasswordEdited() }
    }
    LaunchedEffect(state.profile) {
        if (state.profile != null) password.clearText()
    }

    LoginScreen(
        state = state,
        username = username,
        password = password,
        onLogin = { viewModel.login(username.text.toString(), password.text.toString()) },
        onRetryRestore = viewModel::restoreSession,
        onUseAnotherAccount = viewModel::useAnotherAccount,
        onLogout = viewModel::logout,
    )
}
