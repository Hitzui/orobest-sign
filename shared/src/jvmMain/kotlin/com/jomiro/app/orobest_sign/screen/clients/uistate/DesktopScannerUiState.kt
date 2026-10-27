package com.jomiro.app.orobest_sign.screen.clients.uistate

import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientDocumentCategory
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientFile
import com.jomiro.app.orobest_sign.feature.clients.domain.scanner.ScannerDevice

internal data class ScannedDocument(val category: ClientDocumentCategory, val file: ClientFile)

internal data class DesktopScannerUiState(
    val selected: ScannerDevice? = null,
    val devices: List<ScannerDevice> = emptyList(),
    val working: Boolean = true,
    val selecting: Boolean = false,
    val preview: ScannedDocument? = null,
    val error: String? = null,
    val lastCategory: ClientDocumentCategory? = null,
) {
    val busy: Boolean get() = working || selecting || preview != null
}
