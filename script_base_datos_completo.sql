-- ============================================================
-- SCRIPT COMPLETO DE BASE DE DATOS - CUENTOS KILLA
-- Proyecto: cuentos-killa-backend
-- Base de datos: PostgreSQL
-- Versión: 1.0.0
-- ============================================================

-- ============================================================
-- 1. TABLAS MAESTRAS PARA CONFIGURACIÓN Y CATÁLOGOS
-- ============================================================

-- 1.1 Tabla de catálogos generales
CREATE TABLE IF NOT EXISTS tabla_catalogo_general (
    id                  BIGSERIAL PRIMARY KEY,
    grupo_codigo        VARCHAR(50) NOT NULL,
    codigo_maestro      VARCHAR(50) NOT NULL UNIQUE,
    valor_mostrar       VARCHAR(150) NOT NULL,
    descripcion         VARCHAR(255),
    estado              BOOLEAN NOT NULL DEFAULT TRUE
);

-- 1.2 Categorías de configuración
CREATE TABLE IF NOT EXISTS config_category (
    id1                 SERIAL PRIMARY KEY,
    code                VARCHAR(50) NOT NULL UNIQUE,
    name                VARCHAR(100) NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 1.3 Ítems de configuración (tabla de clave compuesta)
CREATE TABLE IF NOT EXISTS config_item (
    id1                 INT NOT NULL REFERENCES config_category(id1) ON DELETE CASCADE,
    id2                 INT NOT NULL,
    label               VARCHAR(200) NOT NULL,
    data                JSONB NOT NULL DEFAULT '{}',
    sensitive           BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id1, id2)
);

-- ============================================================
-- 2. TABLAS DE UBICACIÓN GEOGRÁFICA (UBIGEO)
-- ============================================================

-- 2.1 Tabla de departamentos (nivel 1)
CREATE TABLE IF NOT EXISTS tabla_departamento (
    id_departamento     VARCHAR(2) PRIMARY KEY,
    nombre              VARCHAR(100) NOT NULL
);

-- 2.2 Tabla de provincias (nivel 2)
CREATE TABLE IF NOT EXISTS tabla_provincia (
    id_provincia        VARCHAR(4) PRIMARY KEY,
    nombre              VARCHAR(100) NOT NULL,
    id_departamento     VARCHAR(2) NOT NULL REFERENCES tabla_departamento(id_departamento)
);

-- 2.3 Tabla de distritos (nivel 3)
CREATE TABLE IF NOT EXISTS tabla_distrito (
    id_distrito         VARCHAR(6) PRIMARY KEY,
    nombre              VARCHAR(100) NOT NULL,
    id_provincia        VARCHAR(4) NOT NULL REFERENCES tabla_provincia(id_provincia)
);

-- ============================================================
-- 3. TABLAS DE USUARIOS Y DIRECCIONES
-- ============================================================

-- 3.1 Tabla de usuarios
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

-- 3.2 Tabla de direcciones
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

-- ============================================================
-- 4. TABLAS DE PRODUCTOS (CUENTOS)
-- ============================================================

-- 4.1 Tabla de cuentos (productos)
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

-- ============================================================
-- 5. TABLAS DE CARRITO DE COMPRAS
-- ============================================================

-- 5.1 Ítems del carrito (persistencia server-side)
CREATE TABLE IF NOT EXISTS cart_item (
    id                      BIGSERIAL PRIMARY KEY,
    cuento_id               BIGINT REFERENCES cuento(id) ON DELETE CASCADE,
    user_id                 BIGINT REFERENCES usuarios(id) ON DELETE CASCADE,
    cantidad                INT DEFAULT 1
);

-- ============================================================
-- 6. TABLAS DE ÓRDENES Y PAGOS
-- ============================================================

-- 6.1 Tabla de órdenes / pedidos
CREATE TABLE IF NOT EXISTS orders (
    id                      BIGSERIAL PRIMARY KEY,
    created_at              TIMESTAMP,
    estado                  VARCHAR(50),
    total                   NUMERIC(12,2),
    motivo_rechazo          TEXT,
    user_id                 BIGINT REFERENCES usuarios(id) ON DELETE CASCADE
);

-- 6.2 Ítems de cada orden
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

-- 6.3 Vouchers de pago manual (comprobantes)
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

-- 6.4 Vouchers de pago (Firebase Storage)
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
-- 7. ÍNDICES PARA OPTIMIZAR RENDIMIENTO
-- ============================================================

-- Índices en tabla usuarios
CREATE INDEX IF NOT EXISTS idx_usuarios_email ON usuarios(email);
CREATE INDEX IF NOT EXISTS idx_usuarios_uid ON usuarios(uid);

-- Índices en tabla direcciones
CREATE INDEX IF NOT EXISTS idx_direcciones_usuario_id ON direcciones(usuario_id);

-- Índices en tabla orders
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_estado ON orders(estado);

-- Índices en tabla order_item
CREATE INDEX IF NOT EXISTS idx_order_item_order_id ON order_item(order_id);
CREATE INDEX IF NOT EXISTS idx_order_item_cuento_id ON order_item(cuento_id);

-- Índices en tabla vouchers
CREATE INDEX IF NOT EXISTS idx_vouchers_order_id ON vouchers(idpedido);

-- Índices en tabla cart_item
CREATE INDEX IF NOT EXISTS idx_cart_item_user_id ON cart_item(user_id);
CREATE INDEX IF NOT EXISTS idx_cart_item_cuento_id ON cart_item(cuento_id);

-- Índices en tabla payment_vouchers
CREATE INDEX IF NOT EXISTS idx_payment_vouchers_order_id ON payment_vouchers(order_id);

-- Índices en tablas de ubicación
CREATE INDEX IF NOT EXISTS idx_tabla_provincia_departamento ON tabla_provincia(id_departamento);
CREATE INDEX IF NOT EXISTS idx_tabla_distrito_provincia ON tabla_distrito(id_provincia);

-- Índices en tabla de configuración
CREATE INDEX IF NOT EXISTS idx_config_item_category ON config_item(id1);

-- Índices en tabla de catálogos
CREATE INDEX IF NOT EXISTS idx_tabla_catalogo_grupo ON tabla_catalogo_general(grupo_codigo);
CREATE INDEX IF NOT EXISTS idx_tabla_catalogo_codigo ON tabla_catalogo_general(codigo_maestro);

-- ============================================================
-- 8. DATOS INICIALES: DEPARTAMENTOS (UBIGEO)
-- ============================================================

INSERT INTO tabla_departamento (id_departamento, nombre) VALUES 
('01', 'Junin'),
('02', 'Apurimac'),
('03', 'Ancash'),
('04', 'Arequipa'),
('05', 'Cusco'),
('06', 'Ayacucho'),
('07', 'Puno'),
('08', 'Huancavelica'),
('09', 'La Libertad'),
('10', 'San Martin'),
('11', 'Tumbes'),
('12', 'Lima'),
('13', 'Tacna'),
('14', 'Ica'),
('15', 'Loreto'),
('16', 'Huanuco'),
('17', 'Piura'),
('18', 'Cajamarca'),
('19', 'Amazonas'),
('20', 'Callao'),
('21', 'Ucayali'),
('22', 'Lambayeque'),
('23', 'Moquegua'),
('24', 'Pasco'),
('25', 'Madre De Dios')
ON CONFLICT (id_departamento) DO NOTHING;

-- ============================================================
-- 9. DATOS INICIALES: PROVINCIAS (UBIGEO)
-- ============================================================

INSERT INTO tabla_provincia (id_provincia, nombre, id_departamento) VALUES 
('0101', 'Chupaca', '01'),
('0102', 'Concepcion', '01'),
('0103', 'Tarma', '01'),
('0104', 'Jauja', '01'),
('0105', 'Huancayo', '01'),
('0106', 'Junin', '01'),
('0107', 'Yauli', '01'),
('0108', 'Chanchamayo', '01'),
('0109', 'Satipo', '01'),
('0201', 'Abancay', '02'),
('0202', 'Anco-huallo', '02'),
('0203', 'Andahuaylas', '02'),
('0204', 'Antabamba', '02'),
('0205', 'Aymaraes', '02'),
('0206', 'Cotabamba', '02'),
('0207', 'Grau', '02'),
('0301', 'Bolognesi', '03'),
('0302', 'Ocros', '03'),
('0303', 'Corongo', '03'),
('0304', 'Sihuas', '03'),
('0305', 'Asuncion', '03'),
('0306', 'Carhuaz', '03'),
('0307', 'Antonio Raymondi', '03'),
('0308', 'Aija', '03'),
('0309', 'Huari', '03'),
('0310', 'Pallasca', '03'),
('0311', 'Casma', '03'),
('0312', 'Santa', '03'),
('0313', 'Huaylas', '03'),
('0314', 'Mariscal Luzuriaga', '03'),
('0315', 'Yungay', '03'),
('0316', 'Recuay', '03'),
('0317', 'Huaraz', '03'),
('0318', 'Huarmey', '03'),
('0319', 'Pomabamba', '03'),
('0320', 'Carlos F. Fitzcarrald', '03'),
('0401', 'Caraveli', '04'),
('0402', 'Caylloma', '04'),
('0403', 'La Union', '04'),
('0404', 'Arequipa', '04'),
('0405', 'Castilla', '04'),
('0406', 'Condesuyos', '04'),
('0407', 'Camana', '04'),
('0408', 'Islay', '04'),
('0501', 'Paruro', '05'),
('0502', 'Acomayo', '05'),
('0503', 'Espinar', '05'),
('0504', 'Anta', '05'),
('0505', 'Quispicanchi', '05'),
('0506', 'Paucartambo', '05'),
('0507', 'Calca', '05'),
('0508', 'Chumbivilcas', '05'),
('0509', 'Cusco', '05'),
('0510', 'Canchis', '05'),
('0511', 'Canas', '05'),
('0512', 'Urubamba', '05'),
('0513', 'La Convencion', '05'),
('0601', 'Vilcas Huaman', '06'),
('0602', 'Huamanga', '06'),
('0603', 'Victor Fajardo', '06'),
('0604', 'La Mar', '06'),
('0605', 'Lucanas', '06'),
('0606', 'Sucre', '06'),
('0607', 'Cangallo', '06'),
('0608', 'Huanca Sancos', '06'),
('0609', 'Parinacochas', '06'),
('0610', 'Paucar Del Sara', '06'),
('0611', 'Huanta', '06'),
('0701', 'Azangaro', '07'),
('0702', 'Puno', '07'),
('0703', 'Carabaya', '07'),
('0704', 'Sandia', '07'),
('0705', 'San Antonio De Putina', '07'),
('0706', 'Yunguyo', '07'),
('0707', 'Melgar', '07'),
('0708', 'San Roman', '07'),
('0709', 'Lampa', '07'),
('0710', 'El Collao', '07'),
('0711', 'Huancane', '07'),
('0712', 'Moho', '07'),
('0713', 'Chucuito', '07'),
('0801', 'Acobamba', '08'),
('0802', 'Huancavelica', '08'),
('0803', 'Tayacaja', '08'),
('0804', 'Angaraes', '08'),
('0805', 'Churcampa', '08'),
('0806', 'Castrovirreyna', '08'),
('0807', 'Huaytara', '08'),
('0901', 'Otuzco', '09'),
('0902', 'Santiago De Chuco', '09'),
('0903', 'Ascope', '09'),
('0904', 'Bolivar', '09'),
('0905', 'Pataz', '09'),
('0906', 'Julcan', '09'),
('0907', 'Gran Chimu', '09'),
('0908', 'Viru', '09'),
('0909', 'Chepen', '09'),
('0910', 'Sanchez Carrion', '09'),
('0911', 'Trujillo', '09'),
('0912', 'Pacasmayo', '09'),
('1001', 'El Dorado', '10'),
('1002', 'San Martin', '10'),
('1003', 'Lamas', '10'),
('1004', 'Bellavista', '10'),
('1005', 'Huallaga', '10'),
('1006', 'Rioja', '10'),
('1007', 'Picota', '10'),
('1008', 'Moyobamba', '10'),
('1009', 'Mariscal Caceres', '10'),
('1010', 'Tocache', '10'),
('1101', 'Zarumilla', '11'),
('1102', 'Contralmirante Villar', '11'),
('1103', 'Tumbes', '11'),
('1201', 'Yauyos', '12'),
('1202', 'Huaura', '12'),
('1203', 'Lima', '12'),
('1204', 'Oyon', '12'),
('1205', 'Huarochiri', '12'),
('1206', 'Cañete', '12'),
('1207', 'Huaral', '12'),
('1208', 'Barranca', '12'),
('1209', 'Cajatambo', '12'),
('1210', 'Canta', '12'),
('1301', 'Tacna', '13'),
('1302', 'Candarave', '13'),
('1303', 'Tarata', '13'),
('1304', 'Jorge Basadre', '13'),
('1401', 'Chincha', '14'),
('1402', 'Nazca', '14'),
('1403', 'Pisco', '14'),
('1404', 'Ica', '14'),
('1405', 'Palpa', '14'),
('1501', 'Maynas', '15'),
('1502', 'Requena', '15'),
('1503', 'Alto Amazonas', '15'),
('1504', 'Ucayali', '15'),
('1505', 'Loreto', '15'),
('1506', 'Mcal. Ramon Castilla', '15'),
('1601', 'Huanuco', '16'),
('1602', 'Ambo', '16'),
('1603', 'Yarowilca', '16'),
('1604', 'Huamalies', '16'),
('1605', 'Lauricocha', '16'),
('1606', 'Huacaybamba', '16'),
('1607', 'Pachitea', '16'),
('1608', 'Marañon', '16'),
('1609', 'Dos De Mayo', '16'),
('1610', 'Puerto Inca', '16'),
('1611', 'Leoncio Prado', '16'),
('1701', 'Paita', '17'),
('1702', 'Ayabaca', '17'),
('1703', 'Sullana', '17'),
('1704', 'Sechura', '17'),
('1705', 'Morropon', '17'),
('1706', 'Huancabamba', '17'),
('1707', 'Piura', '17'),
('1708', 'Talara', '17'),
('1801', 'Santa Cruz', '18'),
('1802', 'Chota', '18'),
('1803', 'Cajamarca', '18'),
('1804', 'Hualgayoc', '18'),
('1805', 'Jaen', '18'),
('1806', 'San Miguel', '18'),
('1807', 'Cajabamba', '18'),
('1808', 'Celendin', '18'),
('1809', 'San Marcos', '18'),
('1810', 'Contumaza', '18'),
('1811', 'San Ignacio', '18'),
('1812', 'Cutervo', '18'),
('1813', 'San Pablo', '18'),
('1901', 'Bagua', '19'),
('1902', 'Chachapoyas', '19'),
('1903', 'Utcubamba', '19'),
('1904', 'Luya', '19'),
('1905', 'Rodriguez De Mendoza', '19'),
('1906', 'Bongara', '19'),
('1907', 'Condorcanqui', '19'),
('2001', 'Callao (prov.const.)', '20'),
('2101', 'Coronel Portillo', '21'),
('2102', 'Padre Abad', '21'),
('2103', 'Purus', '21'),
('2104', 'Atalaya', '21'),
('2201', 'Ferrañafe', '22'),
('2202', 'Chiclayo', '22'),
('2203', 'Lambayeque', '22'),
('2301', 'Mariscal Nieto', '23'),
('2302', 'Gral. Sanchez Cerro', '23'),
('2303', 'Ilo', '23'),
('2401', 'Daniel Alcides Carrion', '24'),
('2402', 'Pasco', '24'),
('2403', 'Oxapampa', '24'),
('2501', 'Manu', '25'),
('2502', 'Tahuamanu', '25'),
('2503', 'Tambopata', '25')
ON CONFLICT (id_provincia) DO NOTHING;

-- ============================================================
-- 10. DATOS INICIALES: CATÁLOGO GENERAL (MAESTROS)
-- ============================================================

INSERT INTO tabla_catalogo_general (grupo_codigo, codigo_maestro, valor_mostrar, descripcion, estado) VALUES
-- Tipos de documento
('TIPO_DOC', 'DNI', 'DNI', 'Documento Nacional de Identidad', TRUE),
('TIPO_DOC', 'CE', 'CE', 'Carné de Extranjería', TRUE),
('TIPO_DOC', 'PASAPORTE', 'Pasaporte', 'Pasaporte', TRUE),
-- Estados de pedido
('ESTADO_PEDIDO', 'PAGO_PENDIENTE', 'Pago pendiente (Generado)', 'El pedido fue registrado, esperando pago', TRUE),
('ESTADO_PEDIDO', 'PAGO_ENVIADO', 'Voucher Enviado (Validar Pago)', 'El voucher del pedido está en validación', TRUE),
('ESTADO_PEDIDO', 'PAGO_VERIFICADO', 'Pago Verificado (Falta Empaquetar)', 'El pago ha sido validado', TRUE),
('ESTADO_PEDIDO', 'EMPAQUETADO', 'Empaquetado (Falta Enviar)', 'El pedido está listo para recojo o envío', TRUE),
('ESTADO_PEDIDO', 'ENVIADO', 'Enviado (En camino)', 'El paquete se encuentra en ruta', TRUE),
('ESTADO_PEDIDO', 'ENTREGADO', 'Entregados (Completados)', 'El pedido fue entregado exitosamente', TRUE),
('ESTADO_PEDIDO', 'PAGO_RECHAZADO', 'Rechazados', 'El pago fue rechazado', TRUE),
-- Monedas
('MONEDA', 'PEN', 'Soles (S/)', 'Moneda nacional del Perú', TRUE),
('MONEDA', 'USD', 'Dólares ($)', 'Dólar estadounidense', TRUE),
-- Categorías de cuento
('CATEGORIA_CUENTO', 'CAT_AVENTURA', 'Aventura', 'Cuentos de aventura para niños', TRUE),
('CATEGORIA_CUENTO', 'CAT_DIDACTICO', 'Didáctico', 'Cuentos para aprendizaje y refuerzo', TRUE),
('CATEGORIA_CUENTO', 'CAT_CLASICO', 'Clásico', 'Historias y fábulas populares', TRUE),
-- Rangos de edad
('RANGO_EDAD', 'EDAD_0_3', '0-3 años', 'Para niños de 0 a 3 años', TRUE),
('RANGO_EDAD', 'EDAD_4_6', '4-6 años', 'Para niños de 4 a 6 años', TRUE),
('RANGO_EDAD', 'EDAD_7_10', '7-10 años', 'Para niños de 7 a 10 años', TRUE)
ON CONFLICT (codigo_maestro) DO NOTHING;

-- ============================================================
-- 11. DATOS INICIALES: USUARIO ADMINISTRADOR
-- ============================================================
-- Hash BCrypt para contraseña '123456':
-- $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy

INSERT INTO usuarios (email, password, nombre, apellido, role, created_at)
SELECT 'cdanpg@gmail.com',
       '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       'Admin', 'Killa', 'ADMIN', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM usuarios WHERE email = 'cdanpg@gmail.com'
);

-- ============================================================
-- FIN DEL SCRIPT
-- ============================================================
-- 
-- NOTAS IMPORTANTES:
-- 1. Base de datos: PostgreSQL
-- 2. Codificación: UTF-8
-- 3. El script crea la estructura completa y carga datos iniciales
-- 4. Se usa ON CONFLICT para evitar duplicados en inserciones
-- 5. Las relaciones de clave foránea están configuradas
-- 6. Los índices optimizan las búsquedas frecuentes
--
-- USUARIO ADMINISTRADOR POR DEFECTO:
-- Email: cdanpg@gmail.com
-- Contraseña: 123456
-- Rol: ADMIN
-- ============================================================
