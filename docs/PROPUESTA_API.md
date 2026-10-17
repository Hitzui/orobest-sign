# API Sign v1 — contrato aprobado e implementado localmente

Acuerdos del propietario: módulo `sign`, todos los usuarios activos (incluidos caja), compras de la agencia completa, sesión persistente, firma editable y dos tablas nuevas sin alterar las existentes. La última versión de la firma se incluye en el comprobante debajo de TOTALES, en el espacio de Firma del Cliente.

Producción prevista: **`https://orobestni.com/api/sign/v1/`**. El propietario subirá los archivos y sincronizará el esquema; esta entrega no accede a la base del hosting.

Código entregable: [`backend/orobest`](../backend/orobest). Instrucciones exactas: [DESPLIEGUE_HOSTING.md](DESPLIEGUE_HOSTING.md).

## Autenticación y persistencia

Sin rol ni permiso adicional de firmas: cualquier usuario **activo**, con nombre de acceso no ambiguo, contraseña válida y agencia activa puede entrar. No se requiere caja asignada para acceder a las compras de su agencia. Los nombres duplicados deben corregirse desde administración; la API no escoge arbitrariamente una cuenta.

Los tokens Sign son independientes de RRHH, se guardan mediante SHA-256 y duran **90 días de inactividad**. Cada petición autenticada renueva esa ventana. El plazo es configurable en `config/sign.php`; no depende del cierre de sesión web a los diez minutos.

Cada petición vuelve a consultar usuario y agencia actuales. Un cambio de agencia surte efecto en la siguiente petición; `/auth/me` permite al móvil actualizar el contexto. Un usuario desactivado, un token revocado o vencido reciben `401`; agencia no disponible, `403`.

KMP dispone de repositorio de autenticación y restauración de sesión, conectado al login en `screen/login`. Android almacena el token cifrado AES-GCM con clave Android Keystore; iOS usa Keychain, solo en el mismo dispositivo. No se guarda la contraseña. JVM conserva la sesión en memoria por ser un destino auxiliar de desarrollo. Una falta de conexión al restaurar no elimina el token; un `401` confirmado sí. El login restaura al abrir la app, permite reintentar y ofrece cierre de sesión.

## Rutas

Todas bajo `/api/sign/v1`, con `Accept: application/json`. Salvo login, requieren `Authorization: Bearer <token>`.

| Método | Ruta | Función |
| --- | --- | --- |
| POST | `auth/login` | Iniciar sesión con JSON `username`, `password` |
| GET | `auth/me` | Identidad, agencia y vencimiento renovado |
| POST | `auth/logout` | Revocar únicamente el token utilizado |
| GET | `compras` | Compras vigentes de todas las cajas de la agencia |
| GET | `compras/{idcompra}` | Detalle resumido y revisión para firmar |
| POST | `compras/{idcompra}/firma` | Crear o editar firma mediante nueva versión |
| GET | `compras/{idcompra}/firma` | Imagen actual autorizada |
| GET | `compras/{idcompra}/firma?version=N` | Imagen de una versión anterior |
| GET | `clientes?page=1&page_size=20&q=` | Listado paginado y búsqueda de todos los clientes |
| GET | `clientes/{idcliente}` | Datos mínimos de un cliente |
| GET | `clientes/{idcliente}/documentos` | Metadatos de documentos adjuntos |
| POST | `clientes/{idcliente}/documentos` | Subir JPG/PNG, PDF, DOC, DOCX o TXT mediante multipart |
| GET | `clientes/{idcliente}/documentos/{idarchivo}/contenido` | Obtener el archivo privado con Bearer |
| DELETE | `clientes/{idcliente}/documentos/{idarchivo}` | Eliminar un adjunto del cliente con Bearer |

El alcance de clientes es global por decisión del propietario: cualquier usuario autenticado en Sign puede buscar todos los clientes. La lista expone únicamente ID, nombres, apellidos, cédula, teléfono y celular. No se filtra por agencia porque `cliente` no tiene una relación directa con ella.

La carga acepta `archivo` (JPG/PNG/PDF/DOC/DOCX/TXT, máximo 10 MiB), `tipo_documento` (`cedula_frente`, `cedula_reverso` o `otro`), `operation_id` (UUID) y `nombre` para documentos de tipo `otro`. Para frente/reverso el servidor asigna los nombres correspondientes. El cliente conserva el UUID y los bytes cuando reintenta una carga de resultado incierto.

La API reutiliza `documentos` y `archivos_clientes` sin modificar su esquema. Los metadatos históricos se muestran como `otro`; la categoría de archivos nuevos se obtiene de su ruta privada. Los archivos nuevos se almacenan en `OROBEST_CLIENT_DOCUMENTS_PATH` o, por defecto, `runtime/client-documents`, con nombres no predecibles y acceso solo mediante el endpoint autenticado. Configura la ruta preferentemente fuera de la carpeta pública.

En KMP, la pantalla compartida permite seleccionar hasta 10 archivos y subirlos con una sola acción. Cada archivo usa su propia operación idempotente: el lote se envía secuencialmente, conserva los ya confirmados y reintenta desde el primer archivo pendiente con el mismo UUID y bytes. Android implementa cámara, galería múltiple y selector múltiple de documentos; iOS implementa cámara/fototeca y selector múltiple de archivos; JVM permite selección múltiple desde el explorador. Los archivos pendientes viven en memoria.

La web usa el mismo servicio desde `create` y `update`, mediante `_form.php` con multipart. Guarda hasta 10 documentos nuevos junto con el cliente en una transacción: si un archivo falla, revierte el lote y limpia los archivos físicos nuevos. Cada adjunto tiene una ruta UUID exclusiva; repetir el nombre original no reemplaza documentos anteriores. Los listados incluyen todos los registros de `documentos` del cliente, incluidos grupos históricos.

El listado muestra todos los archivos de cada categoría con botón **Ver**, o indica que no están disponibles si falta el archivo físico. La app consulta `contenido` con Bearer para mostrar imágenes con zoom y PDF (paginados dentro de Android, PDFKit en iOS y lector del sistema en escritorio). Los documentos anteriores de `uploads/clientes/{idcliente}/` se listan como **Otros documentos** y no requieren moverlos a `runtime`; deben existir el registro y el archivo físico correspondiente. No se construyen enlaces públicos ni se transmite el token al lector externo.

TXT se muestra dentro de la app. DOC/DOCX se abre con una aplicación compatible en Android/escritorio y con el visor WebKit en iOS; la integración iOS requiere verificación en macOS/dispositivo. La web descarga cualquiera de los formatos conservando el nombre original.

Cada adjunto ofrece **Eliminar** con confirmación de Composables UI. La app solo lo retira del listado después de recibir `data.deleted = true`; si la conexión falla, conserva el listado y permite actualizar antes de reintentar. El servidor elimina el registro de `archivos_clientes` y su archivo físico (histórico o privado), sin eliminar el cliente ni los demás adjuntos. El ID del archivo se consulta dentro de los documentos del cliente indicado; una repetición de DELETE sobre un ID ya ausente es inocua. La eliminación requiere Bearer y método DELETE, usando el mismo acceso global de clientes. Web y Sign comparten el servicio `app\services\ClienteDocumentService`, adaptado desde el módulo Sign; subir los PHP no ejecuta eliminaciones ni modifica el esquema.

## Respuestas de sesión

Login devuelve:

```json
{
  "success": true,
  "data": {
    "access_token": "<token emitido solo al iniciar sesión>",
    "token_type": "Bearer",
    "expires_at": "2027-01-13T00:00:00Z",
    "usuario": { "idusuario": 123, "username": "usuario", "nombre": "Nombre" },
    "agencia": { "idagencia": 1, "codagencia": "A", "descripcion": "Sucursal" }
  }
}
```

Los datos del ejemplo son ilustrativos. `me` devuelve el mismo perfil sin reenviar el token. Logout devuelve `data.logged_out = true`.

## Listado y detalle de compras

La pertenencia se obtiene por `compra.idcaja → caja.idagencia`, comparada con `usuario.idagencia`. No se filtra por la caja asignada ni por el creador de la compra.

Parámetros opcionales del listado:

- `page`: desde 1; por defecto 1.
- `page_size`: de 1 a 100; por defecto 20.
- `desde`, `hasta`: fechas `YYYY-MM-DD`, ambos días incluidos.
- `q`: búsqueda por número de compra, hasta 100 caracteres.
- `firma`: `firmada` o `pendiente`.

Sin fechas se incluyen todas las compras vigentes de la agencia. No se fuerza un filtro de hoy. El orden es fecha descendente, luego `idcompra` descendente. Se rechazan `idagencia`, `idcaja` e `idusuario` como filtros elegibles por el móvil.

`data.items` contiene `idcompra`, `numcompra`, `fecha` con zona `-06:00`, `estado`, `total` como texto decimal exacto, cliente mínimo si existe y metadatos de firma o `null`. `data.pagination` contiene `page`, `page_size`, `total` y `total_pages`.

El detalle añade `revision` (hash del contenido actual de compra/detalles) y, si hay firma, `firma.compra_modificada`. Es necesario consultar ese detalle antes de capturar/enviar una firma. No se exponen rutas de archivos, hashes de contraseñas, cuentas bancarias ni todos los atributos del cliente.

## Crear y editar una firma

Enviar `multipart/form-data` mediante POST:

| Campo | Contenido |
| --- | --- |
| `firma` | Archivo PNG, máximo 2 MiB y 2048 × 1024 píxeles |
| `operation_id` | UUID de la operación; conservarlo en cada reintento idéntico |
| `compra_revision` | `revision` devuelta por el detalle consultado |
| `base_version` | 0 para primera firma; versión actual para editar |

El servidor verifica la compra y su agencia, decodifica el PNG y genera una imagen normalizada con fondo blanco. Una edición crea **una versión nueva** y conserva archivos/metadatos anteriores.

Respuesta: `data.firma` incluye ID, compra, versión, usuario que registró, revisión de compra, SHA-256, MIME, tamaño, dimensiones y fecha UTC. `data.replayed` indica que la misma operación ya había sido confirmada.

- Un reintento idéntico no genera otra versión.
- Cambiar el contenido de una operación ya utilizada devuelve `409 operation_conflict`.
- Editar desde una versión antigua devuelve `409 signature_changed`.
- Firmar una revisión antigua de la compra devuelve `409 purchase_changed`.
- Crear o editar en una compra anulada devuelve `409 purchase_not_active`.
- La comprobación de versión está protegida por una transacción y bloqueo de la compra.

Una compra modificada conserva su firma histórica y queda marcada en el detalle como modificada. No se bloquea ni cambia la edición de compras en la web. Las anuladas conservan las imágenes; no aparecen en el listado de vigentes ni admiten nuevas firmas. El comprobante presenta la versión más reciente disponible, manteniendo la proporción original de 2:1 y un ancho de impresión de 60 mm; la fecha del comprobante permanece debajo. Si no hay archivo de firma disponible, se conserva la línea para firma manuscrita.

## Imágenes y almacenamiento

La API GET devuelve `image/png` con autorización por agencia y sin caché. El directorio interno y la ruta relativa no se envían al móvil.

El almacenamiento puede configurarse fuera de la carpeta pública con `OROBEST_SIGN_STORAGE_PATH`. La ubicación predeterminada es `runtime/signatures`, protegida por un `.htaccess` de denegación generado por el servicio; el hosting Apache debe respetarlo. Los archivos se crean sin reutilizar nombres ni sobrescribir versiones previas.

Estructura real: `<raíz-de-firmas>/<idcompra>/<48-caracteres-hexadecimales-aleatorios>.png`. Por ejemplo, una compra con ID 123 guarda sus versiones dentro de `runtime/signatures/123/`. `compra_firma.file_path` almacena la ruta relativa y su versión. No se sobrescribe un archivo fijo `firma.png`; ese nombre solo identifica el adjunto enviado desde el móvil.

El móvil ya consulta el detalle/revisión antes de capturar, permite ver la imagen de la versión anterior y envía PNG de 1600 × 800. Los reintentos conservan UUID, bytes, revisión y versión base durante esa captura. Solo una respuesta de guardado válida actualiza el listado como éxito; `409` requiere una nueva consulta explícita antes de firmar otra vez.

## Errores

```json
{ "success": false, "error": { "code": "purchase_not_found", "message": "Compra no encontrada en la agencia asignada." } }
```

Estados: `401` sesión/credenciales, `403` agencia, `404` recurso fuera de alcance o inexistente, `405` método incorrecto, `409` conflicto, `413` tamaño y `422` validación. Un error inesperado devuelve `500 server_error`, sin detalles SQL ni trazas al cliente.

## Tablas nuevas

- `sign_api_token`: token hasheado, usuario, creación, último uso, expiración y revocación.
- `compra_firma`: versiones por compra, autor/agencia de captura, operación idempotente, revisión de compra, metadatos y ubicación interna del PNG.

La migración de Sign añade únicamente estas dos tablas, sus índices y FK. La API de clientes reutiliza `documentos` y `archivos_clientes` sin cambios de esquema. La migración aislada es `modules/sign/migrations/m261015_000038_create_sign_tables.php`. El esquema previo de `usuario`, `agencia`, `caja`, `compra` y las tablas documentales no se modifica.
