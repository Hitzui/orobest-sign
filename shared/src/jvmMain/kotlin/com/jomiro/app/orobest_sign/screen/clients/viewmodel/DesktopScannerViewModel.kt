package com.jomiro.app.orobest_sign.screen.clients.viewmodel

import androidx.lifecycle.viewModelScope
import com.jomiro.app.orobest_sign.core.presentation.StateViewModel
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientDocumentCategory
import com.jomiro.app.orobest_sign.feature.clients.domain.scanner.DocumentScanner
import com.jomiro.app.orobest_sign.feature.clients.domain.scanner.ScannerDevice
import com.jomiro.app.orobest_sign.feature.clients.domain.scanner.ScannerException
import com.jomiro.app.orobest_sign.feature.clients.domain.scanner.ScannerPreferenceStore
import com.jomiro.app.orobest_sign.screen.clients.uistate.DesktopScannerUiState
import com.jomiro.app.orobest_sign.screen.clients.uistate.ScannedDocument
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

internal class DesktopScannerViewModel(
    private val scanner: DocumentScanner,
    private val preferences: ScannerPreferenceStore,
) : StateViewModel<DesktopScannerUiState>(DesktopScannerUiState()) {
    private var operation: Job? = null
    private var generation = 0
    private var scanAfterSelection: ClientDocumentCategory? = null

    init {
        perform {
            val selected = preferences.read()
            currentCoroutineContext().ensureActive()
            updateState { it.copy(selected = selected) }
        }
    }

    fun scan(category: ClientDocumentCategory) {
        if (state.value.busy) return
        updateState { it.copy(lastCategory = category, error = null) }
        val selected = state.value.selected
        if (selected == null) {
            scanAfterSelection = category
            listDevices()
        } else {
            perform {
                val file = scanner.scan(selected)
                currentCoroutineContext().ensureActive()
                updateState { it.copy(preview = ScannedDocument(category, file)) }
            }
        }
    }

    fun retry() { state.value.lastCategory?.let(::scan) }

    fun changeScanner() {
        if (state.value.busy) return
        scanAfterSelection = null
        listDevices()
    }

    fun refreshDevices() {
        if (!state.value.working && state.value.selecting) listDevices()
    }

    private fun listDevices() {
        updateState { it.copy(selecting = true, devices = emptyList()) }
        perform {
            val devices = scanner.devices()
            currentCoroutineContext().ensureActive()
            updateState { it.copy(devices = devices, error = if (devices.isEmpty())
                "No se encontraron escáneres WIA ni TWAIN. Comprueba que Epson Scan 2 tenga seleccionado el equipo de red y pulsa Actualizar." else null) }
        }
    }

    fun select(device: ScannerDevice) {
        if (state.value.working || !state.value.selecting || device !in state.value.devices) return
        val category = scanAfterSelection
        perform {
            preferences.write(device)
            currentCoroutineContext().ensureActive()
            updateState { it.copy(selected = device, selecting = false) }
            scanAfterSelection = null
            if (category != null) {
                val file = scanner.scan(device)
                currentCoroutineContext().ensureActive()
                updateState { it.copy(preview = ScannedDocument(category, file)) }
            }
        }
    }

    fun takePreview(): ScannedDocument? {
        val document = state.value.preview ?: return null
        updateState { it.copy(preview = null, lastCategory = null) }
        return document
    }

    fun cancel() {
        generation++
        operation?.cancel()
        operation = null
        scanAfterSelection = null
        updateState { it.copy(working = false, selecting = false, preview = null, error = null, lastCategory = null) }
    }

    private fun perform(block: suspend () -> Unit) {
        val current = ++generation
        updateState { it.copy(working = true, error = null) }
        operation = viewModelScope.launch {
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (current == generation) updateState { it.copy(error = (error as? ScannerException)?.userMessage
                    ?: "No se pudo acceder al escáner o guardar la selección. Revisa la conexión y los permisos de Windows.") }
            } finally {
                if (current == generation) updateState { it.copy(working = false) }
            }
        }
    }
}
