package com.xflprflx.neokioski.models;

import com.xflprflx.neokioski.models.enums.Role;
import jakarta.persistence.*;

import org.hibernate.annotations.BatchSize;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "tb_users")
@org.hibernate.annotations.Cache(usage = org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE)
@BatchSize(size = 100)
/**
 * Entidade que representa um usuário do sistema, com credenciais e papéis de acesso.
 */
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
    private LocalDateTime lastSeen;

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
    
    public LocalDateTime getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(LocalDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return id != null && id.equals(user.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
