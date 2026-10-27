package com.jomiro.app.orobest_sign.feature.clients.data.scanner

import com.jomiro.app.orobest_sign.feature.clients.domain.scanner.ScannerDevice
import com.jomiro.app.orobest_sign.feature.clients.domain.scanner.ScannerPreferenceStore
import java.util.prefs.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class WindowsScannerPreferences(
    private val preferences: Preferences = Preferences.userRoot().node("com/jomiro/orobest-sign/scanner"),
) : ScannerPreferenceStore {

    override suspend fun read(): ScannerDevice? = withContext(Dispatchers.IO) {
        val id = preferences.get("deviceId", "")
        if (id.isBlank()) null else ScannerDevice(id, preferences.get("deviceName", "Escáner guardado"))
    }

    override suspend fun write(device: ScannerDevice): Unit = withContext(Dispatchers.IO) {
        preferences.put("deviceId", device.id)
        preferences.put("deviceName", device.name)
        preferences.flush()
    }
}
