package com.xflprflx.neokioski.models;

import com.xflprflx.neokioski.models.enums.NotificationType;
import jakarta.persistence.*;

@Entity
@Table(name = "tb_message_notifications")
public class MessageNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User recipient;

    @ManyToOne(optional = false)
    @JoinColumn(name = "message_id")
    private ChatMessage message;

    private boolean isRead = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    public MessageNotification() {
    }

    public MessageNotification(User recipient, ChatMessage message, NotificationType type) {
        this.recipient = recipient;
        this.message = message;
        this.type = type;
        this.isRead = false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getRecipient() {
        return recipient;
    }

    public void setRecipient(User recipient) {
        this.recipient = recipient;
    }

    public ChatMessage getMessage() {
        return message;
    }

    public void setMessage(ChatMessage message) {
        this.message = message;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }
}
