CREATE TABLE IF NOT EXISTS tb_task_task_step_definitions
(
    task_id bigint NOT NULL,
    task_step_definition_id bigint NOT NULL,
    CONSTRAINT fkm5dxq3tnx5r32ym3xr4qmx192 FOREIGN KEY (task_id)
    REFERENCES tb_tasks (id) MATCH SIMPLE
    ON UPDATE NO ACTION
    ON DELETE NO ACTION,
    CONSTRAINT fktfoe9mgtx1m7jia26c3eqaq25 FOREIGN KEY (task_step_definition_id)
    REFERENCES tb_task_step_definitions (id) MATCH SIMPLE
    ON UPDATE NO ACTION
    ON DELETE NO ACTION
);