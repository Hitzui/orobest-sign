# Firma

Responsabilidad prevista: capturar trazos, limpiar/confirmar la captura, exportarla como PNG y subirla asociada a `idcompra`.

La captura no equivale a una carga exitosa: el estado de éxito requiere confirmación del servidor. El backend ya implementa carga PNG, edición mediante versiones, historial e idempotencia. Cada envío utiliza operation_id, compra_revision y base_version.

Las capas y las adaptaciones de exportación nativa se incorporarán con la funcionalidad; ver `docs/PROPUESTA_API.md`.
