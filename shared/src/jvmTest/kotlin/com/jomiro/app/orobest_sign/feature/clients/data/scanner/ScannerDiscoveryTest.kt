package com.jomiro.app.orobest_sign.feature.clients.data.scanner

import com.jomiro.app.orobest_sign.feature.clients.domain.scanner.ScannerDevice
import com.jomiro.app.orobest_sign.feature.clients.domain.scanner.ScannerException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ScannerDiscoveryTest {
    @Test
    fun networkEpsonTwain32IsFoundWhenWiaIsEmptyAndTwain64IsUnavailable() = runTest {
        val found = collectScannerDevices { driver -> when (driver) {
            NativeScannerDriver.Wia -> emptyList()
            NativeScannerDriver.Twain32 -> listOf(ScannerDevice("EPSON Synthetic Series", "EPSON Synthetic Series"))
            NativeScannerDriver.Twain64 -> throw ScannerException("No 64-bit DSM installed")
        } }
        assertEquals(1, found.size)
        assertEquals("twain32:EPSON Synthetic Series", found.single().id)
        assertTrue(found.single().name.contains("TWAIN 32 bits"))
        assertEquals(NativeScannerDriver.Twain32, scannerDriver(found.single().id))
    }

    @Test
    fun sameSourceNameKeepsArchitectureAndLegacyWiaPreferenceRemainsValid() = runTest {
        val found = collectScannerDevices { driver -> listOf(ScannerDevice(
            if (driver == NativeScannerDriver.Wia) "legacy-wia-device" else "Same Scanner",
            "Same Scanner",
        )) }
        assertEquals(3, found.size)
        assertEquals(NativeScannerDriver.Wia, scannerDriver("legacy-wia-device"))
        assertEquals(NativeScannerDriver.Twain32, scannerDriver("twain32:Same Scanner"))
        assertEquals(NativeScannerDriver.Twain64, scannerDriver("twain64:Same Scanner"))
    }

    @Test
    fun cancellationStopsDiscoveryInsteadOfTryingOtherDrivers() = runTest {
        var requests = 0
        assertFailsWith<CancellationException> {
            collectScannerDevices { requests++; throw CancellationException("Left client screen") }
        }
        assertEquals(1, requests)
    }

    @Test
    fun totalDriverFailureIsReportedRatherThanClaimingThereAreNoDevices() = runTest {
        assertFailsWith<ScannerException> {
            collectScannerDevices { throw ScannerException("Driver failed to load") }
        }
    }
}
