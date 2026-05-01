-- Adicionar campo de opções de justificativa na configuração do SLA
ALTER TABLE tb_task_sla_configs ADD COLUMN IF NOT EXISTS justification_options TEXT;

-- Adicionar campos de justificativa no resultado do SLA
ALTER TABLE tb_task_sla_results ADD COLUMN IF NOT EXISTS justification VARCHAR(500);
ALTER TABLE tb_task_sla_results ADD COLUMN IF NOT EXISTS justification_comments TEXT;
