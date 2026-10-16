# API Sign v1 — contrato aprobado e implementado localmente

Acuerdos del propietario: módulo `sign`, todos los usuarios activos (incluidos caja), compras de la agencia completa, sesión persistente, firma editable y dos tablas nuevas sin alterar las existentes. La imagen se usará en el comprobante; su ubicación visual se decidirá después.

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

Una compra modificada conserva su firma histórica y queda marcada en el detalle como modificada. No se bloquea ni cambia la edición de compras en la web. Las anuladas conservan las imágenes; no aparecen en el listado de vigentes ni admiten nuevas firmas. La política visual del comprobante cuando haya cambios se acordará junto con su ubicación.

## Imágenes y almacenamiento

La API GET devuelve `image/png` con autorización por agencia y sin caché. El directorio interno y la ruta relativa no se envían al móvil.

El almacenamiento puede configurarse fuera de la carpeta pública con `OROBEST_SIGN_STORAGE_PATH`. La ubicación predeterminada es `runtime/signatures`, protegida por un `.htaccess` de denegación generado por el servicio; el hosting Apache debe respetarlo. Los archivos se crean sin reutilizar nombres ni sobrescribir versiones previas.

## Errores

```json
{ "success": false, "error": { "code": "purchase_not_found", "message": "Compra no encontrada en la agencia asignada." } }
```

Estados: `401` sesión/credenciales, `403` agencia, `404` recurso fuera de alcance o inexistente, `405` método incorrecto, `409` conflicto, `413` tamaño y `422` validación. Un error inesperado devuelve `500 server_error`, sin detalles SQL ni trazas al cliente.

## Tablas nuevas

- `sign_api_token`: token hasheado, usuario, creación, último uso, expiración y revocación.
- `compra_firma`: versiones por compra, autor/agencia de captura, operación idempotente, revisión de compra, metadatos y ubicación interna del PNG.

Solo se añaden estas dos tablas, sus índices y FK. La migración aislada es `modules/sign/migrations/m261015_000038_create_sign_tables.php`. El esquema previo de `usuario`, `agencia`, `caja` y `compra` se comprobó antes/después de la ejecución local.
