# Agencia

Responsabilidad prevista: representar y mostrar la agencia devuelta por la sesión autenticada. No habrá administración ni selector libre de agencias en el móvil.

El servidor obtiene `usuario.idagencia` y valida que la agencia esté activa. El filtro de compras se aplica mediante `compra.idcaja → caja.idagencia`.

`domain/model/Agency.kt` representa la agencia del perfil de sesión. La API valida su estado en cada petición; ver `docs/ANALISIS_WEB.md` y `docs/PROPUESTA_API.md`.
