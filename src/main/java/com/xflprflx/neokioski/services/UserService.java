package com.xflprflx.neokioski.services;

import com.xflprflx.neokioski.models.User;
import com.xflprflx.neokioski.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@org.springframework.transaction.annotation.Transactional
public class UserService {

    private final UserRepository repository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public UserService(UserRepository repository, org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> findAll() {
        return repository.findAll();
    }

    public List<User> findAll(String filter) {
        if (filter == null || filter.isEmpty()) {
            return repository.findAll();
        } else {
            return repository.findByNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(filter, filter);
        }
    }

    public Optional<User> findById(Long id) {
        return repository.findById(id);
    }

    public void save(User user) {
        if (user == null) {
            return;
        }
        
        // Se o usuário já existe e a senha está vazia, mantemos a senha antiga
        if (user.getId() != null && (user.getPassword() == null || user.getPassword().isEmpty())) {
            repository.findById(user.getId()).ifPresent(oldUser -> {
                user.setPassword(oldUser.getPassword());
            });
        } else if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            // Se foi fornecida uma nova senha, criptografamos (a menos que já pareça um hash BCrypt)
            if (!user.getPassword().startsWith("$2a$") && !user.getPassword().startsWith("$2b$") && !user.getPassword().startsWith("$2y$")) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            }
        }
        
        repository.save(user);
    }

    public void delete(User user) {
        repository.delete(user);
    }

    public void updateNotifyStoreOrder(Long id, boolean value) {
        repository.updateNotifyStoreOrder(id, value);
    }

    public void updateNotifyChatSounds(Long id, boolean value) {
        repository.updateNotifyChatSounds(id, value);
    }

    public void updateLastSeen(Long id, java.time.LocalDateTime lastSeen) {
        repository.updateLastSeen(id, lastSeen);
    }
}
