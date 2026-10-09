package br.com.srsups.paradiseinhell;

/** Shared chance scaling: guaranteed rewards and permanent region caps are unchanged. */
public final class Luck {
    private Luck() {}
    public static float chance(float baseChance, float multiplier) {
        if (!Float.isFinite(baseChance) || baseChance < 0 || baseChance > 1
            || !Float.isFinite(multiplier) || multiplier < 1) throw new IllegalArgumentException("Invalid luck");
        return Math.min(1, baseChance * multiplier);
    }
}
