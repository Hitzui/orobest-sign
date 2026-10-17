package com.jomiro.app.orobest_sign.screen.clients

import androidx.compose.runtime.Composable
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientFile

enum class ClientDocumentSource { Camera, Gallery, Pdf }

/** Opens the native camera or system document/gallery picker and returns optimized bytes in memory. */
@Composable
expect fun rememberClientDocumentPicker(onSelected: (List<ClientFile>) -> Unit): (ClientDocumentSource) -> Unit
