package br.com.srsups.paradiseinhell;

import java.util.EnumMap;

/** One attempt. Arena screens own entities/resources; this object owns progression. */
public final class RunSession {
    private final RegionDefinition region;
    private final GameDifficulty difficulty;
    public GameDifficulty difficulty() { return difficulty; }
    private final PlayerState player;
    private final RunProgress progress;
    private final PermanentBonuses permanent;
    private final RunEncounters encounters;
    private int pendingForgeMaterials;
    private final String id=java.util.UUID.randomUUID().toString();
    private String characterId = "";
    private boolean characterBound;
    public String characterId() { return characterId; }
    public void bindCharacter(String value) {
        if (characterBound && !characterId.equals(value)) throw new IllegalStateException("Attempt belongs to another character");
        if (!value.isEmpty()) java.util.UUID.fromString(value);
        characterId = value; characterBound = true;
    }
    private final RunStatistics statistics=new RunStatistics();
    private final Necromancy necromancy=new Necromancy(new java.util.Random(), statistics);
    private RunReport report;
    private boolean sharedRecorded;
    private long soulHundredths;
    public String id() { return id; }
    public RunStatistics statistics() { return statistics; }
    public int earnedSouls() { return isDemo()?0:GameDifficulty.wholeReward(soulHundredths); }
    public boolean sharedRecorded() { return sharedRecorded; }
    public void markSharedRecorded() { sharedRecorded=true; }
    public RunReport report(RunOutcome outcome,int slot) {
        if(!finished) throw new IllegalStateException("Attempt still active");
        if(!isDemo() && (outcome==RunOutcome.VICTORY)!=bossDefeated) throw new IllegalStateException("Invalid result");
        if(report==null) report=statistics.snapshot(this,outcome,slot);
        else if(report.outcome()!=outcome || report.saveSlot()!=slot+1) throw new IllegalStateException("Report already finalized");
        return report;
    }
    private final DivineFury fury = new DivineFury();
    public Necromancy necromancy() { return necromancy; }
    public DivineFury fury() { return fury; }
    public int xpDrop(int base) { return isDemo() ? base : Math.max(1, Math.round(base * fury.xpMultiplier())); }
    private final EnumMap<Upgrade, Integer> ranks = new EnumMap<>(Upgrade.class);
    private final EnumMap<EnemyType, Integer> arenaKills = new EnumMap<>(EnemyType.class);
    private final java.util.Map<String, ArenaVariation> variations = new java.util.HashMap<>();
    private final java.util.Random rewardRandom = new java.util.Random();
    private java.util.List<Upgrade> rewardChoices;
    private boolean arenaRewardClaimed;
    private String arenaId;
    private float arenaElapsed;
    private boolean arenaComplete, finished, bossDefeated, settled;
    private int completedArenas, chestObols;

    private RunSession(RegionDefinition region, Profession profession, PermanentBonuses permanent, GameDifficulty difficulty,
                       java.util.random.RandomGenerator random) {
        this.region = region;
        this.difficulty = java.util.Objects.requireNonNull(difficulty);
        encounters = new RunEncounters(random, profession.luckMultiplier);
        this.permanent = java.util.Objects.requireNonNull(permanent);
        player = new PlayerState(profession);
        permanent.apply(player);
        progress = new RunProgress(region == null);
        arenaId = region == null ? null : region.start();
        statistics.enterRoom(isDemo()?"demo":arenaId,isDemo()?"Demo de sobrevivência":arena().title(),player.getVida());
        player.setStatistics(statistics);
        if (region != null) {
            java.util.Random variationRandom = new java.util.Random();
            for (ArenaDefinition arena : region.arenas())
                if (arena.kind() == ArenaDefinition.Kind.COMBAT) variations.put(arena.id(), ArenaVariation.draw(variationRandom));
        }
    }
    public static RunSession demo() { return demo(Profession.CACADOR); }
    public static RunSession demo(Profession profession) { return demo(profession, GameDifficulty.MEDIUM); }
    public static RunSession demo(Profession profession, GameDifficulty difficulty) {
        return new RunSession(null, profession, PermanentBonuses.NONE, difficulty, new java.util.Random());
    }
    public static RunSession campaign(RegionDefinition region) { return campaign(region, Profession.CACADOR); }
    public static RunSession campaign(RegionDefinition region, Profession profession) {
        return campaign(region, profession, PermanentBonuses.NONE);
    }
    public static RunSession campaign(RegionDefinition region, Profession profession, PermanentBonuses permanent) {
        return campaign(region, profession, permanent, GameDifficulty.MEDIUM);
    }
    public static RunSession campaign(RegionDefinition region, Profession profession, PermanentBonuses permanent,
                                      java.util.random.RandomGenerator random) {
        return campaign(region, profession, permanent, GameDifficulty.MEDIUM, random);
    }
    public static RunSession campaign(RegionDefinition region, Profession profession, PermanentBonuses permanent, GameDifficulty difficulty) {
        return campaign(region, profession, permanent, difficulty, new java.util.Random());
    }
    public static RunSession campaign(RegionDefinition region, Profession profession, PermanentBonuses permanent,
                                      GameDifficulty difficulty, java.util.random.RandomGenerator random) {
        if (region == null) throw new IllegalArgumentException("Missing region");
        return new RunSession(region, profession, permanent, difficulty, random);
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
    public ArenaVariation variation() { return isDemo() ? ArenaVariation.BALANCED : variation(arenaId); }
    public ArenaVariation variation(String destination) {
        region().arena(destination);
        return variations.getOrDefault(destination, ArenaVariation.BALANCED);
    }
    public boolean canClaimArenaReward() {
        return !finished && !isDemo() && arenaComplete && !arenaRewardClaimed && arena().reward() != ArenaReward.NONE;
    }
    public java.util.List<Upgrade> arenaRewardChoices() {
        requireReward(ArenaReward.RARE_BOON);
        if (rewardChoices == null) rewardChoices = UpgradeChoices.draw(this, 3, rewardRandom, BoonRarity.RARE);
        return rewardChoices;
    }
    public String claimRecoveryReward(boolean preferHealth) {
        requireReward(ArenaReward.RECOVERY_CHEST);
        boolean heal = preferHealth && player.getVida() < player.getVidaMaxima();
        int before = earnedObols();
        if (heal) player.curar(40); else addChestObols(5);
        arenaRewardClaimed = true;
        return heal ? "+40 de vida" : "+" + (earnedObols() - before) + " óbolos ao retornar";
    }
    public void claimBoonReward(Upgrade boon) {
        var options = arenaRewardChoices();
        if (options.isEmpty()) {
            if (boon != null) throw new IllegalArgumentException("No eligible rare boon");
            addChestObols(8);
        } else {
            if (!options.contains(boon)) throw new IllegalArgumentException("Not an offered reward");
            applyUpgrade(boon);
        }
        arenaRewardClaimed = true;
    }
    private void requireReward(ArenaReward reward) {
        if (!canClaimArenaReward() || arena().reward() != reward) throw new IllegalStateException("Arena reward unavailable");
    }
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
        addSouls(xpDrop(SoulRewards.MINI_BOSS)); statistics.kill();
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
        statistics.advance(delta);
        if (!isDemo()) {
            arenaElapsed += delta;
            if (arena().kind() == ArenaDefinition.Kind.COMBAT)
                fury.update(delta, arenaComplete, player.powerRatio(), arena().difficultySeconds(), necromancy.allies().size());
        }
    }
    public int kills(EnemyType type) { return arenaKills.getOrDefault(type, 0); }
    public void recordKill(EnemyType type) { recordKill(type, SoulRewards.enemy(type)); }
    public void recordKill(EnemyType type, int baseSouls) {
        requireActive();
        if (type == null || baseSouls <= 0) throw new IllegalArgumentException("Invalid enemy reward");
        if (!isDemo() && arena().kind() != ArenaDefinition.Kind.COMBAT) throw new IllegalStateException("Not a combat arena");
        progress.recordKill(); statistics.kill();
        if (isDemo()) return;
        addSouls(xpDrop(baseSouls));
        arenaKills.merge(type, 1, Integer::sum);
        if (!arenaComplete && goalsMet()) {
            arenaComplete = true;
            statistics.unlockExit();
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
        addSouls(SoulRewards.FLOOR_BOSS); statistics.kill(); statistics.unlockExit();
        arenaComplete = true;
        completedArenas++;
        progress.recordKill();
    }
    public void enterArena(String destination) {
        requireActive();
        if (isDemo() || !arenaComplete || !arena().exits().contains(destination))
            throw new IllegalStateException("Unavailable route: " + destination);
        if (canClaimArenaReward()) throw new IllegalStateException("Collect the arena reward first");
        statistics.enterRoom(destination,region.arena(destination).title(),player.getVida());
        arenaId = destination;
        arenaRewardClaimed = false; rewardChoices = null;
        arenaElapsed = 0;
        arenaComplete = false;
        arenaKills.clear();
        encounters.enterRoom();
        fury.enterRoom();
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
        EnemyDifficulty base = difficulty.apply(EnemyDifficulty.at(progress.elapsed(), isDemo() ? 0 : arena().difficultySeconds()));
        return isDemo() ? base : new EnemyDifficulty(base.healthMultiplier() * fury.healthMultiplier(),
            base.damageMultiplier() * fury.damageMultiplier());
    }
    public float spawnInterval() {
        return Math.max(0.32f, 1.7f - difficultyTime() / 210) * (isDemo() ? 1 : arena().waveIntervalMultiplier() * fury.spawnMultiplier());
    }
    public int waveSize() { return 1 + (int) (difficultyTime() / 60); }
    public void addChestObols(int amount) {
        requireActive();
        if (isDemo() || amount < 0) throw new IllegalArgumentException("Unavailable chest currency");
        chestObols = Math.addExact(chestObols, amount);
    }
    private int baseObols() { return Math.addExact(chestObols, Math.min(10, progress.kills() / 10) + (bossDefeated ? 10 : 0)); }
    public int earnedObols() { return isDemo() ? 0 : difficulty.reward(baseObols()); }
    /** Whole coins this chest adds now; fractional remainder stays with the attempt. */
    public int previewChestObols(int amount) {
        if (isDemo() || amount < 0) throw new IllegalArgumentException("Unavailable chest currency");
        return difficulty.reward(Math.addExact(baseObols(), amount)) - earnedObols();
    }
    private void addSouls(int base) {
        soulHundredths = Math.addExact(soulHundredths, difficulty.rewardHundredths(base));
    }
    public boolean canSettle() { return !isDemo() && finished && !settled; }
    public void markSettled() {
        if (!canSettle()) throw new IllegalStateException("Return already settled or unavailable");
        settled = true;
    }
    public void end() { statistics.end(player.getVida()); finished = true; necromancy.clear(); }
    public RunSession newAttempt() {
        RunSession next = new RunSession(region, player.getProfession(), permanent, difficulty, new java.util.Random());
        if (encounters.regionMaterialClaimed()) next.encounters.blockRegionMaterial();
        if (!isDemo() && player.getWeaponRarity() == WeaponRarity.RARE) next.player.forgeWeapon();
        return next;
    }
    private void requireActive() { if (finished) throw new IllegalStateException("Attempt has ended"); }
}
