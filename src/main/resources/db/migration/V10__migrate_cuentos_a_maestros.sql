-- V10__migrate_cuentos_a_maestros.sql
-- Mapea los valores en texto libre historico a los codigos oficiales de Maestros.

-- 1. Actualización de Edades Recomendadas
UPDATE cuento SET edad_recomendada = 'EDAD_0_3' WHERE edad_recomendada = '0 a 3 años';
UPDATE cuento SET edad_recomendada = 'EDAD_4_6' WHERE edad_recomendada = '4 a 6 años';
UPDATE cuento SET edad_recomendada = 'EDAD_7_9' WHERE edad_recomendada = '7 a 9 años';
UPDATE cuento SET edad_recomendada = 'EDAD_10_12' WHERE edad_recomendada = '10 a 12 años';

-- 2. Actualización de Categorías
UPDATE cuento SET categoria = 'CAT_AVENTURA' WHERE categoria = 'Aventuras';
UPDATE cuento SET categoria = 'CAT_FANTASIA' WHERE categoria = 'Fantasía' OR categoria = 'Fantasia' OR categoria = 'Fantas\u00EDa';
UPDATE cuento SET categoria = 'CAT_MISTERIO' WHERE categoria = 'Misterio';
UPDATE cuento SET categoria = 'CAT_CIENCIA_FIC' WHERE categoria = 'Ciencia Ficción' OR categoria = 'Ciencia Ficcion' OR categoria = 'Ciencia ficcion' OR categoria = 'Ciencia Ficción';
UPDATE cuento SET categoria = 'CAT_FABULA' WHERE categoria = 'Fábulas' OR categoria = 'Fabulas' OR categoria = 'Fabula' OR categoria = 'Fábula';
UPDATE cuento SET categoria = 'CAT_EDUCATIVO' WHERE categoria = 'Educativo';
UPDATE cuento SET categoria = 'CAT_HUMOR' WHERE categoria = 'Humor';
UPDATE cuento SET categoria = 'CAT_CLASICO' WHERE categoria = 'Clásico' OR categoria = 'Clasico' OR categoria = 'Clásicos' OR categoria = 'Clasicos';
