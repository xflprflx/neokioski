package com.xflprflx.neokioski.models.enums;

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
