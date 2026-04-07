CREATE TABLE IF NOT EXISTS public.tb_tasks_task_field_values
(
    task_field_values_id bigint NOT NULL,
    task_id bigint NOT NULL,
    CONSTRAINT tb_tasks_task_field_values_pkey PRIMARY KEY (task_field_values_id, task_id),
    CONSTRAINT tb_tasks_task_field_values_task_field_values_id_key UNIQUE (task_field_values_id),
    CONSTRAINT fk5lepwegtu176sc4o0onmub3kr FOREIGN KEY (task_id)
    REFERENCES public.tb_tasks (id) MATCH SIMPLE
    ON UPDATE NO ACTION
    ON DELETE NO ACTION,
    CONSTRAINT fkikt1bqd76u4e25cem687s9ul3 FOREIGN KEY (task_field_values_id)
    REFERENCES public.tb_task_field_values (id) MATCH SIMPLE
    ON UPDATE NO ACTION
    ON DELETE NO ACTION
);