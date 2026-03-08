-- ============================================================
-- V4__boletas_resiliencia_generacion.sql
-- Estado de generación de boleta y soporte de reintentos
-- ============================================================

ALTER TABLE boletas
    ALTER COLUMN file_path DROP NOT NULL;

ALTER TABLE boletas
    ADD COLUMN IF NOT EXISTS estado_generacion VARCHAR(20) NOT NULL DEFAULT 'GENERADA',
    ADD COLUMN IF NOT EXISTS intentos INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS ultimo_error VARCHAR(500),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

UPDATE boletas
SET estado_generacion = 'GENERADA',
    intentos = CASE WHEN intentos IS NULL OR intentos < 1 THEN 1 ELSE intentos END,
    updated_at = CURRENT_TIMESTAMP
WHERE estado_generacion IS NULL OR estado_generacion <> 'GENERADA';

CREATE INDEX IF NOT EXISTS idx_boletas_estado_generacion ON boletas(estado_generacion);
