# Firma

Responsabilidad: capturar trazos, limpiar/confirmar la captura, exportarla como PNG y subirla asociada a `idcompra`.

La captura no equivale a una carga exitosa: el estado de éxito requiere confirmación del servidor. El backend ya implementa carga PNG, edición mediante versiones, historial e idempotencia. Cada envío utiliza operation_id, compra_revision y base_version.

`domain` contiene coordenadas normalizadas, revisión/versión base, operación de carga y contratos. `data` implementa HTTP autenticado, multipart y renderizado/PNG con adaptaciones Android e iOS/JVM. La vista y ViewModel están en `screen/signature`.

La app se mantiene en vertical y la hoja gira 90° para sujetar el teléfono de lado; las coordenadas se capturan en el lienzo horizontal, por lo que el PNG queda derecho. Las guías visuales no forman parte de la imagen exportada. Una edición captura trazos nuevos y conserva la versión anterior en el servidor.

El envío incierto se conserva en memoria para un reintento idéntico. Los conflictos no se rebasan automáticamente; se requiere consultar la revisión actual y confirmar el descarte de trazos. Ver `docs/PROPUESTA_API.md` y `docs/ARQUITECTURA.md`.
