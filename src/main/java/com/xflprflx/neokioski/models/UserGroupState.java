package com.xflprflx.neokioski.models;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "tb_user_group_state")
public class UserGroupState {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "group_id")
    private Group group;

    @Column(nullable = false)
    private Instant lastReadTimestamp;

    public UserGroupState() {
    }

    public UserGroupState(User user, Group group, Instant lastReadTimestamp) {
        this.user = user;
        this.group = group;
        this.lastReadTimestamp = lastReadTimestamp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Instant getLastReadTimestamp() {
        return lastReadTimestamp;
    }

    public void setLastReadTimestamp(Instant lastReadTimestamp) {
        this.lastReadTimestamp = lastReadTimestamp;
    }
}
