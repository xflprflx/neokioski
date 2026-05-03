package com.xflprflx.neokioski.models.enums;

/**
 * Enumeração que define os diferentes papéis e níveis de acesso dos usuários do sistema.
 */
public enum Role {
    ADMIN(1, "Administrador"),
    SELLER(2, "Comercial"),
    LOGISTICS(3, "Logística");

    private int code;
    private String description;

    Role(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
