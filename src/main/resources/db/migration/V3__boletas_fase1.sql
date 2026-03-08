-- ============================================================
-- V3__boletas_fase1.sql
-- ParÃ¡metros de facturaciÃ³n y boletas PDF locales
-- ============================================================

CREATE TABLE IF NOT EXISTS boletas (
    id                  BIGSERIAL PRIMARY KEY,
    order_id            BIGINT NOT NULL UNIQUE REFERENCES orders(id) ON DELETE CASCADE,
    numero_comprobante  VARCHAR(30) NOT NULL UNIQUE,
    serie               VARCHAR(10) NOT NULL,
    correlativo         INT NOT NULL,
    file_path           TEXT NOT NULL,
    fecha_emision       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_boletas_order_id ON boletas(order_id);
CREATE INDEX IF NOT EXISTS idx_boletas_numero ON boletas(numero_comprobante);

INSERT INTO tabla_catalogo_general (grupo_codigo, codigo_maestro, valor_mostrar, descripcion, estado)
VALUES
('FACTURACION', 'EMPRESA_RUC', '00000000000', 'RUC de la empresa emisora', TRUE),
('FACTURACION', 'EMPRESA_RAZON_SOCIAL', 'KillaCuentos', 'RazÃ³n social del emisor', TRUE),
('FACTURACION', 'EMPRESA_DIRECCION_FISCAL', 'POR CONFIGURAR', 'DirecciÃ³n fiscal del emisor', TRUE),
('FACTURACION', 'EMPRESA_LOGO_URL', '', 'URL o path del logo institucional', TRUE),
('FACTURACION', 'SUNAT_CPE_03', '03', 'CatÃ¡logo 01 - Boleta de Venta', TRUE),
('FACTURACION', 'SUNAT_IGV_20', '20', 'CatÃ¡logo 07 - Exonerado OperaciÃ³n Onerosa', TRUE),
('FACTURACION', 'SUNAT_DOC_1', 'DNI', 'CatÃ¡logo 06 - Tipo documento DNI', TRUE),
('FACTURACION', 'SUNAT_DOC_4', 'CE', 'CatÃ¡logo 06 - Tipo documento CE', TRUE),
('FACTURACION', 'SUNAT_DOC_7', 'PASAPORTE', 'CatÃ¡logo 06 - Tipo documento Pasaporte', TRUE),
('FACTURACION', 'BOLETA_SERIE_ACTIVA', 'B001', 'Serie activa de boleta (patrÃ³n B###)', TRUE),
('FACTURACION', 'BOLETA_CORRELATIVO_ACTUAL', '0', 'Correlativo actual de boleta', TRUE)
ON CONFLICT (codigo_maestro) DO NOTHING;

