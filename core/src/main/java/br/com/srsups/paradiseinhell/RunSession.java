package br.com.srsups.paradiseinhell;

import java.util.EnumMap;

/** One attempt. Arena screens own entities/resources; this object owns progression. */
public final class RunSession {
    private final RegionDefinition region;
    private final PlayerState player;
    private final RunProgress progress;
    private final PermanentBonuses permanent;
    private final RunEncounters encounters;
    private int pendingForgeMaterials;
    private final EnumMap<Upgrade, Integer> ranks = new EnumMap<>(Upgrade.class);
    private final EnumMap<EnemyType, Integer> arenaKills = new EnumMap<>(EnemyType.class);
    private String arenaId;
    private float arenaElapsed;
    private boolean arenaComplete, finished, bossDefeated, settled;
    private int completedArenas, chestObols;

    private RunSession(RegionDefinition region, Profession profession, PermanentBonuses permanent) {
        this(region, profession, permanent, new java.util.Random());
    }
    private RunSession(RegionDefinition region, Profession profession, PermanentBonuses permanent, java.util.random.RandomGenerator random) {
        this.region = region;
        encounters = new RunEncounters(random, profession.luckMultiplier);
        this.permanent = java.util.Objects.requireNonNull(permanent);
        player = new PlayerState(profession);
        permanent.apply(player);
        progress = new RunProgress(region == null);
        arenaId = region == null ? null : region.start();
    }
    public static RunSession demo() { return demo(Profession.CACADOR); }
    public static RunSession demo(Profession profession) { return new RunSession(null, profession, PermanentBonuses.NONE); }
    public static RunSession campaign(RegionDefinition region) { return campaign(region, Profession.CACADOR); }
    public static RunSession campaign(RegionDefinition region, Profession profession) {
        return campaign(region, profession, PermanentBonuses.NONE);
    }
    public static RunSession campaign(RegionDefinition region, Profession profession, PermanentBonuses permanent) {
        if (region == null) throw new IllegalArgumentException("Missing region");
        return new RunSession(region, profession, permanent);
    }
    public static RunSession campaign(RegionDefinition region, Profession profession, PermanentBonuses permanent,
                                      java.util.random.RandomGenerator random) {
        if (region == null) throw new IllegalArgumentException("Missing region");
        return new RunSession(region, profession, permanent, random);
    }
    public boolean isDemo() { return region == null; }
    public PlayerState player() { return player; }
    public RunProgress progress() { return progress; }
    public RegionDefinition region() {
        if (isDemo()) throw new IllegalStateException("Demo has no region");
        return region;
    }
    public ArenaDefinition arena() { return region().arena(arenaId); }
    public float arenaElapsed() { return arenaElapsed; }
    public RunEncounters encounters() { return encounters; }
    public boolean miniBossDue() {
        return !isDemo() && arena().kind() == ArenaDefinition.Kind.COMBAT && encounters.miniBossDue(arenaElapsed);
    }
    public void markMiniBossSpawned() {
        requireActive();
        if (!miniBossDue()) throw new IllegalStateException("Mini-boss unavailable");
        encounters.markMiniBossSpawned();
    }
    public boolean recordMiniBossDefeat() {
        requireActive();
        if (isDemo() || arena().kind() != ArenaDefinition.Kind.COMBAT) throw new IllegalStateException("Not a campaign fight");
        boolean material = encounters.defeatMiniBoss();
        progress.recordKill(); // A mini-boss never substitutes a common/fast/tank goal.
        if (material) pendingForgeMaterials++;
        return material;
    }
    public int pendingForgeMaterials() { return pendingForgeMaterials; }
    public void clearPendingForgeMaterials() { pendingForgeMaterials = 0; }
    public boolean canForgeWeapon() { return !finished && !isDemo() && encounters.blacksmithOpen() && player.canForgeWeapon(); }
    public boolean finished() { return finished; }
    /** True means access unlocked; spawns continue until the player chooses to leave. */
    public boolean arenaComplete() { return arenaComplete; }
    public int completedArenas() { return completedArenas; }
    public void advance(float delta) {
        requireActive();
        progress.advance(delta);
        if (!isDemo()) arenaElapsed += delta;
    }
    public int kills(EnemyType type) { return arenaKills.getOrDefault(type, 0); }
    public void recordKill(EnemyType type) {
        requireActive();
        if (!isDemo() && arena().kind() != ArenaDefinition.Kind.COMBAT) throw new IllegalStateException("Not a combat arena");
        progress.recordKill();
        if (isDemo()) return;
        arenaKills.merge(type, 1, Integer::sum);
        if (!arenaComplete && goalsMet()) {
            arenaComplete = true;
            completedArenas++;
        }
    }
    public boolean goalsMet() {
        return !isDemo() && arena().kind() == ArenaDefinition.Kind.COMBAT
            && arena().killGoals().entrySet().stream().allMatch(goal -> kills(goal.getKey()) >= goal.getValue());
    }
    public boolean bossDefeated() { return bossDefeated; }
    public void recordBossDefeat() {
        requireActive();
        if (isDemo() || arena().kind() != ArenaDefinition.Kind.BOSS_ROOM || bossDefeated)
            throw new IllegalStateException("Boss defeat unavailable");
        bossDefeated = true;
        arenaComplete = true;
        completedArenas++;
        progress.recordKill();
    }
    public void enterArena(String destination) {
        requireActive();
        if (isDemo() || !arenaComplete || !arena().exits().contains(destination))
            throw new IllegalStateException("Unavailable route: " + destination);
        arenaId = destination;
        arenaElapsed = 0;
        arenaComplete = false;
        arenaKills.clear();
        encounters.enterRoom();
    }
    public int rank(Upgrade upgrade) { return ranks.getOrDefault(upgrade, 0); }
    public void applyUpgrade(Upgrade upgrade) {
        requireActive();
        if (rank(upgrade) >= upgrade.maxRank) throw new IllegalStateException("Upgrade at maximum rank");
        upgrade.apply(player);
        ranks.merge(upgrade, 1, Integer::sum);
    }
    public float difficultyTime() { return isDemo() ? progress.elapsed() : Math.min(300, arena().difficultySeconds() + arenaElapsed); }
    public EnemyDifficulty enemyDifficulty() {
        return EnemyDifficulty.at(progress.elapsed(), isDemo() ? 0 : arena().difficultySeconds());
    }
    public float spawnInterval() { return Math.max(0.32f, 1.7f - difficultyTime() / 210); }
    public int waveSize() { return 1 + (int) (difficultyTime() / 60); }
    public void addChestObols(int amount) {
        requireActive();
        if (isDemo() || amount < 0) throw new IllegalArgumentException("Unavailable chest currency");
        chestObols = Math.addExact(chestObols, amount);
    }
    public int earnedObols() {
        return isDemo() ? 0 : Math.addExact(chestObols, Math.min(10, progress.kills() / 10) + (bossDefeated ? 10 : 0));
    }
    public boolean canSettle() { return !isDemo() && finished && !settled; }
    public void markSettled() {
        if (!canSettle()) throw new IllegalStateException("Return already settled or unavailable");
        settled = true;
    }
    public void end() { finished = true; }
    public RunSession newAttempt() {
        RunSession next = new RunSession(region, player.getProfession(), permanent);
        if (encounters.regionMaterialClaimed()) next.encounters.blockRegionMaterial();
        return next;
    }
    private void requireActive() { if (finished) throw new IllegalStateException("Attempt has ended"); }
}
