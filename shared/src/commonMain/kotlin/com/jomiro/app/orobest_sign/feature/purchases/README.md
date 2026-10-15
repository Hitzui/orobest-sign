# Compras

Responsabilidad prevista: listar, filtrar, consultar y seleccionar compras visibles en la agencia de la sesión, siempre identificadas mediante `idcompra`.

El móvil no creará ni modificará compras. La API deberá aplicar la misma autorización de agencia al listado, detalle, consulta de imagen y carga de firma.

El backend ya implementa listado paginado y detalle de compras vigentes por agencia, sin restringir a la caja o creador y sin forzar fecha de hoy. El cliente KMP y su presentación se incorporarán al comenzar la UI; ver el contrato en `docs/PROPUESTA_API.md`.
