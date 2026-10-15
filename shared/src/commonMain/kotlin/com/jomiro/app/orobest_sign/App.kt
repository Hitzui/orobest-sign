package com.jomiro.app.orobest_sign

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.composables.ui.theme.ComposablesTheme
import com.jomiro.app.orobest_sign.core.di.appModules
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration

@Composable
@Preview
fun App() {
    KoinApplication(configuration = koinConfiguration { modules(appModules) }) {
        ComposablesTheme {
            // Las pantallas se incorporarán en la siguiente etapa.
        }
    }
}
