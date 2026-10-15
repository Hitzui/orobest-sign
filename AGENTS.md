# Guía del proyecto

## Contexto

OroBest Sign es un proyecto Kotlin Multiplatform (Android, iOS y JVM) complementario a la web Yii2 situada en `C:\xampp\htdocs\orobest`. El repositorio móvil es independiente del backend.

Leer `README.md`, `docs/ARQUITECTURA.md`, `docs/ANALISIS_WEB.md` y `docs/PROPUESTA_API.md` antes de implementar funcionalidades.

## Acuerdos actuales

- La etapa actual prepara arquitectura, librerías y análisis. Las pantallas móviles todavía no están autorizadas en esta etapa.
- **Consultar al usuario antes de crear o modificar APIs, migraciones o archivos del hosting.** El prefijo y las tablas de `docs/PROPUESTA_API.md` son propuestas, no decisiones aprobadas.
- Conservar la estructura de la tabla `usuario`. Si se considera necesario un ajuste, explicar el motivo y consultarlo primero.
- No reutilizar permisos ni tokens de RRHH para firmas sin una decisión explícita.
- La firma se vincula por `idcompra`. La agencia de la compra se deriva de `compra.idcaja → caja.idagencia`; la autorización corresponde al servidor.
- El análisis detectó dos cuentas activas con el mismo `username`; resolver la ambigüedad de datos antes de habilitar ese acceso. No copiar información personal ni credenciales a este repositorio.

## Convenciones

- Usar Koin para inyección por constructor y Lifecycle ViewModel + StateFlow para MVVM compartido.
- Organizar por funcionalidad y capas siguiendo `docs/ARQUITECTURA.md`.
- Usar Composables UI y sus iconos Lucide; las dependencias están en el catálogo Gradle.
- HTTP con Ktor y Kotlinx Serialization; motores nativos en sus source sets.
- Versiones y coordenadas centralizadas en `gradle/libs.versions.toml`.
- No fijar URLs de hosting o credenciales en el código. Acordar la configuración al definir el contrato del backend.

## Verificación

En Windows, ejecutar para cambios de infraestructura compartida:

```powershell
.\gradlew.bat :shared:jvmTest :shared:testAndroidHostTest :androidApp:assembleDebug
```

La compilación y las pruebas iOS requieren macOS/Xcode; indicar cuando no se hayan verificado.
