# Análisis de la web OroBest

Fecha de revisión: **15 de octubre de 2026**.

> Este documento conserva el análisis inicial, anterior a las migraciones de Sign. Posteriormente se aprobó e implementó el módulo, con acceso para todos los usuarios activos y dos tablas auxiliares. El estado vigente está en [PROPUESTA_API.md](PROPUESTA_API.md) y [DESPLIEGUE_HOSTING.md](DESPLIEGUE_HOSTING.md).

Fuente: proyecto local `C:\xampp\htdocs\orobest` y la base de datos indicada por su configuración local. Se consultaron metadatos y agregados en una transacción de solo lectura. Los resultados describen esa base local, no certifican el estado del hosting.

## 1. Plataforma y alcance

- PHP 8.1+, Yii2 `~2.0.53`, ActiveRecord y autenticación de usuarios mediante `app\models\Usuario`.
- Base compatible MySQL/MariaDB; tablas revisadas de tipo InnoDB.
- Zona horaria de la aplicación: `America/Managua`.
- JSON ya habilitado en el parser de peticiones de Yii.
- URLs amigables habilitadas; existen reglas y controladores de otras integraciones.

La aplicación de firmas necesita usuarios, agencias y compras. `caja` interviene obligatoriamente como relación de pertenencia; no implica crear funciones móviles para administrar cajas. `cliente` y `deta_compra` solo serán necesarios en lectura si se acuerda mostrar identidad del firmante y detalle de la compra.

## 2. Relaciones verificadas

```text
usuario
  idusuario (PK)
  idagencia ------------------------------> agencia.idagencia
  idcaja -----------------> caja.idcaja
                            caja.idagencia -> agencia.idagencia

compra
  idcompra (PK)
  idcaja -----------------> caja.idcaja
  idusuario -------------> usuario.idusuario
  idcliente -------------> cliente.idcliente
```

Las líneas anteriores incluyen relaciones lógicas y claves foráneas reales. La relación `usuario.idagencia` y la referencia `compra.idusuario` no tienen FK declarada en el esquema local; sí tienen FK `usuario.idcaja`, `caja.idagencia`, `compra.idcaja` y `compra.idcliente`.

### Regla propuesta de visibilidad

Una compra pertenece a la agencia de su **caja**:

```sql
-- Ejemplo conceptual de lectura; no es una API implementada.
SELECT p.idcompra, p.numcompra, p.fecha, p.total
FROM compra AS p
INNER JOIN caja AS c ON c.idcaja = p.idcaja
WHERE c.idagencia = :agencia_del_usuario_autenticado
  AND p.estado = 1;
```

La agencia del parámetro procede del usuario autenticado en el servidor. Para detalle y carga se añadirá también `p.idcompra = :idcompra`. No basta con filtrar el listado.

No inferir la agencia a partir de `numcompra` ni de la agencia actual del creador: el usuario puede cambiar de agencia después de crear la compra. Si se reasigna una caja a otra agencia, también cambiaría esta pertenencia derivada; conviene confirmar si la web permite esa operación antes de definir una pertenencia histórica inmutable.

## 3. Tabla `usuario`

| Campo | Tipo local | Uso relevante |
| --- | --- | --- |
| `idusuario` | `int`, PK autoincremental | Identidad estable |
| `activo` | `bit(1)`, obligatorio | Autorizar únicamente cuentas activas |
| `username` | `varchar(255)`, nullable | Credencial de acceso actual |
| `password` | `varchar(255)`, nullable | Hash validado por Yii |
| `nombre`, `email`, `titulo` | `varchar(255)`, nullable | Identidad descriptiva |
| `auth_key`, `access_token` | `varchar(255)`, nullable | Mecanismos existentes de la web |
| `idcaja` | `int`, nullable, FK | Caja asignada |
| `idagencia` | `int`, nullable, sin FK local | Agencia asignada |
| `created_at`, `updated_at` | `datetime` | Auditoría |

El modelo `Usuario` implementa `IdentityInterface`. `findByUsername()` usa `findOne`, y `validatePassword()` delega en `Yii::$app->security->validatePassword`. `LoginForm` rechaza usuarios inactivos. La autenticación móvil podrá reutilizar estos mecanismos, conservando el hash existente.

### Hallazgos

- **No es necesario cambiar la estructura de `usuario`** para login, agencia o asociación de firmas.
- Hay **un grupo duplicado de `username`: dos cuentas activas con el mismo nombre, no vacío**. `findOne` hace ambiguo el acceso para ese nombre. Antes del login móvil, revisar qué cuenta debe conservarlo y corregir los datos mediante administración; la API propuesta rechazará nombres ambiguos. No se publican nombres ni registros personales en este documento.
- La columna `username` no tiene restricción única local. Añadirla sería una mejora futura opcional, previa limpieza y autorización; no es un cambio aplicado ni un requisito para comenzar con el resto de cuentas.
- `usuario.idagencia` permite nulos y no tiene FK/índice dedicado. En los datos revisados está informado y es válido para todos los usuarios. La futura API debe verificar su existencia y estado al autenticar y en operaciones protegidas.
- El modelo no define relación `getAgencia()` ni validador `exist` para `idagencia`. Se puede consultar `Agencia` explícitamente desde el nuevo servicio sin tocar la tabla ni cambiar el comportamiento de otros módulos.
- La tabla usa `latin1`, mientras agencias y compras usan `utf8mb4`. No se propone una conversión en esta etapa; confirmar la codificación JSON y los nombres con tildes en pruebas de integración.

## 4. Tabla `agencia`

PK `idagencia`. `codagencia` tiene índice único; `descripcion` identifica la agencia en pantalla. `activo` es `tinyint(1)` con valor predeterminado 1.

Otros campos incluyen dirección, municipio, `deparatamento` (nombre existente), teléfonos, RUC, correo, logo `longblob`, contador `numcompra` y auditoría.

Para la sesión móvil bastaría inicialmente con `idagencia`, `codagencia` y `descripcion`. No enviar el logo binario ni todos los atributos del ActiveRecord por defecto. Una agencia inactiva no debe habilitar nuevas operaciones de firma.

## 5. Tablas `caja` y `compra`

`caja` tiene PK `idcaja`, código único, `idagencia` nullable con FK y bandera `activo`. La FK de agencia cuenta con índice.

| Campo de `compra` | Tipo local | Observación |
| --- | --- | --- |
| `idcompra` | `int`, PK autoincremental | Identificador para asociar la firma |
| `numcompra` | `varchar(255)`, obligatorio | Número visible; sin índice único local |
| `idcaja` | `int`, obligatorio, FK e índice | Enlace de la compra con la agencia |
| `idusuario` | `int`, obligatorio | Usuario registrado en la compra |
| `idcliente` | `int`, nullable, FK e índice | Identidad del cliente si se requiere |
| `fecha` | `datetime`, obligatorio | Fecha sin zona en la columna |
| `estado` | `int`, nullable | El servicio usa 1 vigente, 0 anulada |
| `efectivo`, `transferencia`, `adelanto`, `total`, `subtotal`, `descuento` | `decimal(12,3)`, nullable | Mantener precisión decimal |
| `forma_pago` | `varchar(255)`, nullable | Forma de pago |
| campos de creación/actualización | IDs y `datetime` | Auditoría existente |

`CompraService::create()` toma usuario y caja del usuario autenticado, genera el número con código/contador de agencia y guarda `estado = 1`. `delete()` anula estableciendo `estado = 0`. `update()` puede cambiar datos, usuario, caja y fecha; por ello hay que decidir qué ocurre con una firma cuando una compra firmada se edita.

No se encontró columna de firma en estas tablas ni tabla cuyo nombre contenga `firma` o `signature`. La plantilla `views/compra/print.php` solo dibuja la línea de firma del cliente; todavía no carga una imagen. El controlador de impresión obtiene la agencia del usuario de la compra, un criterio diferente del enlace por caja propuesto para el móvil.

## 6. Resultado de comprobaciones locales

| Comprobación | Resultado |
| --- | --- |
| Usuarios revisados | 22 |
| Usuarios sin agencia o con agencia inexistente | 0 |
| Usuarios con caja inexistente | 0 |
| Usuarios cuya agencia difiere de su caja asignada | 0 |
| Grupos de nombres de usuario duplicados | 1, con 2 cuentas activas |
| Compras revisadas | 4.422 |
| Compras sin caja válida o sin agencia de caja | 0 |
| Compras con usuario inexistente | 0 |
| Compras cuya caja difiere de la agencia actual de su usuario | 0 |
| Compras con `estado = 1` | 4.389 |
| Compras con `estado = 0` | 33 |

Estos agregados no sustituyen las validaciones del servidor. No se extrajeron hashes, tokens, credenciales ni información de clientes al repositorio móvil.

## 7. Comportamiento web y APIs existentes

### Compras web

`CompraController::actionIndex()` impone la caja asignada al usuario. `CompraSearch` añade `estado > 0` y, salvo administrador, filtra por `compra.idusuario`. Por tanto, **la visibilidad web actual es más estrecha que todas las compras de una agencia**.

El requisito móvil de filtrar por agencia debe ser una decisión explícita del nuevo módulo. No se debe cambiar `CompraSearch` para conseguirlo, porque afectaría a la web.

### `ApiCompraController`

- `index`: recibe `idcaja`, fecha y paginación, consultando `ComprasClientesView`.
- `totales`: obtiene agregados por caja/fecha.
- `detalle`: busca por `numcompra`.
- Este controlador no declara autenticación Bearer ni restricción por agencia del usuario en sus acciones. No ofrece el contrato necesario de firma por `idcompra`.

### `ApiAgenciaController`

Expone listados generales de agencias y cajas con `find()->all()`. El móvil de firmas necesita la agencia autorizada de su sesión, no un catálogo libre de todas las agencias.

### RRHH y otras integraciones

`ApiRrhhController`, `RrhhBearerAuth` y `rrhh_api_token` ya implementan credenciales móviles dedicadas, validación de usuario/agencia y rol `supervisor_movil`. Es una referencia de organización; su propósito y permiso pertenecen a otra aplicación. Hay además controladores `caponeras` y otras APIs existentes.

La configuración tiene una regla `/api/rrhh/<action>`; no existe todavía un prefijo reservado a firmas. El nuevo módulo debe utilizar un prefijo y tokens propios, una vez aprobados.

## 8. Recomendación

1. Conservar `usuario` y sus credenciales existentes.
2. Revisar los dos registros con `username` duplicado antes de habilitar ese acceso.
3. Acordar un módulo Yii de firmas con rutas versionadas y permiso propio.
4. Guardar sesiones móviles y firmas en tablas auxiliares, sin columnas nuevas en `usuario` ni `compra` como primera opción.
5. Autorizar cada operación por agencia mediante la caja de la compra.
6. Definir firma única/reemplazo, compras elegibles y tratamiento de compras editadas/anuladas.
7. Preparar el manifiesto de archivos para hosting solo cuando se apruebe el diseño de API.

La propuesta detallada y las preguntas están en [PROPUESTA_API.md](PROPUESTA_API.md). No se ejecutaron migraciones ni se modificaron archivos web.
