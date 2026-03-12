-- V7__add_categoria_to_cuento.sql
-- RM-01: Añadir columna categoria a la tabla cuento para permitir el filtrado por categoría
ALTER TABLE cuento ADD COLUMN categoria VARCHAR(50);
