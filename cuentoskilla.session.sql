--  SELECT * FROM usuarios ;


-- CREATE TABLE direcciones (
--   id SERIAL PRIMARY KEY,
--   calle TEXT,
--   ciudad TEXT,
--   departamento TEXT,
--   provincia TEXT,
--   distrito TEXT,
--   referencia TEXT,
--   codigo_postal TEXT,
--   es_principal BOOLEAN DEFAULT false,
--   es_facturacion BOOLEAN DEFAULT false,
--   usuario_id BIGINT REFERENCES usuarios( id   ),

--   fecha_creacion TIMESTAMP,
--   fecha_actualizacion TIMESTAMP,
--   creado_por TEXT,
--   actualizado_por TEXT
-- );