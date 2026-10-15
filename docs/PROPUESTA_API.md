# Propuesta de integración API — pendiente de aprobación

Este documento es una propuesta para consultar al propietario del proyecto. **No se han creado APIs, migraciones ni archivos de despliegue en la web.** Los nombres, rutas y tablas siguientes todavía pueden ajustarse.

## 1. Organización propuesta

Reservar **`/api/sign/v1`** para OroBest Sign dentro del backend Yii existente.

Ubicación orientativa del código web:

```text
modules/sign/
  Module.php
  controllers/
    AuthController.php
    ComprasController.php
  components/
    SignBearerAuth.php
  services/
    AuthService.php
    CompraAccessService.php
    FirmaService.php
  models/
    SignApiToken.php
    CompraFirma.php
```

El módulo podrá reutilizar `app\models\Usuario`, `Compra`, `Caja` y `Agencia`. Un único servicio de acceso aplicará la pertenencia por agencia a listado, detalle y firma. Las rutas propuestas se dirigirían al módulo `sign` mediante reglas explícitas en Yii.

La versión aparece en la URL para fijar el contrato del móvil. La configuración y los controladores actuales de compras, agencias y RRHH conservan su contrato.

## 2. Autenticación propuesta

Inicio con `username` y `password` existentes. Validar en el backend:

1. Credenciales válidas y nombre de usuario no ambiguo.
2. Cuenta activa.
3. Agencia existente y activa.
4. Permiso explícito de firmas, cuyo nombre debe acordarse; sugerencia: `firmarCompras` mediante el RBAC ya existente.

Emitir un token opaco propio de Sign, conservando únicamente su hash en una tabla auxiliar. Incluir revocación y expiración de sesión. Confirmar la duración antes de implementarlo.

El token de RRHH y `usuario.access_token` no se reutilizarían para este módulo. El campo de contraseñas y la estructura de `usuario` pueden mantenerse.

En cada operación protegida se vuelven a comprobar estado, agencia y permiso actuales; una reasignación de agencia invalida el contexto anterior del móvil y requiere actualizar la sesión. La respuesta de sesión incluiría identidad mínima y agencia autorizada.

## 3. Rutas orientativas

| Método | Ruta bajo `/api/sign/v1` | Propósito |
| --- | --- | --- |
| POST | `/auth/login` | Validar credenciales y emitir sesión Sign |
| POST | `/auth/logout` | Revocar el token de la sesión actual |
| GET | `/auth/me` | Consultar usuario y agencia actuales |
| GET | `/compras` | Listado paginado de compras autorizadas |
| GET | `/compras/{idcompra}` | Detalle autorizado de una compra |
| POST | `/compras/{idcompra}/firma` | Recibir PNG y asociarlo a esa compra |
| GET | `/compras/{idcompra}/firma` | Recuperar la imagen con autorización |

Filtros posibles del listado: fecha desde/hasta, texto de número de compra, firmada/pendiente y página. No se necesita `idagencia` elegible por el cliente. Orden estable sugerido: fecha descendente y `idcompra` descendente; tamaño de página acotado por el servidor.

Detalle y firma de otra agencia responderían como recurso no disponible (`404`). Autenticación inválida: `401`; cuenta/agencia/permiso no habilitado: `403`; conflictos de firma o compra modificada: `409`; datos inválidos: `422`; imagen demasiado grande: `413`.

El contrato final fijará un formato JSON uniforme de datos, paginación y error con un código estable para KMP. Importe como decimal exacto y fechas con zona horaria explícita; no reutilizar directamente todas las columnas de ActiveRecord como respuesta.

## 4. Persistencia propuesta

### Tabla auxiliar de sesiones: `sign_api_token`

Campos orientativos: ID, `idusuario`, hash único del token, creación, expiración, último uso y revocación. FK al usuario con tipo compatible. Puede incluir el contexto de agencia para detectar cambios durante una sesión.

Esta tabla no requiere alterar `usuario`. Tiene que ser independiente de `rrhh_api_token` para que revocar una app no cierre la sesión de la otra.

### Tabla auxiliar de firmas: `compra_firma`

Campos orientativos:

- ID de firma y `idcompra` con FK.
- `idusuario` que registró la captura y agencia al registrar, para auditoría.
- Ruta interna de la imagen generada por el servidor.
- MIME, tamaño, dimensiones y hash SHA-256 del contenido.
- Fecha de registro del servidor.
- Identificador de operación para reconocer reintentos de la misma carga.
- Referencia de versión/estado de la compra si se acuerda invalidar firmas al editarla.

Si se aprueba **una sola firma sin reemplazo**, usar unicidad por `idcompra`. Si se necesita historial, definir un modelo de versiones antes de crear la migración. No resolver un reemplazo sobrescribiendo silenciosamente el mismo archivo.

Estas tablas son propuestas de diseño; no se han escrito sentencias de creación ni ejecutado cambios de esquema.

## 5. Captura y envío de imagen

- Captura de trazos en el móvil y exportación a **PNG**.
- Envío propuesto: `multipart/form-data` con un campo de archivo `firma` y un identificador estable de operación.
- `idcompra` procede de la ruta. Usuario y agencia proceden del token, no del formulario móvil.
- El servidor valida que la imagen sea un PNG decodificable, comprueba límites acordados y genera el nombre interno.
- La compra debe seguir siendo elegible al confirmar la carga, incluso ante dos solicitudes simultáneas.
- Un reintento idéntico devuelve el resultado previamente confirmado. Una operación reutilizada con otro contenido o una firma diferente donde no se permite reemplazo devuelve conflicto.
- Confirmar en una misma operación el registro de metadatos y la disponibilidad del archivo, con limpieza en caso de fallo.

Sugerencia inicial para acordar: máximo 2 MiB y hasta 2048 × 1024 píxeles, fondo blanco. El límite debe coordinarse con `upload_max_filesize`, `post_max_size` y el límite del servidor del hosting.

Se propone almacenamiento privado fuera de la raíz pública y entrega mediante una ruta autenticada. La futura impresión web podrá leer la imagen en el servidor después de autorizar la compra; no necesita una URL pública permanente.

## 6. Compras elegibles y efecto de cambios

Propuesta inicial: listar compras vigentes (`estado = 1`) y permitir firmar únicamente compras vigentes sin firma. El usuario vería las compras de todas las cajas de su agencia, aunque las haya creado otro usuario.

Por definir con el propietario:

- ¿Fecha actual como filtro inicial o intervalo completo configurable?
- ¿Basta el número de compra, fecha y total o se necesita el nombre del cliente y detalle?
- ¿Se permite firmar compras antiguas o de cajas inactivas?
- ¿Qué ocurre al modificar importes, detalle o cliente de una compra ya firmada?
- ¿Una anulación conserva la firma como histórico? Se recomienda conservar la evidencia, aunque deje de ser válida como compra vigente.
- ¿Está permitido reasignar cajas a otras agencias? Si ocurre, hay que acordar cómo conservar pertenencia y acceso históricos.

## 7. Preparación de hosting

Después de aprobar y desarrollar el módulo, el paquete de despliegue debe enumerar exactamente:

- Archivos nuevos en `modules/sign`.
- Nuevas migraciones, con su secuencia y comando de ejecución.
- Registro del módulo y reglas nuevas de `config/web.php` mediante un cambio acotado.
- Directorio de imágenes, configuración de ruta y permisos de escritura.
- Configuración de límites de carga y verificación del paso de `Authorization` en el hosting.
- Opcionalmente, cambios de lectura de firma en el comprobante web si se aprueban.
- Pruebas de login, aislamiento entre agencias, compra anulada, reintento y carga PNG en el entorno desplegado.

Hay entradas `index.php` y `web/index.php` en el proyecto local. Antes de fijar rutas de imágenes o reglas de servidor, confirmar cuál es la raíz pública usada realmente en el hosting y el prefijo del dominio. La ubicación privada debe seguir funcionando con esa raíz.

No se requiere instalar KMP en el hosting: se despliegan únicamente los archivos PHP y las migraciones correspondientes. El móvil consume HTTPS.

## 8. Preguntas antes de crear las APIs

1. **Organización:** ¿apruebas módulo `sign` y prefijo `/api/sign/v1`? ¿Qué dominio y carpeta pública utiliza el hosting?
2. **Acceso:** ¿qué usuarios podrán firmar? ¿Creamos el permiso RBAC `firmarCompras`? ¿Cuánto debe durar la sesión?
3. **Visibilidad:** ¿todas las compras vigentes de la agencia, incluidas las creadas por otros usuarios? ¿Solo del día inicialmente?
4. **Firma:** ¿una firma por compra, sin reemplazo, o se necesita reemplazo con historial? ¿PNG con fondo blanco y límites propuestos?
5. **Integración web:** ¿la imagen debe aparecer también en `compra/print`? ¿Qué debe ocurrir si una compra firmada se edita o anula?
6. **Base de datos:** ¿apruebas tablas auxiliares `sign_api_token` y `compra_firma`, conservando la estructura de `usuario`? Queda pendiente revisar las dos cuentas activas con nombre duplicado.

Las respuestas determinarán el contrato definitivo, los DTO y repositorios KMP y el manifiesto de archivos para subir al hosting.
