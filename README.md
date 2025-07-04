# Cuentos de Killa - Backend
Proyecto base en Spring Boot 3.3 con conexión a PostgreSQL.

## Configuración

Propiedades relevantes en `application.yml`:

```yaml
file:
  upload-dir: ./uploads/vouchers
upload:
  max-size: 5242880 # bytes (5 MB)
storage:
  provider: local
```

`upload.max-size` define el límite de tamaño de los archivos subidos.
`storage.provider` permitirá seleccionar el proveedor (solo `local` soportado por ahora).

## Sentry

Se incluye la dependencia **Sentry** para registrar errores en todos los endpoints.
Defina la variable de entorno `SENTRY_DSN` con el DSN de su proyecto para habilitar los reportes.
Para que el plugin de Maven pueda subir el código fuente a Sentry debe
establecer la variable `SENTRY_AUTH_TOKEN` con un token válido.
