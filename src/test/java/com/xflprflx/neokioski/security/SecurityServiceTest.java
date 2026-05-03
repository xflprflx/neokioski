package com.xflprflx.neokioski.security;

import com.xflprflx.neokioski.models.User;
import com.xflprflx.neokioski.repositories.UserRepository;
import com.xflprflx.neokioski.services.PresenceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Test: SecurityService — Contexto de Autenticação")
class SecurityServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PresenceService presenceService;

    @InjectMocks
    private SecurityService securityService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getAuthenticatedUser - principal é UserDetails retorna Optional preenchido")
    void getAuthenticatedUser_principalIsUserDetails_returnsPresent() {
        UserDetails mockUser = mock(UserDetails.class);
        setSecurityContext(mockUser);

        Optional<UserDetails> result = securityService.getAuthenticatedUser();

        assertTrue(result.isPresent());
        assertEquals(mockUser, result.get());
    }

    @Test
    @DisplayName("getAuthenticatedUser - principal não é UserDetails retorna Optional vazio")
    void getAuthenticatedUser_principalIsNotUserDetails_returnsEmpty() {
        setSecurityContext("anonymous");

        Optional<UserDetails> result = securityService.getAuthenticatedUser();

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getAuthenticatedInternalUser - usuário encontrado no repositório retorna Optional preenchido")
    void getAuthenticatedInternalUser_userFoundInRepository_returnsUser() {
        UserDetails mockUserDetails = mock(UserDetails.class);
        when(mockUserDetails.getUsername()).thenReturn("testuser");
        setSecurityContext(mockUserDetails);

        User internalUser = new User();
        internalUser.setId(1L);
        internalUser.setUsername("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(internalUser));

        Optional<User> result = securityService.getAuthenticatedInternalUser();

        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
    }

    @Test
    @DisplayName("getAuthenticatedInternalUser - sem autenticação retorna Optional vazio")
    void getAuthenticatedInternalUser_notAuthenticated_returnsEmpty() {
        setSecurityContext("not-a-user-details-object");

        Optional<User> result = securityService.getAuthenticatedInternalUser();

        assertTrue(result.isEmpty());
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("getAuthenticatedInternalUser - usuário não encontrado no repositório retorna Optional vazio")
    void getAuthenticatedInternalUser_userNotFoundInRepository_returnsEmpty() {
        UserDetails mockUserDetails = mock(UserDetails.class);
        when(mockUserDetails.getUsername()).thenReturn("desconhecido");
        setSecurityContext(mockUserDetails);

        when(userRepository.findByUsername("desconhecido")).thenReturn(Optional.empty());

        Optional<User> result = securityService.getAuthenticatedInternalUser();

        assertTrue(result.isEmpty());
    }

    private void setSecurityContext(Object principal) {
        SecurityContext ctx = new SecurityContextImpl();
        ctx.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null));
        SecurityContextHolder.setContext(ctx);
    }
}