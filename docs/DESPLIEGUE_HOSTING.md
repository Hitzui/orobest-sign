# Archivos para subir a orobestni.com

El backend está instalado localmente en `C:\xampp\htdocs\orobest`. La copia entregable y versionada se encuentra en `E:\Mobil\orobest-sign\backend\orobest`.

**No se ha conectado ni migrado la base del hosting.** El propietario sincroniza el esquema local y sube los archivos. La ubicación acordada para la firma en el comprobante es debajo de TOTALES, en el espacio de Firma del Cliente.

## 1. Sincronizar el esquema de firmas y el permiso de impresión

La migración inicial de Sign creó:

1. `sign_api_token`
2. `compra_firma`

La migración siguiente agrega `usuario.imprimir_sin_firma`, con valor predeterminado `0` para todas las cuentas existentes. La migración `m261026_000040_add_huella_to_compra.php` añade `compra.huella` (`LONGTEXT NULL`) para almacenar el PNG de huella en Base64. Sincroniza solo los cambios indicados, sin copiar datos entre bases.

No se modifican otras columnas ni tablas. La herramienta Yii registra las migraciones locales en su historial habitual.

Comando local para consultar/aplicar únicamente las migraciones del módulo:

```powershell
C:\xampp\php\php.exe yii migrate/up --migrationPath=@app/modules/sign/migrations --interactive=0
```

No ejecutar un `migrate/up` general para esta entrega: el backend tiene migraciones de otros módulos.

La migración de `imprimir_sin_firma` debe aplicarse en la base destino, o debe sincronizarse la columna indicada, antes de usar esta función.

## 2. Subir los archivos nuevos

Copia los siguientes archivos desde `backend/orobest/` hacia **la carpeta raíz del proyecto Yii en tu hosting**, manteniendo sus rutas. Es la carpeta que contiene `config`, `models`, `controllers` y `vendor`.

```text
config/sign.php
modules/sign/Module.php
modules/sign/components/ApiException.php
modules/sign/components/SignBearerAuth.php
modules/sign/controllers/ApiController.php
modules/sign/controllers/AuthController.php
modules/sign/controllers/ComprasController.php
modules/sign/controllers/ClientesController.php
modules/sign/models/SignApiToken.php
modules/sign/models/CompraFirma.php
modules/sign/services/AuthService.php
modules/sign/services/CompraAccessService.php
modules/sign/services/FirmaService.php
modules/sign/services/SignaturePrintRenderer.php
modules/sign/services/ClienteDocumentService.php
services/ClienteDocumentService.php
services/ClienteDocumentException.php
modules/sign/migrations/m261022_000039_add_imprimir_sin_firma_to_usuario.php
modules/sign/migrations/m261026_000040_add_huella_to_compra.php
modules/sign/migrations/m261015_000038_create_sign_tables.php
models/Usuario.php
views/usuario/_form.php
views/usuario/index.php
views/usuario/view.php
```

Tras actualizar la web, un administrador puede activar **Permitir impresión sin firma** en el formulario de cada usuario; queda desactivado por defecto. No es necesario subir `backend/tests`, `backend/tools`, archivos Kotlin, el APK ni el repositorio completo al hosting.

### Actualización de eliminación desde la app

Si ya subiste la primera entrega de clientes, actualiza juntos estos archivos desde `backend/orobest/`:

```text
config/sign.php
modules/sign/controllers/ApiController.php
modules/sign/controllers/ClientesController.php
modules/sign/services/ClienteDocumentService.php
services/ClienteDocumentService.php
services/ClienteDocumentException.php
```

Los dos últimos son dependencias del servicio compartido entre web y Sign. El endpoint nuevo es `DELETE /api/sign/v1/clientes/{idcliente}/documentos/{idarchivo}`. La app pide confirmación antes de enviarlo; elimina únicamente el adjunto seleccionado y su registro. Esta actualización no requiere migración ni cambios de estructura en la base de datos. Conserva las rutas de almacenamiento personalizadas de tu `config/sign.php` al incorporar la regla nueva.

### Formulario web de clientes y carga múltiple

Actualiza también estos archivos desde `backend/orobest/` para crear/editar clientes, descargar los adjuntos de la app y eliminarlos individualmente desde el formulario o la ficha:

```text
controllers/ClienteController.php
models/Cliente.php
services/ClienteService.php
views/cliente/_form.php
views/cliente/view.php
```

Se añaden hasta 10 documentos nuevos al pulsar **Guardar**, sin reemplazar los existentes aunque tengan el mismo nombre. La web y Sign guardan los nuevos documentos en la misma carpeta privada configurada y siguen leyendo `uploads/clientes/{idcliente}/` para archivos históricos.

### Actualizar el comprobante

Para incluir la firma en el PDF, copia también desde `backend/orobest/` y reemplaza en el proyecto Yii del hosting:

```text
controllers/CompraController.php
models/Usuario.php
modules/sign/migrations/m261022_000039_add_imprimir_sin_firma_to_usuario.php
modules/sign/migrations/m261026_000040_add_huella_to_compra.php
modules/sign/services/FirmaService.php
modules/sign/services/SignaturePrintRenderer.php
views/compra/view.php
views/compra/print.php
views/usuario/_form.php
views/usuario/index.php
views/usuario/view.php
```

La copia de entrega contiene los mismos archivos que `C:\xampp\htdocs\orobest`. Es necesario actualizar `FirmaService.php` y `SignaturePrintRenderer.php` junto con el controlador: recuperan la última imagen de firma y generan su variante de impresión. `compra.huella` guarda el PNG Base64 de la huella; la nueva migración debe aplicarse antes de desplegar el controlador. El comprobante muestra la huella disponible sobre la firma. La firma se presenta centrada debajo de TOTALES, a 60 mm de ancho y sin deformar su proporción. `views/compra/view.php` mantiene el botón de impresión disponible: comprueba la firma al pulsarlo y, si falta y el usuario autenticado no tiene permiso, muestra SweetAlert sin recargar el detalle. La acción del PDF vuelve a validar en el servidor; sin firma y sin permiso, redirige al detalle con el mensaje de error. El permiso `usuario.imprimir_sin_firma` se administra desde el formulario de usuarios y está desactivado por defecto.

### Refuerzo de firma para impresora matricial y láser — 19 de octubre de 2026

Para una instalación que ya imprime la firma, actualiza juntos únicamente estos archivos desde `backend/orobest/`:

```text
controllers/CompraController.php
modules/sign/services/FirmaService.php
modules/sign/services/SignaturePrintRenderer.php
views/compra/view.php
```

Al generar de nuevo el PDF, la firma se convierte en una copia temporal en memoria de **negro sólido sobre blanco**, con un refuerzo de aproximadamente 0,10 mm por cada lado del trazo. En el PNG habitual de 1600 píxeles, el trazo de 4 píxeles pasa a unos 10 píxeles, equivalentes a **0,375 mm** al imprimirlo a 60 mm de ancho. Se conservan posición, proporción y tamaño del bloque de firma.

La mejora se aplica también a las firmas ya guardadas: la imagen azul original, sus versiones y sus hashes no se modifican. No requiere migración ni una nueva instalación de la app; se activa con los tres PHP del servidor. Para comprobar el resultado, vuelve a generar el comprobante desde la web y realiza una prueba en cada impresora a tamaño real, evitando el modo borrador/ahorro de tóner.

Se incluye una [comparativa PDF a 60 mm](entregables/Comparativa_firma_impresion.pdf), generada con mPDF y trazos sintéticos, para revisar o imprimir el antes/después sin usar datos de clientes.

## 3. Cambio puntual en `config/web.php`

Al final de tu **propio archivo del hosting**, reemplaza:

```php
return $config;
```

por:

```php
return \yii\helpers\ArrayHelper::merge($config, require __DIR__ . '/sign.php');
```

Este es el cambio de configuración requerido en `config/web.php`. Conserva tus credenciales y configuración del hosting; no reemplaces todo `config/web.php` con el archivo local.

El fragmento añade el módulo y las rutas de autenticación, compras, clientes y documentos bajo `/api/sign/v1`, junto con las reglas existentes de RRHH y otras APIs. No requiere cambios de Composer ni archivos de `vendor`.

## 4. Almacenamiento privado

Opción preferida: en `config/sign.php`, configurar `storagePath` con una ruta absoluta fuera de `public_html` (o definir la variable de entorno `OROBEST_SIGN_STORAGE_PATH`). PHP debe poder crear carpetas y escribir allí.

Configura también `clientDocumentsStoragePath` con una ruta absoluta privada para los archivos de clientes o define `OROBEST_CLIENT_DOCUMENTS_PATH`. Las fotos de cédula y PDFs son datos personales; no los guardes en una carpeta pública. El valor predeterminado es `runtime/client-documents` y se protege con denegación Apache, pero es preferible configurar una carpeta fuera de `public_html`.

Si conservas los valores predeterminados, PHP usará `runtime/signatures` y `runtime/client-documents`. El módulo crea los directorios y un `.htaccess` que deniega el acceso directo. La entrega de PNG y documentos se hace por controladores autenticados. Respaldar ambos directorios, junto con `compra_firma` y los registros existentes de `documentos`/`archivos_clientes`; sincronizar esquemas no copia archivos.

El dominio por sí solo no permite conocer la raíz pública del hosting. Las rutas de API están previstas para la aplicación publicada en `https://orobestni.com/`; si realmente está dentro de una subcarpeta, hay que incluir ese prefijo en `ApiConfig` del móvil.

## 5. Requisitos de la carga

- PHP 8.1+ con `gd`, `fileinfo`, `zip`, PDO MySQL y las extensiones que ya usa Yii.
- `upload_max_filesize` de al menos `10M`, `max_file_uploads` de al menos `10` y `post_max_size = 128M` para admitir un lote web completo de 10 archivos de 10 MiB más el multipart. El límite HTTP del hosting también debe admitir ese total. La app envía cada documento por separado.
- El servidor debe pasar el encabezado `Authorization` a PHP. No se cambia `.htaccess` de toda la web anticipadamente: se verifica con una petición autenticada después de subir.

## 6. Verificar después de subir

Sin sesión, esta URL debe devolver **401 y JSON**, no una página de login:

```text
https://orobestni.com/api/sign/v1/auth/me
```

Después verificar login, `me` tras reiniciar el móvil, compras de la agencia completa, rechazo de una compra de otra sucursal, carga y edición de una firma, búsqueda de clientes, carga/listado/descarga autenticada de documentos, y logout. Los ejemplos completos de campos y respuestas están en [PROPUESTA_API.md](PROPUESTA_API.md).

El comprobante PDF muestra la firma debajo de TOTALES, conservando la proporción PNG y la fecha del comprobante. Esta parte requiere subir los archivos de la sección **Actualizar el comprobante**; la API y la impresión leen las firmas existentes sin migraciones adicionales.

## Verificación local de esta entrega

```powershell
C:\xampp\php\php.exe backend\tests\run.php C:\xampp\htdocs\orobest
C:\xampp\php\php.exe backend\tests\signature_print.php
.\gradlew.bat :shared:jvmTest :shared:testAndroidHostTest :androidApp:assembleDebug
```

El test PHP crea una base temporal **local** con datos sintéticos, comprueba aislamiento y versiones, y la elimina al terminar. No modifica las compras ni las cuentas reales.

Verificación del refuerzo de impresión: 71 comprobaciones de integración y 12 comprobaciones de imagen aprobadas. Se verificaron negro sólido sin grises, grosor del trazo, proporción, conservación de espacios, puntos, transparencias y preservación del archivo original. La calidad física debe comprobarse en la matricial y la láser de destino.
