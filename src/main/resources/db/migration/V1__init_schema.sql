-- ============================================================
-- V1__init_schema.sql
-- Migración inicial: crea todas las tablas del MVP de Cuentos Killa
-- ============================================================

-- 1. Tabla de usuarios
CREATE TABLE IF NOT EXISTS usuarios (
    id          BIGSERIAL PRIMARY KEY,
    uid         VARCHAR(255),
    email       VARCHAR(255),
    password    VARCHAR(255),
    nombre      VARCHAR(255),
    apellido    VARCHAR(255),
    telefono    VARCHAR(20),
    documento   VARCHAR(20),
    role        VARCHAR(20),          -- ADMIN | USER
    created_at  TIMESTAMP
);

-- 2. Tabla de direcciones
CREATE TABLE IF NOT EXISTS direcciones (
    id                  BIGSERIAL PRIMARY KEY,
    calle               VARCHAR(255),
    ciudad              VARCHAR(255),
    departamento        VARCHAR(255),
    provincia           VARCHAR(255),
    distrito            VARCHAR(255),
    referencia          VARCHAR(255),
    codigo_postal       VARCHAR(10),
    es_principal        BOOLEAN DEFAULT FALSE,
    es_facturacion      BOOLEAN DEFAULT FALSE,
    usuario_id          BIGINT REFERENCES usuarios(id),
    fecha_creacion      TIMESTAMP,
    fecha_actualizacion TIMESTAMP,
    creado_por          VARCHAR(255),
    actualizado_por     VARCHAR(255)
);

-- 3. Tabla de cuentos (productos)
CREATE TABLE IF NOT EXISTS cuento (
    id                BIGSERIAL PRIMARY KEY,
    titulo            VARCHAR(255),
    autor             VARCHAR(255),
    descripcion_corta TEXT,
    editorial         VARCHAR(255),
    tipo_edicion      VARCHAR(255),
    nro_paginas       INT DEFAULT 0,
    fecha_publicacion DATE,
    fecha_ingreso     DATE,
    edad_recomendada  VARCHAR(50),
    stock             INT DEFAULT 0,
    precio            DOUBLE PRECISION DEFAULT 0,
    imagen_url        TEXT,
    habilitado        BOOLEAN DEFAULT TRUE
);

-- 4. Tabla de órdenes / pedidos
CREATE TABLE IF NOT EXISTS orders (
    id              BIGSERIAL PRIMARY KEY,
    created_at      TIMESTAMP,
    estado          VARCHAR(30),      -- GENERADO, PAGO_PENDIENTE, PAGADO, etc.
    total           NUMERIC(12,2),
    motivo_rechazo  TEXT,
    user_id         BIGINT REFERENCES usuarios(id)
);

-- 5. Ítems de cada orden
CREATE TABLE IF NOT EXISTS order_item (
    id              BIGSERIAL PRIMARY KEY,
    cuento_id       BIGINT REFERENCES cuento(id),
    order_id        BIGINT REFERENCES orders(id),
    cantidad        INT DEFAULT 1,
    precio_unitario DOUBLE PRECISION DEFAULT 0,
    nombre          VARCHAR(255),
    imagen_url      TEXT,
    subtotal        NUMERIC(12,2)
);

-- 6. Vouchers (comprobantes de pago manual)
CREATE TABLE IF NOT EXISTS vouchers (
    id              BIGSERIAL PRIMARY KEY,
    fecha           DATE,
    hora            TIME,
    peso            VARCHAR(50),
    dispositivo     VARCHAR(100),
    ip              VARCHAR(50),
    nombre_archivo  VARCHAR(255),
    tipo_archivo    VARCHAR(100),
    file_path       TEXT,
    idpedido        BIGINT NOT NULL REFERENCES orders(id)
);

-- 7. Ítems del carrito (persistencia server-side)
CREATE TABLE IF NOT EXISTS cart_item (
    id          BIGSERIAL PRIMARY KEY,
    cuento_id   BIGINT REFERENCES cuento(id),
    user_id     BIGINT REFERENCES usuarios(id),
    cantidad    INT DEFAULT 1
);

-- ============================================================
-- Datos iniciales: usuario administrador de fábrica
-- La contraseña '123456' debe estar hasheada con BCrypt.
-- El hash siguiente corresponde a '$2a$10$...' de '123456'.
-- Ajústalo si tu PasswordEncoder produce un hash distinto.
-- ============================================================
INSERT INTO usuarios (email, password, nombre, apellido, role, created_at)
SELECT 'cdanpg@gmail.com',
       '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       'Admin', 'Killa', 'ADMIN', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM usuarios WHERE email = 'cdanpg@gmail.com'
);
