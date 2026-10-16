package com.jomiro.app.orobest_sign

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.jomiro.app.orobest_sign.core.designsystem.theme.OroBestTheme
import com.jomiro.app.orobest_sign.core.di.appModules
import com.jomiro.app.orobest_sign.screen.login.LoginRoute
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration

@Composable
@Preview
fun App() {
    KoinApplication(configuration = koinConfiguration { modules(appModules) }) {
        OroBestTheme {
            LoginRoute()
        }
    }
}
