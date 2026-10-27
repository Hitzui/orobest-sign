package com.jomiro.app.orobest_sign.feature.clients.data.scanner

import com.jomiro.app.orobest_sign.feature.clients.domain.scanner.ScannerDevice
import com.jomiro.app.orobest_sign.feature.clients.domain.scanner.ScannerException
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.util.UUID
import java.util.prefs.Preferences
import javax.imageio.ImageIO
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ScannedImageTest {
    @Test
    fun bmpFromDriverBecomesBoundedJpegWithReadableContents() {
        val file = Files.createTempFile("synthetic-scan-", ".bmp").toFile()
        val source = BufferedImage(5000, 600, BufferedImage.TYPE_INT_RGB)
        try {
            val graphics = source.createGraphics()
            try {
                graphics.color = Color.WHITE
                graphics.fillRect(0, 0, source.width, source.height)
                graphics.color = Color.BLUE
                graphics.fillRect(0, 0, 1000, 600)
            } finally { graphics.dispose() }
            ImageIO.write(source, "bmp", file)
            val scanned = normalizeScannedImage(file)
            assertEquals("image/jpeg", scanned.mimeType)
            assertTrue(scanned.fileName.endsWith(".jpg"))
            assertTrue(scanned.bytes.size in 1..(10 * 1024 * 1024))
            val decoded = ImageIO.read(ByteArrayInputStream(scanned.bytes))
            try {
                assertEquals(4000, decoded.width)
                assertEquals(480, decoded.height)
                val blue = Color(decoded.getRGB(100, 100))
                assertTrue(blue.blue > 200 && blue.red < 30)
                val white = Color(decoded.getRGB(3000, 100))
                assertTrue(white.red > 240 && white.green > 240 && white.blue > 240)
            } finally { decoded.flush() }
        } finally {
            source.flush()
            file.delete()
        }
    }

    @Test
    fun invalidDriverOutputIsRejectedBeforeUpload() {
        val file = Files.createTempFile("synthetic-invalid-scan-", ".bmp").toFile()
        try {
            file.writeText("not an image")
            assertFailsWith<ScannerException> { normalizeScannedImage(file) }
            file.writeBytes(byteArrayOf())
            assertFailsWith<ScannerException> { normalizeScannedImage(file) }
        } finally { file.delete() }
    }

    @Test
    fun scannerPreferenceSurvivesStoreRecreationWithoutSavingDocumentBytes() = runTest {
        val node = Preferences.userRoot().node("com/jomiro/orobest-sign/scanner-test-${UUID.randomUUID()}")
        try {
            val first = WindowsScannerPreferences(node)
            assertEquals(null, first.read())
            val device = ScannerDevice("synthetic-device", "Escáner sintético")
            first.write(device)
            assertEquals(device, WindowsScannerPreferences(node).read())
            val twainDevice = ScannerDevice("twain32:EPSON Synthetic Series", "EPSON Synthetic Series · TWAIN 32 bits")
            first.write(twainDevice)
            assertEquals(twainDevice, WindowsScannerPreferences(node).read())
            assertEquals(setOf("deviceId", "deviceName"), node.keys().toSet())
        } finally {
            node.removeNode()
            Preferences.userRoot().flush()
        }
    }
}
