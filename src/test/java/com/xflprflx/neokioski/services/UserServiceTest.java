package com.xflprflx.neokioski.services;

import com.xflprflx.neokioski.models.User;
import com.xflprflx.neokioski.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    public void shouldRestoreOldPasswordIfNewOneIsNull() {
        // Arrange
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setPassword("OLD_HASH");

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setPassword(null); // Emulated form behavior when not changing password

        when(repository.findById(1L)).thenReturn(Optional.of(existingUser));

        // Act
        userService.save(updatedUser);

        // Assert
        assertEquals("OLD_HASH", updatedUser.getPassword());
        verify(passwordEncoder, never()).encode(any());
        verify(repository).save(updatedUser);
    }

    @Test
    public void shouldEncodeNewPasswordIfProvided() {
        // Arrange
        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setPassword("new_password");

        when(passwordEncoder.encode("new_password")).thenReturn("NEW_HASH");

        // Act
        userService.save(updatedUser);

        // Assert
        assertEquals("NEW_HASH", updatedUser.getPassword());
        verify(repository).save(updatedUser);
        // Should not have called findById because password was not null/empty
        verify(repository, never()).findById(any());
    }

    @Test
    public void shouldNotReencodeIfAlreadyABcryptHash() {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setPassword("$2a$10$abcdefghij"); // Looks like a BCrypt hash

        // Act
        userService.save(user);

        // Assert
        assertEquals("$2a$10$abcdefghij", user.getPassword());
        verify(passwordEncoder, never()).encode(any());
        verify(repository).save(user);
    }

    @Test
    public void shouldCallRepositoryUpdateMethodsForPatch() {
        // Act
        userService.updateNotifyStoreOrder(1L, true);
        userService.updateNotifyChatSounds(1L, false);

        // Assert
        verify(repository).updateNotifyStoreOrder(1L, true);
        verify(repository).updateNotifyChatSounds(1L, false);
    }
}
