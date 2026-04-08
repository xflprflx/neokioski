CREATE TABLE IF NOT EXISTS tb_template_step_next_allowed
(
    next_step_definition_id bigint NOT NULL,
    template_step_relation_id bigint NOT NULL,
    CONSTRAINT fkpbg2ee8y9ogni880magxcrcle FOREIGN KEY (next_step_definition_id)
    REFERENCES public.tb_task_step_definitions (id) MATCH SIMPLE
    ON UPDATE NO ACTION
    ON DELETE NO ACTION,
    CONSTRAINT fksha1ntqdxb3h9n81x995qsrcs FOREIGN KEY (template_step_relation_id)
    REFERENCES public.tb_template_step_relations (id) MATCH SIMPLE
    ON UPDATE NO ACTION
    ON DELETE NO ACTION
);