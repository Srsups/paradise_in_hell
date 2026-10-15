package br.com.srsups.paradiseinhell;

/** Completion rewards belong to content, not room IDs. */
public enum ArenaReward {
    NONE(""),
    RECOVERY_CHEST("Baú garantido: escolha cura ou óbolos"),
    RARE_BOON("Dádiva extra: Rara ou superior");
    public final String description;
    ArenaReward(String description) { this.description = description; }
}
