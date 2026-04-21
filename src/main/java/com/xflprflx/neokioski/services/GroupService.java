package com.xflprflx.neokioski.services;

import com.xflprflx.neokioski.models.Group;
import com.xflprflx.neokioski.models.User;
import com.xflprflx.neokioski.repositories.GroupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
/**
 * Serviço que agrupa a lógica de criação, edição, listagem e gerenciamento de grupos de usuários.
 */
public class GroupService {

    private final GroupRepository repository;

    public GroupService(GroupRepository repository) {
        this.repository = repository;
    }

    /**
     * Métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public List<Group> findActiveGroupsByUser(User user) {
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (user == null || user.getId() == null) {
            /*
             * Retorna o resultado final processado pelo métodx.
             */
            return List.of();
        }
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return repository.findByUsersIdAndIsActiveTrue(user.getId());
    }

    /**
     * Métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public List<Group> findAll() {
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return repository.findAllWithUsers();
    }

    /**
     * Métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public List<Group> findAll(String filterText) {
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (filterText == null || filterText.isEmpty()) {
            /*
             * Retorna o resultado final processado pelo métodx.
             */
            return repository.findAllWithUsers();
        } else {
            // Se houver necessidade de filtro customizado no repositório, podemos adicionar depois.
            // Por enquanto, faremos o filtro simples se necessário ou retornaremos tudo.
            /*
             * Retorna o resultado final processado pelo métodx.
             */
            return repository.findAllWithUsers().stream()
                    .filter(g -> g.getName().toLowerCase().contains(filterText.toLowerCase()))
                    .toList();
        }
    }

    /**
     * Métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public Optional<Group> findById(Long id) {
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return repository.findByIdWithUsers(id);
    }

    @Transactional
    /**
     * Métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public Group save(Group group) {
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (group == null) {
            /*
             * Retorna o resultado final processado pelo métodx.
             */
            return null;
        }
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return repository.save(group);
    }

    @Transactional
    /**
     * Métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public void delete(Group group) {
        repository.delete(group);
    }

    /**
     * Métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public long count() {
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return repository.count();
    }
}
