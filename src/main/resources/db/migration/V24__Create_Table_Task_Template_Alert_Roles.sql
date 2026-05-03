CREATE TABLE IF NOT EXISTS tb_task_template_alert_roles
(
    task_template_id bigint NOT NULL,
    role character varying(255) COLLATE pg_catalog."default",
    CONSTRAINT fkjkrf7se3bhlpcbyumsidbwnvj FOREIGN KEY (task_template_id)
    REFERENCES tb_task_templates (id) MATCH SIMPLE
    ON UPDATE NO ACTION
    ON DELETE NO ACTION,
    CONSTRAINT tb_task_template_alert_roles_role_check CHECK (role::text = ANY (ARRAY['ADMIN'::character varying, 'SELLER'::character varying, 'LOGISTICS'::character varying]::text[]))
);