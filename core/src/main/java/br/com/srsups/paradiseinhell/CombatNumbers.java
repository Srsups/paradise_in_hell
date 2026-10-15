package br.com.srsups.paradiseinhell;

import java.util.Locale;

public final class CombatNumbers {
    private CombatNumbers() {}
    public static String formatRate(float value) {
        String text = String.format(Locale.ROOT, "%.2f", value);
        while (text.endsWith("0")) text = text.substring(0, text.length() - 1);
        return text.endsWith(".") ? text.substring(0, text.length() - 1) : text;
    }
    public static String format(float value) {
        return value == (int) value ? String.valueOf((int) value) : String.format(Locale.ROOT, "%.1f", value);
    }
}
