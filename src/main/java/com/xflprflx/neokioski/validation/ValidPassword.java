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

    int minLength() default 6;

    boolean requireUppercase() default true;

    boolean requireLowercase() default true;

    boolean requireDigit() default true;

    boolean requireSpecial() default true;
}
