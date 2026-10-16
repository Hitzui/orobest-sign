# OroBest Sign

Aplicación **Kotlin Multiplatform (KMP)** complementaria al sistema web OroBest. Su objetivo es capturar la firma del cliente en el móvil, convertirla en imagen y asociarla a una compra mediante **`idcompra`**.

Repositorio: <https://github.com/Hitzui/orobest-sign>

## Estado actual

Base KMP preparada con DI, MVVM, cliente HTTP, serialización y Composables UI/Lucide. El backend Sign v1 está implementado y migrado **localmente**, con acceso por agencia, sesión persistente y edición versionada de firmas.

La entrada `App` inicializa Koin, el tema y el **login con sesión persistente**. Al abrir la app se restaura la sesión guardada consultando el servidor; si no hay sesión válida se muestra el formulario. Después del acceso se muestra la **lista paginada de compras de la agencia**, con filtros y acciones de firma. La captura abre una pantalla completa, exporta PNG y lo sube a la API, con edición versionada y reintentos idempotentes. El éxito se muestra únicamente cuando el servidor confirma el guardado.

Dominio aprobado: `https://orobestni.com/`. El propietario subirá los archivos y sincronizará únicamente las dos tablas nuevas: [instrucciones de hosting](docs/DESPLIEGUE_HOSTING.md).

Paleta visual aprobada e integrada en el tema compartido: **`#ABBCDA` · `#FFFFFF` · `#415D8A` · `#D0E1F5`**. Ver [colores y usos](docs/PALETA.md).

## Flujo previsto

1. Iniciar sesión con un usuario existente de OroBest.
2. El servidor valida sus credenciales, estado y agencia asignada; acceden todos los usuarios activos, incluidos los de caja, sin un rol adicional.
3. Consultar las compras de esa agencia y seleccionar una por `idcompra`.
4. Capturar y confirmar la firma del cliente.
5. Subir la imagen y recibir confirmación del vínculo con la compra.
6. Editar la firma creando una nueva versión, conservando el historial.

La agencia autorizada se determina en el servidor. El móvil no puede elegir otra agencia enviando un identificador distinto.

## Tecnologías

Versiones centralizadas en [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

| Área | Librería | Versión |
| --- | --- | --- |
| Lenguaje | Kotlin Multiplatform | 2.4.20 |
| UI compartida | Compose Multiplatform | 1.12.1 |
| Componentes UI | [Composables UI](https://github.com/composablehorizons/composables-ui) | 0.2.0 |
| Iconos | Composables Icons / Lucide | 1.0.0 |
| Inyección de dependencias | Koin Core, Compose y Compose ViewModel | 4.2.2 |
| MVVM | JetBrains AndroidX Lifecycle / ViewModel | 2.11.0 |
| Asincronía y estado | Coroutines / StateFlow | 1.11.0 |
| HTTP | Ktor Client | 3.6.0 |
| JSON | Kotlinx Serialization | 1.11.0 |

Motores HTTP: **OkHttp** en Android, **Darwin** en iOS y **CIO** en JVM. Pruebas de red con Ktor MockEngine; soporte de pruebas de coroutines incluido.

Los componentes se usarán mediante `com.composables.ui`; los iconos mediante `com.composables.icons.lucide`, de acuerdo con la documentación oficial. No es necesario copiar todo el repositorio de componentes.

## Estructura

```text
androidApp/                    Entrada Android y configuración nativa
iosApp/                        Entrada SwiftUI / proyecto Xcode
desktopApp/                    Entrada JVM para desarrollo
shared/src/
  commonMain/kotlin/com/jomiro/app/orobest_sign/
    App.kt                     Raíz de composición: Koin + OroBestTheme
    core/
      di/                      Módulos de Koin
      designsystem/theme/      Paleta y tema de Composables UI
      network/                 Cliente HTTP y contrato de motor nativo
      presentation/            StateViewModel con StateFlow de solo lectura
      session/                 Contrato de almacenamiento seguro del token
    feature/
      auth/                    Dominio, DTO, cliente y repositorio de autenticación
      agency/                  Modelo de agencia de la sesión
      purchases/               Dominio, DTO, cliente y repositorio de compras
      signature/               Dominio, HTTP, repositorio y exportador PNG
    screen/login/
      LoginRoute.kt            Conexión de Compose con ViewModel y estado de campos
      LoginScreen.kt           Formulario y estados de sesión con Composables UI
      uistate/LoginUiState.kt   Estado inmutable, separado de la vista
      viewmodel/               LoginViewModel con StateFlow y restauración
      di/                      Registro del ViewModel con Koin
    screen/purchases/
      PurchasesRoute.kt        Alcance de sesión y conexión con el ViewModel
      PurchasesScreen.kt       Listado, filtros, paginación y acciones de firma
      PurchaseFormatting.kt    Presentación exacta de importes y fechas
      uistate/                 Estado inmutable separado de la vista
      viewmodel/               Cargas, filtros, reintentos y selección
      di/                      Repositorio y ViewModel por constructor
    screen/signature/
      SignatureRoute.kt        Alcance de captura y navegación de regreso
      SignatureScreen.kt       Hoja horizontal, vista previa y confirmaciones
      components/              Lienzo táctil y disposición girada
      uistate/                 Estado de trazos, envío y conflictos
      viewmodel/               Revisión, versiones, PNG y reintentos
      di/                      Registro de dependencias de firma
  androidMain/                 OkHttp y almacenamiento cifrado con Keystore
  iosMain/                     Darwin y almacenamiento en Keychain
  jvmMain/                     CIO y sesión en memoria para desarrollo
  commonTest/                  Pruebas compartidas de HTTP y restauración de sesión
  jvmTest/                     Prueba del grafo de dependencias
docs/
  ARQUITECTURA.md              Reglas KMP, DI y MVVM
  ANALISIS_WEB.md              Tablas, relaciones y hallazgos verificados
  PROPUESTA_API.md             Contrato aprobado e implementado de Sign v1
  DESPLIEGUE_HOSTING.md        Archivos exactos y sincronización del esquema
backend/
  orobest/                    Archivos nuevos del módulo Yii2 y su configuración
  tests/                      Integración PHP en una base local efímera
  tools/                      Migración local aislada y verificación del esquema
```

La organización por funcionalidad separa dominio y datos en `feature/auth`, `feature/purchases` y `feature/signature`, con presentación en `screen/login`, `screen/purchases` y `screen/signature`.

## Relación con la web

Backend local analizado: `C:\xampp\htdocs\orobest` (PHP 8.1+, Yii2 y base de datos MySQL/MariaDB).

```text
usuario.idagencia ────────────────> agencia.idagencia
compra.idcaja ──> caja.idcaja
                 caja.idagencia ─> agencia.idagencia
```

`caja` es necesaria para relacionar compras con agencias, aunque la app no administrará cajas. La firma se asociará por `idcompra`, no por el número visible de compra ni por la agencia actual del usuario que creó la compra.

La tabla `usuario` puede conservar su estructura. Se detectaron dos cuentas activas con el mismo `username`, que requieren revisión de datos antes del login móvil. Consulta los detalles en [el análisis web](docs/ANALISIS_WEB.md).

La migración local añadió únicamente `sign_api_token` y `compra_firma`; los esquemas de `usuario`, `agencia`, `caja` y `compra` se verificaron sin cambios. La sesión Sign dura 90 días de inactividad y se renueva con cada petición autenticada. La firma se integrará visualmente en el comprobante cuando se defina el lugar.

## Requisitos y comandos

- Android Studio con soporte KMP y Android SDK 37; Android mínimo API 24.
- JDK 21 para el daemon de Gradle (criterio configurado en `gradle/gradle-daemon-jvm.properties`).
- Gradle Wrapper incluido: 9.5.1.
- `local.properties` con la ubicación local del SDK; se excluye de Git.
- Para compilar o ejecutar iOS: macOS y Xcode.

En PowerShell desde la raíz:

```powershell
.\gradlew.bat :androidApp:assembleDebug
.\gradlew.bat :desktopApp:run
.\gradlew.bat :shared:jvmTest :shared:testAndroidHostTest
C:\xampp\php\php.exe backend\tests\run.php C:\xampp\htdocs\orobest
```

En macOS/Linux se usa `./gradlew`; para iOS abrir `iosApp/iosApp.xcodeproj`. Las comprobaciones Android/JVM no sustituyen una compilación iOS en macOS.

Verificación del 15 de octubre de 2026 en Windows: tests JVM/Android y APK debug correctos; 36 comprobaciones PHP de integración correctas, incluyendo aislamiento de agencias, sesión, carga, edición, historial y reintentos. iOS/Keychain quedan pendientes de compilación y verificación en macOS. El almacenamiento Android requiere además validación en un dispositivo/emulador con Keystore.

## Configuración y sesión

`core/network/ApiConfig.kt` centraliza la dirección aprobada `https://orobestni.com/api/sign/v1/` y permite inyectar otra dirección HTTPS para pruebas. Las credenciales web, conexiones de base de datos y tokens reales no pertenecen al repositorio.

Android cifra el token con AES-GCM y una clave de Keystore; iOS usa Keychain. No se guarda la contraseña. `AuthRepository.restoreSession()` restaura el perfil consultando el servidor y conserva el token ante fallos de red. JVM mantiene sesión solo en memoria para desarrollo. La cola offline de cargas todavía no está implementada.

El login obtiene su ViewModel por Koin y observa `LoginUiState` con Lifecycle. Un fallo de conexión al restaurar ofrece **Reintentar** o **Usar otra cuenta**; solo se publica una sesión activa después de verificarla con el servidor. La contraseña vive únicamente en memoria del formulario, sin `rememberSaveable`, y se limpia al autenticar. El cierre de sesión elimina el token local incluso si no hay conexión para revocarlo en el servidor.

## Compras y filtros

- Consulta las compras vigentes de **todas las cajas de la agencia**, con 20 compras por página, ordenadas por fecha descendente por el servidor.
- Busca por número de compra con el botón de búsqueda o la tecla buscar del teclado.
- **Todas / Pendientes / Firmadas** aplica el estado de firma y los campos actuales.
- **Rango de fechas** permite indicar desde/hasta en `AAAA-MM-DD`, con días incluidos. Las fechas se validan antes de consultar; por defecto se incluye todo el historial vigente.
- **Limpiar** elimina los filtros y vuelve a la primera página. **Actualizar** repite la consulta aplicada.
- Los importes conservan los decimales exactos de la API, sin conversión a `Double` ni moneda asumida. Las fechas se muestran en el calendario de la agencia.
- Ante `401`/`403` se vuelve a verificar la sesión. Las compras se liberan al salir del alcance autenticado; los errores de red permiten reintentar.
- Los botones **Agregar firma / Editar firma**, con iconos Lucide, abren la captura vinculada por `idcompra`. Al guardar se actualiza el listado conservando los filtros.

## Captura y almacenamiento de firma

- La app móvil declara orientación vertical. La hoja de captura y sus controles se dibujan girados 90°: sostener el teléfono de lado con su parte superior hacia la izquierda. La aplicación no cambia de orientación al girar físicamente el teléfono.
- En ventanas que el sistema mantenga apaisadas, la hoja aprovecha esa disposición sin un giro adicional. Android en pantallas grandes y sistemas iPad recientes pueden imponer reglas de orientación/redimensionado propias; validar en los dispositivos previstos.
- Antes de capturar se consulta la compra actual: revisión, estado y versión de firma. Al editar se muestra la imagen anterior, descargada con autorización; **Capturar nueva** permite reemplazarla mediante una nueva versión.
- **Limpiar**, **Cancelar** y **Guardar firma**. No se permite guardar un lienzo vacío ni enviar dos veces mientras hay un guardado en curso. Salir con trazos pendientes requiere confirmación mediante Composables UI.
- La exportación genera un **PNG horizontal de 1600 × 800**, fondo blanco y tinta azul de marca, sin controles ni línea guía. Android codifica con Bitmap y iOS/JVM con Skia.
- La carga multipart incluye `operation_id`, `compra_revision`, `base_version` y el archivo PNG. Si no se confirma la respuesta se conserva el mismo envío para **Reintentar guardado**, sin permitir cambiar sus trazos durante ese reintento.
- Si otra persona cambia la compra/firma, se solicita recarga explícita y se confirma el descarte de los trazos antes de capturar sobre los datos actuales.
- Los trazos y el envío pendiente viven en memoria: todavía no existe una cola offline persistente. Tras cerrar el proceso, volver a consultar la compra para comprobar su última firma.
- En el hosting, el backend guarda los archivos bajo `runtime/signatures/<idcompra>/<48-caracteres-aleatorios>.png`, o bajo `OROBEST_SIGN_STORAGE_PATH` si está configurado. La tabla `compra_firma` guarda la ubicación, versión y vínculo con la compra. Cada edición conserva el archivo anterior.
- La imagen se consulta por la API autenticada `compras/{idcompra}/firma`, no mediante una URL pública al archivo. Ver [despliegue](docs/DESPLIEGUE_HOSTING.md).

Verificación de captura en Windows: pruebas Android/JVM del flujo de envío, conflictos, acceso y reintentos, más exportación PNG real en JVM. Compilación Android debug comprobada. El giro/tacto en dispositivo y la compilación/ejecución iOS requieren validación en sus entornos nativos.

## Siguiente etapa

Probar el flujo completo en dispositivo y hosting: capturar, guardar, volver a consultar y editar. Queda por decidir la ubicación de la firma en el comprobante y su tratamiento visual si una compra firmada fue modificada. Las instrucciones del backend están en [DESPLIEGUE_HOSTING.md](docs/DESPLIEGUE_HOSTING.md).
