package com.xflprflx.neokioski.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para {@link PasswordConstraintValidator}.
 * Garante 100% de cobertura das regras de validação de senha.
 */
@DisplayName("Validador de Restrição de Senha - Testes de Cobertura")
class PasswordConstraintValidatorTest {

    private PasswordConstraintValidator validator;

    @Mock
    private ValidPassword validPassword;

    @Mock
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validator = new PasswordConstraintValidator();
        
        // Configuração padrão: 6 caracteres, maiúscula, minúscula, número e especial
        when(validPassword.minLength()).thenReturn(6);
        when(validPassword.requireUppercase()).thenReturn(true);
        when(validPassword.requireLowercase()).thenReturn(true);
        when(validPassword.requireDigit()).thenReturn(true);
        when(validPassword.requireSpecial()).thenReturn(true);
        
        validator.initialize(validPassword);
    }

    @Test
    @DisplayName("Deve permitir senha nula (delega para @NotNull)")
    void shouldReturnTrueWhenPasswordIsNull() {
        assertTrue(validator.isValid(null, context), "Senha nula deve ser considerada válida (delegar para @NotNull)");
    }

    @Test
    @DisplayName("Deve permitir senha vazia (delega para @NotBlank)")
    void shouldReturnTrueWhenPasswordIsEmpty() {
        assertTrue(validator.isValid("", context), "Senha vazia deve ser considerada válida (delegar para @NotBlank)");
    }

    @Test
    @DisplayName("Deve rejeitar senha com comprimento menor que o mínimo")
    void shouldReturnFalseWhenPasswordIsTooShort() {
        assertFalse(validator.isValid("Ab1!s", context), "Senha com menos de 6 caracteres deve ser inválida");
    }

    @Test
    @DisplayName("Deve rejeitar senha sem letras maiúsculas")
    void shouldReturnFalseWhenUppercaseIsMissing() {
        assertFalse(validator.isValid("abc12!", context), "Senha sem letra maiúscula deve ser inválida");
    }

    @Test
    @DisplayName("Deve rejeitar senha sem letras minúsculas")
    void shouldReturnFalseWhenLowercaseIsMissing() {
        assertFalse(validator.isValid("ABC12!", context), "Senha sem letra minúscula deve ser inválida");
    }

    @Test
    @DisplayName("Deve rejeitar senha sem dígitos numéricos")
    void shouldReturnFalseWhenDigitIsMissing() {
        assertFalse(validator.isValid("Abcdef!", context), "Senha sem dígitos deve ser inválida");
    }

    @Test
    @DisplayName("Deve rejeitar senha sem caracteres especiais")
    void shouldReturnFalseWhenSpecialCharIsMissing() {
        assertFalse(validator.isValid("Abcde1", context), "Senha sem caracteres especiais deve ser inválida");
    }

    @Test
    @DisplayName("Deve aceitar senha que atende a todos os requisitos")
    void shouldReturnTrueWhenAllRequirementsAreMet() {
        assertTrue(validator.isValid("Abcde1!", context), "Senha atendendo todos os critérios deve ser válida");
    }

    @Test
    @DisplayName("Deve respeitar requisitos desativados na configuração")
    void shouldReturnTrueWhenRequirementsAreDisabled() {
        // Reconfigurando para aceitar qualquer coisa com 3 caracteres
        when(validPassword.minLength()).thenReturn(3);
        when(validPassword.requireUppercase()).thenReturn(false);
        when(validPassword.requireLowercase()).thenReturn(false);
        when(validPassword.requireDigit()).thenReturn(false);
        when(validPassword.requireSpecial()).thenReturn(false);
        
        validator.initialize(validPassword);
        
        assertTrue(validator.isValid("abc", context));
        assertTrue(validator.isValid("123", context));
        assertTrue(validator.isValid("!!!", context));
    }

    @ParameterizedTest
    @ValueSource(strings = {"@", "$", "!", "%", "*", "?", "&"})
    @DisplayName("Deve aceitar todos os caracteres especiais permitidos individualmente")
    void shouldAcceptAllAllowedSpecialCharacters(String specialChar) {
        assertTrue(validator.isValid("Abc12" + specialChar, context), "Deve aceitar o caractere especial: " + specialChar);
    }

    @Test
    @DisplayName("Deve falhar se apenas um requisito complexo for omitido")
    void shouldReturnFalseWhenOnlyOneRequirementIsMissingInComplexScenario() {
        // Testando combinações onde quase tudo está certo
        assertFalse(validator.isValid("ABCDE1!", context), "Falta minúscula");
        assertFalse(validator.isValid("abcde1!", context), "Falta maiúscula");
        assertFalse(validator.isValid("Abcde!!", context), "Falta dígito");
        assertFalse(validator.isValid("Abcde12", context), "Falta especial");
    }
}
