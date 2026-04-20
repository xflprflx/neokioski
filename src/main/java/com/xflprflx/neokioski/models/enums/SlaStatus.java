package com.xflprflx.neokioski.models.enums;

/**
 * Enumeração que define o status em relação ao tempo estipulado no SLA (No prazo, Atrasado, etc).
 */
public enum SlaStatus {
    ON_TIME("No Prazo"),
    DELAYED("Atrasado");

    private final String description;

    SlaStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
