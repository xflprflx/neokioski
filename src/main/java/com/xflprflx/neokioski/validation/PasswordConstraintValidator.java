package com.xflprflx.neokioski.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validador customizado para garantir a força da senha. Verifica se a senha
 * atende aos requisitos mínimos de segurança.
 */
public class PasswordConstraintValidator implements ConstraintValidator<ValidPassword, String> {

    private int minLength;
    private boolean requireUppercase;
    private boolean requireLowercase;
    private boolean requireDigit;
    private boolean requireSpecial;

    /**
     * Inicializa o validador com as propriedades da anotação {@link ValidPassword}.
     * 
     * @param constraintAnnotation A anotação {@link ValidPassword} que está sendo
     *                             processada.
     */
    @Override
    public void initialize(ValidPassword constraintAnnotation) {
        this.minLength = constraintAnnotation.minLength();
        this.requireUppercase = constraintAnnotation.requireUppercase();
        this.requireLowercase = constraintAnnotation.requireLowercase();
        this.requireDigit = constraintAnnotation.requireDigit();
        this.requireSpecial = constraintAnnotation.requireSpecial();
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        // No Vaadin, o binder pode passar nulo ou vazio se o campo não foi preenchido.
        // Nestes casos, permitimos se não for obrigatório no binder.
        if (password == null || password.isEmpty())
            return true;
            
        if (password.length() < minLength)
            return false;
        if (requireUppercase && !password.matches(".*[A-Z].*"))
            return false;
        if (requireLowercase && !password.matches(".*[a-z].*"))
            return false;
        if (requireDigit && !password.matches(".*\\d.*"))
            return false;
        if (requireSpecial && !password.matches(".*[@$!%*?&].*"))
            return false;
        return true;
    }
}
