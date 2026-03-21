package com.xflprflx.neokioski.models;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    private Instant timestamp;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private Group group;

    @OneToMany(mappedBy = "chatMessage", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<MessageReaction> reactions = new ArrayList<>();

    @OneToMany(mappedBy = "chatMessage", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("editTimestamp DESC")
    private List<MessageEdit> edits = new ArrayList<>();

    private boolean isDeleted = false;

    @ManyToOne
    @JoinColumn(name = "reply_to_id")
    private ChatMessage replyTo;

    public ChatMessage() {
    }

    public ChatMessage(String content, User user, Group group) {
        this.content = content;
        this.user = user;
        this.group = group;
        this.timestamp = Instant.now();
    }

    public ChatMessage(String content, User user, Group group, ChatMessage replyTo) {
        this(content, user, group);
        this.replyTo = replyTo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public List<MessageReaction> getReactions() {
        return reactions;
    }

    public void setReactions(List<MessageReaction> reactions) {
        this.reactions = reactions;
    }

    public ChatMessage getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(ChatMessage replyTo) {
        this.replyTo = replyTo;
    }

    public List<MessageEdit> getEdits() {
        return edits;
    }

    public void setEdits(List<MessageEdit> edits) {
        this.edits = edits;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }
}
