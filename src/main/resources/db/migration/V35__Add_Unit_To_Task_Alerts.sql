-- Migração para adicionar suporte a unidades de tempo nos alertas
ALTER TABLE tb_task_template_alerts RENAME COLUMN timeout_minutes TO timeout_value;
ALTER TABLE tb_task_template_alerts ADD COLUMN timeout_unit VARCHAR(255) DEFAULT 'MINUTES';
UPDATE tb_task_template_alerts SET timeout_unit = 'MINUTES' WHERE timeout_unit IS NULL;
ALTER TABLE tb_task_template_alerts ALTER COLUMN timeout_unit SET NOT NULL;
