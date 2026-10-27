package com.jomiro.app.orobest_sign.screen.clients

import androidx.lifecycle.ViewModelStore
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientDocumentCategory
import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientFile
import com.jomiro.app.orobest_sign.feature.clients.domain.scanner.DocumentScanner
import com.jomiro.app.orobest_sign.feature.clients.domain.scanner.ScannerDevice
import com.jomiro.app.orobest_sign.feature.clients.domain.scanner.ScannerException
import com.jomiro.app.orobest_sign.feature.clients.domain.scanner.ScannerPreferenceStore
import com.jomiro.app.orobest_sign.screen.clients.viewmodel.DesktopScannerViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DesktopScannerTest {
    @BeforeTest fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun firstScanRequiresSelectionAndNextInstanceUsesRememberedDevice() = runTest {
        val preferences = MemoryPreferences()
        val scanner = FakeScanner()
        val vm = DesktopScannerViewModel(scanner, preferences)
        advanceUntilIdle()
        vm.scan(ClientDocumentCategory.IdentityBack)
        advanceUntilIdle()
        assertTrue(vm.state.value.selecting)
        assertEquals(listOf(epson, replacement), vm.state.value.devices)
        assertEquals(0, scanner.scans)
        vm.select(epson)
        advanceUntilIdle()
        assertEquals(epson, preferences.device)
        assertFalse(vm.state.value.selecting)
        assertEquals(ClientDocumentCategory.IdentityBack, vm.state.value.preview?.category)
        assertTrue(vm.state.value.busy)
        assertNotNull(vm.takePreview())
        assertNull(vm.takePreview())
        assertFalse(vm.state.value.busy)

        val recreated = DesktopScannerViewModel(scanner, preferences)
        advanceUntilIdle()
        recreated.scan(ClientDocumentCategory.IdentityFront)
        advanceUntilIdle()
        assertFalse(recreated.state.value.selecting)
        assertEquals(epson, scanner.lastDevice)
        assertEquals(1, scanner.listings)
        assertEquals(2, scanner.scans)
    }

    @Test
    fun unavailableScannerKeepsPreferenceAndReplacementDoesNotScanUntilRequested() = runTest {
        val preferences = MemoryPreferences(epson)
        val scanner = FakeScanner().apply { failure = ScannerException("Escáner desconectado") }
        val vm = DesktopScannerViewModel(scanner, preferences)
        advanceUntilIdle()
        vm.scan(ClientDocumentCategory.Other)
        advanceUntilIdle()
        assertEquals("Escáner desconectado", vm.state.value.error)
        assertNull(vm.state.value.preview)
        assertFalse(vm.state.value.busy)
        assertEquals(epson, preferences.device)
        vm.changeScanner()
        advanceUntilIdle()
        vm.select(replacement)
        advanceUntilIdle()
        assertEquals(replacement, preferences.device)
        assertEquals(1, scanner.scans)
        scanner.failure = null
        vm.scan(ClientDocumentCategory.Other)
        advanceUntilIdle()
        assertEquals(replacement, scanner.lastDevice)
        assertNotNull(vm.state.value.preview)
    }

    @Test
    fun cancelIgnoresLateAcquisitionAndDoesNotLoseSavedScanner() = runTest {
        val result = CompletableDeferred<ClientFile>()
        val scanner = FakeScanner().apply { acquire = { withContext(NonCancellable) { result.await() } } }
        val preferences = MemoryPreferences(epson)
        val vm = DesktopScannerViewModel(scanner, preferences)
        advanceUntilIdle()
        vm.scan(ClientDocumentCategory.Other)
        runCurrent()
        assertTrue(vm.state.value.working)
        vm.scan(ClientDocumentCategory.IdentityFront)
        assertEquals(1, scanner.scans)
        vm.cancel()
        assertFalse(vm.state.value.busy)
        result.complete(image)
        advanceUntilIdle()
        assertNull(vm.state.value.preview)
        assertNull(vm.state.value.error)
        assertEquals(epson, preferences.device)
    }

    @Test
    fun leavingClientCancelsAcquisitionAndCannotPublishPreview() = runTest {
        val result = CompletableDeferred<ClientFile>()
        val scanner = FakeScanner().apply { acquire = { result.await() } }
        val vm = DesktopScannerViewModel(scanner, MemoryPreferences(epson))
        val store = ViewModelStore().apply { put("scanner", vm) }
        advanceUntilIdle()
        vm.scan(ClientDocumentCategory.IdentityFront)
        runCurrent()
        store.clear()
        result.complete(image)
        advanceUntilIdle()
        assertNull(vm.state.value.preview)
    }

    @Test
    fun emptyDeviceListCanBeRefreshedWithoutForgettingPendingCategory() = runTest {
        val scanner = FakeScanner().apply { available = emptyList() }
        val vm = DesktopScannerViewModel(scanner, MemoryPreferences())
        advanceUntilIdle()
        vm.scan(ClientDocumentCategory.IdentityFront)
        advanceUntilIdle()
        assertTrue(vm.state.value.selecting)
        assertNotNull(vm.state.value.error)
        scanner.available = listOf(replacement)
        vm.refreshDevices()
        advanceUntilIdle()
        vm.select(replacement)
        advanceUntilIdle()
        assertEquals(ClientDocumentCategory.IdentityFront, vm.state.value.preview?.category)
        vm.cancel()
        assertNull(vm.state.value.preview)
    }

    private class MemoryPreferences(var device: ScannerDevice? = null) : ScannerPreferenceStore {
        override suspend fun read() = device
        override suspend fun write(device: ScannerDevice) { this.device = device }
    }

    private inner class FakeScanner : DocumentScanner {
        var available = listOf(epson, replacement)
        var failure: Exception? = null
        var listings = 0
        var scans = 0
        var lastDevice: ScannerDevice? = null
        var acquire: suspend () -> ClientFile = { image }
        override suspend fun devices(): List<ScannerDevice> { listings++; return available }
        override suspend fun scan(device: ScannerDevice): ClientFile {
            scans++
            lastDevice = device
            failure?.let { throw it }
            return acquire()
        }
    }

    private val epson = ScannerDevice("synthetic-device-1", "Epson de prueba")
    private val replacement = ScannerDevice("synthetic-device-2", "Epson de reemplazo")
    private val image = ClientFile(byteArrayOf(1, 2, 3), "escaneo.jpg", "image/jpeg")
}
