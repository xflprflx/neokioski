-- Migration para otimizar a ordenação de edições de mensagens no chat
-- Isso evita o custo de ordenação em memória durante a renderização de grupos com muitas mensagens editadas.
CREATE INDEX idx_message_edits_msg_timestamp ON tb_message_edits(chat_message_id, edit_timestamp DESC);
