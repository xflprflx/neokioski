CREATE TABLE IF NOT EXISTS tb_task_triggered_alerts
(
    alert_id bigint,
    task_id bigint NOT NULL,
    CONSTRAINT fkd7jslelyvduy4pk0svhbf75lg FOREIGN KEY (task_id)
    REFERENCES tb_tasks (id) MATCH SIMPLE
    ON UPDATE NO ACTION
    ON DELETE NO ACTION
);