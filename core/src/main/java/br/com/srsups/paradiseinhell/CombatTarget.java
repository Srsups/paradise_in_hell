package br.com.srsups.paradiseinhell;

/** Shared live target for weapons, status effects and resurrected allies. */
public interface CombatTarget {
    float centerX();
    float centerY();
    float health();
    float maxHealth();
    float strength();
    void hurt(float amount);
    default boolean dead() { return health() <= 0; }
    default boolean targetable() { return !dead(); }
    default boolean floorBoss() { return false; }
    default void push(float dx, float dy, CombatEffects.LineOfSight line) { }
}

