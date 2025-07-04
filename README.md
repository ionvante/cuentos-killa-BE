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
