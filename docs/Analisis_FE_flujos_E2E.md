# Plan integral de pruebas y mejora de flujos UX (Frontend + APIs)

Fecha base del documento: 2026-03-09  
Estado: Propuesto para ejecucion  
Alcance confirmado: Frontend + APIs (sin incluir implementacion interna del backend)

## 1) Resumen ejecutivo

Este documento define un plan de pruebas integral para `cuentos-killa-FE` basado en el estado real del proyecto (rutas, guards, componentes y servicios actuales), con foco en:

- Validar flujos E2E por actor (`Visitante`, `Cliente autenticado`, `Administrador`, `Sistemas externos`).
- Reducir riesgo funcional en compra, pago y postcompra.
- Identificar mejoras UX de alto impacto separadas en:
  - `Quick wins` (2-4 semanas)
  - `Roadmap` (5-10 semanas)

Fuentes de verdad usadas para mapear el sistema:

- Rutas y control de acceso: `src/app/app.routes.ts`, `src/app/guards/auth.guard.ts`, `src/app/guards/admin.guard.ts`
- Flujos cliente: `navbar`, `cart`, `mini-cart`, `checkout`, `pago`, `voucher`, `order-list`, `order-detail`, `login`, `register`, `profile`
- Flujos admin: `admin/*` (dashboard, cuentos, pedidos, usuarios, maestros/config)
- Integraciones API: `auth.service.ts`, `pedido.service.ts`, `pago.service.ts`, `maestros.service.ts`, `cliente.service.ts`, `cuento.service.ts`, `user.service.ts`

## 2) Mapa de actores y responsabilidades

| Actor | Objetivo principal | Responsabilidades en flujo | Riesgos principales |
|---|---|---|---|
| Visitante | Descubrir cuentos y preparar compra | Navegar catalogo, buscar, ver detalle, agregar al carrito, iniciar login/registro | Friccion al pasar a checkout por autenticacion obligatoria, confusion de estado en carrito mini/fijo |
| Cliente autenticado | Completar compra y gestionar postcompra | Checkout por pasos, elegir entrega, pagar (MP/voucher), ver pedidos, descargar PDF | Errores de validacion, estados de pedido inconsistentes, fallos de callback de pago |
| Administrador | Operar catalogo, pagos y usuarios | CRUD cuentos, validar/rechazar pagos, cambiar estados logistica, gestionar usuarios y maestros | Cambios de estado no homologados, errores de endpoint/permiso, baja trazabilidad |
| Sistemas externos | Procesar pagos y soporte logistica | Mercado Pago, reglas de cobertura courier/shalom via maestros y ubigeo | Callbacks incompletos, dependencia de configuracion de flags, latencia/caida API |

## 3) Mapa integral de flujos E2E

Columnas estandar: `ID flujo`, `actor`, `precondicion`, `pasos UI`, `rutas`, `APIs involucradas`, `resultado esperado`, `puntos de fallo`, `criticidad`.

| ID flujo | Actor | Precondicion | Pasos UI | Rutas | APIs involucradas | Resultado esperado | Puntos de fallo | Criticidad |
|---|---|---|---|---|---|---|---|---|
| F01-Descubrimiento | Visitante | App cargada | Entrar a home, navegar catalogo, buscar por texto, abrir detalle | `/home`, `/cuentos`, `/cuento/:id` | `GET /cuentos`, `GET /cuentos/:id` | Usuario encuentra cuentos y accede a detalle | Carga lenta artificial en catalogo, filtros cliente limitados | P1 |
| F02-Carrito | Visitante/Cliente | Cuento visible | Agregar item, abrir mini-cart, ajustar cantidad, ir a carrito | overlay mini-cart, `/carrito` | LocalStorage + estado local (sin API directa) | Totales y cantidades consistentes en mini-cart y carrito | Desfase visual entre mini-cart y pagina carrito | P1 |
| F03-Auth en contexto | Visitante | Intento de checkout sin sesion | Click checkout desde navbar/mini-cart, redireccion a login con `returnTo`, login y retorno | `/checkout` -> `/login?returnTo=/checkout` | `POST /auth/login` | Retorno al checkout sin perder contexto | Parametro returnTo no respetado por error de ruta/navegacion | P0 |
| F04-Registro | Visitante | Email no registrado | Completar form, validaciones, crear cuenta, redirigir a login | `/register`, `/login` | `POST /auth/register`, `GET /maestros/grupo/TIPO_DOCUMENTO` | Registro exitoso y continuidad al login | Mensajes 409/429/validaciones no consistentes | P1 |
| F05-Checkout paso a paso | Cliente autenticado | Carrito con items | Completar datos personales, direccion, entrega, confirmacion | `/checkout` | `GET /users/perfil`, `GET /direcciones/usuario/:uid`, `GET /ubigeo/*`, `GET /maestros/grupo/TIPO_DOCUMENTO`, `GET /maestros/grupo/TIPO_ENTREGA`, `GET /maestros/grupo/COBERTURA_COURIER`, `POST /orders` | Pedido creado con datos completos y navegacion a pago | Dependencias ubigeo, validacion dinamica de documento, reglas de entrega | P0 |
| F06-Pago Mercado Pago | Cliente autenticado/invitado | Pedido creado | Abrir pago, crear preferencia MP, redireccion externa, callback, confirmar pago | `/pago/:id` | `GET /orders/:id`, `GET /orders/:id/status`, `POST /orders/mercadopago/create-preference`, `POST /orders/:id/confirmar-pago-mercadopago` | Estado de pedido actualizado y mensaje correcto | Callback incompleto, estado no homologado, error de confirmacion | P0 |
| F07-Pago por voucher | Cliente autenticado/invitado | Pedido pendiente de pago | Subir voucher (imagen/pdf), confirmar envio | `/pago/:id`, `/voucher/:id` | `POST /orders/:id/voucherF`, `GET /orders/:id/status` | Pedido pasa a `PAGO_ENVIADO` | Validacion de tamano/tipo de archivo, errores de red | P0 |
| F08-Postcompra cliente | Cliente autenticado | Pedido existente | Ver lista, filtrar, abrir detalle, descargar PDF, reemplazar voucher si aplica | `/pedidos`, `/pedidos/:id` | `GET /orders`, `GET /orders/:id`, `GET /orders/:id/pdf`, `POST /orders/:id/voucherF`, `GET /pedidos/:id/voucher-url` | Cliente puede monitorear y gestionar su pedido | Endpoint mixto `/orders` y `/pedidos`, estado inconsistente | P1 |
| F09-Backoffice pedidos | Administrador | Sesion ADMIN | Listar pedidos, validar/rechazar pago, cambiar estado logistica, descargar boleta/voucher | `/admin/pedidos` | `GET /orders`, `PATCH /orders/:id/status`, `GET /orders/:id/pdf`, `GET /pedidos/:id/voucher-url`, `GET /maestros/grupo/ESTADO_PEDIDO` | Operacion de pedido controlada y trazable | Estado no alineado entre clientes/admin, motivo rechazo no estandar | P0 |
| F10-Backoffice catalogo/usuarios/maestros | Administrador | Sesion ADMIN | CRUD cuentos, habilitar usuarios, promover admin, gestionar maestros/config | `/admin/cuentos*`, `/admin/usuarios`, `/admin/maestros`, `/admin/config*` | `GET/POST/PUT /cuentos`, `PUT /cuentos/:id/estado`, `GET /users`, `PUT /users/:id/estado`, `POST /users/:id/make-admin`, `GET/POST/PUT/DELETE /maestros`, `GET /maestros/:id/audit` | Gobierno de catalogo y parametros consistente | Falta normalizacion de algunos contratos y validaciones cruzadas | P1 |

## 4) Matriz de pruebas por nivel

### 4.1 Estrategia por nivel

| Nivel | Objetivo | Cobertura minima | Salida esperada |
|---|---|---|---|
| Smoke | Validar que el core de negocio esta operativo | Login, catalogo, checkout, pago, admin pedidos | Entorno apto para pruebas funcionales |
| Funcional | Validar reglas de UI/formularios/estado | Formularios, navegacion, errores, estados de pedido | Comportamiento esperado por HU |
| Integracion FE-API | Validar contratos y manejo de errores API | Servicios Angular y respuestas reales/mocks BE | Sin bloqueos criticos por contrato |
| Regresion | Evitar reintroduccion de fallas en flujos P0/P1 | Compra, pago, postcompra, admin pedidos | Cero regresiones P0, maximo residual P1 aceptado |
| A11y | Detectar barreras basicas de uso | Teclado, foco, errores accesibles, contrastes base | Flujo comprable sin mouse en paths criticos |
| Performance | Reducir latencia percibida y cuellos visibles | LCP catalogo, carga detalle, transiciones checkout | Tiempo de respuesta dentro de umbrales definidos |
| Seguridad funcional | Validar autorizacion y aislamiento | Guards, acceso admin, acceso pedidos por usuario | Sin acceso indebido por UI + validacion API |

### 4.2 Catalogo de datasets de prueba

| Dataset | Descripcion | Uso principal |
|---|---|---|
| DS-01 | Visitante sin sesion y carrito vacio | Smoke de descubrimiento y auth en contexto |
| DS-02 | Visitante con carrito persistido en localStorage | Mini-cart, recordatorio de carrito abandonado |
| DS-03 | Cliente USER sin direcciones guardadas | Checkout completo creando direccion nueva |
| DS-04 | Cliente USER con 2 direcciones (CASA/TRABAJO) | Seleccion y reuso de direccion en checkout |
| DS-05 | Pedido `PAGO_PENDIENTE` con total > 0 | Pago MP y voucher |
| DS-06 | Pedido `PAGO_ENVIADO` | Reemplazo voucher y administracion de verificacion |
| DS-07 | Cuenta ADMIN operativa | Backoffice pedidos/cuentos/usuarios/maestros |
| DS-08 | Catalogo con categorias y edades desde maestros | Filtros catalogo y consistencia de datos |
| DS-09 | Maestro de cobertura courier por ubigeo (incluye no cobertura) | Fallback courier -> Shalom |
| DS-10 | Respuestas API de error 401/403/404/409/429/500 | Resiliencia UI y mensajes al usuario |

### 4.3 Casos criticos operables

| ID caso | Nivel | Prioridad | Tipo | Dataset | Evidencia esperada | Criterio de salida |
|---|---|---|---|---|---|---|
| T01-Checkout-auth-returnTo | Smoke/Funcional | P0 | Manual + Auto E2E | DS-01 | Redireccion a `/login?returnTo=/checkout` y retorno correcto tras login | Pass |
| T02-Login-errores | Funcional | P1 | Manual + Unit | DS-10 | Mensajes correctos para 401 y 429 en login | Pass |
| T03-Registro-errores | Funcional | P1 | Manual + Unit | DS-10 | Mensaje correcto para 409 y bloqueo por campos invalidos | Pass |
| T04-Checkout-stepper-validacion | Funcional | P0 | Manual + Auto E2E | DS-03 | No avanza de paso sin campos validos; foco en primer invalido | Pass |
| T05-Documento-dinamico | Funcional/Integracion | P1 | Manual + Unit | DS-03 | Cambio de reglas de documento por tipo (longitud/patron/mensaje) | Pass |
| T06-Ubigeo-cascada | Integracion FE-API | P0 | Manual + Auto API-contract | DS-09 | Carga de provincias/distritos por seleccion, sin estado roto | Pass |
| T07-Cobertura-fallback-entrega | Funcional/Integracion | P0 | Manual + Auto E2E | DS-09 | Si no hay cobertura courier, sugerencia Shalom y motivo en payload | Pass |
| T08-Direcciones-reuso-checkout | Funcional | P0 | Manual + Auto E2E | DS-04 | Seleccion de direccion guardada y no perdida de datos | Pass |
| T09-Pago-flags-metodos | Integracion FE-API | P1 | Manual | DS-05 | Render correcto segun `METODOS_PAGO` habilitados/deshabilitados | Pass |
| T10-MP-callback-approved | Integracion FE-API | P0 | Manual + Auto E2E | DS-05 | Callback approved/pending/failure refleja estado y mensaje correcto | Pass |
| T11-Voucher-upload-reglas | Funcional | P0 | Manual + Unit | DS-06 | Rechaza tipo/tamano invalido, acepta PNG/JPG/PDF <= 5MB | Pass |
| T12-Postcompra-listado-detalle | Smoke/Funcional | P1 | Manual + Auto E2E | DS-05 | Lista y detalle de pedidos disponibles para usuario propietario | Pass |
| T13-Descarga-pdf-voucher-url | Integracion FE-API | P1 | Manual | DS-06 | Descarga PDF y apertura de voucher sin error de endpoint | Pass |
| T14-Admin-validar-rechazar-pago | Funcional/Integracion | P0 | Manual + Auto E2E | DS-07 + DS-06 | Cambio a `PAGO_VERIFICADO` o `PAGO_RECHAZADO` con motivo cuando aplica | Pass |
| T15-Admin-cambio-estado-logistico | Funcional | P1 | Manual | DS-07 | Transicion de estado en pedidos persistida y visible | Pass |
| T16-Admin-cuentos-crud | Regresion | P1 | Manual + Unit | DS-07 + DS-08 | Crear/editar/deshabilitar cuento sin inconsistencias de maestros | Pass |
| T17-Admin-usuarios-estado-rol | Seguridad funcional | P1 | Manual | DS-07 | Habilitar/deshabilitar y promover a admin con feedback correcto | Pass |
| T18-Guard-admin-acceso | Seguridad funcional | P0 | Auto Unit + Manual | DS-01/DS-03/DS-07 | USER no accede a `/admin`, ADMIN si accede | Pass |
| T19-A11y-formularios-criticos | A11y | P1 | Manual + Tooling | DS-03 | Navegacion teclado, foco visible, errores legibles en login/register/checkout | Pass |
| T20-UX-movil-navegacion | Funcional/A11y | P1 | Manual | DS-02 | Coexistencia correcta de bottom-nav y mini-cart segun contexto de ruta | Pass |
| T21-Resiliencia-errores-api | Integracion/Regresion | P0 | Manual + Auto | DS-10 | Mensajes de error no bloquean flujo; usuario puede reintentar | Pass |
| T22-Estado-pedido-consistencia | Regresion | P0 | Manual + Auto contract | DS-05/DS-06 | Mismo estado logico visible en pago, lista, detalle y admin | Pass |

## 5) Mejoras UX priorizadas (agregar/quitar funcionalidad)

### 5.1 Quick wins (2-4 semanas)

| ID | Tipo | Cambio propuesto | Alcance tecnico (real) | Impacto UX esperado |
|---|---|---|---|---|
| QW-01 | Quitar | Eliminar demora artificial de catalogo | `CuentosComponent`: retirar `setTimeout(1000)` en carga | Menor tiempo percibido, mejor conversion a detalle |
| QW-02 | Quitar | Eliminar rating/resenas aleatorias inyectadas en FE | `CuentosComponent`, `DetalleCuentoComponent`, `CuentoCardComponent` | Mayor confianza y consistencia de informacion |
| QW-03 | Corregir | Homologar estados de pedido en FE | `PagoComponent`, `OrderListComponent`, `OrderDetailComponent`, `AdminPedidosComponent` | Menos confusion cliente/admin, menos tickets soporte |
| QW-04 | Corregir | Resolver accion placeholder `pagarAhora` en detalle pedido | `OrderDetailComponent` | Evita promesas falsas y rutas muertas |
| QW-05 | Corregir | Evitar suscripciones anidadas en navbar | `NavbarComponent` (`items$` + `usuarioLogueado$`) | Menor riesgo de fugas, estado de usuario mas estable |
| QW-06 | Ajustar | Reglas de visibilidad contextual mini-cart y navegacion movil | `AppComponent`, `MiniCartComponent`, `BottomNavComponent` | Menos obstruccion en checkout/pago y mejor uso movil |
| QW-07 | Corregir | Unificar endpoint de voucher/orden en FE para evitar ruta especial | `PedidoService.getVoucherUrl` | Menor deuda tecnica y menos fallos por contrato |
| QW-08 | Corregir | Alinear clave de persistencia de carrito (`cart` vs `carrito`) | `CartService` | Menor riesgo de inconsistencias en restauracion |

### 5.2 Roadmap (5-10 semanas)

| ID | Tipo | Cambio propuesto | Dependencia | Impacto UX esperado |
|---|---|---|---|---|
| RM-01 | Agregar | Busqueda/filtros server-side para catalogo | API backend de catalogo | Escalabilidad de catalogo y filtros confiables |
| RM-02 | Corregir | Contrato unico de estados de pedido FE/API | Alineacion backend + frontend | Trazabilidad de pedido end-to-end |
| RM-03 | Agregar | Observabilidad de eventos UX (analytics) | Instrumentacion FE + backend | Medicion real de abandono/embudos |
| RM-04 | Agregar | Reintentos guiados y mensajes contextuales en pago/postcompra | Manejo de errores API estandar | Menos abandono por fallos transitorios |
| RM-05 | Agregar | Suite E2E Playwright para casos P0/P1 | Pipeline CI | Prevencion de regresiones de negocio |
| RM-06 | Agregar | Pruebas de contrato API automatizadas (states/endpoints clave) | Entorno de contrato compartido | Menor ruptura FE-API por cambios de backend |

## 6) Impactos en interfaces publicas (propuestos)

### 6.1 Estandarizar enum de estados de pedido

Estado objetivo compartido FE/API:

- `PAGO_PENDIENTE`
- `PAGO_ENVIADO`
- `PAGO_VERIFICADO`
- `ENVIADO`
- `ENTREGADO`
- `PAGO_RECHAZADO`

Accion requerida:

- Definir un contrato unico (DTO/OpenAPI) y adaptar mapeos en todas las vistas.

### 6.2 Unificar endpoints de ordenes y voucher

Problema actual:

- FE consume mixto `orders` y `pedidos` para un mismo dominio.

Propuesta:

- Consolidar en un namespace unico (ejemplo: `/orders`) para estado, detalle, PDF y voucher URL.
- Mantener compatibilidad temporal con alias y deprecacion explicita.

### 6.3 Endpoint de busqueda/filtros server-side para cuentos

Propuesta minima:

- `GET /cuentos/search?q=&categoria=&edad=&precioMin=&precioMax=&sort=&page=&size=`

Beneficio:

- Reemplaza filtrado completo en cliente y reduce costos de carga/render.

### 6.4 Contrato de eventos UX (analytics)

Eventos minimos propuestos:

- `checkout_started`
- `checkout_step_completed` (step 1/2/3)
- `checkout_failed_validation`
- `payment_method_selected`
- `payment_redirected_mp`
- `payment_callback_received` (approved/pending/failure)
- `voucher_upload_started`
- `voucher_upload_success`
- `voucher_upload_failed`

Campos base sugeridos:

- `user_type` (guest/user/admin)
- `order_id` (si aplica)
- `step`
- `error_code` (si aplica)
- `timestamp`

## 7) Plan de ejecucion de pruebas

### 7.1 Secuencia recomendada

1. Smoke de entorno y autenticacion.
2. Bloque compra completo (`catalogo -> carrito -> checkout -> pago`).
3. Bloque postcompra y backoffice de pedidos.
4. Regresion de flujos P0/P1.
5. A11y + performance + seguridad funcional.

### 7.2 Criterio de salida global

- P0: 100% pass antes de liberar.
- P1: >= 95% pass, con incidencias restantes documentadas y workaround.
- P2: pueden diferirse con aprobacion de negocio.
- Sin defectos abiertos de severidad alta en compra/pago.

## 8) Supuestos y defaults

- Entregable unico: este documento maestro.
- Alcance: Frontend + APIs, sin redisenar backend completo en esta fase.
- Priorizacion por defecto:
  - `Quick wins`: 2-4 semanas
  - `Roadmap`: 5-10 semanas
- Automatizacion objetivo:
  - Mantener unitarias actuales en Karma/Jasmine.
  - Agregar Playwright para E2E de casos P0/P1.

## 9) Riesgos y mitigaciones

| Riesgo | Impacto | Mitigacion |
|---|---|---|
| Contrato FE/API cambia sin versionado | Alto | Pruebas de contrato + versionado de endpoints |
| Estados de pedido divergentes por modulo | Alto | Enum unico compartido y pruebas T22 |
| Dependencia de flags/metodos de pago mal configurada | Medio/Alto | Caso T09 en smoke de release |
| Caidas de APIs de maestros/ubigeo | Alto | Manejo de fallback + pruebas de resiliencia T21 |
| Regresiones por cambios rapidos en UX | Medio | Suite E2E P0/P1 en CI |

