-- ============================================================
-- V1__init_schema.sql
-- Migración inicial: crea todas las tablas del MVP de Cuentos Killa
-- Basado en entidades JPA del proyecto
-- ============================================================

-- 1. Tabla de catálogos maestros (tabla_catalogo_general)
-- Usada para almacenar datos de configuración catalog/maestro
CREATE TABLE IF NOT EXISTS tabla_catalogo_general (
    id                  BIGSERIAL PRIMARY KEY,
    grupo_codigo        VARCHAR(50) NOT NULL,
    codigo_maestro      VARCHAR(50) NOT NULL UNIQUE,
    valor_mostrar       VARCHAR(150) NOT NULL,
    descripcion         VARCHAR(255),
    estado              BOOLEAN NOT NULL DEFAULT TRUE
);

-- 2. Tabla de departamentos (ubigeo nivel 1)
CREATE TABLE IF NOT EXISTS tabla_departamento (
    id_departamento     VARCHAR(2) PRIMARY KEY,
    nombre              VARCHAR(100) NOT NULL
);

-- 3. Tabla de provincias (ubigeo nivel 2)
CREATE TABLE IF NOT EXISTS tabla_provincia (
    id_provincia        VARCHAR(4) PRIMARY KEY,
    nombre              VARCHAR(100) NOT NULL,
    id_departamento     VARCHAR(2) NOT NULL REFERENCES tabla_departamento(id_departamento)
);

-- 4. Tabla de distritos (ubigeo nivel 3)
CREATE TABLE IF NOT EXISTS tabla_distrito (
    id_distrito         VARCHAR(6) PRIMARY KEY,
    nombre              VARCHAR(100) NOT NULL,
    id_provincia        VARCHAR(4) NOT NULL REFERENCES tabla_provincia(id_provincia)
);

-- 5. Tabla de usuarios
CREATE TABLE IF NOT EXISTS usuarios (
    id                      BIGSERIAL PRIMARY KEY,
    uid                     VARCHAR(255),
    email                   VARCHAR(255),
    password                VARCHAR(255),
    nombre                  VARCHAR(255),
    apellido                VARCHAR(255),
    telefono                VARCHAR(20),
    documento_tipo          VARCHAR(50),
    documento_numero        VARCHAR(20),
    documento               VARCHAR(20),
    role                    VARCHAR(20),
    created_at              TIMESTAMP
);

-- 6. Tabla de direcciones
CREATE TABLE IF NOT EXISTS direcciones (
    id                      BIGSERIAL PRIMARY KEY,
    calle                   VARCHAR(255),
    ciudad                  VARCHAR(255),
    departamento            VARCHAR(255),
    provincia               VARCHAR(255),
    distrito                VARCHAR(255),
    referencia              VARCHAR(255),
    codigo_postal           VARCHAR(10),
    es_principal            BOOLEAN DEFAULT FALSE,
    es_facturacion          BOOLEAN DEFAULT FALSE,
    usuario_id              BIGINT REFERENCES usuarios(id) ON DELETE CASCADE,
    fecha_creacion          TIMESTAMP,
    fecha_actualizacion     TIMESTAMP,
    creado_por              VARCHAR(255),
    actualizado_por         VARCHAR(255)
);

-- 7. Tabla de cuentos (productos)
CREATE TABLE IF NOT EXISTS cuento (
    id                      BIGSERIAL PRIMARY KEY,
    titulo                  VARCHAR(255),
    autor                   VARCHAR(255),
    descripcion_corta       TEXT,
    editorial               VARCHAR(255),
    tipo_edicion            VARCHAR(255),
    nro_paginas             INT DEFAULT 0,
    fecha_publicacion       DATE,
    fecha_ingreso           DATE,
    edad_recomendada        VARCHAR(50),
    stock                   INT DEFAULT 0,
    precio                  DOUBLE PRECISION DEFAULT 0,
    imagen_url              TEXT,
    habilitado              BOOLEAN DEFAULT TRUE
);

-- 8. Tabla de órdenes / pedidos
CREATE TABLE IF NOT EXISTS orders (
    id                      BIGSERIAL PRIMARY KEY,
    created_at              TIMESTAMP,
    estado                  VARCHAR(50),
    total                   NUMERIC(12,2),
    motivo_rechazo          TEXT,
    user_id                 BIGINT REFERENCES usuarios(id) ON DELETE CASCADE
);

-- 9. Ítems de cada orden
CREATE TABLE IF NOT EXISTS order_item (
    id                      BIGSERIAL PRIMARY KEY,
    cuento_id               BIGINT REFERENCES cuento(id) ON DELETE SET NULL,
    order_id                BIGINT REFERENCES orders(id) ON DELETE CASCADE,
    cantidad                INT DEFAULT 1,
    precio_unitario         DOUBLE PRECISION DEFAULT 0,
    nombre                  VARCHAR(255),
    imagen_url              TEXT,
    subtotal                NUMERIC(12,2)
);

-- 10. Vouchers de pago manual (comprobantes)
CREATE TABLE IF NOT EXISTS vouchers (
    id                      BIGSERIAL PRIMARY KEY,
    fecha                   DATE,
    hora                    TIME,
    peso                    VARCHAR(50),
    dispositivo             VARCHAR(100),
    ip                      VARCHAR(50),
    nombre_archivo          VARCHAR(255),
    tipo_archivo            VARCHAR(100),
    file_path               TEXT,
    idpedido                BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE
);

-- 11. Ítems del carrito (persistencia server-side)
CREATE TABLE IF NOT EXISTS cart_item (
    id                      BIGSERIAL PRIMARY KEY,
    cuento_id               BIGINT REFERENCES cuento(id) ON DELETE CASCADE,
    user_id                 BIGINT REFERENCES usuarios(id) ON DELETE CASCADE,
    cantidad                INT DEFAULT 1
);

-- 12. Categorías de configuración
CREATE TABLE IF NOT EXISTS config_category (
    id1                     SERIAL PRIMARY KEY,
    code                    VARCHAR(50) NOT NULL UNIQUE,
    name                    VARCHAR(100) NOT NULL,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 13. Ítems de configuración (clave compuesta)
CREATE TABLE IF NOT EXISTS config_item (
    id1                     INT NOT NULL REFERENCES config_category(id1) ON DELETE CASCADE,
    id2                     INT NOT NULL,
    label                   VARCHAR(200) NOT NULL,
    data                    JSONB NOT NULL DEFAULT '{}',
    sensitive               BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id1, id2)
);

-- 14. Vouchers de pago (Firebase Storage)
CREATE TABLE IF NOT EXISTS payment_vouchers (
    id                      BIGSERIAL PRIMARY KEY,
    order_id                BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    filename                VARCHAR(255),
    mime_type               VARCHAR(100),
    size                    BIGINT DEFAULT 0,
    firebase_path           TEXT,
    upload_date             TIMESTAMP
);

-- ============================================================
-- Índices para mejorar rendimiento
-- ============================================================

CREATE INDEX IF NOT EXISTS idx_usuarios_email ON usuarios(email);
CREATE INDEX IF NOT EXISTS idx_usuarios_uid ON usuarios(uid);
CREATE INDEX IF NOT EXISTS idx_direcciones_usuario_id ON direcciones(usuario_id);
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_estado ON orders(estado);
CREATE INDEX IF NOT EXISTS idx_order_item_order_id ON order_item(order_id);
CREATE INDEX IF NOT EXISTS idx_order_item_cuento_id ON order_item(cuento_id);
CREATE INDEX IF NOT EXISTS idx_vouchers_order_id ON vouchers(idpedido);
CREATE INDEX IF NOT EXISTS idx_cart_item_user_id ON cart_item(user_id);
CREATE INDEX IF NOT EXISTS idx_cart_item_cuento_id ON cart_item(cuento_id);
CREATE INDEX IF NOT EXISTS idx_payment_vouchers_order_id ON payment_vouchers(order_id);
CREATE INDEX IF NOT EXISTS idx_tabla_provincia_departamento ON tabla_provincia(id_departamento);
CREATE INDEX IF NOT EXISTS idx_tabla_distrito_provincia ON tabla_distrito(id_provincia);
CREATE INDEX IF NOT EXISTS idx_config_item_category ON config_item(id1);
CREATE INDEX IF NOT EXISTS idx_tabla_catalogo_grupo ON tabla_catalogo_general(grupo_codigo);
CREATE INDEX IF NOT EXISTS idx_tabla_catalogo_codigo ON tabla_catalogo_general(codigo_maestro);

-- ============================================================
-- Datos iniciales: usuario administrador de fábrica
-- Hash BCrypt para contraseña '123456'
-- ============================================================
INSERT INTO usuarios (email, password, nombre, apellido, role, created_at)
SELECT 'cdanpg@gmail.com',
       '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       'Admin', 'Killa', 'ADMIN', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM usuarios WHERE email = 'cdanpg@gmail.com'
);
