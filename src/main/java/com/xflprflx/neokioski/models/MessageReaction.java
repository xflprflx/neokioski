package com.xflprflx.neokioski.models;

import jakarta.persistence.*;

@Entity
@Table(name = "tb_message_reactions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"chat_message_id", "user_id", "emoji"})
})
public class MessageReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String emoji;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "chat_message_id", nullable = false)
    private ChatMessage chatMessage;

    public MessageReaction() {
    }

    public MessageReaction(String emoji, User user, ChatMessage chatMessage) {
        this.emoji = emoji;
        this.user = user;
        this.chatMessage = chatMessage;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public ChatMessage getChatMessage() {
        return chatMessage;
    }

    public void setChatMessage(ChatMessage chatMessage) {
        this.chatMessage = chatMessage;
    }
}
