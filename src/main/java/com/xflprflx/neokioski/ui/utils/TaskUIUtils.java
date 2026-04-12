package com.xflprflx.neokioski.ui.utils;

import com.vaadin.flow.component.html.Span;

public class TaskUIUtils {

    public static String[] getThemeColors(String theme) {
        if (theme == null) theme = "gray";
        
        String bg, fg;
        boolean isDark = false;
        switch (theme.toLowerCase()) {
            case "blue": bg = "#e3f2fd"; fg = "#1565c0"; break;
            case "yellow": bg = "#fff9c4"; fg = "#dc6d00"; break;
            case "green": bg = "#e8f5e9"; fg = "#2e7d32"; break;
            case "red": bg = "#ffebee"; fg = "#c62828"; break;
            case "purple": bg = "#f3e5f5"; fg = "#7b1fa2"; break;
            case "orange": bg = "#fff3e0"; fg = "#ef6c00"; break;
            case "cyan": bg = "#e0f7fa"; fg = "#00838f"; break;
            case "pink": bg = "#fce4ec"; fg = "#c2185b"; break;
            case "gray": bg = "#f5f5f5"; fg = "#616161"; break;
            case "black": bg = "#212121"; fg = "#ffffff"; isDark = true; break;
            default:
                bg = "var(--lumo-contrast-5pct)";
                fg = "var(--lumo-contrast-80pct)";
        }
        return new String[]{bg, fg, String.valueOf(isDark)};
    }

    public static String getThemeColorRGB(String theme) {
        if (theme == null) theme = "gray";
        return switch (theme.toLowerCase()) {
            case "blue" -> "21, 101, 192";
            case "yellow" -> "220, 109, 0";
            case "green" -> "46, 125, 50";
            case "red" -> "198, 40, 40";
            case "purple" -> "123, 31, 162";
            case "orange" -> "239, 108, 0";
            case "cyan" -> "0, 131, 143";
            case "pink" -> "194, 24, 91";
            case "gray" -> "97, 97, 97";
            case "black" -> "255, 255, 255";
            default -> "110, 110, 110";
        };
    }

    public static String resolveTemplateEmoji(String templateName) {
        if (templateName == null) return "📋";
        String lower = templateName.toLowerCase();
        if (lower.contains("bug")) return "🐛";
        if (lower.contains("feature")) return "⭐";
        if (lower.contains("review")) return "🔄";
        if (lower.contains("meeting")) return "📄";
        if (lower.contains("design")) return "🎨";
        if (lower.contains("qa")) return "🔍";
        return "📋";
    }

    public static void applyThemeStyles(Span span, String theme) {
        applyThemeStyles((com.vaadin.flow.component.HasStyle) span, theme);
    }

    public static void applyThemeStyles(com.vaadin.flow.component.HasStyle component, String theme) {
        String[] colors = getThemeColors(theme);
        component.getStyle().set("background-color", colors[1]);
        if (theme != null && theme.equalsIgnoreCase("black")) {
            component.getStyle().set("color", "#212121");
        } else {
            component.getStyle().set("color", "#ffffff");
        }
    }
}
