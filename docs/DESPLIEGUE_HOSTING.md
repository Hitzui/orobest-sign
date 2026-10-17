# Archivos para subir a orobestni.com

El backend está instalado localmente en `C:\xampp\htdocs\orobest`. La copia entregable y versionada se encuentra en `E:\Mobil\orobest-sign\backend\orobest`.

**No se ha conectado ni migrado la base del hosting.** El propietario sincroniza el esquema local y sube los archivos. La ubicación acordada para la firma en el comprobante es debajo de TOTALES, en el espacio de Firma del Cliente.

## 1. Sincronizar exclusivamente las tablas nuevas

La migración local ya creó:

1. `sign_api_token`
2. `compra_firma`

En tu herramienta de comparación de esquemas, selecciona solamente estas dos tablas, sus índices y claves foráneas. Mantén tipos compatibles con los IDs existentes. No sincronices datos: ni usuarios, compras, tokens ni registros de firmas. No necesitas ejecutar la migración en el hosting si ya sincronizaste su estructura.

Los esquemas de `usuario`, `agencia`, `caja` y `compra` se verificaron sin cambios. La herramienta Yii registró la migración local en su historial habitual.

Comando local para consultar/aplicar únicamente las migraciones del módulo:

```powershell
C:\xampp\php\php.exe yii migrate/up --migrationPath=@app/modules/sign/migrations --interactive=0
```

No ejecutar un `migrate/up` general para esta entrega: el backend tiene migraciones de otros módulos.

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
modules/sign/services/ClienteDocumentService.php
services/ClienteDocumentService.php
services/ClienteDocumentException.php
modules/sign/migrations/m261015_000038_create_sign_tables.php
```

La migración puede conservarse como referencia de versión; subirla no la ejecuta. No es necesario subir `backend/tests`, `backend/tools`, archivos Kotlin, el APK ni el repositorio completo al hosting.

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
modules/sign/services/FirmaService.php
views/compra/print.php
```

La copia de entrega contiene los mismos archivos que `C:\xampp\htdocs\orobest`. Es necesario actualizar `FirmaService.php` junto con el controlador porque incorpora el método que recupera la última imagen. El controlador lee la firma dentro del almacenamiento privado configurado; no publica ni construye una URL pública para la imagen. El comprobante la muestra centrada sobre el texto **Firma del Cliente** y la fecha, debajo de TOTALES, a 60 mm de ancho y sin deformar su proporción. Si aún no existe firma, se mantiene el espacio de firma manuscrita.

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

El comprobante PDF muestra la firma debajo de TOTALES, conservando la proporción PNG y la fecha del comprobante. Esta parte requiere subir los dos archivos web indicados arriba; la API y la impresión leen las firmas existentes sin migraciones adicionales.

## Verificación local de esta entrega

```powershell
C:\xampp\php\php.exe backend\tests\run.php C:\xampp\htdocs\orobest
.\gradlew.bat :shared:jvmTest :shared:testAndroidHostTest :androidApp:assembleDebug
```

El test PHP crea una base temporal **local** con datos sintéticos, comprueba aislamiento y versiones, y la elimina al terminar. No modifica las compras ni las cuentas reales.
