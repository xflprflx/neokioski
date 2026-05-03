package com.xflprflx.neokioski.ui.utils;

import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Span;
import java.util.List;

/**
 * Classe utilitária contendo métodxs auxiliares para a construção e formatação da interface de tarefas (badges de status, cores).
 */
public class TaskUIUtils {

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    public static String[] getThemeColors(String theme) {
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (theme == null) theme = "gray";
        
        String bg, fg;
        boolean isDark = false;
        switch (theme.toLowerCase()) {
            case "blue": case "azul": bg = "#e3f2fd"; fg = "#1565c0"; break;
            case "yellow": case "amarelo": bg = "#fef9c3"; fg = "#ca8a04"; break;
            case "green": case "verde": bg = "#e8f5e9"; fg = "#2e7d32"; break;
            case "red": case "vermelho": bg = "#ffebee"; fg = "#c62828"; break;
            case "purple": case "roxo": case "violeta": bg = "#f3e5f5"; fg = "#7b1fa2"; break;
            case "orange": case "laranja": bg = "#fff3e0"; fg = "#ef6c00"; break;
            case "cyan": case "ciano": bg = "#e0f7fa"; fg = "#00838f"; break;
            case "pink": case "rosa": bg = "#fce4ec"; fg = "#c2185b"; break;
            case "gray": case "cinza": bg = "#f5f5f5"; fg = "#616161"; break;
            case "black": case "preto": bg = "#212121"; fg = "#000000"; isDark = true; break;
            case "lime": case "lima": bg = "#f7fee7"; fg = "#4d7c0f"; break;
            case "indigo": bg = "#eef2ff"; fg = "#4338ca"; break;
            case "teal": case "turquesa": bg = "#f0fdfa"; fg = "#0f766e"; break;
            case "gold": case "ouro": bg = "#fefce8"; fg = "#a16207"; break;
            case "brown": case "marrom": bg = "#efebe9"; fg = "#5d4037"; break;
            default:
                bg = "var(--lumo-contrast-5pct)";
                fg = "var(--lumo-contrast-80pct)";
        }
        /*
         * Retorna o componente renderizado ou o valor da operação atual.
         */
        return new String[]{bg, fg, String.valueOf(isDark)};
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    public static String getThemeColorRGB(String theme) {
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (theme == null) theme = "gray";
        /*
         * Retorna o componente renderizado ou o valor da operação atual.
         */
        return switch (theme.toLowerCase()) {
            case "blue", "azul" -> "21, 101, 192";
            case "yellow", "amarelo" -> "202, 138, 4";
            case "green", "verde" -> "46, 125, 50";
            case "red", "vermelho" -> "198, 40, 40";
            case "purple", "roxo", "violeta" -> "123, 31, 162";
            case "orange", "laranja" -> "239, 108, 0";
            case "cyan", "ciano" -> "0, 131, 143";
            case "pink", "rosa" -> "194, 24, 91";
            case "gray", "cinza" -> "97, 97, 97";
            case "black", "preto" -> "0, 0, 0";
            case "lime", "lima" -> "77, 124, 15";
            case "indigo" -> "67, 56, 202";
            case "teal", "turquesa" -> "15, 118, 110";
            case "gold", "ouro" -> "161, 98, 7";
            case "brown", "marrom" -> "93, 64, 55";
            default -> "110, 110, 110";
        };
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    public static String resolveTemplateEmoji(String templateName) {
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (templateName == null) return "📋";
        String lower = templateName.toLowerCase();
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (lower.contains("bug")) return "🐛";
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (lower.contains("feature")) return "⭐";
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (lower.contains("review")) return "🔄";
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (lower.contains("meeting")) return "📄";
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (lower.contains("design")) return "🎨";
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (lower.contains("qa")) return "🔍";
        /*
         * Retorna o componente renderizado ou o valor da operação atual.
         */
        return "📋";
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    public static void applyThemeStyles(Span span, String theme) {
        applyThemeStyles((com.vaadin.flow.component.HasStyle) span, theme);
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    public static void applyThemeStyles(com.vaadin.flow.component.HasStyle component, String theme) {
        String[] colors = getThemeColors(theme);
        component.getStyle().set("background-color", colors[1]);
        // Se o tema for preto ou ouro (muito escuro), usa texto branco. 
        // Se for amarelo claro (bg), usaria preto, mas aqui usamos fg como fundo.
        component.getStyle().set("color", "#ffffff");
    }

    /**
     * Retorna a configuração de tradução (I18n) para DatePickers no padrão pt-BR.
     */
    public static DatePicker.DatePickerI18n getPtBrI18n() {
        DatePicker.DatePickerI18n i18n = new DatePicker.DatePickerI18n();
        i18n.setMonthNames(List.of("Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", 
                                   "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"));
        i18n.setWeekdays(List.of("Domingo", "Segunda-feira", "Terça-feira", "Quarta-feira", 
                                 "Quinta-feira", "Sexta-feira", "Sábado"));
        i18n.setWeekdaysShort(List.of("dom", "seg", "ter", "qua", "qui", "sex", "sáb"));
        i18n.setToday("Hoje");
        i18n.setCancel("Cancelar");
        return i18n;
    }
}
