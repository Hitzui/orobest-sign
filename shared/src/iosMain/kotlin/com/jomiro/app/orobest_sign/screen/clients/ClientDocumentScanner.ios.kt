package com.jomiro.app.orobest_sign.screen.clients

import androidx.compose.runtime.Composable
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientDocumentCategory
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientFile

@Composable
internal actual fun ClientDocumentScanner(
    clientId: Int,
    enabled: Boolean,
    onBusyChange: (Boolean) -> Unit,
    onSelected: (ClientDocumentCategory, ClientFile) -> Unit,
) = Unit
