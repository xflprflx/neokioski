CREATE TABLE IF NOT EXISTS tb_task_templates_authorizer_users
(
    task_template_id bigint NOT NULL,
    user_id bigint NOT NULL,
    CONSTRAINT fkfiqnqfbhns88ubaroekr021np FOREIGN KEY (task_template_id)
    REFERENCES tb_task_templates (id) MATCH SIMPLE
    ON UPDATE NO ACTION
    ON DELETE NO ACTION,
    CONSTRAINT fkrk537ofdj7uvnsl3romc9i5jv FOREIGN KEY (user_id)
    REFERENCES tb_users (id) MATCH SIMPLE
    ON UPDATE NO ACTION
    ON DELETE NO ACTION
);