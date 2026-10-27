package com.jomiro.app.orobest_sign.feature.clients.data.scanner

import com.jomiro.app.orobest_sign.feature.clients.domain.scanner.ScannerDevice
import com.jomiro.app.orobest_sign.feature.clients.domain.scanner.ScannerException
import java.io.File
import javax.imageio.ImageIO
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assume.assumeTrue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Real PowerShell/JSON/image round trip with a synthetic source, never a physical scanner. */
class ScannerBridgeTest {
    @Test
    fun driverConsoleMessagesCannotCorruptDeviceListOrAcquiredImage() = runBlocking {
        assumeTrue(isWindowsDesktop())
        val before = temporaryDirectories()
        val scanner = scanner()
        val device = scanner.devices().first { it.id.startsWith("twain32:") }
        assertEquals("twain32:Escáner sintético", device.id)
        val scanned = scanner.scan(device)
        assertEquals("image/jpeg", scanned.mimeType)
        val image = ImageIO.read(scanned.bytes.inputStream())
        try {
            assertEquals(80, image.width)
            assertEquals(50, image.height)
        } finally { image.flush() }
        assertTrue((temporaryDirectories() - before).isEmpty(), "The bridge must remove its temporary files")
    }

    @Test
    fun structuredDriverErrorAndCancellationRemainReadableDespiteConsoleNoise() = runBlocking {
        assumeTrue(isWindowsDesktop())
        val before = temporaryDirectories()
        val scanner = scanner()
        val error = assertFailsWith<ScannerException> {
            scanner.scan(ScannerDevice("twain32:busy", "Synthetic"))
        }
        assertTrue(error.userMessage.contains("ocupado"))
        assertFailsWith<CancellationException> {
            scanner.scan(ScannerDevice("twain32:cancel", "Synthetic"))
        }
        assertTrue((temporaryDirectories() - before).isEmpty())
    }

    @Test
    fun nativeProcessTerminationReportsMissingResponseAndExitCode() = runBlocking {
        assumeTrue(isWindowsDesktop())
        val before = temporaryDirectories()
        val error = assertFailsWith<ScannerException> {
            scanner().scan(ScannerDevice("twain32:crash", "Synthetic"))
        }
        assertTrue(error.userMessage.contains("terminó sin entregar una respuesta"))
        assertTrue(error.userMessage.contains("0x17"))
        assertTrue((temporaryDirectories() - before).isEmpty())
    }

    private fun scanner() = WindowsDocumentScanner(Json) { name ->
        if (name == "TwainBridge.cs") syntheticBridge.byteInputStream()
        else javaClass.getResourceAsStream("/scanner/$name")
    }

    private fun temporaryDirectories(): Set<String> = File(System.getProperty("java.io.tmpdir"))
        .listFiles().orEmpty().filter { it.name.startsWith("orobest-scan-") }.map { it.absolutePath }.toSet()

    private val syntheticBridge = """
        using System;
        using System.Drawing;
        using System.Drawing.Imaging;
        namespace Orobest.Scanner {
            public static class TwainBridge {
                public static object Run(string action, string device, string path) {
                    Console.WriteLine("Native driver diagnostic before response: {not JSON}");
                    Console.Error.WriteLine("Native driver stderr diagnostic");
                    if (device == "crash") Environment.Exit(23);
                    if (device == "busy") return new { status = "error", code = "device_busy" };
                    if (device == "cancel") return new { status = "cancelled" };
                    if (action == "List") return new { status = "ok", devices = new[] {
                        new { id = "Esc\u00e1ner sint\u00e9tico", name = "Esc\u00e1ner sint\u00e9tico" }
                    } };
                    using (var image = new Bitmap(80, 50)) {
                        using (var graphics = Graphics.FromImage(image)) graphics.Clear(Color.Blue);
                        image.Save(path, ImageFormat.Bmp);
                    }
                    Console.WriteLine("Native driver diagnostic after transfer");
                    return new { status = "ok" };
                }
            }
        }
    """.trimIndent()
}
