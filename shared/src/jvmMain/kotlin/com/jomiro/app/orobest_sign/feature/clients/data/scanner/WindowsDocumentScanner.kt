package com.jomiro.app.orobest_sign.feature.clients.data.scanner

import com.jomiro.app.orobest_sign.feature.clients.domain.model.ClientFile
import com.jomiro.app.orobest_sign.feature.clients.domain.scanner.DocumentScanner
import com.jomiro.app.orobest_sign.feature.clients.domain.scanner.ScannerDevice
import com.jomiro.app.orobest_sign.feature.clients.domain.scanner.ScannerException
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class ScannerDeviceDto(val id: String, val name: String)

@Serializable
private data class ScannerBridgeResponse(val status: String, val devices: List<ScannerDeviceDto> = emptyList(), val code: String? = null)

internal enum class NativeScannerDriver(val prefix: String, val script: String, val label: String) {
    Wia("", "wia.ps1", "WIA"),
    Twain32("twain32:", "twain.ps1", "TWAIN 32 bits"),
    Twain64("twain64:", "twain.ps1", "TWAIN 64 bits"),
}

internal fun scannerDriver(id: String): NativeScannerDriver = when {
    id.startsWith(NativeScannerDriver.Twain32.prefix) -> NativeScannerDriver.Twain32
    id.startsWith(NativeScannerDriver.Twain64.prefix) -> NativeScannerDriver.Twain64
    else -> NativeScannerDriver.Wia // Preserve preferences saved by the WIA-only version.
}

internal suspend fun collectScannerDevices(list: suspend (NativeScannerDriver) -> List<ScannerDevice>): List<ScannerDevice> {
    val found = mutableListOf<ScannerDevice>()
    var successful = false
    var lastFailure: ScannerException? = null
    for (driver in NativeScannerDriver.entries) {
        try {
            found += list(driver).map { ScannerDevice(driver.prefix + it.id, "${it.name} · ${driver.label}") }
            successful = true
        } catch (error: ScannerException) {
            // Absence/failure of one driver family must not hide the others.
            lastFailure = error
        }
    }
    if (!successful) throw requireNotNull(lastFailure)
    return found.distinctBy { it.id }.sortedBy { it.name }
}

/** Enumerates WIA and both TWAIN architectures, independently of scanner brand/model. */
internal class WindowsDocumentScanner(
    private val json: Json,
    private val resourceStream: (String) -> InputStream? = { WindowsDocumentScanner::class.java.getResourceAsStream("/scanner/$it") },
) : DocumentScanner {
    private val acquisition = Mutex()

    override suspend fun devices(): List<ScannerDevice> = collectScannerDevices { driver ->
        execute("List", driver = driver) { response, _ ->
            response.devices.filter { it.id.isNotBlank() }.map { ScannerDevice(it.id, it.name) }
        }
    }

    override suspend fun scan(device: ScannerDevice): ClientFile {
        val driver = scannerDriver(device.id)
        return execute("Scan", device.id.removePrefix(driver.prefix), driver) { _, directory ->
            normalizeScannedImage(File(directory, "scan.bmp"))
        }
    }

    private suspend fun <T> execute(
        action: String,
        deviceId: String? = null,
        driver: NativeScannerDriver = NativeScannerDriver.Wia,
        result: (ScannerBridgeResponse, File) -> T,
    ): T = acquisition.withLock {
        withContext(Dispatchers.IO) {
            if (!isWindowsDesktop()) throw ScannerException("El escaneo directo está disponible en Windows.")
            val directory = Files.createTempDirectory("orobest-scan-").toFile()
            var process: Process? = null
            try {
                fun copyResource(name: String) {
                    val resource = resourceStream(name)
                        ?: throw ScannerException("No se encontró el componente de escaneo. Reinstala la aplicación de escritorio.")
                    resource.use { input -> File(directory, name).outputStream().use { input.copyTo(it) } }
                }
                copyResource(driver.script)
                if (driver != NativeScannerDriver.Wia) {
                    copyResource("NTwain.dll")
                    copyResource("TwainBridge.cs")
                }
                val script = File(directory, driver.script)
                val systemDirectory = if (driver == NativeScannerDriver.Twain32) "SysWOW64" else "System32"
                val executable = File(System.getenv("SystemRoot") ?: "C:\\Windows", "$systemDirectory/WindowsPowerShell/v1.0/powershell.exe")
                val command = mutableListOf(executable.absolutePath, "-NoLogo", "-NoProfile", "-NonInteractive", "-STA",
                    "-ExecutionPolicy", "Bypass", "-File", script.absolutePath, "-Action", action)
                // Pass device IDs as arguments, never interpolate driver strings into code.
                if (deviceId != null) command.addAll(listOf("-DeviceId", deviceId))
                val output = File(directory, "result.json")
                // Native drivers can print arbitrary text while acquiring an image.
                // Scripts publish result.json atomically; never parse their console output.
                process = ProcessBuilder(command)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start()
                val running = process
                try {
                    withTimeout(if (action == "Scan") 300_000L else 30_000L) {
                        while (!running.waitFor(100, TimeUnit.MILLISECONDS)) currentCoroutineContext().ensureActive()
                    }
                } catch (_: TimeoutCancellationException) {
                    throw ScannerException("El escáner tardó demasiado en responder. Revisa la conexión o el controlador y vuelve a intentarlo.")
                }
                currentCoroutineContext().ensureActive()
                val exitCode = running.exitValue()
                val exitDetail = if (exitCode == 0) "" else " (código 0x${Integer.toHexString(exitCode).uppercase()})"
                if (exitCode != 0 || !output.isFile) {
                    throw ScannerException("El componente ${driver.label} terminó sin entregar una respuesta$exitDetail. Cierra Epson Scan 2 y reintenta; si se repite, comunica este mensaje para revisar el controlador.")
                }
                if (output.length() !in 1..64 * 1024) {
                    throw ScannerException("El componente ${driver.label} entregó una respuesta inválida. Vuelve a intentarlo; si se repite, reinstala la versión actual de Orobest.")
                }
                val response = try {
                    json.decodeFromString<ScannerBridgeResponse>(output.readText(Charsets.UTF_8).trim().removePrefix("\uFEFF"))
                } catch (_: Exception) {
                    throw ScannerException("No se pudo leer el archivo de respuesta del componente ${driver.label}. Vuelve a intentarlo; si se repite, reinstala la versión actual de Orobest.")
                }
                if (response.status == "cancelled") throw CancellationException("Escaneo cancelado desde el controlador")
                if (response.status != "ok") throw ScannerException(scannerErrorMessage(response.code))
                result(response, directory).also { currentCoroutineContext().ensureActive() }
            } finally {
                // Cancellation/closing the client must stop acquisition before deleting its files.
                withContext(NonCancellable) cleanup@{
                    process?.let { running ->
                        if (running.isAlive) {
                            running.destroyForcibly()
                            running.waitFor(5, TimeUnit.SECONDS)
                        }
                    }
                    // Windows can briefly retain DLL/output handles after process exit.
                    repeat(30) {
                        if (directory.deleteRecursively()) return@cleanup
                        delay(100)
                    }
                    // Last resort if an external driver/antivirus keeps a handle open.
                    directory.walkTopDown().forEach { it.deleteOnExit() }
                }
            }
        }
    }
}

internal fun isWindowsDesktop(): Boolean = System.getProperty("os.name", "").startsWith("Windows", ignoreCase = true)

private fun scannerErrorMessage(code: String?): String = when (code) {
    "device_not_found" -> "El escáner guardado ya no está disponible. Enciéndelo y reintenta, o pulsa Cambiar escáner."
    "device_offline" -> "No hay comunicación con el escáner. Revisa que esté encendido y conectado por USB o red."
    "device_busy" -> "El escáner está ocupado. Cierra Epson Scan u otra aplicación que lo esté utilizando y reintenta."
    "paper_empty" -> "El escáner indica que no hay papel. Coloca el documento en el cristal o revisa el alimentador."
    "paper_jam" -> "El escáner detectó un atasco de papel. Resuélvelo antes de reintentar."
    "cover_open" -> "Cierra la tapa del escáner y vuelve a intentarlo."
    "wia_unavailable" -> "Windows no tiene disponible WIA. Instala el controlador de escáner del fabricante y revisa el servicio Adquisición de imágenes de Windows."
    "twain_unavailable" -> "No está disponible el controlador TWAIN de esta arquitectura. Instala Epson Scan 2 y selecciona su equipo de red."
    "twain_open_failed" -> "No se pudo abrir el escáner TWAIN. Cierra Epson Scan 2, verifica su configuración de red y vuelve a intentarlo."
    "unsupported_transfer" -> "El controlador TWAIN no ofrece transferencia de imagen compatible. Prueba otra entrada del mismo escáner."
    "scanner_timeout" -> "El controlador del escáner no terminó a tiempo. Revisa si dejó una ventana abierta y reintenta."
    "invalid_image" -> "El controlador no devolvió una imagen válida. Revisa sus ajustes y vuelve a intentarlo."
    else -> "No se pudo escanear. Revisa el documento, la conexión y el controlador; puedes reintentar o cambiar de escáner."
}
