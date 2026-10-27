package com.jomiro.app.orobest_sign.core.di

import com.jomiro.app.orobest_sign.core.session.SessionStore
import com.jomiro.app.orobest_sign.feature.clients.data.scanner.WindowsDocumentScanner
import com.jomiro.app.orobest_sign.feature.clients.data.scanner.WindowsScannerPreferences
import com.jomiro.app.orobest_sign.feature.clients.domain.scanner.DocumentScanner
import com.jomiro.app.orobest_sign.feature.clients.domain.scanner.ScannerPreferenceStore
import com.jomiro.app.orobest_sign.screen.clients.viewmodel.DesktopScannerViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** JVM es auxiliar de desarrollo: no escribe credenciales en preferencias sin cifrar. */
internal actual val platformModule: Module = module {
    single<DocumentScanner> { WindowsDocumentScanner(get()) }
    single<ScannerPreferenceStore> { WindowsScannerPreferences() }
    viewModelOf(::DesktopScannerViewModel)
    single<SessionStore> {
        object : SessionStore {
            private var token: String? = null
            override suspend fun readToken() = token
            override suspend fun writeToken(token: String) { this.token = token }
            override suspend fun clear() { token = null }
        }
    }
}
