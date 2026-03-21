package com.xflprflx.neokioski.services;

import com.xflprflx.neokioski.models.Group;
import com.xflprflx.neokioski.repositories.GroupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class GroupService {

    private final GroupRepository repository;

    public GroupService(GroupRepository repository) {
        this.repository = repository;
    }

    public List<Group> findAll() {
        return repository.findAllWithUsers();
    }

    public List<Group> findAll(String filterText) {
        if (filterText == null || filterText.isEmpty()) {
            return repository.findAllWithUsers();
        } else {
            // Se houver necessidade de filtro customizado no repositório, podemos adicionar depois.
            // Por enquanto, faremos o filtro simples se necessário ou retornaremos tudo.
            return repository.findAllWithUsers().stream()
                    .filter(g -> g.getName().toLowerCase().contains(filterText.toLowerCase()))
                    .toList();
        }
    }

    public Optional<Group> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public Group save(Group group) {
        if (group == null) {
            return null;
        }
        return repository.save(group);
    }

    @Transactional
    public void delete(Group group) {
        repository.delete(group);
    }

    public long count() {
        return repository.count();
    }
}
