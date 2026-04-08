CREATE TABLE IF NOT EXISTS tb_template_step_relation_alert_roles
(
    relation_id bigint NOT NULL,
    role character varying(255) COLLATE pg_catalog."default",
    CONSTRAINT fkr5tp7ut3l3mudwandnonc45vj FOREIGN KEY (relation_id)
    REFERENCES tb_template_step_relations (id) MATCH SIMPLE
    ON UPDATE NO ACTION
    ON DELETE NO ACTION,
    CONSTRAINT tb_template_step_relation_alert_roles_role_check CHECK (role::text = ANY (ARRAY['ADMIN'::character varying, 'SELLER'::character varying, 'LOGISTICS'::character varying]::text[]))
);