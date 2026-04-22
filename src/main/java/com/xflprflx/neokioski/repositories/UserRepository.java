package com.xflprflx.neokioski.repositories;

import com.xflprflx.neokioski.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
/**
 * Repositório de acesso a dados para persistência e buscas de usuários, essencial para autenticação e gestão de acesso (User).
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Busca um usuário pelo seu nome de login.
     *
     * @param username O nome de usuário a ser buscado.
     * @return Um Optional contendo o usuário, se encontrado.
     */
    @org.springframework.data.jpa.repository.QueryHints({
            @jakarta.persistence.QueryHint(name = "org.hibernate.cacheable", value = "true")
    })
    Optional<User> findByUsername(String username);

    java.util.List<User> findByNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(String name, String username);
    
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE User u SET u.notifyStoreOrder = :value WHERE u.id = :id")
    void updateNotifyStoreOrder(@org.springframework.data.repository.query.Param("id") Long id, @org.springframework.data.repository.query.Param("value") boolean value);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE User u SET u.notifyChatSounds = :value WHERE u.id = :id")
    void updateNotifyChatSounds(@org.springframework.data.repository.query.Param("id") Long id, @org.springframework.data.repository.query.Param("value") boolean value);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE User u SET u.lastSeen = :lastSeen WHERE u.id = :id")
    void updateLastSeen(@org.springframework.data.repository.query.Param("id") Long id, @org.springframework.data.repository.query.Param("lastSeen") java.time.LocalDateTime lastSeen);
}
