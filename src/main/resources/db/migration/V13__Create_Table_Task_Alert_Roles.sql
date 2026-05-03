CREATE TABLE IF NOT EXISTS tb_task_alert_roles
(
    task_alert_id bigint NOT NULL,
    role character varying(255) COLLATE pg_catalog."default",
    CONSTRAINT fkfu01ga7tjaa1hu24noj9h30qk FOREIGN KEY (task_alert_id)
    REFERENCES tb_task_template_alerts (id) MATCH SIMPLE
    ON UPDATE NO ACTION
    ON DELETE NO ACTION,
    CONSTRAINT tb_task_alert_roles_role_check CHECK (role::text = ANY (ARRAY['ADMIN'::character varying, 'SELLER'::character varying, 'LOGISTICS'::character varying]::text[]))
);