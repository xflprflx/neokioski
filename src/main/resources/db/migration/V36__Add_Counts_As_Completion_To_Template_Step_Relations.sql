-- V36: Adiciona coluna 'counts_as_completion' à tabela de relações step-template.
-- Esse booleano indica qual passo pontua a conclusão para fins de ranking de concluidores
-- no Dashboard, independentemente do passo finalizador (isFinalizer).
-- DEFAULT FALSE garante retrocompatibilidade: templates existentes continuam usando
-- o fallback para isFinalizer na lógica do DashboardService.
ALTER TABLE tb_template_step_relations ADD COLUMN counts_as_completion BOOLEAN NOT NULL DEFAULT FALSE;
