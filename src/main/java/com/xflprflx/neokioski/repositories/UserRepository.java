package com.xflprflx.neokioski.repositories;

import com.xflprflx.neokioski.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    /**
     * Busca um usuário pelo seu nome de login.
     *
     * @param username O nome de usuário a ser buscado.
     * @return Um Optional contendo o usuário, se encontrado.
     */
    Optional<User> findByUsername(String username);

    java.util.List<User> findByNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(String name, String username);
}
