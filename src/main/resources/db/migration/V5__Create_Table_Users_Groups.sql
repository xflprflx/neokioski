CREATE TABLE IF NOT EXISTS tb_users_groups
(
    group_id bigint NOT NULL,
    user_id bigint NOT NULL,
    CONSTRAINT tb_users_groups_pkey PRIMARY KEY (group_id, user_id),
    CONSTRAINT fk901j1nrg5nr83snoefh8uxr9w FOREIGN KEY (group_id)
    REFERENCES tb_groups (id) MATCH SIMPLE
    ON UPDATE NO ACTION
    ON DELETE NO ACTION,
    CONSTRAINT fkg9bk4ndx1si6xk8hyye53orf4 FOREIGN KEY (user_id)
    REFERENCES tb_users (id) MATCH SIMPLE
    ON UPDATE NO ACTION
    ON DELETE NO ACTION
);