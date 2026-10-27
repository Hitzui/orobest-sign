# Escaneo de documentos en Windows

La versión de escritorio permite adquirir documentos directamente mediante **WIA o TWAIN**, incluida una Epson de red configurada en Epson Scan 2. La integración enumera los dispositivos de Windows y las fuentes TWAIN de 32/64 bits, sin nombres ni modelos fijos.

## Preparar la PC

1. Instala el controlador de **escáner** de Epson correspondiente al modelo. El controlador de impresión por sí solo no basta.
2. Enciende el equipo y conéctalo por USB o por la red que admita su controlador.
3. Para equipos en red, selecciona la impresora en **Epson Scan 2 Utility** y verifica que Epson Scan 2 pueda escanear. Que funcione allí no garantiza que aparezca en WIA: muchos controladores Epson se publican como fuentes TWAIN de 32 bits. No es necesario que aparezca en Escáner de Windows si la fuente TWAIN está disponible.
4. Ejecuta OroBest Sign en Windows. Se usa Windows PowerShell 5.1, incluido en Windows, en procesos STA de 32/64 bits. El MSI incorpora NTwain 3.7.6 (MIT) para acceder a TWAIN mediante .NET Framework 4.6.2 o posterior, sin instalar otra aplicación de escaneo. Las fuentes exclusivamente de 64 bits requieren que su instalador haya suministrado el gestor TWAIN de 64 bits; su ausencia no bloquea WIA ni TWAIN de 32 bits.

## Uso

En **Clientes → Documentos**, aparece el panel **Escáner de documentos**:

1. Coloca una cara del documento sobre el cristal.
2. Pulsa **Escanear frente**, **Escanear reverso** o **Escanear otro**.
3. La primera vez, elige un equipo de la lista. Las entradas indican **WIA**, **TWAIN 32 bits** o **TWAIN 64 bits**. Para una Epson que funciona en Epson Scan 2 pero no aparece en WIA, selecciona su entrada TWAIN. La selección se recuerda para ese usuario de Windows en esa PC, también después de cerrar la app. Si un controlador Epson administra varias conexiones, se usa la selección de red configurada en Epson Scan 2.
4. Se solicita escaneo en color a 300 ppp y se prefiere el cristal si el controlador permite esos ajustes. Los controladores que no los exponen usan sus valores disponibles. Se adquiere **una imagen por acción**, sin activar lotes ADF ni dúplex automáticamente. Si el controlador TWAIN no permite adquisición sin interfaz, se intenta abrir su ventana; completa allí el escaneo para recibir la vista previa en Orobest.
5. Revisa la vista previa con zoom. **Usar imagen** la deja seleccionada en la categoría indicada; **Descartar** la libera.
6. Pulsa **Subir documentos** para enviarla al cliente. Escanear o revisar no sube nada automáticamente. Para la otra cara o más documentos, repite el proceso.

Se conserva el selector habitual **Tomar foto / Elegir fotos / Elegir archivos** de escritorio para adjuntar archivos existentes.

## Cambiar o recuperar el escáner

- **Cambiar escáner** vuelve a consultar los equipos disponibles y guarda la nueva selección. Cambiar de Epson no exige cambiar código si su controlador ofrece WIA o TWAIN compatible. La preferencia WIA de la versión anterior se conserva.
- Si el equipo guardado desaparece, se apaga o se desconecta, la app conserva la preferencia y ofrece reintentar; también puedes seleccionar otro.
- **Actualizar** en el selector vuelve a consultar los equipos, útil después de conectar o instalar uno.
- Si está ocupado, cierra Epson Scan 2 u otra aplicación que lo esté utilizando. Si no aparece, revisa el controlador de escáner y su configuración de red. Para WIA, revisa también el servicio **Adquisición de imágenes de Windows**. La detección TWAIN se realiza separadamente para ver fuentes de 32 bits aunque Orobest se ejecute en 64 bits.
- Si una política de la PC impide ejecutar Windows PowerShell, el escaneo directo mostrará un error; el selector de archivos sigue disponible.
- Desde 1.0.3, los mensajes que el controlador escriba en consola no interfieren con la respuesta del escaneo. Si aparece **terminó sin entregar una respuesta**, comunica el texto completo, incluido el código de salida si lo hay: indica que el proceso terminó sin publicar su resultado, por ejemplo por un cierre nativo del controlador. No equivale a que el equipo no haya sido detectado.

## Almacenamiento y ciclo de vida

Solo se guardan el identificador y el nombre del escáner mediante `java.util.prefs.Preferences`. No se guardan documentos ni credenciales en esas preferencias. La preferencia es de la PC/usuario de Windows y no depende del cliente o usuario de OroBest.

Los puentes WIA/TWAIN utilizan un directorio temporal por operación, que se elimina al terminar, fallar o cancelar. La imagen se convierte a JPG, conserva su proporción y se limita a 4000 píxeles en su lado mayor y 10 MiB para respetar el contrato de carga. La vista previa y el documento seleccionado viven en memoria. Cancelar o salir de la pantalla del cliente detiene el proceso de adquisición y descarta su vista previa; una terminación abrupta del sistema puede dejar temporales en la carpeta temporal del usuario de Windows.

La respuesta se escribe en `result.json` dentro de ese directorio, en UTF-8 y mediante renombrado de un archivo temporal, separada de stdout/stderr del controlador. Estos canales de consola se descartan; no se guardan registros persistentes con información del documento.

El envío usa la API existente de documentos, con sus mismos reintentos y permisos. No requiere PHP nuevo ni cambios de esquema. Android/iOS continúan usando sus selectores y cámara; el panel de escáner solo aparece en Windows.

## Verificación

Las pruebas JVM cubren selección inicial, reutilización de la preferencia, sustitución del equipo, desconexión, cancelación/respuestas tardías, cierre del alcance de cliente y conversión de una imagen BMP sintética a JPG. La enumeración WIA se puede comprobar sin adquirir documentos, ejecutando una copia temporal del script y leyendo su archivo de respuesta:

```powershell
$directory = Join-Path $env:TEMP ('orobest-wia-check-' + [guid]::NewGuid())
New-Item -ItemType Directory -Path $directory | Out-Null
try {
    Copy-Item shared/src/jvmMain/resources/scanner/wia.ps1 $directory
    powershell.exe -NoProfile -NonInteractive -STA -ExecutionPolicy Bypass -File (Join-Path $directory 'wia.ps1') -Action List
    Get-Content -LiteralPath (Join-Path $directory 'result.json') -Encoding UTF8
} finally {
    Remove-Item -LiteralPath $directory -Recurse -Force
}
```

La adquisición física y la calidad del escaneo deben validarse en la Epson y PC de destino.

La versión 1.0.2 incorpora TWAIN después de detectar que una Epson L5590 de red funcionaba en Epson Scan 2 pero no se publicaba en WIA. En desarrollo se comprobó la enumeración real de fuentes Epson de 32 bits que el detector WIA anterior no mostraba. Las pruebas de descubrimiento cubren ese caso, el fallo aislado de una familia de controladores y la conservación de la arquitectura en la preferencia. La adquisición física en la L5590 del cliente sigue requiriendo validación.

Verificación de 1.0.2: doce pruebas JVM específicas de escaneo aprobadas y MSI generado. Desde el JAR del paquete se enumeraron fuentes WIA/TWAIN reales, incluidas nueve fuentes TWAIN de 32 bits, y se comprobó el error de fuente inexistente sin adquirir documentos. Se verificó la eliminación de los temporales en ambos caminos; el cierre reintenta brevemente para permitir que Windows libere los archivos DLL y la salida del proceso.

Verificación de 1.0.3: quince pruebas JVM específicas de escaneo aprobadas, incluidas tres de comunicación real con Windows PowerShell y una fuente sintética. Cubren mensajes arbitrarios en stdout/stderr durante listado/adquisición, imagen BMP de prueba normalizada a JPG, errores, cancelación, terminación abrupta con código y limpieza temporal. MSI generado e identidad Orobest/SEIN/1.0.3 y UpgradeCode comprobados. Desde los JAR del paquete se enumeraron dos equipos WIA y nueve fuentes TWAIN de 32 bits y se validaron errores de ID inexistente y limpieza para ambos puentes. No se realizó adquisición física; la captura en la L5590 de destino sigue pendiente.

Verificación del 17 de octubre de 2026 en Windows: pruebas JVM/Android y APK debug correctos, distribución de escritorio generada, ocho pruebas JVM específicas de escaneo aprobadas. El puente JVM del paquete enumeró equipos WIA reales, manejó un identificador inexistente sin adquirir imágenes y eliminó sus directorios temporales en ambos casos. No se realizó adquisición física de documentos.
