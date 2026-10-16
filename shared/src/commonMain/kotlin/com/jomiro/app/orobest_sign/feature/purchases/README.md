# Compras

Responsabilidad: listar y filtrar compras visibles en la agencia de la sesión, siempre identificadas mediante `idcompra`.

El móvil no crea ni modifica compras. La API aplica la autorización de agencia al listado, detalle, consulta de imagen y carga de firma.

El cliente KMP implementa `PurchasesRepository` con Ktor, serialización y token de `SessionStore`. El servidor pagina las compras vigentes de la agencia, sin restringir a la caja o creador y sin forzar fecha de hoy. Admite número, desde/hasta y estado de firma. Ver el contrato en `docs/PROPUESTA_API.md`.

Presentación en `screen/purchases`, con UiState, ViewModel y DI separados. Los botones agregar/editar seleccionan la compra; la captura y consulta del detalle/revisión se conectarán al desarrollar firma.
