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
  presentation/
    ...ViewModel.kt
    ...UiState.kt
    ...Screen.kt
  di/             Registro del repositorio, casos de uso y ViewModel
```

Estas subcapas se crearán con código real al implementar cada funcionalidad. Evitar repositorios ficticios, DTO anticipados y casos de uso que solo reenvían llamadas sin aportar una regla.

Dependencias: `presentation → domain ← data`. `domain` no importa Ktor, Koin, Compose ni clases PHP. Los DTO son detalles de `data`, no el estado de la UI.

## Inyección de dependencias

- Koin inicia en `App` mediante `KoinApplication`, ligado al ciclo de vida de la composición.
- `core/di/AppModules.kt` registra JSON, motor HTTP y cliente como singletons.
- Tanto el cliente como el motor se liberan al cerrar el contenedor.
- Los módulos de funcionalidad se incorporarán a `appModules` cuando existan sus implementaciones.
- Repositorios y ViewModels reciben dependencias por constructor. El contenedor no se consulta dentro del dominio.
- Los ViewModels se registrarán con el DSL `viewModel` de Koin y se obtendrán en Compose mediante `koinViewModel`, conservando el alcance de Lifecycle.

## MVVM

`core/presentation/StateViewModel.kt` proporciona un `StateFlow` de solo lectura y actualización atómica protegida.

- Cada ViewModel tendrá un estado inmutable específico de su funcionalidad.
- Las acciones de la vista invocarán métodos del ViewModel.
- Las operaciones suspendidas usarán `viewModelScope`; no `GlobalScope`.
- Compose observará el estado mediante `collectAsStateWithLifecycle`.
- Los errores esperados se traducirán a estados de UI sin confundirlos con operaciones exitosas.
- La cancelación de coroutines debe propagarse. No capturar `CancellationException` como error de negocio.
- La vista dibuja y envía acciones; no consulta HTTP ni decide la agencia autorizada.

No se han creado ViewModels de login/compras/firma porque dependen de contratos todavía pendientes.

## Red

El cliente compartido ya dispone de:

- Negociación JSON con tolerancia a campos nuevos del servidor.
- `Accept: application/json`, sin forzar JSON para futuras cargas multipart.
- Tiempo de conexión de 15 segundos y tiempos de petición/socket de 60 segundos.
- Validación de estados HTTP (`expectSuccess`).
- Redirecciones desactivadas; las futuras rutas de API deberán responder directamente.
- Motores OkHttp / Darwin / CIO seleccionados mediante `expect` / `actual`.

No hay URL, Bearer, DTO de compra, endpoint de login ni repositorio remoto configurados. Se incorporarán al aprobar `docs/PROPUESTA_API.md`.

## UI e iconos

`App` ya utiliza `ComposablesTheme` de `com.composables:ui:0.2.0`. Está disponible `com.composables:icons-lucide:1.0.0`; sus vectores se usarán con el componente `Icon` de Composables UI.

El contenedor raíz permanece sin contenido de pantallas. La captura de firma necesitará una superficie de trazos y un exportador PNG; su implementación y las adaptaciones nativas se decidirán al desarrollar esa funcionalidad.

## Datos y sesión

El móvil se comunica exclusivamente por HTTP con el backend. No conoce credenciales SQL ni ejecuta consultas contra la base de datos web.

La pertenencia de una compra se validará en el servidor mediante su caja y la agencia del usuario autenticado. No se debe sustituir esta comprobación por filtros locales ni por la agencia actual del creador de la compra.

Los importes de `compra` son `DECIMAL(12,3)`; al acordar los DTO se conservarán como texto decimal o representación exacta. Las fechas de la web son `datetime` en contexto `America/Managua`; el contrato debe definir explícitamente zona y formato antes de convertirlas a instantes en KMP.

La persistencia de tokens, el trabajo offline y los reintentos de imágenes son decisiones futuras. Un reintento de carga debe poder resolverse sin generar firmas duplicadas; esta regla pertenece también al backend.

## Comprobaciones

- Las pruebas de `HttpClientFactoryTest` usan MockEngine y no contactan con OroBest.
- Verifican lectura JSON compatible y rechazo de una respuesta `401`.
- `AppModulesTest` comprueba que Koin puede resolver el cliente real JVM y el serializador compartido.
- Ejecutar los tests Android/JVM y `assembleDebug` al modificar la infraestructura compartida.
- Validar el framework y los tests iOS en macOS cuando ese entorno esté disponible.
