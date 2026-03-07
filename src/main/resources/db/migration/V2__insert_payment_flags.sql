-- ============================================================
-- V2__insert_payment_flags.sql
-- Inserta Feature Flags en el Catálogo General
-- ============================================================

INSERT INTO tabla_catalogo_general (grupo_codigo, codigo_maestro, valor_mostrar, descripcion, estado)
VALUES
  ('METODOS_PAGO', 'MP_ENABLED', 'Mercado Pago', 'Habilitar pasarela Mercado Pago', TRUE),
  ('METODOS_PAGO', 'YAPE_ENABLED', 'Yape / Plin', 'Habilitar pago manual (Subir Captura)', TRUE)
ON CONFLICT (codigo_maestro) DO NOTHING;
