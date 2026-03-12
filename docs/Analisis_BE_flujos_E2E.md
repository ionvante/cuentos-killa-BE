# Plan Integral de Pruebas E2E y Mejora de Flujos

Fecha: 2026-03-09

## 1. Objetivo y alcance

Este documento define un plan integral de pruebas end-to-end del backend de Cuentos de Killa, orientado a:

- QA: cobertura funcional, casos de error, permisos y evidencias.
- Producto: validacion de journeys reales y deteccion de fricciones.
- Desarrollo: identificacion de inconsistencias de contrato, endpoints, seguridad y estados.

El alcance se centra en journeys completos por actor y no solo en endpoints aislados. La fuente de verdad es el backend actual: seguridad, controladores, estados del pedido, flujo de direcciones, comprobantes, boleta, administracion, catalogos y webhook de Mercado Pago.

No se inventan flujos FE no respaldados por la implementacion observable del sistema.

## 2. Mapa de actores

### 2.1 Visitante

Usuario no autenticado que puede:

- Ver catalogo de cuentos.
- Consultar paginacion y detalle de cuento.
- Consultar ubigeo.
- Consultar maestros y configuracion publica expuesta por backend.

No puede:

- Usar carrito.
- Crear pedidos.
- Gestionar direcciones.
- Consultar perfil.
- Descargar boletas o comprobantes privados.

### 2.2 Cliente autenticado

Usuario con rol `USER` que puede:

- Iniciar sesion y mantener contexto de usuario.
- Gestionar perfil.
- Gestionar carrito.
- Gestionar direcciones propias.
- Crear pedidos.
- Crear preferencia de pago.
- Consultar y dar seguimiento a sus pedidos.
- Subir o eliminar comprobantes segun el estado del pedido.
- Descargar su boleta cuando exista.

### 2.3 Admin

Usuario con rol `ADMIN` que puede:

- Ver todos los pedidos.
- Cambiar estados de pedido.
- Gestionar cuentos.
- Reintentar boletas.
- Consultar estadisticas.
- Gestionar configuracion, maestros y parametros de facturacion.
- Ver o eliminar comprobantes de cualquier pedido.

### 2.4 Mercado Pago / Webhook

Sistema externo que:

- Notifica pagos aprobados.
- Reintenta notificaciones.
- Puede enviar payload por body o parametros.

### 2.5 Soporte / Operacion

Actor secundario de negocio, hoy representado de forma parcial por admin y procedimientos manuales. Interviene en:

- Rechazo o validacion de comprobantes.
- Reintentos de boleta.
- Seguimiento de pedidos en estado inconsistente.
- Atencion de reclamos de pago o entrega.

## 3. Inventario de macroflujos

### 3.1 Descubrimiento publico

Objetivo:
- Permitir exploracion de catalogo antes del login.

Actor principal:
- Visitante

Sistemas:
- `CuentoController`
- `UbigeoController`
- `MaestroController`
- `ConfigController`

Flujos incluidos:
- Listado de cuentos.
- Paginacion.
- Detalle de cuento.
- Consulta de ubigeo.
- Consulta de maestros/configuracion expuesta publicamente.

### 3.2 Autenticacion y sesion

Objetivo:
- Registrar usuario, autenticar y recuperar contexto para perfil/checkout.

Actor principal:
- Visitante / Cliente autenticado

Sistemas:
- `AuthController`
- `SecurityConfig`
- `UserController`

Flujos incluidos:
- Registro.
- Login.
- Uso del contrato de usuario en perfil/checkout.

### 3.3 Perfil y datos del cliente

Objetivo:
- Mantener datos personales consistentes para checkout y facturacion.

Actor principal:
- Cliente autenticado

Sistemas:
- `UserController`
- `UserResponseDTO`

Flujos incluidos:
- Consulta de perfil.
- Actualizacion de perfil.
- Consistencia de documento, telefono y nombre.

### 3.4 Direcciones

Objetivo:
- Mantener direcciones propias y permitir su reutilizacion en checkout.

Actor principal:
- Cliente autenticado
- Admin, como supervisor con permisos elevados

Sistemas:
- `DireccionController`
- `DireccionService`

Flujos incluidos:
- Listado de direcciones propias.
- Listado por usuario con control de permisos.
- Alta, edicion y eliminacion.
- Tipos de direccion.

### 3.5 Carrito

Objetivo:
- Preparar compra autenticada.

Actor principal:
- Cliente autenticado

Sistemas:
- `CartController`
- `CartService`

Flujos incluidos:
- Listar carrito.
- Agregar item.
- Eliminar item.

### 3.6 Checkout, pedido y seguimiento

Objetivo:
- Convertir carrito o seleccion de productos en pedido trazable.

Actor principal:
- Cliente autenticado
- Admin en consulta global

Sistemas:
- `OrderController`
- `OrderService`

Flujos incluidos:
- Crear pedido.
- Crear pedido con direccion guardada.
- Crear pedido con direccion nueva.
- Consultar pedido.
- Consultar estado.
- Eliminar pedido pendiente.

### 3.7 Pago con Mercado Pago

Objetivo:
- Generar preferencia y cerrar el pago con sincronizacion posterior.

Actor principal:
- Cliente autenticado
- Mercado Pago / Webhook

Sistemas:
- `OrderController`
- `MercadoPagoService`
- `MercadoPagoWebhookController`

Flujos incluidos:
- Crear preferencia.
- Iniciar pago.
- Confirmacion desde frontend.
- Confirmacion via webhook.
- Reintentos e idempotencia.

### 3.8 Pago manual y comprobantes

Objetivo:
- Permitir evidencia de pago y su administracion.

Actor principal:
- Cliente autenticado
- Admin

Sistemas:
- `OrderController`
- `PaymentVoucherController`
- `StorageService`
- `PaymentVoucherService`

Flujos incluidos:
- Subida de voucher local.
- Subida de voucher Firebase.
- Consulta de URL.
- Eliminacion.
- Restricciones por estado.

### 3.9 Boleta

Objetivo:
- Generar, descargar y reintentar comprobante de venta.

Actor principal:
- Cliente autenticado
- Admin
- Soporte / Operacion

Sistemas:
- `BoletaController`
- `BoletaService`

Flujos incluidos:
- Generacion automatica al pasar a `PAGO_VERIFICADO`.
- Descarga por cliente o admin.
- Estado no listo.
- Reintento manual admin.

### 3.10 Gestion administrativa

Objetivo:
- Operar el ciclo de vida del pedido y la configuracion interna.

Actor principal:
- Admin

Sistemas:
- `AdminController`
- `OrderController`
- `StatsController`
- `CuentoController`
- `FacturacionConfigController`
- `MaestroController`
- `ConfigController`

Flujos incluidos:
- Cambio de estado de pedidos.
- Mantenimiento de cuentos.
- Estadisticas.
- Parametros de facturacion.
- Maestros.
- Configuracion.

## 4. Matriz de cobertura

| Flujo | Actor | Criticidad | Tipo de prueba | Datos requeridos | Resultado esperado | Observabilidad |
|---|---|---|---|---|---|---|
| Catalogo publico | Visitante | P0 | Manual + API | Cuentos activos/inactivos | Lista correcta, sin auth | Logs app, respuesta API |
| Detalle de cuento | Visitante | P0 | Manual + API | ID valido e invalido | `200` o `404` correcto | Logs, payload |
| Registro | Visitante | P0 | API + Manual | Usuario nuevo, email existente | Alta exitosa o `409` | Logs auth, BD |
| Login | Visitante | P0 | API + Manual | Credenciales validas e invalidas | Token y contrato usuario consistente | Logs auth |
| Perfil | Cliente | P1 | API + Manual | Usuario autenticado | Lectura/actualizacion correcta | Logs users |
| Direcciones | Cliente/Admin | P1 | API + E2E | Usuario con y sin direcciones | CRUD con permisos correctos | Logs direcciones |
| Carrito | Cliente | P0 | API + Manual | Usuario autenticado, cuento existente | Agrega/lista/elimina | Logs cart |
| Crear pedido | Cliente | P0 | API + E2E | Usuario, items, direccion | Orden creada con snapshot valido | Logs orders, BD |
| Crear preferencia MP | Cliente | P0 | API + E2E | Pedido valido | `initPoint` y `orderId` | Logs orders/MP |
| Webhook MP | Sistema externo | P0 | API + Simulado | Pago aprobado, duplicado, payload parcial | Pedido actualizado de forma segura | Logs webhook |
| Voucher pago | Cliente/Admin | P0 | API + Manual | Pedido, archivo valido/invalido | Upload o rechazo por estado/tipo | Logs storage |
| URL/Delete voucher | Cliente/Admin | P1 | API | Pedido con voucher | URL valida o permisos correctos | Logs voucher |
| Cambio de estado | Admin | P0 | API + Manual | Pedido por estado | Transicion permitida y notificacion | Logs orders/email |
| Boleta | Cliente/Admin | P0 | API + E2E | Pedido `PAGO_VERIFICADO` | Descarga/reintento segun estado | Logs boleta, archivo |
| Estadisticas | Admin | P1 | API | Datos historicos | Respuesta agregada valida | Logs stats |
| Mantenimiento cuentos | Admin | P2 | API + Manual | JSON valido/invalido, imagen | CRUD correcto | Logs cuentos |
| Config/maestros/facturacion | Admin/Publico actual | P2 | API | Parametros y permisos | Acceso consistente con politica | Logs config |

## 5. Escenarios detallados por flujo

## 5.1 Descubrimiento publico

### Casos base

- Camino feliz:
  - Listado de cuentos devuelve items visibles.
  - Paginacion responde con page/size esperados.
  - Detalle de cuento responde `200`.
- Permiso:
  - Visitante puede consumir `/api/v1/cuentos/**`, `/api/v1/ubigeo/**`.
- Dato faltante o invalido:
  - ID inexistente devuelve `404`.
  - Paginacion con parametros extremos no debe romper.

### Hallazgos a verificar

- No hay filtro explicito por `habilitado` en el listado publico.
- `/api/v1/maestros/**` y `/api/v1/config/**` estan expuestos publicamente por `SecurityConfig`; validar si eso es deseado.

## 5.2 Autenticacion

### Casos base

- Registro exitoso.
- Registro con email ya existente.
- Login exitoso con contrato de usuario correcto.
- Login con credenciales invalidas.

### Compatibilidad

- Validar que perfil y login devuelvan mismo naming de usuario.
- Verificar divergencia entre `docs/USER_RESPONSE_CONTRACT.md` y backend actual respecto a `documento`.

## 5.3 Perfil

### Casos base

- Obtener perfil autenticado.
- Actualizar nombre, apellido, telefono, documento.

### Casos de error

- Sin token.
- Payload parcial o con formato invalido.

### Consistencia a validar

- El contrato de salida debe ser estable para FE.
- El perfil debe ser suficiente para precargar checkout.

## 5.4 Direcciones

### Camino feliz

- Listar direcciones propias por `/api/v1/direcciones/me`.
- Crear direccion con `tipoDireccion` valido.
- Editar direccion propia.
- Eliminar direccion propia.

### Permisos

- Cliente no puede listar por `usuarioId` ajeno.
- Cliente no puede editar/eliminar direccion ajena.
- Admin si puede operar sobre direcciones de otros usuarios.

### Validaciones

- `tipoDireccion` invalido.
- Direccion inexistente.
- Usuario sin direcciones.

### Compatibilidad UX

- Validar uso de direcciones guardadas en checkout con snapshot de pedido.

## 5.5 Carrito

### Camino feliz

- Usuario autenticado lista carrito vacio y con items.
- Agrega item.
- Elimina item.

### Riesgos a probar

- El backend no define guest cart.
- Verificar que no se pueda manipular carrito ajeno si se envia payload adulterado.

## 5.6 Checkout y pedido

### Camino feliz

- Crear pedido con direccion nueva.
- Crear pedido con `direccionId` valido.
- Consultar pedido propio.
- Consultar estado.

### Casos de error

- `direccionId` ajeno.
- Item inexistente.
- Pedido inexistente.
- Eliminar pedido no permitido por estado.

### Casos de consistencia

- Snapshot de direccion persistido en orden.
- Edicion posterior de direccion maestra no debe alterar pedido historico.

## 5.7 Pago Mercado Pago

### Camino feliz

- Crear preferencia retorna `initPoint` y `orderId`.
- Webhook de pago aprobado actualiza pedido.

### Casos alternos

- Confirmacion frontend retorna `pending`.
- Reintento de webhook.
- Payload por query params.
- Payload por body.

### Casos de error

- Webhook vacio.
- `data.id` faltante.
- Pago no aprobado.
- `externalReference` ausente.

### Riesgos UX/flujo

- Existen varias etapas funcionalmente cercanas: `create-preference`, `pay`, `confirmar-pago-mercadopago`, `webhook`.
- El journey de pago no esta simplificado ni documentado como una unica secuencia.

## 5.8 Pago manual y comprobantes

### Camino feliz

- Cliente sube comprobante valido.
- Admin consulta URL.
- Cliente o admin elimina comprobante cuando aplica.

### Casos de error

- Archivo vacio.
- Tipo de archivo invalido.
- Pedido ya pagado o en estado no permitido.
- Comprobante inexistente.
- Intento de eliminar comprobante ajeno.

### Riesgos UX

- Multiplicidad de rutas: `voucher`, `voucherF`, `voucher-url`, `DELETE /voucher`.
- Necesidad de consolidar en una experiencia mas clara para cliente y admin.

## 5.9 Boleta

### Camino feliz

- Cambio a `PAGO_VERIFICADO` genera boleta.
- Cliente descarga su boleta.
- Admin descarga cualquier boleta.

### Casos alternos

- Boleta aun no lista.
- Error de generacion.
- Reintento manual admin exitoso.
- Reintento manual admin con error persistente.

### Casos de consistencia

- Boleta usa direccion congelada del pedido.
- Reintento no debe revertir el pedido.

### Riesgos UX

- Dos rutas funcionalmente equivalentes: `/orders/{id}/pdf` y `/orders/{id}/boleta`.

## 5.10 Gestion admin

### Camino feliz

- Ver todos los pedidos.
- Cambiar estado de pedido.
- Consultar estadisticas.
- Gestionar cuentos.
- Gestionar parametros de facturacion.

### Casos de error

- Estado invalido.
- Cambio no permitido por permisos.
- Parametro inexistente.

### Riesgos de flujo

- `AdminController` hoy parece placeholder y duplica funciones cubiertas por `OrderController`.
- Conviene decidir si se elimina del journey operativo o se convierte en modulo real.

## 5.11 Sistema externo / webhook

### Casos a cubrir

- Webhook valido y aprobado.
- Webhook duplicado.
- Webhook con body vacio y params validos.
- Webhook con payload parcial.
- Error al consultar Mercado Pago.

### Riesgo critico

- `SecurityConfig` exige autenticacion para `/api/v1/webhooks/**`, pero el controller describe consumo por sistema externo. Es una inconsistencia funcional y de despliegue que debe resolverse antes de certificar el flujo.

## 6. Datos de prueba sugeridos

- Usuario nuevo sin direcciones ni pedidos.
- Cliente autenticado con:
  - 2 direcciones guardadas.
  - carrito con 2 cuentos.
  - pedido `PAGO_PENDIENTE`.
  - pedido `PAGO_ENVIADO`.
  - pedido `PAGO_VERIFICADO`.
  - pedido con boleta en `ERROR`.
- Admin con acceso a todos los modulos.
- Pago Mercado Pago aprobado con `externalReference`.
- Voucher local y voucher Firebase.

## 7. Priorizacion de ejecucion

### P0

- Login y contrato de usuario.
- Catalogo publico.
- Carrito autenticado.
- Crear pedido.
- Crear preferencia y webhook de pago.
- Subida de comprobante.
- Cambio de estado a `PAGO_VERIFICADO`.
- Descarga y reintento de boleta.

### P1

- Perfil.
- Direcciones.
- Consulta de pedidos e historial.
- Descarga de boleta por rutas alternativas.
- Estadisticas admin.

### P2

- Maestros.
- Configuracion.
- Parametros de facturacion.
- Casos operativos raros y mantenimiento interno.

## 8. Hallazgos y mejoras UX / funcionales

## 8.1 Añadir

### Alta prioridad

- Definir de forma explicita si existira guest checkout o si autenticacion sera obligatoria en toda compra.
- Agregar trazabilidad funcional visible para admin en revision de comprobantes, ultimo intento de boleta y motivo de rechazo.
- Documentar un journey unico de pago que indique que paso hace FE, que paso hace backend y que paso cierra el webhook.

### Media prioridad

- Añadir matriz oficial de permisos por actor y endpoint.
- Añadir guia de evidencias para QA: logs, estados, archivos generados, cambios en BD.

## 8.2 Simplificar

### Alta prioridad

- Consolidar gestion de estado de pedidos en un solo modulo operativo. Hoy hay solape entre `/api/v1/admin/**` y `/api/v1/orders/{id}/status`.
- Simplificar flujo de pago. Hoy hay superposicion entre:
  - `POST /api/v1/orders/mercadopago/create-preference`
  - `POST /api/v1/orders/{id}/pay`
  - `POST /api/v1/orders/{id}/confirmar-pago-mercadopago`
  - `POST /api/v1/webhooks/mercadopago`
- Unificar experiencia de boleta en una sola ruta publica del producto y dejar la otra como compatibilidad interna.

### Media prioridad

- Unificar manejo de comprobantes en una sola experiencia, evitando dividir entre local/Firebase en la capa funcional.
- Reducir estados semanticamente cercanos del pedido: `PAGADO`, `VERIFICADO`, `PAGO_VERIFICADO`.

## 8.3 Retirar / Consolidar

### Alta prioridad

- Revisar si `AdminController` debe retirarse del journey funcional hasta que tenga implementacion real, o completar sus acciones para que no sea un modulo placeholder.
- Corregir `docs/USER_RESPONSE_CONTRACT.md` para que deje de publicar `documento` como campo oficial si el backend ya opera con `documentoNumero`.

### Media prioridad

- Revisar exposicion publica de `/api/v1/maestros/**` y `/api/v1/config/**`.
- Revisar si mantener ambas rutas de boleta y multiples rutas de voucher aporta valor o solo complejidad.

## 9. Riesgos abiertos que deben seguirse

- Inconsistencia de seguridad en webhook de Mercado Pago.
- Exposicion publica de maestros/configuracion sin confirmacion de necesidad de negocio.
- Multiplicidad de rutas para boleta y comprobantes.
- Duplicidad de modulos admin/estado.
- Contrato de usuario desfasado respecto a documentacion.
- Dependencia del login para usar carrito y checkout, sin una decision formal de producto.

## 10. Recomendacion de uso del documento

- QA debe usar este plan como matriz base para pruebas de regresion y smoke por release.
- Producto debe priorizar primero los hallazgos de `alta prioridad` que afectan conversion, claridad del journey de pago y trazabilidad operativa.
- Desarrollo debe tomar los flujos P0 como base minima de automatizacion estable y usar este documento para alinear FE, BE y soporte.
