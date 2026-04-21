package com.xflprflx.neokioski.models;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "tb_chat_messages")
@BatchSize(size = 100)
@NamedEntityGraphs({
    @NamedEntityGraph(
        name = "ChatMessage.withTaskFull",
        attributeNodes = {
            @NamedAttributeNode(value = "task", subgraph = "task.full"),
            @NamedAttributeNode("user"),
            @NamedAttributeNode("group"),
            @NamedAttributeNode("replyTo"),
            @NamedAttributeNode("reactions"),
            @NamedAttributeNode("edits")
        },
        subgraphs = {
            @NamedSubgraph(
                name = "task.full",
                attributeNodes = {
                    @NamedAttributeNode("creator"),
                    @NamedAttributeNode("group"),
                    @NamedAttributeNode("currentStep"),
                    @NamedAttributeNode("taskTemplate")
                }
            )
        }
    ),
    @NamedEntityGraph(
        name = "ChatMessage.minimal",
        attributeNodes = {
            @NamedAttributeNode("user"),
            @NamedAttributeNode("group"),
            @NamedAttributeNode(value = "replyTo", subgraph = "message.basic"),
            @NamedAttributeNode(value = "reactions", subgraph = "reaction.basic"),
            @NamedAttributeNode("edits")
        },
        subgraphs = {
            @NamedSubgraph(
                name = "message.basic",
                attributeNodes = {
                    @NamedAttributeNode("user")
                }
            ),
            @NamedSubgraph(
                name = "reaction.basic",
                attributeNodes = {
                    @NamedAttributeNode("user")
                }
            )
        }
    )
})
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    private Instant timestamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @OneToMany(mappedBy = "chatMessage", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 100)
    private Set<MessageReaction> reactions = new LinkedHashSet<>();

    @OneToMany(mappedBy = "chatMessage", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("editTimestamp DESC")
    @BatchSize(size = 100)
    private Set<MessageEdit> edits = new LinkedHashSet<>();

    private boolean isDeleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_id")
    private ChatMessage replyTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private Task task;

    private String alertSound;

    private String alertRoles;

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

    public Set<MessageReaction> getReactions() {
        return reactions;
    }

    public void setReactions(Set<MessageReaction> reactions) {
        this.reactions = reactions;
    }

    public ChatMessage getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(ChatMessage replyTo) {
        this.replyTo = replyTo;
    }

    public Set<MessageEdit> getEdits() {
        return edits;
    }

    public void setEdits(Set<MessageEdit> edits) {
        this.edits = edits;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public String getAlertSound() {
        return alertSound;
    }

    public void setAlertSound(String alertSound) {
        this.alertSound = alertSound;
    }

    public String getAlertRoles() {
        return alertRoles;
    }

    public void setAlertRoles(String alertRoles) {
        this.alertRoles = alertRoles;
    }
}
