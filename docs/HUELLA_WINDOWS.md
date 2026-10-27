# Captura de huella en Windows

En la captura de firma de OroBest Sign para escritorio aparece **Capturar huella**. El control usa `dpuareu.jar` y el SDK instalado de DigitalPersona/U.are.U en esa computadora. La captura se convierte a PNG y viaja con el envío de firma; Android e iOS no muestran el control.

## Requisitos del equipo

- Windows con el controlador/servicio U.are.U instalado y el lector USB conectado.
- JRE/JDK y runtime del SDK con arquitectura compatible con la JVM de la aplicación (normalmente x64). El SDK instalado debe poner sus DLL nativas, incluido `dpuareu_jni.dll` y dependencias, en una ubicación que Windows pueda resolver.
- La aplicación incluye `dpuareu.jar`; no redistribuye DLL del SDK ni instala controladores. El instalador de OroBest por sí solo no reemplaza la instalación del fabricante.

La imagen se conserva como PNG en `compra.huella`, Base64. Base64 no es cifrado. El valor solo se entrega dentro del flujo autenticado de captura y se incluye en el PDF de la compra autorizada. No se genera ni guarda una plantilla biométrica; la asistencia de empleados queda en el otro proyecto.

## Base de datos y hosting

Aplicar la migración `modules/sign/migrations/m261026_000040_add_huella_to_compra.php`, que añade únicamente `compra.huella LONGTEXT NULL`. Antes de desplegar el controlador actualizado, sincroniza esa columna en el hosting. No copiar datos locales a producción.

El endpoint autenticado de firma acepta el campo multipart opcional `huella` (PNG). El servidor valida formato/dimensiones/tamaño y guarda la codificación Base64 en la fila de la compra, sin cambiar el historial ni las versiones de la firma. El comprobante agrega la huella sobre la firma cuando la imagen está disponible.

La adquisición física debe verificarse en una PC Windows con el SDK/controlador instalados y el lector conectado; la compilación no simula ni prueba el dispositivo.
