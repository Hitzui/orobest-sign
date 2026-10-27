package com.jomiro.app.orobest_sign.feature.clients.domain.scanner

import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientFile

internal data class ScannerDevice(val id: String, val name: String)

internal interface DocumentScanner {
    suspend fun devices(): List<ScannerDevice>
    suspend fun scan(device: ScannerDevice): ClientFile
}

/** Only the device identity is persisted; document contents remain in memory. */
internal interface ScannerPreferenceStore {
    suspend fun read(): ScannerDevice?
    suspend fun write(device: ScannerDevice)
}

internal class ScannerException(val userMessage: String) : Exception(userMessage)
