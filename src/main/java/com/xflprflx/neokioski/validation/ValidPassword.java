package com.xflprflx.neokioski.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

/**
 * Anotação para validação de senha forte. Deve conter maiúsculas, minúsculas,
 * números, caracteres especiais e tamanho mínimo.
 */
@Documented
@Constraint(validatedBy = PasswordConstraintValidator.class)
@Target({ TYPE, FIELD, ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {
    /**
     * Mensagem de erro padrão.
     */
    String message() default "A senha deve conter ao menos 6 caracteres, incluindo maiúsculas, minúsculas, números e símbolos (@$!%*?&).";

    /**
     * Grupos de validação.
     */
    Class<?>[] groups() default {};

    /**
     * Payload para metadados de validação.
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * Tamanho mínimo exigido para a senha. O padrão é 6.
     *
     * @return O comprimento mínimo da senha.
     */
    int minLength() default 6;

    /**
     * Define se a senha exige pelo menos uma letra maiúscula.
     *
     * @return true se a letra maiúscula for obrigatória, false caso contrário.
     */
    boolean requireUppercase() default true;

    /**
     * Define se a senha exige pelo menos uma letra minúscula.
     *
     * @return true se a letra minúscula for obrigatória, false caso contrário.
     */
    boolean requireLowercase() default true;

    /**
     * Define se a senha exige pelo menos um número (dígito).
     *
     * @return true se o número for obrigatório, false caso contrário.
     */
    boolean requireDigit() default true;

    /**
     * Define se a senha exige pelo menos um caractere especial (símbolo).
     *
     * @return true se o caractere especial for obrigatório, false caso contrário.
     */
    boolean requireSpecial() default true;
}
