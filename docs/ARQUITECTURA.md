# Arquitectura KMP y MVVM

## Objetivo

Compartir lógica, estado y UI entre Android e iOS, con JVM como destino auxiliar de desarrollo. Las implementaciones que dependen del sistema operativo viven en los source sets nativos.

## Organización por funcionalidad y capas

Paquete base: `com.jomiro.app.orobest_sign`.

```text
feature/<funcionalidad>/
  domain/
    model/        Modelos del negocio, independientes del JSON
    repository/   Interfaces de repositorios
    usecase/      Reglas o acciones de negocio que lo justifiquen
  data/
    remote/       Fuentes HTTP y DTO con Kotlinx Serialization
    mapper/       Conversión de DTO a dominio
    repository/   Implementaciones de repositorios
  di/             Registro del repositorio, casos de uso y ViewModel

screen/login/
  LoginRoute.kt                 Obtención del ViewModel y estado local de los campos
  LoginScreen.kt                Vista sin acceso directo a HTTP/repositorios
  uistate/LoginUiState.kt        Estado inmutable de presentación
  viewmodel/LoginViewModel.kt    Acciones y coordinación de sesión
  di/LoginModule.kt              Registro del ViewModel

screen/purchases/
  PurchasesRoute.kt              Alcance de sesión, campos y observación de estado
  PurchasesScreen.kt             Listado, filtros, paginación y selección de compra
  PurchaseFormatting.kt          Decimales exactos y calendario de la agencia
  uistate/PurchasesUiState.kt    Estado de consulta y presentación
  viewmodel/PurchasesViewModel.kt
  di/PurchasesModule.kt          Cliente, repositorio y ViewModel

screen/signature/
  SignatureRoute.kt              Alcance de captura, cierre y éxito confirmado
  SignatureScreen.kt             Pantalla completa y confirmaciones
  components/                   Hoja girada y lienzo de trazos normalizados
  uistate/SignatureUiState.kt
  viewmodel/SignatureViewModel.kt
  di/SignatureModule.kt          Repositorio, exportador y ViewModel

feature/clients/
  domain/model/                   Cliente y documentos, independientes del JSON
  domain/repository/              Contrato de listado y carga documental
  data/remote/                    API Sign de clientes con búsqueda y multipart
  data/repository/                Implementación ligada a SessionStore
  di/                             Registro de API, repositorio y ViewModel

screen/clients/
  ClientsRoute.kt                 Navegación de listado a documentos
  ClientDocumentPicker.kt         Contrato común para cámara y selectores
  ClientDocumentPreview.kt        Descarga, visor de imágenes con zoom y PDF nativo
  uistate/ClientsUiState.kt       Estado de consulta, carga y reintento
  viewmodel/ClientsViewModel.kt   Búsqueda, documentos y errores de sesión
```

Estas subcapas se crearán con código real al implementar cada funcionalidad. Evitar repositorios ficticios, DTO anticipados y casos de uso que solo reenvían llamadas sin aportar una regla.

Dependencias: `presentation → domain ← data`. `domain` no importa Ktor, Koin, Compose ni clases PHP. Los DTO son detalles de `data`, no el estado de la UI.

## Inyección de dependencias

- Koin inicia en `App` mediante `KoinApplication`, ligado al ciclo de vida de la composición.
- `core/di/AppModules.kt` registra JSON, motor HTTP y cliente como singletons.
- Tanto el cliente como el motor se liberan al cerrar el contenedor.
- `appModules` incluye autenticación y los módulos de login, compras, firma y clientes, con repositorios y ViewModels por constructor.
- Repositorios y ViewModels reciben dependencias por constructor. El contenedor no se consulta dentro del dominio.
- `LoginViewModel` se registra con `viewModelOf` de Koin y se obtiene en Compose mediante `koinViewModel`, conservando el alcance de Lifecycle.

## MVVM

`core/presentation/StateViewModel.kt` proporciona un `StateFlow` de solo lectura y actualización atómica protegida.

- Cada ViewModel tendrá un estado inmutable específico de su funcionalidad.
- Las acciones de la vista invocarán métodos del ViewModel.
- Las operaciones suspendidas usarán `viewModelScope`; no `GlobalScope`.
- Compose observará el estado mediante `collectAsStateWithLifecycle`.
- Los errores esperados se traducirán a estados de UI sin confundirlos con operaciones exitosas.
- La cancelación de coroutines debe propagarse. No capturar `CancellationException` como error de negocio.
- La vista dibuja y envía acciones; no consulta HTTP ni decide la agencia autorizada.

El login está implementado en `screen/login` con `LoginUiState` separado, `LoginViewModel` y vista. Coordina restauración inicial, acceso, validación, reintento y cierre de sesión. Después del acceso muestra `screen/purchases`, cuyo ViewModel carga páginas y valida filtros. Una nueva consulta cancela la anterior y comprueba su identificador para descartar respuestas antiguas. Cada cambio de filtro vuelve a página 1; actualizar/reintentar conserva la consulta aplicada. La selección abre `screen/signature` con su propio ViewModelStore, liberado al cerrar la captura.

## Red

El cliente compartido ya dispone de:

- Negociación JSON con tolerancia a campos nuevos del servidor.
- `Accept: application/json`, sin forzar JSON para futuras cargas multipart.
- Tiempo de conexión de 15 segundos y tiempos de petición/socket de 60 segundos.
- Validación de estados HTTP (`expectSuccess`).
- Redirecciones desactivadas; las futuras rutas de API deberán responder directamente.
- Motores OkHttp / Darwin / CIO seleccionados mediante `expect` / `actual`.

`ApiConfig.Production` configura el dominio aprobado `https://orobestni.com/api/sign/v1/`. `AuthApi` usa JSON y Bearer para login, perfil y logout; `DefaultAuthRepository` implementa restauración y cierre de sesión. Al crear `LoginViewModel` se restaura la sesión: solo se consulta `auth/me` si existe un token local. `PurchasesApi` y `ClientsApi` consultan listados con Bearer; el token se obtiene en los repositorios desde `SessionStore`, sin exponerlo a la UI. La carga de clientes usa multipart y conserva la operación y bytes en memoria para reintentos. Un rechazo `401`/`403` solicita revalidación desde el login.

## UI e iconos

`App` utiliza `OroBestTheme`, que aplica la paleta aprobada sobre `ComposablesTheme` de `com.composables:ui:0.2.0`. Los colores y sus usos están documentados en [PALETA.md](PALETA.md) y centralizados en `core/designsystem/theme`. Está disponible `com.composables:icons-lucide:1.0.0`; sus vectores se usarán con el componente `Icon` de Composables UI.

El contenedor raíz muestra `LoginRoute`. El formulario usa Composables UI e iconos Lucide; la contraseña usa `BasicSecureTextField` de Foundation con tokens del tema. Los campos viven en la ruta, la contraseña solo en memoria, sin guardarse en el estado del sistema operativo. El acceso exitoso muestra `HomeRoute` con las pestañas Compras y Clientes. La lista de compras usa `LazyColumn`, tarjetas blancas con bordes suaves, estado de firma y botones con iconos para agregar/editar. La selección abre `SignatureRoute`; un guardado confirmado actualiza el listado y muestra la versión guardada.

`HomeRoute` presenta la barra inferior **Compras | Clientes** y conserva un `ViewModelStore` por pestaña durante la sesión, identificado por usuario/agencia en la raíz y liberado al salir. Al cambiar de pestaña se conservan consultas, paginación y estado de los ViewModels; los campos y el desplazamiento usan estado guardable de Compose. La barra se oculta durante la captura de firma a pantalla completa. Sin filtros de fecha se consulta todo el historial vigente de la agencia.

Desde la pestaña Clientes se abre la lista global y paginada de clientes; cada cliente abre su pantalla de documentos. La cámara/fototeca y el selector PDF se implementan en los source sets: Android usa Activity Result y FileProvider; iOS usa UIImagePickerController y UIDocumentPicker; JVM permite elegir archivos. Las imágenes se normalizan antes de subirlas y los adjuntos/reintentos solo viven en memoria.

En JVM/Windows, `ClientDocumentScanner` agrega adquisición WIA/TWAIN con `DesktopScannerViewModel` (StateFlow) e inyección por constructor desde el módulo de plataforma. `WindowsDocumentScanner` consulta WIA y TWAIN de 32/64 bits en procesos PowerShell STA aislados, serializa el acceso y cancela el proceso al salir. El recurso C# `TwainBridge.cs` usa NTwain (MIT); Gradle obtiene su DLL net462 de NuGet con versión centralizada y SHA-256 fijado, e incorpora DLL, scripts y licencia al JAR JVM. Los identificadores TWAIN incluyen arquitectura y nombre de la fuente; los ID WIA anteriores siguen siendo válidos. `WindowsScannerPreferences` conserva exclusivamente ID/nombre del equipo en preferencias por usuario de Windows. Cada pantalla de documentos posee su propio alcance de ViewModel para impedir que una adquisición tardía llegue a otro cliente. La vista previa se acepta en memoria antes de pasar al flujo existente de carga; el puente limpia sus temporales. Ver [ESCANER_WINDOWS.md](ESCANER_WINDOWS.md).

Los scripts publican su respuesta UTF-8 en `result.json` mediante escritura temporal y renombrado dentro del directorio de la operación. stdout/stderr se descartan: los mensajes nativos del controlador no forman parte del protocolo. La JVM verifica terminación, presencia, tamaño y JSON de la respuesta antes de leer la imagen; una salida sin respuesta indica el código de terminación cuando está disponible. Las pruebas Windows ejecutan el script real con una fuente sintética que emite ruido en consola, devuelve errores/cancelación o termina abruptamente, sin adquirir documentos físicos.

El selector de archivos permite lotes de hasta 10 documentos (JPG/PNG, PDF, DOC, DOCX y TXT). El ViewModel genera un UUID por documento y envía el lote secuencialmente; ante un resultado incierto conserva el documento actual y los restantes, sin repetir los confirmados. El backend común `app\services\ClienteDocumentService` es utilizado por Sign y el formulario web para validar contenido, añadir sin sobrescribir, resolver rutas históricas/privadas y eliminar adjuntos individuales.

Cada documento disponible ofrece **Ver**. `ClientsViewModel` descarga su contenido con Bearer mediante el repositorio y conserva los bytes solo mientras se consulta; cerrar el visor cancela la petición y libera su referencia. Las respuestas tardías no pueden reemplazar una vista previa nueva. Se usa el MIME de la respuesta HTTP, ya que los registros históricos pueden guardar extensiones en `tipo`. Las imágenes tienen zoom y desplazamiento; Android muestra PDF paginados con `PdfRenderer`, iOS usa PDFKit y escritorio abre el lector PDF predeterminado. Android elimina su PDF temporal después de renderizar cada página; escritorio conserva una copia temporal hasta cerrar el proceso para permitir que el lector externo la abra.

## Datos y sesión

El móvil se comunica exclusivamente por HTTP con el backend. No conoce credenciales SQL ni ejecuta consultas contra la base de datos web.

La pertenencia de una compra se validará en el servidor mediante su caja y la agencia del usuario autenticado. No se debe sustituir esta comprobación por filtros locales ni por la agencia actual del creador de la compra.

Los importes de `compra` son `DECIMAL(12,3)` y se conservan como texto decimal, incluso al añadir separadores visuales, sin conversión a coma flotante ni moneda supuesta. Las fechas de la API son ISO 8601 con offset `-06:00`; el listado muestra su día/hora de agencia, sin convertirlos al huso horario del dispositivo.

`SessionStore` tiene implementaciones nativas: Android cifra con AES-GCM usando una clave no exportable de Android Keystore; iOS guarda en Keychain con `WhenUnlockedThisDeviceOnly`. JVM solo mantiene el token en memoria para desarrollo. El servidor conserva únicamente el hash y renueva la sesión hasta 90 días desde el último uso. La contraseña no se persiste.

El repositorio serializa operaciones de sesión con Mutex y expone el perfil como StateFlow. Al restaurar consulta `auth/me`: errores de red conservan el token, `401` lo elimina y `403` conserva la credencial pero no publica un perfil autorizado. El logout elimina el token local incluso si falla la petición de revocación; en ese caso el servidor lo conserva hasta revocación o vencimiento.

## Captura PNG y orientación

- Android declara `screenOrientation=portrait`; Xcode declara únicamente orientación vertical en iPhone/iPad. La hoja se mide con dimensiones intercambiadas y se coloca con una capa girada 90°, incluidos sus controles. Compose transforma también la entrada táctil a las coordenadas locales. Una ventana ya apaisada no recibe otro giro; validar las políticas de ventanas/orientación en tabletas recientes.
- `SignatureCanvas` consume un puntero por trazo y almacena coordenadas normalizadas. La UI y el exportador usan la misma geometría suavizada. El PNG es 1600 × 800, blanco y azul de marca, sin guías. Codificación con Bitmap en Android y Skia en iOS/JVM; el renderizado se ejecuta fuera del hilo UI.
- `SignaturePlatformEffect` intercepta Atrás de Android mediante Activity Compose y mantiene la pantalla encendida durante la captura en Android/iOS, restaurando el valor al salir. Los diálogos de descarte se miden y giran para mantener la misma orientación visual que la hoja.
- `SignatureRepository` consulta detalle/revisión y la imagen de la versión actual con Bearer. La edición muestra la imagen previa y permite capturar nuevos trazos. La firma anterior no se altera hasta la confirmación del nuevo guardado.
- El primer envío genera UUID y PNG; los reintentos conservan ese mismo contenido, revisión y versión base. La UI bloquea modificar el envío incierto y ofrece reintentar. Un conflicto `409` conserva los trazos visibles pero exige recarga explícita, con confirmación para descartarlos; nunca cambia silenciosamente la revisión bajo una firma capturada.
- Solo se publica éxito si la API devuelve el ID y la versión esperados. No se permite guardar vacío, con el puntero aún activo ni mientras se guarda. Salir con trazos/envío incierto requiere confirmación. `401`/`403` solicita revalidación del acceso.
- Los trazos y el envío pendiente están en memoria del ViewModel; todavía no hay cola offline persistente. Tras terminar el proceso se debe volver a consultar la compra, incluida su última versión, antes de otro envío.

El backend guarda PNG en `<storageRoot>/<idcompra>/<nombre-aleatorio>.png` y la tabla `compra_firma` conserva ruta/versiones. El trabajo offline/cola persistente de carga móvil aún no está implementado.

## Comprobaciones

- Las pruebas de `HttpClientFactoryTest` usan MockEngine y no contactan con OroBest.
- Verifican lectura JSON compatible y rechazo de una respuesta `401`.
- `AppModulesTest` comprueba que Koin puede resolver el cliente real JVM y el serializador compartido.
- `AuthRepositoryTest` comprueba restauración desde un repositorio recreado, conservación de sesión sin conexión y eliminación ante `401`.
- `LoginSessionTest` verifica el flujo ViewModel/repositorio con HTTP simulado: restauración al recrear la presentación, agencia actual del servidor, recuperación tras fallo de red, sesión vencida, validación, bloqueo de envíos duplicados y logout sin conexión.
- `PurchasesFlowTest` verifica Bearer, parámetros autorizados, filtros/paginación, valores nulos, precisión decimal, fechas, reintentos, rechazo de acceso y descarte de respuestas antiguas.
- `SignatureFlowTest` verifica consulta de revisión/imagen autorizada, multipart PNG, reintento idéntico, conflictos sin rebase automático, descarte, bloqueo de guardados vacíos/duplicados y rechazo de acceso.
- `SignaturePngTest` renderiza y decodifica PNG real en JVM, verifica dimensiones, fondo blanco, orientación, trazos/puntos y ausencia de la guía. `jvmTest` incluye el runtime nativo de Compose Desktop para estas comprobaciones gráficas.
- `backend/tests/run.php` verifica reglas de agencia, sesiones, firmas versionadas y conflictos en una base local efímera.
- Ejecutar los tests Android/JVM y `assembleDebug` al modificar la infraestructura compartida.
- Validar el framework y los tests iOS en macOS cuando ese entorno esté disponible.
