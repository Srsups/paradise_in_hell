package br.com.srsups.paradiseinhell;

import java.util.Random;
import java.util.random.RandomGenerator;
import com.badlogic.gdx.utils.JsonValue;

/** One blacksmith draw per run. Region material eligibility persists through room changes. */
public final class RunEncounters {
    public static final float BLACKSMITH_CHANCE = 0.03f, MATERIAL_CHANCE = 0.015f;
    public static final float FARMER_CHANCE = .10f;
    public static final float MINI_BOSS_TIME = 45;
    private final RandomGenerator random;
    private boolean blacksmithScheduled;
    private final float luckMultiplier;
    private boolean blacksmithVisited, blacksmithOpen, miniBossSpawned, miniBossDefeated, materialDropped;
    private boolean farmerRolled, farmerScheduled, farmerVisited, farmerOpen;
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
    public float farmerChance() { return Luck.chance(FARMER_CHANCE, luckMultiplier); }
    /** Lazy and independent of the material RNG; retries and room changes cannot reroll. */
    public boolean openFarmer(boolean eligible, RandomGenerator encounterRandom) {
        if (!eligible || farmerVisited) return false;
        if (!farmerRolled) {
            farmerRolled = true;
            farmerScheduled = encounterRandom.nextFloat() < farmerChance();
        }
        if (!farmerScheduled) return false;
        farmerVisited = farmerOpen = true;
        return true;
    }
    public boolean farmerOpen() { return farmerOpen; }
    public void closeFarmer() { farmerOpen = false; }
    public JsonValue checkpointState(){
        JsonValue value=new JsonValue(JsonValue.ValueType.object);
        SnapshotJson.put(value,"blacksmithScheduled",blacksmithScheduled);SnapshotJson.put(value,"blacksmithVisited",blacksmithVisited);
        SnapshotJson.put(value,"blacksmithOpen",blacksmithOpen);SnapshotJson.put(value,"miniBossSpawned",miniBossSpawned);
        SnapshotJson.put(value,"miniBossDefeated",miniBossDefeated);SnapshotJson.put(value,"materialDropped",materialDropped);
        SnapshotJson.put(value,"farmerRolled",farmerRolled);SnapshotJson.put(value,"farmerScheduled",farmerScheduled);
        SnapshotJson.put(value,"farmerVisited",farmerVisited);SnapshotJson.put(value,"farmerOpen",farmerOpen);
        SnapshotJson.put(value,"luck",luckMultiplier);value.addChild("random",RunRandom.checkpoint(random));return value;
    }
    public void restoreCheckpointState(JsonValue value){
        SnapshotJson.object(value,"blacksmithScheduled","blacksmithVisited","blacksmithOpen","miniBossSpawned","miniBossDefeated",
            "materialDropped","farmerRolled","farmerScheduled","farmerVisited","farmerOpen","luck","random");
        if(SnapshotJson.nonNegative(value,"luck")!=luckMultiplier)throw SnapshotJson.invalid("sorte");
        blacksmithScheduled=SnapshotJson.flag(value,"blacksmithScheduled");blacksmithVisited=SnapshotJson.flag(value,"blacksmithVisited");
        blacksmithOpen=SnapshotJson.flag(value,"blacksmithOpen");miniBossSpawned=SnapshotJson.flag(value,"miniBossSpawned");
        miniBossDefeated=SnapshotJson.flag(value,"miniBossDefeated");materialDropped=SnapshotJson.flag(value,"materialDropped");
        farmerRolled=SnapshotJson.flag(value,"farmerRolled");farmerScheduled=SnapshotJson.flag(value,"farmerScheduled");
        farmerVisited=SnapshotJson.flag(value,"farmerVisited");farmerOpen=SnapshotJson.flag(value,"farmerOpen");
        if(blacksmithVisited&&!blacksmithScheduled||blacksmithOpen&&!blacksmithVisited||miniBossDefeated&&!miniBossSpawned
            ||farmerScheduled&&!farmerRolled||farmerVisited&&!farmerScheduled||farmerOpen&&!farmerVisited)throw SnapshotJson.invalid("encontros");
        RunRandom restored=RunRandom.fromCheckpointState(SnapshotJson.required(value,"random"));
        if(random instanceof RunRandom resumable)resumable.restoreState(restored.state());
    }
}
