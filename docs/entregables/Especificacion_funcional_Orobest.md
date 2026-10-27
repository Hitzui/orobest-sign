# Orobest
## Especificación funcional y resumen de entrega

**Cliente:** Orobest  
**Proveedor de desarrollo:** SEIN  
**Plataformas de entrega:** Android y Windows  
**Fecha de emisión:** 17 de octubre de 2026

### 1. Objeto del servicio

Desarrollo e integración de una aplicación complementaria al sistema web Orobest para consultar compras, registrar firmas manuscritas de clientes y gestionar sus documentos. Este documento presenta las funcionalidades implementadas y el valor del servicio para su revisión y aprobación de pago.

### 2. Funcionalidades de la aplicación

#### 2.1. Acceso y navegación

- Inicio de sesión con los usuarios autorizados del sistema Orobest, validando que el usuario y su agencia estén activos.
- Conservación de la sesión en Android para facilitar el acceso posterior y opción de cierre de sesión.
- Navegación mediante las secciones **Compras** y **Clientes**.

#### 2.2. Consulta de compras

- Listado paginado de las compras de la agencia del usuario, incluyendo las compras de sus distintas cajas.
- Búsqueda por número de compra y filtros por rango de fechas y estado de firma: todas, pendientes o firmadas.
- Consulta de la compra seleccionada y acceso a las acciones para agregar o editar su firma.

#### 2.3. Registro de firmas

- Captura de la firma manuscrita del cliente en una pantalla dedicada, con opciones para limpiar, cancelar y guardar.
- Vinculación de la firma con la compra correspondiente y consulta de la firma previamente registrada.
- Edición mediante una nueva versión, conservando las versiones anteriores en el sistema.
- Integración con el comprobante de compra para mostrar la firma más reciente en el espacio de firma del cliente.

#### 2.4. Consulta de clientes y gestión documental

- Listado global de clientes con búsqueda y paginación, y consulta de sus documentos adjuntos.
- Captura de fotografías desde el dispositivo móvil y selección de imágenes o archivos existentes. Organización por **cédula frente**, **cédula reverso** y **otros documentos**.
- Incorporación de hasta diez archivos por selección, sin reemplazar los documentos anteriores. Compatibilidad con imágenes JPG/PNG, PDF, Word (DOC/DOCX) y texto (TXT).
- Visualización de imágenes con ampliación y consulta de documentos mediante el visor disponible en cada plataforma. Eliminación individual de adjuntos con confirmación.

<!-- pagebreak -->

### 3. Escaneo directo en Windows

- Captura de documentos desde un escáner instalado en la computadora, mediante las opciones **Escanear frente**, **Escanear reverso** y **Escanear otro**.
- Selección del escáner, conservación del equipo elegido en la computadora y opción para cambiarlo. Integración con controladores WIA y TWAIN de 32 y 64 bits, para conexiones USB o de red compatibles con el controlador instalado.
- Captura de una cara por acción, vista previa con ampliación y selección de la imagen antes de enviarla al sistema. Se mantiene disponible la selección de archivos del equipo.

**Prueba operativa:** el escaneo fue confirmado con una Epson L5590 conectada por red. En la configuración actual, el color y la resolución de captura pueden depender del perfil guardado en el software o controlador del escáner.

### 4. Flujo de trabajo documental

El usuario busca al cliente, abre sus documentos, toma una fotografía, selecciona un archivo o realiza un escaneo; después revisa la imagen y confirma su incorporación. El envío al sistema se realiza al pulsar **Subir documentos**, permitiendo comprobar los adjuntos desde la ficha del cliente.

### 5. Componentes de la entrega

- Aplicación Android para consulta de compras, captura de firmas y gestión documental.
- Aplicación Windows con escaneo directo e instalador identificado como **Orobest**, fabricante **SEIN**, icono propio y accesos directos en el escritorio y menú Inicio.
- Integración con el sistema web Orobest para autenticación, consulta y almacenamiento de firmas y documentos, junto con documentación de uso y requisitos.

La consulta y el envío de información requieren conexión con el sistema web. En Windows, el escaneo utiliza el controlador y la configuración de conexión disponibles en la computadora.

### 6. Valor del servicio

| Concepto | Importe |
| --- | ---: |
| Desarrollo e integración de la aplicación Orobest y las funcionalidades descritas | US$250.00 |
| **Valor total del servicio** | **US$250.00** |

**Monto en letras:** doscientos cincuenta dólares estadounidenses.

### 7. Conformidad y aprobación de pago

Se presenta la aplicación y el alcance funcional descrito para revisión del cliente y autorización del pago correspondiente al servicio de desarrollo e integración.

**Nombre del representante:** __________________________________________  
**Cargo:** __________________________________________________________  
**Firma:** __________________________  **Fecha:** ______________________
