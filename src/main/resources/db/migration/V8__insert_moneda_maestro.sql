-- ============================================================
-- V8__insert_moneda_maestro.sql
-- Inserción de configuración de Moneda en la tabla de maestros.
-- ============================================================

INSERT INTO tabla_catalogo_general (grupo_codigo, codigo_maestro, valor_mostrar, descripcion, estado)
VALUES ('MONEDA', 'PEN', 'S/', 'Sol Peruano', true)
ON CONFLICT (codigo_maestro) DO NOTHING;
