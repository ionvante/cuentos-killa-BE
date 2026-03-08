# Contrato único de respuesta de usuario (FE checkout/perfil/login)

Este documento define la **fuente única** del contrato de usuario para Frontend en los flujos de:

- login (`POST /api/v1/auth/login`)
- perfil (`GET /api/v1/users/perfil`)
- checkout (datos de usuario autenticado reutilizando el mismo shape)

## DTO oficial

El backend expone `UserResponseDTO` con este naming exacto (sin variantes):

```json
{
  "id": 1,
  "email": "cliente@demo.com",
  "nombre": "Ana",
  "apellido": "Pérez",
  "telefono": "999888777",
  "documentoTipo": "DNI",
  "documentoNumero": "12345678",
  "documento": "12345678",
  "role": "USER"
}
```

## Regla de compatibilidad FE

- FE debe consumir **exactamente** los campos anteriores.
- Evitar variantes de naming como: `firstName`, `lastName`, `docType`, `docNumber`, `phoneNumber`, etc.
- Cualquier cambio de naming debe actualizarse primero en `UserResponseDTO` y en este documento.

## Referencias de implementación

- `src/main/java/com/forjix/cuentoskilla/model/DTOs/UserResponseDTO.java`
- `src/main/java/com/forjix/cuentoskilla/model/DTOs/LoginResponse.java`
- `src/main/java/com/forjix/cuentoskilla/controller/AuthController.java`
- `src/main/java/com/forjix/cuentoskilla/controller/UserController.java`
