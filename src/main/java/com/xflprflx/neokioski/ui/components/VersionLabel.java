package com.xflprflx.neokioski.ui.components;

import com.vaadin.flow.component.html.Span;

/**
 * Rótulo discreto com a versão (SHA curto do commit) em execução.
 * O valor vem da variável de ambiente APP_VERSION, injetada no build da imagem Docker.
 */
public class VersionLabel extends Span {

    private static final String VERSION = resolveVersion();

    public VersionLabel() {
        super("v " + VERSION);
        getStyle().set("font-size", "var(--lumo-font-size-xxs)");
        getStyle().set("color", "var(--lumo-tertiary-text-color)");
        getStyle().set("opacity", "0.7");
        getElement().setAttribute("title", "Versão em execução");
    }

    private static String resolveVersion() {
        String v = System.getenv("APP_VERSION");
        if (v == null || v.isBlank()) {
            return "dev";
        }
        return v.length() > 7 ? v.substring(0, 7) : v;
    }
}
