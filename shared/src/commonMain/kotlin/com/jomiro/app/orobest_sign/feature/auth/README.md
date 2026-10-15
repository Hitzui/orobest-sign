# Autenticación

Implementado: contrato de dominio, DTO, cliente remoto y repositorio para login, restauración con `auth/me` y logout. El repositorio publica el perfil y la agencia mediante StateFlow; `SessionStore` guarda el token de forma segura en Android/iOS.

Acceden todos los usuarios activos con agencia activa, incluidos los de caja, sin rol adicional. Koin registra el repositorio en `core/di/AppModules.kt`. Faltan los ViewModels y pantallas de esta funcionalidad. No se usan los tokens ni el rol de RRHH.

Referencia: `docs/ARQUITECTURA.md` y `docs/PROPUESTA_API.md` en la raíz del repositorio.
