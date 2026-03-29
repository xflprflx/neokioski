package com.xflprflx.neokioski.models;

import com.xflprflx.neokioski.models.enums.Role;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "tb_users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;
    @Column(nullable = false, unique = true)
    private String username;
    @Column(nullable = false)
    private String password;
    private boolean isActive;
    @Enumerated(EnumType.STRING)
    private Role role;
    private boolean notifyStoreOrder;
    private boolean notifyChatSounds = true;

    @ManyToMany(mappedBy = "users")
    private Set<Group> groups = new HashSet<>();

    public User() {
    }

    public boolean isNotifyStoreOrder() {
        return notifyStoreOrder;
    }

    public void setNotifyStoreOrder(boolean notifyStoreOrder) {
        this.notifyStoreOrder = notifyStoreOrder;
    }

    public boolean isNotifyChatSounds() {
        return notifyChatSounds;
    }

    public void setNotifyChatSounds(boolean notifyChatSounds) {
        this.notifyChatSounds = notifyChatSounds;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Set<Group> getGroups() {
        return groups;
    }

    public void setGroups(Set<Group> groups) {
        this.groups = groups;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id) && Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username);
    }
}
