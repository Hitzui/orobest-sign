# OroBest Sign

Aplicación **Kotlin Multiplatform (KMP)** complementaria al sistema web OroBest. Su objetivo es capturar la firma del cliente en el móvil, convertirla en imagen y asociarla a una compra mediante **`idcompra`**.

Repositorio: <https://github.com/Hitzui/orobest-sign>

## Estado actual

Etapa de análisis y preparación técnica. La base incluye inyección de dependencias, soporte MVVM compartido, cliente HTTP multiplataforma, serialización y Composables UI con iconos Lucide.

La entrada `App` inicializa Koin y el tema de Composables UI, sin contenido de pantallas. El login, la selección de compras y la captura de firma se implementarán en las siguientes etapas. La API y sus migraciones están **pendientes de consulta y aprobación**; no hay una URL de servidor configurada ni peticiones automáticas.

## Flujo previsto

1. Iniciar sesión con un usuario existente de OroBest.
2. El servidor valida sus credenciales, estado, permisos y agencia asignada.
3. Consultar las compras de esa agencia y seleccionar una por `idcompra`.
4. Capturar y confirmar la firma del cliente.
5. Subir la imagen y recibir confirmación del vínculo con la compra.

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
    feature/
      auth/                    Alcance documentado del login
      agency/                  Alcance documentado de agencia
      purchases/               Alcance documentado de compras
      signature/               Alcance documentado de firma
  androidMain/                 Motor OkHttp
  iosMain/                     Motor Darwin
  jvmMain/                     Motor CIO
  commonTest/                  Pruebas compartidas de infraestructura HTTP
  jvmTest/                     Prueba del grafo de dependencias
docs/
  ARQUITECTURA.md              Reglas KMP, DI y MVVM
  ANALISIS_WEB.md              Tablas, relaciones y hallazgos verificados
  PROPUESTA_API.md             Propuesta pendiente y preguntas de integración
```

Cada funcionalidad incorporará `domain`, `data`, `presentation` y `di` conforme se aprueben los contratos. Actualmente las carpetas `feature` documentan responsabilidades; no contienen implementaciones de negocio ni pantallas.

## Relación con la web

Backend local analizado: `C:\xampp\htdocs\orobest` (PHP 8.1+, Yii2 y base de datos MySQL/MariaDB).

```text
usuario.idagencia ────────────────> agencia.idagencia
compra.idcaja ──> caja.idcaja
                 caja.idagencia ─> agencia.idagencia
```

`caja` es necesaria para relacionar compras con agencias, aunque la app no administrará cajas. La firma se asociará por `idcompra`, no por el número visible de compra ni por la agencia actual del usuario que creó la compra.

La tabla `usuario` puede conservar su estructura. Se detectaron dos cuentas activas con el mismo `username`, que requieren revisión de datos antes del login móvil. Consulta los detalles en [el análisis web](docs/ANALISIS_WEB.md).

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
```

En macOS/Linux se usa `./gradlew`; para iOS abrir `iosApp/iosApp.xcodeproj`. Las comprobaciones Android/JVM no sustituyen una compilación iOS en macOS.

Verificación inicial del 15 de octubre de 2026 en Windows: `:shared:jvmTest`, `:shared:testAndroidHostTest` y `:androidApp:assembleDebug` completados correctamente. iOS queda pendiente de verificación en macOS.

## Configuración futura

La dirección HTTPS y el prefijo de la API se incorporarán después de acordar la organización del backend y del hosting. Las credenciales web, conexiones de base de datos y tokens no pertenecen al código móvil ni al repositorio.

El almacenamiento de sesión se decidirá con el login; si debe persistir entre ejecuciones, se implementará mediante almacenamiento seguro nativo. No se ha añadido persistencia de credenciales ni sincronización offline en esta etapa.

## Próxima decisión

Revisar [la propuesta API](docs/PROPUESTA_API.md): prefijo propio, permiso de firma, tablas auxiliares de tokens y firmas, compras elegibles, política de reemplazo e integración de la imagen en el comprobante web. Una vez acordado, se implementará primero el backend y su contrato, y luego los flujos MVVM del móvil.
