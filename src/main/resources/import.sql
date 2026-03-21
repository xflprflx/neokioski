INSERT INTO tb_users(name, username, password, role, is_active) VALUES ('Filipe Machado', 'flpmcd', '$2a$10$zFWu9UncFTPVxU5UwDROmOsAU8/GahmwjmzHCLs8c.8XqGfg3w4wq', 'ADMIN', true);
INSERT INTO tb_users(name, username, password, role, is_active) VALUES ('Rubens Machado', 'rbnmcd', '$2a$10$zFWu9UncFTPVxU5UwDROmOsAU8/GahmwjmzHCLs8c.8XqGfg3w4wq', 'SELLER', true);

INSERT INTO tb_groups(name, is_active) VALUES ('Lojas POA', true);

INSERT INTO tb_users_groups(group_id, user_id) VALUES (1, 1);
INSERT INTO tb_users_groups(group_id, user_id) VALUES (1, 2);
