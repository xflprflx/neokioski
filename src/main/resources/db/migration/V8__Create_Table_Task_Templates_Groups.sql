CREATE TABLE IF NOT EXISTS tb_task_templates_groups
(
    group_id bigint NOT NULL,
    task_template_id bigint NOT NULL,
    CONSTRAINT fk61h4ic0bbycvyr54jtlr2g14s FOREIGN KEY (group_id)
    REFERENCES tb_groups (id) MATCH SIMPLE
    ON UPDATE NO ACTION
    ON DELETE NO ACTION,
    CONSTRAINT fkerv83w1ik1nupv2eykfsfodfi FOREIGN KEY (task_template_id)
    REFERENCES tb_task_templates (id) MATCH SIMPLE
    ON UPDATE NO ACTION
    ON DELETE NO ACTION
);