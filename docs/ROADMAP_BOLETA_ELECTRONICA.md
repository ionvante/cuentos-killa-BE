# Roadmap y Diseño Técnico: Implementación de Boletas de Venta (SUNAT - Perú)

## Contexto Legal y Negocio
En Perú, el e-commerce exige la emisión de comprobantes de pago. Para KillaCuentos, al tratarse de venta de libros y cuentos, aplica la **Ley N° 31053**, que establece la **exoneración del IGV**.
Esto simplifica el cálculo de impuestos (Subtotal = Total), pero requiere especificar los códigos correctos según los estándares de SUNAT para que la boleta o factura tenga validez legal ("Representación Impresa").

## 1. Diseño de Tablas Maestras (Base de Datos)
Para evitar "hardcodear" información, se parametrizarán los siguientes datos en la tabla `tabla_catalogo_general` (`Maestro`) o en una nueva entidad `ParametrosEmpresa`:

*   **Datos Emisor (KillaCuentos):** RUC, Razón Social, Dirección Fiscal, y Logo.
*   **Catálogo SUNAT 01 (Tipo de Comprobante):** Código `03` (Boleta de Venta). *El código `01` se reservará para Facturas futuras.*
*   **Catálogo SUNAT 07 (Tipo de Afectación IGV):** Código `20` (Exonerado - Operación Onerosa). Este es el código vital para declarar libros.
*   **Catálogo SUNAT 06 (Tipos de Documento Cliente):** Mapeo de identidad: DNI (`1`), CE (`4`), Pasaporte (`7`).
*   **Control de Numeración:** Una tabla o registro para controlar la **Serie** (ej. `B001`) y el **Correlativo** (ej. `1`, para formar `B001-00000001`).

## 2. Desarrollo Backend (Spring Boot / Java)

### Opción A (Recomendada para Fase 1): Generación de PDF (Representación Impresa)
Ideal si la facturación real se hace manualmente en el Portal SOL de SUNAT al final del día.
1.  **BoletaService:** Lógica de negocio que procesa la `Order` cuando el Administrador cambia el estado a "Validar Pago".
2.  **Librería de PDF:** Integración de **JasperReports** u **OpenPDF** en el `pom.xml`.
3.  **Plantilla (.jrxml o template):** Diseño formal tamaño A4 o Ticket (80mm) con el logo de KillaCuentos, datos del cliente, detalle de la orden y total exonerado.
4.  **Generación y Almacenamiento:** El servicio tomará el correlativo actual, inyectará la data a la plantilla, y generará físicamente el PDF guardándolo en `/uploads/boletas`.
5.  **Exposición API:** Nuevo endpoint `GET /api/v1/pedidos/{id}/boleta` para que el frontend pueda descargar el archivo.

### Opción B (Evolución Fase 2): Integración Facturación Electrónica APIRest
Ideal para automatización total, declarando a SUNAT en tiempo real.
1.  **Proveedor OSE/PSE:** Conexión con APIs de facturación (Nubefact, APIPeru, Migo, etc.).
2.  **Envío XML:** El Backend transforma la `Order` a un payload JSON estandarizado y lo envía al Facturador.
3.  **Recepción:** El Facturador se encarga de ir a SUNAT, devolver el ticket CDR (Constancia de Recepción) y el enlace directo al PDF en la nube del proveedor, ahorrando espacio local.

## 3. Desarrollo Frontend (Angular UI)

*   **Flujo de Compra (Checkout):** 
    *   Mantener la obligatoriedad de `Nombres` y `Documento (DNI/CE)` en el formulario de envío. Aunque para montos menores a S/700 la SUNAT permite "Boletas genéricas" sin DNI, capturar estos datos desde siempre da más formalidad al recibo y previene vacíos.
*   **Panel de Usuario (Mis Pedidos):**
    *   Habilitar un estado visual: Cuando una orden pasa a estado `APROBADO` / `PAGO VERIFICADO`, se mostrará un nuevo botón con el icono de un PDF ("Descargar Boleta de Venta").
    *   Al hacer click, llamará al endpoint `/api/v1/pedidos/{id}/boleta` para mostrar el archivo generado por Java.
*   **Panel de Administrador (Dashboard):**
    *   La tabla de Órdenes mostrará una nueva columna "Comprobante", indicando la serie y número generado (ej. `B001-00000062`).
    *   El admin también tendrá acceso a descargar el mismo PDF para sus registros contables.

## 4. Historias de Usuario (Fase 1: Generación de PDF Local)

### Épica: Facturación Electrónica - Representación Impresa

| ID | Capa | Historia de Usuario | Criterios de Aceptación |
| :--- | :--- | :--- | :--- |
| **US-BE-01** | Backend | **Como** Administrador, **quiero** poder almacenar los parámetros de mi Empresa y las Series de facturación en la BD **para** no depender de variables duras al emitir comprobantes. | 1. Tabla `ParametrosEmpresa` creada (RUC, Razón Social, etc).<br>2. Endpoint CRUD seguro para que el Admin gestione estos datos. |
| **US-BE-02** | Backend | **Como** Sistema, **quiero** generar un PDF con estructura de Boleta al aprobarse un pedido, **para** cumplir con la entrega del comprobante (Art. 31053 Exonerado IGV). | 1. Template integrado (JasperReports/OpenPDF).<br>2. Servicio intercepta cambio de estado a "Pago Verificado".<br>3. Genera PDF con el correlativo actual y lo guarda en `/uploads/boletas`. |
| **US-BE-03** | Backend | **Como** Cliente/Admin, **quiero** un endpoint para consultar el PDF de mi boleta **para** poder descargarlo a mi dispositivo. | 1. Endpoint `GET /api/v1/pedidos/{id}/boleta`.<br>2. Validación de seguridad (solo dueño del pedido o Admin puede descargar). |
| **US-FE-01** | Frontend | **Como** Cliente, **quiero** que el formulario de Datos de Envío valide obligatoriamente mi Documento (DNI/CE/Pasaporte) y Nombres completos **para** que mi boleta tenga mis datos correctos. | 1. Campos DNI/CE y Nombres no pueden estar vacíos ni ser inválidos.<br>2. Mapeo de valores con códigos SUNAT en el payload final (1=DNI, 4=CE). |
| **US-FE-02** | Frontend | **Como** Cliente logueado, **quiero** ver un botón de "Descargar Boleta" en Mis Pedidos cuando el estado sea 'Pago Validado' **para** obtener mi recibo oficial. | 1. Botón visible solo en estados finales.<br>2. Al dar click, abre una pestaña nueva o acciona descarga del blob PDF llamando a `US-BE-03`. |
| **US-FE-03** | Frontend | **Como** Administrador, **quiero** una columna de "Comprobante" en el Dashboard de Ventas **para** saber qué correlativo (Ej. B001-0004) se le asignó a cada venta y descargarla. | 1. Tabla de pedidos suma columna "Comprobante".<br>2. Botón de descarga/visualización incrustado. |

---
*Documento autogenerado tras evaluación de arquitectura E2E y análisis de negocio para la plataforma KillaCuentos.*
