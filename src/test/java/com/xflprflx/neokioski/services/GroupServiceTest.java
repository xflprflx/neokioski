package com.xflprflx.neokioski.services;

import com.xflprflx.neokioski.models.Group;
import com.xflprflx.neokioski.models.User;
import com.xflprflx.neokioski.repositories.GroupRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Test: GroupService")
class GroupServiceTest {

    @Mock
    private GroupRepository repository;

    @InjectMocks
    private GroupService groupService;

    @Test
    @DisplayName("findActiveGroupsByUser - usuário nulo retorna lista vazia sem tocar o repositório")
    void findActiveGroupsByUser_nullUser_returnsEmpty() {
        List<Group> result = groupService.findActiveGroupsByUser(null);

        assertTrue(result.isEmpty());
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("findActiveGroupsByUser - usuário sem ID retorna lista vazia")
    void findActiveGroupsByUser_userWithNullId_returnsEmpty() {
        User user = new User();

        List<Group> result = groupService.findActiveGroupsByUser(user);

        assertTrue(result.isEmpty());
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("findActiveGroupsByUser - usuário válido delega ao repositório")
    void findActiveGroupsByUser_validUser_delegatesToRepository() {
        User user = new User();
        user.setId(1L);
        Group group = new Group();
        when(repository.findByUsersIdAndIsActiveTrue(1L)).thenReturn(List.of(group));

        List<Group> result = groupService.findActiveGroupsByUser(user);

        assertEquals(1, result.size());
        verify(repository).findByUsersIdAndIsActiveTrue(1L);
    }

    @Test
    @DisplayName("findAll sem parâmetros retorna todos os grupos")
    void findAll_noArgs_returnsAll() {
        Group g = new Group();
        when(repository.findAllWithUsers()).thenReturn(List.of(g));

        List<Group> result = groupService.findAll();

        assertEquals(1, result.size());
        verify(repository).findAllWithUsers();
    }

    @Test
    @DisplayName("findAll com filtro nulo retorna todos os grupos")
    void findAll_nullFilter_returnsAll() {
        Group g = new Group();
        when(repository.findAllWithUsers()).thenReturn(List.of(g));

        List<Group> result = groupService.findAll(null);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("findAll com filtro vazio retorna todos os grupos")
    void findAll_emptyFilter_returnsAll() {
        Group g = new Group();
        when(repository.findAllWithUsers()).thenReturn(List.of(g));

        List<Group> result = groupService.findAll("");

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("findAll com filtro de nome retorna apenas grupos correspondentes")
    void findAll_withNameFilter_returnsMatchingGroups() {
        Group g1 = new Group();
        g1.setName("Vendas");
        Group g2 = new Group();
        g2.setName("Suporte");
        when(repository.findAllWithUsers()).thenReturn(List.of(g1, g2));

        List<Group> result = groupService.findAll("vend");

        assertEquals(1, result.size());
        assertEquals("Vendas", result.get(0).getName());
    }

    @Test
    @DisplayName("findAll com filtro sem correspondência retorna lista vazia")
    void findAll_withFilterNoMatch_returnsEmpty() {
        Group g = new Group();
        g.setName("Vendas");
        when(repository.findAllWithUsers()).thenReturn(List.of(g));

        List<Group> result = groupService.findAll("Inexistente");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("findById delega ao repositório")
    void findById_delegatesToRepository() {
        Group g = new Group();
        when(repository.findByIdWithUsers(1L)).thenReturn(Optional.of(g));

        Optional<Group> result = groupService.findById(1L);

        assertTrue(result.isPresent());
        verify(repository).findByIdWithUsers(1L);
    }

    @Test
    @DisplayName("save com grupo nulo retorna null sem persistir")
    void save_nullGroup_returnsNull() {
        Group result = groupService.save(null);

        assertNull(result);
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("save com grupo válido persiste e retorna o grupo salvo")
    void save_validGroup_savesAndReturns() {
        Group g = new Group();
        when(repository.save(g)).thenReturn(g);

        Group result = groupService.save(g);

        assertNotNull(result);
        verify(repository).save(g);
    }

    @Test
    @DisplayName("delete delega ao repositório")
    void delete_delegatesToRepository() {
        Group g = new Group();

        groupService.delete(g);

        verify(repository).delete(g);
    }

    @Test
    @DisplayName("count delega ao repositório")
    void count_delegatesToRepository() {
        when(repository.count()).thenReturn(5L);

        long count = groupService.count();

        assertEquals(5L, count);
        verify(repository).count();
    }
}