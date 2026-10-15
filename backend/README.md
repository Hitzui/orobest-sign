# Backend entregable de OroBest Sign

`orobest/` contiene únicamente los archivos nuevos que se instalan en la web Yii2. Es la fuente versionada del módulo; la instalación local está en `C:\xampp\htdocs\orobest`.

- Contrato: [docs/PROPUESTA_API.md](../docs/PROPUESTA_API.md).
- Lista de archivos y cambio de configuración: [docs/DESPLIEGUE_HOSTING.md](../docs/DESPLIEGUE_HOSTING.md).
- `tests/run.php`: integración con base local efímera y datos sintéticos.
- `tools/migrate-local.php`: limita la migración a MySQL local y verifica que las cuatro tablas de negocio conserven su esquema.

No se incluye `config/db.php`, el archivo completo `config/web.php`, credenciales ni datos del hosting. El propietario realiza la sincronización de esquema y la subida de archivos.
