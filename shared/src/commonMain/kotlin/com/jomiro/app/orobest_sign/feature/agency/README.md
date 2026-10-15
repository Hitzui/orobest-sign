# Agencia

Responsabilidad prevista: representar y mostrar la agencia devuelta por la sesión autenticada. No habrá administración ni selector libre de agencias en el móvil.

El servidor obtiene `usuario.idagencia` y valida que la agencia esté activa. El filtro de compras se aplica mediante `compra.idcaja → caja.idagencia`.

Los modelos y las capas se incorporarán al aprobar el contrato; ver `docs/ANALISIS_WEB.md`.
