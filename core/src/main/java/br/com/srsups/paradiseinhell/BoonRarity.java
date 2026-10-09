package br.com.srsups.paradiseinhell;

/** Inherent power tier; separate from the number of times a boon was acquired. */
public enum BoonRarity {
    COMMON("Comum", 70, 1, 1, 1),
    RARE("Rara", 23, 0.35f, 0.85f, 1),
    EPIC("Épica", 6, 0.85f, 0.5f, 1),
    MYTHOLOGICAL("Mitológica", 1, 1, 0.8f, 0.2f);
    public final String label;
    public final float baseWeight, red, green, blue;
    BoonRarity(String label, float baseWeight, float red, float green, float blue) {
        this.label = label; this.baseWeight = baseWeight; this.red = red; this.green = green; this.blue = blue;
    }
    public float weight(float luck) {
        if (!Float.isFinite(luck) || luck < 1) throw new IllegalArgumentException("Invalid luck");
        return baseWeight * (float) Math.pow(luck, ordinal());
    }
}
