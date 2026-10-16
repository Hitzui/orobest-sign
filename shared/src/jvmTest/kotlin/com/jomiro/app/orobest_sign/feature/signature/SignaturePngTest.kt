package com.jomiro.app.orobest_sign.feature.signature

import com.jomiro.app.orobest_sign.feature.signature.data.image.DefaultSignaturePngEncoder
import com.jomiro.app.orobest_sign.feature.signature.domain.model.SignaturePoint
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SignaturePngTest {
    @Test
    fun exportsUprightLandscapePngWithWhiteBackgroundAndNoGuide() {
        val png = DefaultSignaturePngEncoder().encode(listOf(listOf(SignaturePoint(0.15f, 0.2f), SignaturePoint(0.35f, 0.3f))))
        assertContentEquals(byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10), png.copyOf(8))
        assertTrue(png.size <= 2 * 1024 * 1024)
        val image = ImageIO.read(ByteArrayInputStream(png))
        assertEquals(1600, image.width)
        assertEquals(800, image.height)
        assertEquals(-1, image.getRGB(1400, 700))
        assertTrue((150..250).any { y -> (230..570).any { x -> image.getRGB(x, y) != -1 } })
        assertTrue((0 until 1600).all { x -> image.getRGB(x, 656) == -1 })
        assertTrue((0 until 800).all { y -> image.getRGB(1200, y) == -1 })
    }

    @Test
    fun aSingleTapIsVisibleAndEmptyDrawingCannotBeExported() {
        val png = DefaultSignaturePngEncoder().encode(listOf(listOf(SignaturePoint(0.5f, 0.5f))))
        val image = ImageIO.read(ByteArrayInputStream(png))
        assertTrue(image.getRGB(800, 400) != -1)
        assertFailsWith<IllegalArgumentException> { DefaultSignaturePngEncoder().encode(emptyList()) }
    }
}
