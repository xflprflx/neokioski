INSERT INTO tb_users(name, username, password, role, is_active, notify_store_order, notify_chat_sounds) VALUES ('Filipe Machado', 'flpmcd', '$2a$10$zFWu9UncFTPVxU5UwDROmOsAU8/GahmwjmzHCLs8c.8XqGfg3w4wq', 'ADMIN', true, true, true);
INSERT INTO tb_users(name, username, password, role, is_active, notify_store_order, notify_chat_sounds) VALUES ('Rubens Machado', 'rbnmcd', '$2a$10$zFWu9UncFTPVxU5UwDROmOsAU8/GahmwjmzHCLs8c.8XqGfg3w4wq', 'SELLER', true, false, false);
INSERT INTO tb_users(name, username, password, role, is_active, notify_store_order, notify_chat_sounds) VALUES ('Mariana Bedin', 'mrnbdn', '$2a$10$zFWu9UncFTPVxU5UwDROmOsAU8/GahmwjmzHCLs8c.8XqGfg3w4wq', 'LOGISTICS', true, false, true);

INSERT INTO tb_groups(name, is_active) VALUES ('Lojas POA', true);
INSERT INTO tb_groups(name, is_active) VALUES ('Lojas JOI', true);

INSERT INTO tb_users_groups(group_id, user_id) VALUES (1, 1);
INSERT INTO tb_users_groups(group_id, user_id) VALUES (1, 2);
INSERT INTO tb_users_groups(group_id, user_id) VALUES (1, 3);

INSERT INTO tb_task_templates(name, show_send_button, requires_signature, play_alert_sound) VALUES ('Pedido Loja', true, false, true);

INSERT INTO tb_task_field_definitions(label, description, input_type) VALUES ('Pedido', 'Número do pedido', 'TEXT');

INSERT INTO tb_template_field_relations(field_order, is_required, task_template_id, task_field_definition_id) VALUES (1, true, 1, 1);

INSERT INTO tb_task_step_definitions(label, color_theme) VALUES ('FAZENDO', 'blue');
INSERT INTO tb_task_step_definitions(label, color_theme) VALUES ('PRONTO', 'green');

INSERT INTO tb_template_step_relations(step_order, is_finalizer, task_template_id, task_step_definition_id, mention_author, mention_previous_actor, should_pulse) VALUES (1, false, 1, 1, false, false, false);
INSERT INTO tb_template_step_relations(step_order, is_finalizer, task_template_id, task_step_definition_id, mention_author, mention_previous_actor, should_pulse) VALUES (2, true, 1, 2, false, false, true);

INSERT INTO tb_task_templates_groups(task_template_id, group_id) VALUES (1, 1);
INSERT INTO tb_task_templates_groups(task_template_id, group_id ) VALUES (1, 2);




