package com.xflprflx.neokioski.models.template;

public enum InputType {
    TEXT(1, "Texto"),
    NUMBER(2, "Número"),
    RADIO(3, "Radio Button"),
    CHECKBOX(4, "Checkbox"),
    DATE(5, "Data"),
    TIME(6, "Hora"),
    DROPDOWN(7, "Menu Suspenso"),
    TEXT_LIST(8, "Lista de Texto (Chips)"),
    MULTISELECT_DROPDOWN(9, "Menu Suspenso Múltiplo");

    private int code;
    private String label;

    InputType(int code, String label) {
        this.code = code;
        this.label = label;
    }
}
