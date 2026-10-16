# Guía del proyecto

## Contexto

OroBest Sign es un proyecto Kotlin Multiplatform (Android, iOS y JVM) complementario a la web Yii2 situada en `C:\xampp\htdocs\orobest`. El repositorio móvil es independiente del backend.

Leer `README.md`, `docs/ARQUITECTURA.md`, `docs/ANALISIS_WEB.md` y `docs/PROPUESTA_API.md` antes de implementar funcionalidades.

## Acuerdos actuales

- La etapa actual implementa el backend aprobado y la sesión compartida. Las pantallas móviles todavía no están autorizadas en esta etapa.
- El usuario aprobó el módulo `sign`, `/api/sign/v1` y dos tablas nuevas: `sign_api_token` y `compra_firma`. No alterar tablas existentes. Consultar cambios de alcance adicionales.
- Acceso para todos los usuarios activos con agencia activa, incluidos los de caja, sin permiso RBAC adicional. Compras por agencia, no por caja ni creador. Sesión persistente y firma editable con versiones.
- Dominio aprobado: `https://orobestni.com/`. El usuario sube los archivos y sincroniza el esquema del hosting después de las migraciones locales. No conectarse a la base de producción.
- Conservar la estructura de la tabla `usuario`. Si se considera necesario un ajuste, explicar el motivo y consultarlo primero.
- No reutilizar permisos ni tokens de RRHH para firmas sin una decisión explícita.
- La firma se vincula por `idcompra`. La agencia de la compra se deriva de `compra.idcaja → caja.idagencia`; la autorización corresponde al servidor.
- El análisis detectó dos cuentas activas con el mismo `username`; resolver la ambigüedad de datos antes de habilitar ese acceso. No copiar información personal ni credenciales a este repositorio.

## Convenciones

- Usar Koin para inyección por constructor y Lifecycle ViewModel + StateFlow para MVVM compartido.
- Organizar por funcionalidad y capas siguiendo `docs/ARQUITECTURA.md`.
- Usar Composables UI y sus iconos Lucide; las dependencias están en el catálogo Gradle.
- Paleta aprobada: #ABBCDA, #FFFFFF, #415D8A y #D0E1F5. Usar `OroBestTheme` y tokens semánticos; ver `docs/PALETA.md`.
- HTTP con Ktor y Kotlinx Serialization; motores nativos en sus source sets.
- Versiones y coordenadas centralizadas en `gradle/libs.versions.toml`.
- La dirección aprobada se centraliza en `ApiConfig.Production`; no dispersar URLs ni incluir credenciales reales en código/documentos.
- `backend/orobest` es la copia versionada y entregable del módulo web. Mantenerla sincronizada con los mismos archivos de la instalación local, sin copiar configuración con secretos ni trabajo de otros módulos.

## Verificación

En Windows, ejecutar para cambios de infraestructura compartida:

```powershell
.\gradlew.bat :shared:jvmTest :shared:testAndroidHostTest :androidApp:assembleDebug
```

La compilación y las pruebas iOS requieren macOS/Xcode; indicar cuando no se hayan verificado.
