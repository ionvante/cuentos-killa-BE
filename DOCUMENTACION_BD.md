# Documentación Base de Datos - Cuentos Killa Backend

## 📋 Información General

- **Proyecto**: Cuentos Killa Backend
- **Base de Datos**: PostgreSQL
- **Script**: `script_base_datos_completo.sql`
- **Versión**: 1.0.0
- **Java Version**: 17
- **Spring Boot**: 3.4.0

## 📂 Estructura de Tablas

### 1. Tablas Maestras de Configuración

#### `tabla_catalogo_general`
Almacena catálogos de configuración del sistema.

```sql
id                  BIGSERIAL PRIMARY KEY
grupo_codigo        VARCHAR(50) - Agrupador de categorías
codigo_maestro      VARCHAR(50) - Código único del maestro
valor_mostrar       VARCHAR(150) - Valor a mostrar en UI
descripcion         VARCHAR(255) - Descripción del catálogo
estado              BOOLEAN - Indica si está activo
```

**Catálogos disponibles:**
- `TIPO_DOC`: DNI, CE, PASAPORTE
- `ESTADO_PEDIDO`: Estados del pedido (Pago pendiente, Enviado, Entregado, etc.)
- `MONEDA`: PEN (Soles), USD (Dólares)
- `CATEGORIA_CUENTO`: Aventura, Didáctico, Clásico
- `RANGO_EDAD`: 0-3, 4-6, 7-10 años

#### `config_category`
Categorías de configuración principal.

```sql
id1                 SERIAL PRIMARY KEY
code                VARCHAR(50) - Código único de categoría
name                VARCHAR(100) - Nombre descriptivo
created_at          TIMESTAMP - Fecha de creación
```

#### `config_item`
Ítems de configuración con datos JSON.

```sql
id1                 INT - Referencia a config_category
id2                 INT - Identificador secundario
label               VARCHAR(200) - Etiqueta del item
data                JSONB - Datos flexibles en JSON
sensitive           BOOLEAN - Indica si es sensible
PRIMARY KEY (id1, id2)
```

### 2. Tablas de Ubicación (UBIGEO)

#### `tabla_departamento`
Departamentos del Perú (nivel 1).

```sql
id_departamento     VARCHAR(2) PRIMARY KEY - Código único
nombre              VARCHAR(100) - Nombre del departamento
```

**Departamentos incluidos**: 25 departamentos del Perú

#### `tabla_provincia`
Provincias del Perú (nivel 2).

```sql
id_provincia        VARCHAR(4) PRIMARY KEY - Código único
nombre              VARCHAR(100) - Nombre de la provincia
id_departamento     VARCHAR(2) FOREIGN KEY - Referencia a departamento
```

#### `tabla_distrito`
Distritos del Perú (nivel 3).

```sql
id_distrito         VARCHAR(6) PRIMARY KEY - Código único
nombre              VARCHAR(100) - Nombre del distrito
id_provincia        VARCHAR(4) FOREIGN KEY - Referencia a provincia
```

### 3. Tablas de Usuarios y Direcciones

#### `usuarios`
Registro de usuarios del sistema.

```sql
id                  BIGSERIAL PRIMARY KEY
uid                 VARCHAR(255) - UID de Firebase (si aplica)
email               VARCHAR(255) - Correo electrónico único
password            VARCHAR(255) - Contraseña (hash BCrypt)
nombre              VARCHAR(255) - Nombre
apellido            VARCHAR(255) - Apellido
telefono            VARCHAR(20) - Teléfono de contacto
documento_tipo      VARCHAR(50) - Tipo de documento (DNI, CE, Pasaporte)
documento_numero    VARCHAR(20) - Número del documento
documento           VARCHAR(20) - Documento (alias)
role                VARCHAR(20) - Rol: ADMIN, USER, etc.
created_at          TIMESTAMP - Fecha de creación
```

**Índices:**
- `idx_usuarios_email` - Búsqueda rápida por email
- `idx_usuarios_uid` - Búsqueda rápida por UID de Firebase

#### `direcciones`
Direcciones registradas de los usuarios.

```sql
id                      BIGSERIAL PRIMARY KEY
calle                   VARCHAR(255) - Nombre de calle
ciudad                  VARCHAR(255) - Ciudad
departamento            VARCHAR(255) - Departamento
provincia               VARCHAR(255) - Provincia
distrito                VARCHAR(255) - Distrito
referencia              VARCHAR(255) - Punto de referencia
codigo_postal           VARCHAR(10) - Código postal
es_principal            BOOLEAN - Dirección principal
es_facturacion          BOOLEAN - Es dirección de facturación
usuario_id              BIGINT FOREIGN KEY - Referencia a usuario
fecha_creacion          TIMESTAMP - Fecha de creación
fecha_actualizacion     TIMESTAMP - Última actualización
creado_por              VARCHAR(255) - Usuario que creó
actualizado_por         VARCHAR(255) - Último usuario que actualizó
```

**Índice:**
- `idx_direcciones_usuario_id` - Búsqueda rápida por usuario

### 4. Tablas de Productos (Cuentos)

#### `cuento`
Catálogo de cuentos disponibles.

```sql
id                  BIGSERIAL PRIMARY KEY
titulo              VARCHAR(255) - Título del cuento
autor               VARCHAR(255) - Autor
descripcion_corta   TEXT - Descripción breve
editorial           VARCHAR(255) - Editorial
tipo_edicion        VARCHAR(255) - Tipo de edición
nro_paginas         INT - Número de páginas
fecha_publicacion   DATE - Fecha de publicación
fecha_ingreso       DATE - Fecha de ingreso al inventario
edad_recomendada    VARCHAR(50) - Rango de edad recomendado
stock               INT - Cantidad en inventario
precio              DOUBLE PRECISION - Precio unitario
imagen_url          TEXT - URL de imagen
habilitado          BOOLEAN - Disponible para venta
```

### 5. Tablas de Carrito

#### `cart_item`
Ítems persistidos en el carrito (server-side).

```sql
id                  BIGSERIAL PRIMARY KEY
cuento_id           BIGINT FOREIGN KEY - Referencia a cuento
user_id             BIGINT FOREIGN KEY - Referencia a usuario
cantidad            INT - Cantidad en carrito
```

**Índices:**
- `idx_cart_item_user_id` - Búsqueda por usuario
- `idx_cart_item_cuento_id` - Búsqueda por cuento

### 6. Tablas de Órdenes y Pagos

#### `orders`
Registro de pedidos.

```sql
id                  BIGSERIAL PRIMARY KEY
created_at          TIMESTAMP - Fecha de creación
estado              VARCHAR(50) - Estado del pedido
total               NUMERIC(12,2) - Monto total
motivo_rechazo      TEXT - Razón si fue rechazado
user_id             BIGINT FOREIGN KEY - Referencia a usuario
```

**Estado posibles:**
- PAGO_PENDIENTE
- PAGO_ENVIADO
- PAGO_VERIFICADO
- EMPAQUETADO
- ENVIADO
- ENTREGADO
- PAGO_RECHAZADO

**Índices:**
- `idx_orders_user_id` - Búsqueda por usuario
- `idx_orders_estado` - Búsqueda por estado

#### `order_item`
Detalles de ítems en cada pedido.

```sql
id                  BIGSERIAL PRIMARY KEY
cuento_id           BIGINT FOREIGN KEY - Referencia a cuento
order_id            BIGINT FOREIGN KEY - Referencia a orden
cantidad            INT - Cantidad pedida
precio_unitario     DOUBLE PRECISION - Precio al momento de compra
nombre              VARCHAR(255) - Nombre del cuento
imagen_url          TEXT - Imagen del cuento
subtotal            NUMERIC(12,2) - Subtotal (cantidad × precio)
```

**Índices:**
- `idx_order_item_order_id` - Búsqueda por orden
- `idx_order_item_cuento_id` - Búsqueda por cuento

#### `vouchers`
Comprobantes de pago manual.

```sql
id                  BIGSERIAL PRIMARY KEY
fecha               DATE - Fecha del comprobante
hora                TIME - Hora del comprobante
peso                VARCHAR(50) - Peso del archivo
dispositivo         VARCHAR(100) - Dispositivo desde el que se subió
ip                  VARCHAR(50) - IP del cliente
nombre_archivo      VARCHAR(255) - Nombre del archivo
tipo_archivo        VARCHAR(100) - MIME type del archivo
file_path           TEXT - Ruta donde se guardó
idpedido            BIGINT FOREIGN KEY - Referencia a orden
```

**Índice:**
- `idx_vouchers_order_id` - Búsqueda por orden

#### `payment_vouchers`
Vouchers de pago almacenados en Firebase Storage.

```sql
id                  BIGSERIAL PRIMARY KEY
order_id            BIGINT FOREIGN KEY - Referencia a orden
filename            VARCHAR(255) - Nombre del archivo
mime_type           VARCHAR(100) - Tipo MIME
size                BIGINT - Tamaño en bytes
firebase_path       TEXT - Ruta en Firebase Storage
upload_date         TIMESTAMP - Fecha de carga
```

**Índice:**
- `idx_payment_vouchers_order_id` - Búsqueda por orden

## 🔐 Seguridad

### Usuario Administrador por Defecto

```
Email:      cdanpg@gmail.com
Contraseña: 123456
Rol:        ADMIN
Hash:       $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
```

⚠️ **IMPORTANTE**: Cambiar la contraseña del administrador en producción.

## 📊 Índices para Optimización

Se han creado 17 índices para optimizar el rendimiento:

| Tabla | Índice | Columna(s) |
|-------|--------|-----------|
| usuarios | idx_usuarios_email | email |
| usuarios | idx_usuarios_uid | uid |
| direcciones | idx_direcciones_usuario_id | usuario_id |
| orders | idx_orders_user_id | user_id |
| orders | idx_orders_estado | estado |
| order_item | idx_order_item_order_id | order_id |
| order_item | idx_order_item_cuento_id | cuento_id |
| vouchers | idx_vouchers_order_id | idpedido |
| cart_item | idx_cart_item_user_id | user_id |
| cart_item | idx_cart_item_cuento_id | cuento_id |
| payment_vouchers | idx_payment_vouchers_order_id | order_id |
| tabla_provincia | idx_tabla_provincia_departamento | id_departamento |
| tabla_distrito | idx_tabla_distrito_provincia | id_provincia |
| config_item | idx_config_item_category | id1 |
| tabla_catalogo_general | idx_tabla_catalogo_grupo | grupo_codigo |
| tabla_catalogo_general | idx_tabla_catalogo_codigo | codigo_maestro |

## 🚀 Cómo Usar el Script

### 1. Crear la Base de Datos

```bash
psql -U postgres -h localhost
CREATE DATABASE cuentos_killa;
```

### 2. Ejecutar el Script

```bash
psql -U postgres -h localhost -d cuentos_killa -f script_base_datos_completo.sql
```

### 3. Verificar la Creación

```bash
psql -U postgres -h localhost -d cuentos_killa
\dt              -- Ver todas las tablas
\di              -- Ver todos los índices
SELECT * FROM usuarios;  -- Verificar usuario admin
```

## 🔗 Relaciones de Claves Foráneas

```
usuarios (1) ──── (N) direcciones
usuarios (1) ──── (N) orders
usuarios (1) ──── (N) cart_item
cuento (1) ──── (N) cart_item
cuento (1) ──── (N) order_item
orders (1) ──── (N) order_item
orders (1) ──── (N) vouchers
orders (1) ──── (N) payment_vouchers
tabla_departamento (1) ──── (N) tabla_provincia
tabla_provincia (1) ──── (N) tabla_distrito
config_category (1) ──── (N) config_item
```

## 📝 Notas Importantes

1. **Codificación**: El script está en UTF-8
2. **ON CONFLICT**: Se utiliza para evitar duplicados al insertar datos iniciales
3. **CASCADE**: Las direcciones, órdenes y carrito se eliminan cuando se elimina el usuario
4. **SET NULL**: Los ítems de orden quedan sin cuento si se elimina el cuento
5. **Timestamps**: Se utiliza `NOW()` para registrar fechas automáticamente

## 🔄 Flujo de Datos Típico

### Compra de Cuento

1. Usuario se registra en `usuarios`
2. Usuario agrega cuento a carrito (`cart_item`)
3. Usuario crea orden (`orders`) con estado `PAGO_PENDIENTE`
4. Ítems del carrito se trasladan a `order_item`
5. Usuario sube comprobante (`vouchers` o `payment_vouchers`)
6. Admin verifica y cambia estado a `PAGO_VERIFICADO`
7. Estado progresa: `EMPAQUETADO` → `ENVIADO` → `ENTREGADO`

## 📞 Soporte

Para ayuda con consultas SQL o modificaciones de estructura, consulte con el equipo de desarrollo.
