-- V9__add_tipo_entrega_order.sql
-- Añadir el campo de tipo de entrega al historial del pedido (por ejemplo, 'SHALOM' o 'COURIER_LIMA')

ALTER TABLE orders ADD COLUMN tipo_entrega VARCHAR(50);
