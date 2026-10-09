package br.com.srsups.paradiseinhell;

import java.util.Locale;

public final class CombatNumbers {
    private CombatNumbers() {}
    public static String format(float value) {
        return value == (int) value ? String.valueOf((int) value) : String.format(Locale.ROOT, "%.1f", value);
    }
}
