package br.com.srsups.paradiseinhell;

import java.util.Random;
import java.util.random.RandomGenerator;

/** One blacksmith draw per run. Region material eligibility persists through room changes. */
public final class RunEncounters {
    public static final float BLACKSMITH_CHANCE = 0.03f, MATERIAL_CHANCE = 0.015f;
    public static final float MINI_BOSS_TIME = 45;
    private final RandomGenerator random;
    private final boolean blacksmithScheduled;
    private final float luckMultiplier;
    private boolean blacksmithVisited, blacksmithOpen, miniBossSpawned, miniBossDefeated, materialDropped;
    public RunEncounters() { this(new Random()); }
    public RunEncounters(RandomGenerator random) { this(random, 1); }
    public RunEncounters(RandomGenerator random, float luckMultiplier) {
        this.random = java.util.Objects.requireNonNull(random);
        this.luckMultiplier = luckMultiplier;
        blacksmithScheduled = random.nextFloat() < Luck.chance(BLACKSMITH_CHANCE, luckMultiplier);
    }
    public void blockRegionMaterial() { materialDropped = true; }
    public boolean regionMaterialClaimed() { return materialDropped; }
    public float materialChance() { return materialDropped ? 0 : Luck.chance(MATERIAL_CHANCE, luckMultiplier); }
    public float blacksmithChance() { return Luck.chance(BLACKSMITH_CHANCE, luckMultiplier); }
    public boolean miniBossDue(float roomSeconds) { return !miniBossSpawned && roomSeconds >= MINI_BOSS_TIME; }
    public void markMiniBossSpawned() {
        if (miniBossSpawned) throw new IllegalStateException("Mini-boss already spawned");
        miniBossSpawned = true;
    }
    public boolean defeatMiniBoss() {
        if (!miniBossSpawned || miniBossDefeated) throw new IllegalStateException("No live mini-boss");
        miniBossDefeated = true;
        if (!materialDropped && random.nextFloat() < Luck.chance(MATERIAL_CHANCE, luckMultiplier)) { materialDropped = true; return true; }
        return false;
    }
    public void enterRoom() { miniBossSpawned = miniBossDefeated = false; }
    public boolean openBlacksmith() {
        if (!blacksmithScheduled || blacksmithVisited) return false;
        blacksmithVisited = blacksmithOpen = true;
        return true;
    }
    public boolean blacksmithOpen() { return blacksmithOpen; }
    public void closeBlacksmith() { blacksmithOpen = false; }
}
