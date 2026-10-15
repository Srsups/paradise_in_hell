package br.com.srsups.paradiseinhell;

import java.util.random.RandomGenerator;

/** Chosen once per combat room per attempt; all goal types retain a positive chance. */
public enum ArenaVariation {
    BALANCED("Hordas mistas", "Tipos equilibrados", 0.50f, 0.35f),
    FAST("Caçada veloz", "Mais rápidos", 0.35f, 0.50f),
    TANK("Marcha pesada", "Mais tanks", 0.40f, 0.25f);
    public final String label, description;
    private final float common, fast;
    ArenaVariation(String label, String description, float common, float fast) {
        this.label = label; this.description = description; this.common = common; this.fast = fast;
    }
    public EnemyType choose(float roll) {
        if (!Float.isFinite(roll) || roll < 0 || roll >= 1) throw new IllegalArgumentException("Invalid enemy roll");
        return roll < common ? EnemyType.COMMON : roll < common + fast ? EnemyType.FAST : EnemyType.TANK;
    }
    public EnemyType chooseWithoutFast(float roll) {
        if (!Float.isFinite(roll) || roll < 0 || roll >= 1) throw new IllegalArgumentException("Invalid enemy roll");
        return roll < common / (1 - fast) ? EnemyType.COMMON : EnemyType.TANK;
    }
    public static ArenaVariation draw(RandomGenerator random) { return values()[random.nextInt(values().length)]; }
}
