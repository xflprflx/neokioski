package com.xflprflx.neokioski.models.enums;

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
