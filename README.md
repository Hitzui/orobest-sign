# OroBest Sign

Aplicación **Kotlin Multiplatform (KMP)** complementaria al sistema web OroBest. Su objetivo es capturar la firma del cliente en el móvil, convertirla en imagen y asociarla a una compra mediante **`idcompra`**.

Repositorio: <https://github.com/Hitzui/orobest-sign>

## Estado actual

Base KMP preparada con DI, MVVM, cliente HTTP, serialización y Composables UI/Lucide. El backend Sign v1 está implementado y migrado **localmente**, con acceso por agencia, sesión persistente y edición versionada de firmas.

La entrada `App` inicializa Koin y el tema sin contenido de pantallas. KMP ya tiene repositorio de autenticación/restauración y almacenes de token nativos. Las pantallas, el cliente móvil de compras y la captura se implementarán en la siguiente etapa. No hay peticiones automáticas desde `App`.

Dominio aprobado: `https://orobestni.com/`. El propietario subirá los archivos y sincronizará únicamente las dos tablas nuevas: [instrucciones de hosting](docs/DESPLIEGUE_HOSTING.md).

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
    App.kt                     Raíz de composición: Koin + ComposablesTheme
    core/
      di/                      Módulos de Koin
      network/                 Cliente HTTP y contrato de motor nativo
      presentation/            StateViewModel con StateFlow de solo lectura
      session/                 Contrato de almacenamiento seguro del token
    feature/
      auth/                    Dominio, DTO, cliente y repositorio de autenticación
      agency/                  Modelo de agencia de la sesión
      purchases/               Alcance documentado de compras
      signature/               Alcance documentado de firma
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

La organización por funcionalidad separa dominio, datos y presentación. Autenticación ya implementa dominio y datos; la presentación se incorporará al comenzar las pantallas. Compras y firma tienen su alcance móvil documentado y backend disponible.

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

## Siguiente etapa

El propietario sincroniza las dos tablas y sube [los archivos indicados](docs/DESPLIEGUE_HOSTING.md). Después se verifica la API desplegada y se desarrollan los flujos MVVM del móvil. Queda por decidir la ubicación de la firma en el comprobante y su tratamiento visual si una compra firmada fue modificada.
