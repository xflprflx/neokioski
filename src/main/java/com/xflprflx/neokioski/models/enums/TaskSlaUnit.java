package com.xflprflx.neokioski.models.enums;

/**
 * Enumeração que especifica as unidades de tempo utilizadas para configurar o SLA (Minutos, Horas, Dias).
 */
public enum TaskSlaUnit {
    SECONDS("Segundos"),
    MINUTES("Minutos"),
    HOURS("Horas");

    private final String label;

    TaskSlaUnit(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
