package com.jomiro.app.orobest_sign.screen.signature

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.net.URLClassLoader
import java.nio.file.Files
import javax.imageio.ImageIO

/** Captura solo imagen gris PNG; no genera ni conserva una plantilla biométrica. */
internal fun captureUareUFingerprintPng(): ByteArray {
    val sdkJar = Files.createTempFile("uareu-sdk-", ".jar")
    val sdkResource = checkNotNull(UareUFingerprint::class.java.getResourceAsStream("/uareu/dpuareu.jar")) {
        "No se encontró el SDK Java U.are.U"
    }
    sdkResource.use { input -> Files.newOutputStream(sdkJar).use(input::copyTo) }
    sdkJar.toFile().deleteOnExit()
    val loader = URLClassLoader(arrayOf(sdkJar.toUri().toURL()), UareUFingerprint::class.java.classLoader)
    val previousContextLoader = Thread.currentThread().contextClassLoader
    Thread.currentThread().contextClassLoader = loader
    var readers: Any? = null
    var opened: Any? = null
    try {
        val global = loader.loadClass("com.digitalpersona.uareu.UareUGlobal")
        val collection = global.getMethod("GetReaderCollection").invoke(null)
        readers = collection
        val collectionClass = loader.loadClass("com.digitalpersona.uareu.ReaderCollection")
        collectionClass.getMethod("GetReaders").invoke(collection)
        val reader = (collection as? List<*>)?.firstOrNull() ?: error("No se encontró un lector U.are.U")
        opened = reader
        val readerClass = loader.loadClass("com.digitalpersona.uareu.Reader")
        val priorityClass = loader.loadClass("com.digitalpersona.uareu.Reader\$Priority")
        fun enumValue(className: String, value: String): Any = loader.loadClass(className)
            .getMethod("valueOf", String::class.java).invoke(null, value)
        readerClass.getMethod("Open", priorityClass)
            .invoke(reader, enumValue("com.digitalpersona.uareu.Reader\$Priority", "COOPERATIVE"))
        val capabilities = readerClass.getMethod("GetCapabilities").invoke(reader)
        val resolutions = capabilities.javaClass.getField("resolutions").get(capabilities) as? IntArray
        val resolution = resolutions?.firstOrNull() ?: error("El lector no informa resoluciones compatibles")
        check(capabilities.javaClass.getField("can_capture").getBoolean(capabilities)) {
            "El dispositivo detectado no permite capturar imágenes"
        }
        val captureMethod = readerClass.methods.first { it.name == "Capture" && it.parameterCount == 4 }
        val capture = captureMethod.invoke(
            reader,
            enumValue("com.digitalpersona.uareu.Fid\$Format", "ANSI_381_2004"),
            enumValue("com.digitalpersona.uareu.Reader\$ImageProcessing", "IMG_PROC_DEFAULT"),
            resolution,
            15_000,
        )
        val quality = capture.javaClass.getField("quality").get(capture).toString()
        check(quality == "GOOD") {
            when (quality) {
                "TIMED_OUT", "NO_FINGER" -> "No se detectó un dedo dentro del tiempo límite"
                "CANCELED" -> "La captura fue cancelada"
                else -> "El lector rechazó la captura: $quality"
            }
        }
        val fid = capture.javaClass.getField("image").get(capture) ?: error("El lector no devolvió una imagen")
        val fidClass = loader.loadClass("com.digitalpersona.uareu.Fid")
        val view = (fidClass.getMethod("getViews").invoke(fid) as? Array<*>)?.firstOrNull()
            ?: error("El lector no devolvió una imagen")
        val viewClass = loader.loadClass("com.digitalpersona.uareu.Fid\$Fiv")
        val width = viewClass.getMethod("getWidth").invoke(view) as Int
        val height = viewClass.getMethod("getHeight").invoke(view) as Int
        val pixels = viewClass.getMethod("getImageData").invoke(view) as ByteArray
        require(width in 1..2048 && height in 1..2048 && pixels.size == width * height) {
            "Formato de imagen de huella no compatible"
        }
        val image = BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY)
        image.raster.setDataElements(0, 0, width, height, pixels)
        return ByteArrayOutputStream().use { output ->
            check(ImageIO.write(image, "png", output)) { "No se pudo codificar la imagen" }
            output.toByteArray()
        }
    } finally {
        runCatching {
            opened?.let { loader.loadClass("com.digitalpersona.uareu.Reader").getMethod("Close").invoke(it) }
        }
        runCatching {
            readers?.let {
                loader.loadClass("com.digitalpersona.uareu.UareUGlobal")
                    .getMethod("DestroyReaderCollection").invoke(null)
            }
        }
        Thread.currentThread().contextClassLoader = previousContextLoader
        runCatching { loader.close() }
    }
}

private object UareUFingerprint
