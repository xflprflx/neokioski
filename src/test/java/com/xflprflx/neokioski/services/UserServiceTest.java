package com.xflprflx.neokioski.services;

import com.xflprflx.neokioski.models.User;
import com.xflprflx.neokioski.repositories.UserRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários completos para UserService.
 * Cobre: CRUD de usuários, filtragem com Hibernate Session, segurança de senhas,
 * anonimização (LGPD) e geração de senha temporária.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Test: UserService — Gestão Completa de Usuários")
public class UserServiceTest {

    @Mock
    private UserRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private UserService userService;

    // =====================================================================
    // findAll
    // =====================================================================

    @Test
    @DisplayName("findAll() sem filtro habilita o filtro de deletados e busca todos")
    void findAll_noFilter_enablesDeletedFilterAndFetchesAll() {
        Session session = setupSessionMock();
        User user = new User();
        when(repository.findAll()).thenReturn(List.of(user));

        List<User> result = userService.findAll();

        assertEquals(1, result.size());
        verify(session).enableFilter("deletedUserFilter");
        verify(session, atLeast(1)).disableFilter("deletedUserFilter");
    }

    @Test
    @DisplayName("findAll(filter) com texto de busca usa o método do repositório por nome/username")
    void findAll_withFilter_usesNameOrUsernameQuery() {
        Session session = setupSessionMock();
        User user = new User();
        when(repository.findByNameContainingIgnoreCaseOrUsernameContainingIgnoreCase("ana", "ana"))
                .thenReturn(List.of(user));

        List<User> result = userService.findAll("ana");

        assertEquals(1, result.size());
        verify(session).enableFilter(anyString());
    }

    @Test
    @DisplayName("findAll(null, true) com showDeleted=true desabilita o filtro e retorna todos incluindo deletados")
    void findAll_showDeleted_disablesFilterAndReturnsAll() {
        Session session = mock(Session.class);
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        User user = new User();
        when(repository.findAll()).thenReturn(List.of(user));

        List<User> result = userService.findAll(null, true);

        assertEquals(1, result.size());
        verify(session, never()).enableFilter(anyString());
        // disableFilter é chamado no else-branch e também no finally-block
        verify(session, atLeast(1)).disableFilter("deletedUserFilter");
    }

    @Test
    @DisplayName("findAll(filter, true) com filtro e showDeleted busca por nome incluindo deletados")
    void findAll_withFilterAndShowDeleted_queriesWithFilter() {
        Session session = mock(Session.class);
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        User user = new User();
        when(repository.findByNameContainingIgnoreCaseOrUsernameContainingIgnoreCase("jo", "jo"))
                .thenReturn(List.of(user));

        List<User> result = userService.findAll("jo", true);

        assertEquals(1, result.size());
        verify(session, never()).enableFilter(anyString());
    }

    // =====================================================================
    // findById / findAllById
    // =====================================================================

    @Test
    @DisplayName("findById delega ao repositório")
    void findById_delegatesToRepository() {
        User user = new User();
        when(repository.findById(1L)).thenReturn(Optional.of(user));

        Optional<User> result = userService.findById(1L);

        assertTrue(result.isPresent());
        verify(repository).findById(1L);
    }

    @Test
    @DisplayName("findAllById delega ao repositório")
    void findAllById_delegatesToRepository() {
        User user = new User();
        when(repository.findAllById(List.of(1L, 2L))).thenReturn(List.of(user));

        List<User> result = userService.findAllById(List.of(1L, 2L));

        assertEquals(1, result.size());
        verify(repository).findAllById(List.of(1L, 2L));
    }

    // =====================================================================
    // save
    // =====================================================================

    @Test
    @DisplayName("Preservar senha antiga se a nova estiver nula (Formulário Vaadin)")
    void shouldRestoreOldPasswordIfNewOneIsNull() {
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setPassword("OLD_HASH");

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setPassword(null);

        when(repository.findById(1L)).thenReturn(Optional.of(existingUser));

        userService.save(updatedUser);

        assertEquals("OLD_HASH", updatedUser.getPassword());
        verify(passwordEncoder, never()).encode(any());
        verify(repository).save(updatedUser);
    }

    @Test
    @DisplayName("Criptografar nova senha em texto plano antes de salvar")
    void shouldEncodeNewPasswordIfProvided() {
        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setPassword("new_password");

        when(passwordEncoder.encode("new_password")).thenReturn("NEW_HASH");

        userService.save(updatedUser);

        assertEquals("NEW_HASH", updatedUser.getPassword());
        verify(repository).save(updatedUser);
        verify(repository, never()).findById(any());
    }

    @Test
    @DisplayName("Não re-criptografar se a senha já for um Hash BCrypt válido")
    void shouldNotReencodeIfAlreadyABcryptHash() {
        User user = new User();
        user.setId(1L);
        user.setPassword("$2a$10$abcdefghij");

        userService.save(user);

        assertEquals("$2a$10$abcdefghij", user.getPassword());
        verify(passwordEncoder, never()).encode(any());
        verify(repository).save(user);
    }

    @Test
    @DisplayName("save com $2b$ (BCrypt variante) não re-criptografa")
    void save_bcrypt2bVariant_doesNotReencode() {
        User user = new User();
        user.setId(1L);
        user.setPassword("$2b$10$abcdefghij");

        userService.save(user);

        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("save com $2y$ (BCrypt variante) não re-criptografa")
    void save_bcrypt2yVariant_doesNotReencode() {
        User user = new User();
        user.setId(1L);
        user.setPassword("$2y$10$abcdefghij");

        userService.save(user);

        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("save com usuário nulo retorna sem persistir")
    void save_nullUser_doesNothing() {
        userService.save(null);

        verifyNoInteractions(repository);
    }

    // =====================================================================
    // delete
    // =====================================================================

    @Test
    @DisplayName("delete delega ao repositório")
    void delete_delegatesToRepository() {
        User user = new User();

        userService.delete(user);

        verify(repository).delete(user);
    }

    // =====================================================================
    // anonymizeUser (LGPD)
    // =====================================================================

    @Test
    @DisplayName("anonymizeUser com usuário nulo retorna imediatamente")
    void anonymizeUser_nullUser_doesNothing() {
        userService.anonymizeUser(null);

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("anonymizeUser com usuário sem ID retorna imediatamente")
    void anonymizeUser_userWithNullId_doesNothing() {
        userService.anonymizeUser(new User());

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("anonymizeUser anonimiza dados sensíveis do usuário conforme LGPD")
    void anonymizeUser_validUser_anonymizesSensitiveData() {
        User user = new User();
        user.setId(1L);
        user.setName("João Silva");
        user.setUsername("joao.silva");

        jakarta.persistence.Query nativeQuery = mock(jakarta.persistence.Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(anyString(), any())).thenReturn(nativeQuery);

        userService.anonymizeUser(user);

        assertEquals("USUÁRIO ANONIMIZADO", user.getName());
        assertTrue(user.getUsername().startsWith("removido_"));
        assertEquals("N/A", user.getPassword());
        assertFalse(user.isActive());
        assertTrue(user.isDeleted());
        verify(repository).save(user);
    }

    // =====================================================================
    // updateNotifyStoreOrder / updateNotifyChatSounds / updateLastSeen
    // =====================================================================

    @Test
    @DisplayName("Atualizar preferências de notificação (Patching)")
    void shouldCallRepositoryUpdateMethodsForPatch() {
        userService.updateNotifyStoreOrder(1L, true);
        userService.updateNotifyChatSounds(1L, false);

        verify(repository).updateNotifyStoreOrder(1L, true);
        verify(repository).updateNotifyChatSounds(1L, false);
    }

    @Test
    @DisplayName("updateLastSeen delega ao repositório com o timestamp fornecido")
    void updateLastSeen_delegatesToRepository() {
        LocalDateTime now = LocalDateTime.now();

        userService.updateLastSeen(1L, now);

        verify(repository).updateLastSeen(1L, now);
    }

    // =====================================================================
    // isPasswordValid
    // =====================================================================

    @Test
    @DisplayName("isPasswordValid com usuário nulo retorna false")
    void isPasswordValid_nullUser_returnsFalse() {
        assertFalse(userService.isPasswordValid(null, "senha"));
    }

    @Test
    @DisplayName("isPasswordValid com senha nula retorna false")
    void isPasswordValid_nullPassword_returnsFalse() {
        assertFalse(userService.isPasswordValid(new User(), null));
    }

    @Test
    @DisplayName("isPasswordValid com senha vazia retorna false")
    void isPasswordValid_emptyPassword_returnsFalse() {
        assertFalse(userService.isPasswordValid(new User(), ""));
    }

    @Test
    @DisplayName("isPasswordValid com credenciais corretas delega ao PasswordEncoder")
    void isPasswordValid_validCredentials_delegatesToPasswordEncoder() {
        User user = new User();
        user.setPassword("HASHED");
        when(passwordEncoder.matches("minha_senha", "HASHED")).thenReturn(true);

        assertTrue(userService.isPasswordValid(user, "minha_senha"));
        verify(passwordEncoder).matches("minha_senha", "HASHED");
    }

    @Test
    @DisplayName("isPasswordValid com senha incorreta retorna false")
    void isPasswordValid_wrongPassword_returnsFalse() {
        User user = new User();
        user.setPassword("HASHED");
        when(passwordEncoder.matches("errada", "HASHED")).thenReturn(false);

        assertFalse(userService.isPasswordValid(user, "errada"));
    }

    // =====================================================================
    // changePassword
    // =====================================================================

    @Test
    @DisplayName("changePassword com usuário encontrado criptografa e persiste a nova senha")
    void changePassword_userFound_encodesAndSaves() {
        User user = new User();
        user.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("nova_senha")).thenReturn("NOVA_HASH");

        userService.changePassword(1L, "nova_senha");

        assertEquals("NOVA_HASH", user.getPassword());
        verify(repository).save(user);
    }

    @Test
    @DisplayName("changePassword com usuário não encontrado não faz nada")
    void changePassword_userNotFound_doesNothing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        userService.changePassword(99L, "senha");

        verify(repository, never()).save(any());
    }

    // =====================================================================
    // generateTemporaryPassword
    // =====================================================================

    @Test
    @DisplayName("generateTemporaryPassword retorna senha de 8 caracteres")
    void generateTemporaryPassword_returnsEightCharPassword() {
        User user = new User();
        user.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenReturn("HASH");

        String tempPass = userService.generateTemporaryPassword(1L);

        assertEquals(8, tempPass.length());
    }

    @Test
    @DisplayName("generateTemporaryPassword gera senha com pelo menos um caractere de cada tipo exigido")
    void generateTemporaryPassword_passwordMeetsComplexityRequirements() {
        User user = new User();
        user.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenReturn("HASH");

        String tempPass = userService.generateTemporaryPassword(1L);

        assertTrue(tempPass.matches(".*[A-Z].*"), "Deve conter maiúscula: " + tempPass);
        assertTrue(tempPass.matches(".*[a-z].*"), "Deve conter minúscula: " + tempPass);
        assertTrue(tempPass.matches(".*[0-9].*"), "Deve conter dígito: " + tempPass);
        assertTrue(tempPass.matches(".*[@$!%*?&].*"), "Deve conter especial: " + tempPass);
    }

    @Test
    @DisplayName("generateTemporaryPassword persiste a senha temporária via changePassword")
    void generateTemporaryPassword_callsChangePassword() {
        User user = new User();
        user.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenReturn("HASH");

        userService.generateTemporaryPassword(1L);

        verify(repository).save(any(User.class));
    }

    // =====================================================================
    // Auxiliar
    // =====================================================================

    private Session setupSessionMock() {
        Session session = mock(Session.class);
        Filter filter = mock(Filter.class);
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.enableFilter(anyString())).thenReturn(filter);
        when(filter.setParameter(anyString(), any())).thenReturn(filter);
        return session;
    }
}