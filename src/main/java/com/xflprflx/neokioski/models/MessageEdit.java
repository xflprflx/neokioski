package com.xflprflx.neokioski.models;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "tb_message_edits")
/**
 * Entidade que armazena o histórico de edições de uma mensagem de chat.
 */
public class MessageEdit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String oldContent;

    private Instant editTimestamp;

    @ManyToOne
    @JoinColumn(name = "chat_message_id")
    private ChatMessage chatMessage;

    public MessageEdit() {
    }

    public MessageEdit(String oldContent, ChatMessage chatMessage) {
        this.oldContent = oldContent;
        this.chatMessage = chatMessage;
        this.editTimestamp = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOldContent() {
        return oldContent;
    }

    public void setOldContent(String oldContent) {
        this.oldContent = oldContent;
    }

    public Instant getEditTimestamp() {
        return editTimestamp;
    }

    public void setEditTimestamp(Instant editTimestamp) {
        this.editTimestamp = editTimestamp;
    }

    public ChatMessage getChatMessage() {
        return chatMessage;
    }

    public void setChatMessage(ChatMessage chatMessage) {
        this.chatMessage = chatMessage;
    }
}
