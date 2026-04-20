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

    /**
     * Valida se a senha fornecida atende a todos os critérios de segurança configurados.
     *
     * @param password A senha em texto plano a ser validada.
     * @param context Contexto do validador contendo informações adicionais.
     * @return true se a senha for válida (ou vazia/nula), false se não cumprir os requisitos.
     */
    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        /*
         * No Vaadin, o binder pode passar nulo ou vazio se o campo não foi preenchido.
         * Nestes casos, consideramos válido para não conflitar com anotações de @NotBlank ou @NotNull.
         */
        if (password == null || password.isEmpty())
            return true;
            
        /*
         * Verifica se o comprimento da senha é menor que o mínimo exigido.
         */
        if (password.length() < minLength)
            return false;

        /*
         * Verifica se a senha possui pelo menos uma letra maiúscula usando expressão regular,
         * caso a exigência esteja ativa.
         */
        if (requireUppercase && !password.matches(".*[A-Z].*"))
            return false;

        /*
         * Verifica se a senha possui pelo menos uma letra minúscula, se exigido.
         */
        if (requireLowercase && !password.matches(".*[a-z].*"))
            return false;

        /*
         * Verifica se a senha contém pelo menos um dígito numérico, se exigido.
         */
        if (requireDigit && !password.matches(".*\\d.*"))
            return false;

        /*
         * Verifica se a senha possui pelo menos um caractere especial dentre os listados, se exigido.
         */
        if (requireSpecial && !password.matches(".*[@$!%*?&].*"))
            return false;

        /*
         * Se a senha passar por todas as verificações ativas, ela é considerada válida.
         */
        return true;
    }
}
