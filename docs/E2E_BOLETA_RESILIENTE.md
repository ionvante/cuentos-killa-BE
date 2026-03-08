# E2E - Flujo de Boleta Resiliente

Fecha: 2026-03-08

## Objetivo
Validar end-to-end el comportamiento resiliente al cambiar un pedido a `PAGO_VERIFICADO`:
- El pedido no debe revertirse si falla la generacion de boleta PDF.
- La boleta debe exponer estado funcional (`GENERADA` o `ERROR`).
- Debe existir reintento manual para admin.
- La descarga debe devolver `BOLETA_NOT_READY` cuando aplique.

## Cobertura automatizada implementada
Se agregaron estas pruebas:

1. [BoletaControllerE2ETest.java](H:\Proyectos\Cuentos de Killa\cuentos-killa-backend\src\test\java\com\forjix\cuentoskilla\controller\BoletaControllerE2ETest.java)
- `descargarBoletaCuandoNoEstaListaRetornaConflict`
- `reintentarBoletaComoAdminGeneradaRetornaOk`
- `reintentarBoletaComoAdminConErrorRetornaAccepted`

2. [OrderServiceBoletaResilienceTest.java](H:\Proyectos\Cuentos de Killa\cuentos-killa-backend\src\test\java\com\forjix\cuentoskilla\service\OrderServiceBoletaResilienceTest.java)
- `updateOrderStatusNoReviertePagoVerificadoSiBoletaLanzaExcepcion`
- `updateOrderStatusMantienePagoVerificadoSiBoletaQuedaEnError`

Nota tecnica:
- Se usan stubs concretos para servicios/clases no-interface en lugar de mocks inline, debido a limitacion de Byte Buddy con Java 25 en el entorno actual.

## Resultado de ejecucion
Comando ejecutado:

```powershell
.\mvnw.cmd -q "-Dtest=BoletaControllerE2ETest,OrderServiceBoletaResilienceTest" test
```

Resultado:
- Exito (exit code `0`).
- Los logs muestran advertencias esperadas en los escenarios de error de boleta, sin romper el flujo de `PAGO_VERIFICADO`.

## Flujos E2E validados
### Flujo 1: Validar pago con boleta no lista
Entrada:
- Pedido en transicion a `PAGO_VERIFICADO`.
- Boleta aun no disponible o en error.

Validacion:
- `GET /api/v1/orders/{id}/boleta` responde `409`.
- Codigo funcional: `BOLETA_NOT_READY`.

### Flujo 2: Reintento manual exitoso (admin)
Entrada:
- Pedido con boleta en `ERROR` o `PENDIENTE`.
- Reintento via endpoint admin.

Validacion:
- `POST /api/v1/orders/{id}/boleta/retry` responde `200`.
- `data.estadoGeneracion = GENERADA`.

### Flujo 3: Reintento manual con falla persistente (admin)
Entrada:
- Reintento ejecutado pero persiste error de generacion.

Validacion:
- `POST /api/v1/orders/{id}/boleta/retry` responde `202`.
- `data.estadoGeneracion = ERROR`.
- `data.ultimoError` poblado.

### Flujo 4: Resiliencia al actualizar estado del pedido
Entrada:
- `PATCH /api/v1/orders/{id}/status` a `PAGO_VERIFICADO`.
- Falla interna al generar boleta.

Validacion:
- El pedido mantiene `PAGO_VERIFICADO`.
- Se registra warning en logs.
- Se ejecuta notificacion de cambio de estado.

## Guia de prueba manual (API real)
Credenciales de prueba disponibles en:
- `C:/Users/Churrucutito/Documents/credenciales_test.txt`

### Paso A: Login admin
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"<admin_email>","password":"<admin_password>"}'
```

Guardar `data.token` en `ADMIN_TOKEN`.

### Paso B: Validar pago
```bash
curl -X PATCH http://localhost:8080/api/v1/orders/{ORDER_ID}/status \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"estado":"PAGO_VERIFICADO"}'
```

Esperado:
- `200 OK` con mensaje de estado actualizado.
- Si la boleta falla internamente, el pedido no se revierte.

### Paso C: Descargar boleta
```bash
curl -i http://localhost:8080/api/v1/orders/{ORDER_ID}/boleta \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Esperado:
- Si esta lista: `200` + PDF.
- Si no esta lista: `409` + `BOLETA_NOT_READY`.

### Paso D: Reintentar boleta (admin)
```bash
curl -X POST http://localhost:8080/api/v1/orders/{ORDER_ID}/boleta/retry \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Esperado:
- `200` si genera PDF.
- `202` si permanece en error, con `ultimoError`.
