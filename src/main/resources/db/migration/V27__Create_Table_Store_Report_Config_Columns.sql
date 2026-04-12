CREATE TABLE IF NOT EXISTS tb_store_report_config_columns
(
    order_index integer NOT NULL,
    config_id bigint NOT NULL,
    attribute_key character varying(255) COLLATE pg_catalog."default",
    label character varying(255) COLLATE pg_catalog."default",
    CONSTRAINT tb_store_report_config_columns_pkey PRIMARY KEY (order_index, config_id),
    CONSTRAINT fkbxafi70bjy15xiplw1v0im4b6 FOREIGN KEY (config_id)
    REFERENCES tb_store_report_config (id) MATCH SIMPLE
    ON UPDATE NO ACTION
    ON DELETE NO ACTION
);