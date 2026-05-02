package com.xflprflx.neokioski.services;

import com.xflprflx.neokioski.models.User;
import com.xflprflx.neokioski.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Classe de testes unitários para o serviço de usuários (UserService).
 * Utiliza o Mockito para simular dependências e isolar as validações de regra de negócio, principalmente focando nas lógicas de senhas.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Test: Gestão de Usuários e Segurança de Senhas")
public class UserServiceTest {

    @Mock
    private UserRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    /**
     * Teste que valida o comportamento do serviço ao salvar um usuário sem alterar a senha.
     * Deve garantir que a senha original (hash antigo) será preservada no objeto antes da persistência.
     */
    @Test
    @DisplayName("Preservar senha antiga se a nova estiver nula (Formulário Vaadin)")
    public void shouldRestoreOldPasswordIfNewOneIsNull() {
        /*
         * Preparação do cenário de teste (Arrange): configuração de mocks, variáveis e dados de entrada necessários para a execução.
         */
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setPassword("OLD_HASH");

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setPassword(null); /* Simulando o comportamento de um formulário Vaadin onde a senha em branco ou nula não deve sobrescrever a atual. */

        when(repository.findById(1L)).thenReturn(Optional.of(existingUser));

        /*
         * Ação (Act): execução do métodx principal ou comportamento em teste utilizando os dados preparados.
         */
        userService.save(updatedUser);

        /*
         * Verificação (Assert): validação dos resultados, confirmação de valores alterados e verificação de interações dos mocks (ex: métodxs chamados).
         */
        assertEquals("OLD_HASH", updatedUser.getPassword());
        verify(passwordEncoder, never()).encode(any());
        verify(repository).save(updatedUser);
    }

    /**
     * Teste que verifica se uma nova senha inserida em texto plano será corretamente
     * criptografada (encode) com o PasswordEncoder antes de ser salva no banco.
     */
    @Test
    @DisplayName("Criptografar nova senha em texto plano antes de salvar")
    public void shouldEncodeNewPasswordIfProvided() {
        /*
         * Preparação do cenário de teste (Arrange): configuração de mocks, variáveis e dados de entrada necessários para a execução.
         */
        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setPassword("new_password");

        when(passwordEncoder.encode("new_password")).thenReturn("NEW_HASH");

        /*
         * Ação (Act): execução do métodx principal ou comportamento em teste utilizando os dados preparados.
         */
        userService.save(updatedUser);

        /*
         * Verificação (Assert): validação dos resultados, confirmação de valores alterados e verificação de interações dos mocks (ex: métodxs chamados).
         */
        assertEquals("NEW_HASH", updatedUser.getPassword());
        verify(repository).save(updatedUser);
        /* Verifica se o repositório NÃO foi chamado para buscar os dados antigos do usuário, pois a nova senha já foi preenchida, não havendo necessidade. */
        verify(repository, never()).findById(any());
    }

    /**
     * Teste que assegura que uma senha já criptografada (que inicie com a assinatura do BCrypt)
     * não será criptografada novamente ao passar pelo métodx de salvamento.
     */
    @Test
    @DisplayName("Não re-criptografar se a senha já for um Hash BCrypt válido")
    public void shouldNotReencodeIfAlreadyABcryptHash() {
        /*
         * Preparação do cenário de teste (Arrange): configuração de mocks, variáveis e dados de entrada necessários para a execução.
         */
        User user = new User();
        user.setId(1L);
        user.setPassword("$2a$10$abcdefghij"); /* Simula um formato característico gerado pelo algoritmo BCrypt, que não necessita ser re-encodado. */

        /*
         * Ação (Act): execução do métodx principal ou comportamento em teste utilizando os dados preparados.
         */
        userService.save(user);

        /*
         * Verificação (Assert): validação dos resultados, confirmação de valores alterados e verificação de interações dos mocks (ex: métodxs chamados).
         */
        assertEquals("$2a$10$abcdefghij", user.getPassword());
        verify(passwordEncoder, never()).encode(any());
        verify(repository).save(user);
    }

    /**
     * Teste para validar que o serviço de usuário repassa corretamente as atualizações pontuais
     * de flags de notificação (PATCH) para o repositório.
     */
    @Test
    @DisplayName("Atualizar preferências de notificação (Patching)")
    public void shouldCallRepositoryUpdateMethodsForPatch() {
        /*
         * Ação (Act): execução do métodx principal ou comportamento em teste utilizando os dados preparados.
         */
        userService.updateNotifyStoreOrder(1L, true);
        userService.updateNotifyChatSounds(1L, false);

        /*
         * Verificação (Assert): validação dos resultados, confirmação de valores alterados e verificação de interações dos mocks (ex: métodxs chamados).
         */
        verify(repository).updateNotifyStoreOrder(1L, true);
        verify(repository).updateNotifyChatSounds(1L, false);
    }
}
