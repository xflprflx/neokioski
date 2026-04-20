package com.xflprflx.neokioski.services;

import com.xflprflx.neokioski.models.User;
import com.xflprflx.neokioski.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@org.springframework.transaction.annotation.Transactional
/**
 * Serviço central para operações relacionadas aos usuários do sistema, como registro, atualização, listagem e vinculação a grupos.
 */
public class UserService {

    private final UserRepository repository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public UserService(UserRepository repository, org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Método ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public List<User> findAll() {
        /*
         * Retorna o resultado final processado pelo método.
         */
        return repository.findAll();
    }

    /**
     * Método ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public List<User> findAll(String filter) {
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (filter == null || filter.isEmpty()) {
            /*
             * Retorna o resultado final processado pelo método.
             */
            return repository.findAll();
        } else {
            /*
             * Retorna o resultado final processado pelo método.
             */
            return repository.findByNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(filter, filter);
        }
    }

    /**
     * Método ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public Optional<User> findById(Long id) {
        /*
         * Retorna o resultado final processado pelo método.
         */
        return repository.findById(id);
    }

    /**
     * Método ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public void save(User user) {
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (user == null) {
            return;
        }
        
        // Se o usuário já existe e a senha está vazia, mantemos a senha antiga
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (user.getId() != null && (user.getPassword() == null || user.getPassword().isEmpty())) {
            repository.findById(user.getId()).ifPresent(oldUser -> {
                user.setPassword(oldUser.getPassword());
            });
        } else if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            // Se foi fornecida uma nova senha, criptografamos (a menos que já pareça um hash BCrypt)
            /*
             * Avalia uma condição (if) para desvio de fluxo da lógica principal.
             */
            if (!user.getPassword().startsWith("$2a$") && !user.getPassword().startsWith("$2b$") && !user.getPassword().startsWith("$2y$")) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            }
        }
        
        repository.save(user);
    }

    /**
     * Método ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public void delete(User user) {
        repository.delete(user);
    }

    /**
     * Método ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public void updateNotifyStoreOrder(Long id, boolean value) {
        repository.updateNotifyStoreOrder(id, value);
    }

    /**
     * Método ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public void updateNotifyChatSounds(Long id, boolean value) {
        repository.updateNotifyChatSounds(id, value);
    }

    /**
     * Método ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public void updateLastSeen(Long id, java.time.LocalDateTime lastSeen) {
        repository.updateLastSeen(id, lastSeen);
    }

    /**
     * Método ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public boolean isPasswordValid(User user, String password) {
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (user == null || password == null || password.isEmpty()) {
            /*
             * Retorna o resultado final processado pelo método.
             */
            return false;
        }
        /*
         * Retorna o resultado final processado pelo método.
         */
        return passwordEncoder.matches(password, user.getPassword());
    }

    /**
     * Método ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public void changePassword(Long userId, String newPassword) {
        repository.findById(userId).ifPresent(user -> {
            user.setPassword(passwordEncoder.encode(newPassword));
            repository.save(user);
        });
    }

    /**
     * Método ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public String generateTemporaryPassword(Long userId) {
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String special = "@$!%*?&";
        String all = upper + lower + digits + special;
        
        StringBuilder sb = new StringBuilder();
        java.security.SecureRandom rnd = new java.security.SecureRandom();
        
        // Garante pelo menos um de cada tipo exigido
        sb.append(upper.charAt(rnd.nextInt(upper.length())));
        sb.append(lower.charAt(rnd.nextInt(lower.length())));
        sb.append(digits.charAt(rnd.nextInt(digits.length())));
        sb.append(special.charAt(rnd.nextInt(special.length())));
        
        // Completa até 8 caracteres (mais seguro que 6 para garantir força)
        /*
         * Inicia um laço de repetição (for) para iterar sobre os elementos.
         */
        for (int i = 0; i < 4; i++) {
            sb.append(all.charAt(rnd.nextInt(all.length())));
        }
        
        // Embaralha para que os tipos obrigatórios não fiquem sempre nas primeiras posições
        char[] passwordArray = sb.toString().toCharArray();
        /*
         * Inicia um laço de repetição (for) para iterar sobre os elementos.
         */
        for (int i = passwordArray.length - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            char temp = passwordArray[i];
            passwordArray[i] = passwordArray[j];
            passwordArray[j] = temp;
        }
        
        String tempPass = new String(passwordArray);
        changePassword(userId, tempPass);
        /*
         * Retorna o resultado final processado pelo método.
         */
        return tempPass;
    }
}
