package br.com.srsups.paradiseinhell;

import java.util.EnumMap;
import com.badlogic.gdx.utils.JsonValue;

/** One attempt. Arena screens own entities/resources; this object owns progression. */
public final class RunSession {
    private final RegionDefinition region;
    private final GameDifficulty difficulty;
    public GameDifficulty difficulty() { return difficulty; }
    private final PlayerState player;
    private final RunProgress progress;
    private final TutorialGuide tutorial;
    public TutorialGuide tutorial() { return tutorial; }
    private final PermanentBonuses permanent;
    private final RunEncounters encounters;
    private FarmerQuest farmerQuest = FarmerQuest.NOT_STARTED;
    private final RunRandom farmerRandom = new RunRandom();
    public boolean questEnabled() { return !isDemo() && region.id().equals(RegionDefinition.PROTOTYPE_ID); }
    public FarmerQuest farmerQuest() { return farmerQuest; }
    void prepareFarmerQuest(FarmerQuest value) {
        if (questEnabled() && value.ordinal() > farmerQuest.ordinal()) farmerQuest = value;
    }
    public boolean openFarmerEncounter(boolean eligible) {
        return encounters.openFarmer(eligible && questEnabled() && !finished && arenaComplete
            && arena().kind() == ArenaDefinition.Kind.COMBAT, farmerRandom);
    }
    private int pendingForgeMaterials;
    private final String id;
    private long attemptSequence;
    public long attemptSequence(){return attemptSequence;}
    public void bindAttemptSequence(long value){
        if(value<=0||attemptSequence!=0&&attemptSequence!=value)throw new IllegalArgumentException("Attempt sequence mismatch");
        attemptSequence=value;
    }
    private String characterId = "";
    private boolean characterBound;
    public String characterId() { return characterId; }
    public void bindCharacter(String value) {
        if (characterBound && !characterId.equals(value)) throw new IllegalStateException("Attempt belongs to another character");
        if (!value.isEmpty()) java.util.UUID.fromString(value);
        characterId = value; characterBound = true;
    }
    private final RunStatistics statistics=new RunStatistics();
    private final Necromancy necromancy=new Necromancy(new RunRandom(), statistics);
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
    private final RunRandom rewardRandom = new RunRandom();
    private final RunRandom boonRandom = new RunRandom();
    public java.util.Random boonRandom(){return boonRandom;}
    private java.util.List<Upgrade> rewardChoices;
    private boolean arenaRewardClaimed;
    private String arenaId;
    private float arenaElapsed;
    private boolean arenaComplete, finished, bossDefeated, settled;
    private int completedArenas, chestObols;

    private RunSession(RegionDefinition region, Profession profession, PermanentBonuses permanent, GameDifficulty difficulty,
                       java.util.random.RandomGenerator random) {
        this(region,profession,permanent,difficulty,random,java.util.UUID.randomUUID().toString(),null);
    }
    private RunSession(RegionDefinition region,Profession profession,PermanentBonuses permanent,GameDifficulty difficulty,
                       java.util.random.RandomGenerator random,String id,PlayerState restoredPlayer) {
        java.util.UUID.fromString(id);this.id=id;
        this.region = region;
        this.difficulty = java.util.Objects.requireNonNull(difficulty);
        encounters = new RunEncounters(random, profession.luckMultiplier);
        this.permanent = java.util.Objects.requireNonNull(permanent);
        player = restoredPlayer==null?new PlayerState(profession):restoredPlayer;
        if(restoredPlayer==null)permanent.apply(player);
        progress = new RunProgress(region == null);
        tutorial = new TutorialGuide(region == null);
        arenaId = region == null ? null : region.start();
        statistics.enterRoom(isDemo()?"demo":arenaId,isDemo()?"Demo de sobrevivência":arena().title(),player.getVida());
        player.setStatistics(statistics);
        if (region != null) {
            java.util.Random variationRandom = new RunRandom();
            for (ArenaDefinition arena : region.arenas())
                if (arena.kind() == ArenaDefinition.Kind.COMBAT) variations.put(arena.id(), ArenaVariation.draw(variationRandom));
        }
    }
    public static RunSession demo() { return demo(Profession.CACADOR); }
    public static RunSession demo(Profession profession) { return demo(profession, GameDifficulty.MEDIUM); }
    public static RunSession demo(Profession profession, GameDifficulty difficulty) {
        return new RunSession(null, profession, PermanentBonuses.NONE, difficulty, new RunRandom());
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
        return campaign(region, profession, permanent, difficulty, new RunRandom());
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
        if (questEnabled() && farmerQuest == FarmerQuest.ACCEPTED) farmerQuest = FarmerQuest.SEED_FOUND;
        addSouls(xpDrop(SoulRewards.MINI_BOSS)); statistics.kill();player.onKill();
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
        progress.recordKill(); statistics.kill();player.onKill();
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
        if (questEnabled() && farmerQuest == FarmerQuest.SEED_FOUND) farmerQuest = FarmerQuest.COMPLETED;
        addSouls(SoulRewards.FLOOR_BOSS); statistics.kill(); statistics.unlockExit();player.onKill();
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
        tutorial.action(TutorialTopic.BOONS);
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
        RunSession next = new RunSession(region, player.getProfession(), permanent, difficulty, new RunRandom());
        if (encounters.regionMaterialClaimed()) next.encounters.blockRegionMaterial();
        if (!isDemo() && player.getWeaponRarity() == WeaponRarity.RARE) next.player.forgeWeapon();
        next.prepareFarmerQuest(farmerQuest);
        return next;
    }
    private void requireActive() { if (finished) throw new IllegalStateException("Attempt has ended"); }
    /** Pure data only. Stage rules belong to RunCheckpoint; live combat entities never enter this state. */
    public JsonValue checkpointState(){
        if(isDemo()||finished||bossDefeated||settled||sharedRecorded||player.estaMorto())throw new IllegalStateException("Only a living campaign attempt can be suspended");
        JsonValue root=new JsonValue(JsonValue.ValueType.object);
        SnapshotJson.put(root,"id",id);SnapshotJson.put(root,"characterId",characterId);SnapshotJson.put(root,"characterBound",characterBound);
        SnapshotJson.put(root,"attemptSequence",attemptSequence);SnapshotJson.put(root,"regionId",region.id());
        SnapshotJson.put(root,"regionFingerprint",RunCheckpoint.fingerprint(region));SnapshotJson.put(root,"profession",player.getProfession().name());
        SnapshotJson.put(root,"difficulty",difficulty.name());SnapshotJson.put(root,"arenaId",arenaId);SnapshotJson.put(root,"arenaElapsed",arenaElapsed);
        SnapshotJson.put(root,"arenaComplete",arenaComplete);SnapshotJson.put(root,"completedArenas",completedArenas);
        SnapshotJson.put(root,"arenaRewardClaimed",arenaRewardClaimed);SnapshotJson.put(root,"chestObols",chestObols);
        SnapshotJson.put(root,"soulHundredths",soulHundredths);SnapshotJson.put(root,"pendingForgeMaterials",pendingForgeMaterials);
        SnapshotJson.put(root,"farmerQuest",farmerQuest.name());
        JsonValue bonuses=new JsonValue(JsonValue.ValueType.object);
        SnapshotJson.put(bonuses,"vitality",permanent.vitality());SnapshotJson.put(bonuses,"strength",permanent.strength());
        SnapshotJson.put(bonuses,"agility",permanent.agility());SnapshotJson.put(bonuses,"endurance",permanent.endurance());
        SnapshotJson.put(bonuses,"recovery",permanent.recovery());SnapshotJson.put(bonuses,"armor",permanent.armor());root.addChild("permanent",bonuses);
        root.addChild("player",player.checkpointState());root.addChild("progress",progress.checkpointState());
        root.addChild("statistics",statistics.checkpointState());root.addChild("encounters",encounters.checkpointState());
        root.addChild("necromancy",necromancy.checkpointState());root.addChild("fury",fury.checkpointState());
        root.addChild("farmerRandom",farmerRandom.checkpointState());root.addChild("rewardRandom",rewardRandom.checkpointState());root.addChild("boonRandom",boonRandom.checkpointState());
        JsonValue boonRanks=new JsonValue(JsonValue.ValueType.object),kills=new JsonValue(JsonValue.ValueType.object),draws=new JsonValue(JsonValue.ValueType.object);
        for(Upgrade upgrade:Upgrade.values())if(rank(upgrade)>0)SnapshotJson.put(boonRanks,upgrade.name(),rank(upgrade));root.addChild("ranks",boonRanks);
        for(EnemyType type:EnemyType.values())if(kills(type)>0)SnapshotJson.put(kills,type.name(),kills(type));root.addChild("arenaKills",kills);
        for(ArenaDefinition definition:region.arenas())if(variations.containsKey(definition.id()))SnapshotJson.put(draws,definition.id(),variations.get(definition.id()).name());root.addChild("variations",draws);
        JsonValue choices=new JsonValue(rewardChoices==null?JsonValue.ValueType.nullValue:JsonValue.ValueType.array);
        if(rewardChoices!=null)for(Upgrade choice:rewardChoices)choices.addChild(new JsonValue(choice.name()));root.addChild("rewardChoices",choices);
        return root;
    }
    public static RunSession fromCheckpointState(RegionDefinition region,JsonValue root){
        java.util.Objects.requireNonNull(region);
        SnapshotJson.object(root,"id","characterId","characterBound","attemptSequence","regionId","regionFingerprint","profession","difficulty",
            "arenaId","arenaElapsed","arenaComplete","completedArenas","arenaRewardClaimed","chestObols","soulHundredths","pendingForgeMaterials","farmerQuest",
            "permanent","player","progress","statistics","encounters","necromancy","fury","farmerRandom","rewardRandom","boonRandom","ranks","arenaKills","variations","rewardChoices");
        String regionId=SnapshotJson.text(root,"regionId"),fingerprint=SnapshotJson.text(root,"regionFingerprint");
        if(!region.id().equals(regionId)||!RunCheckpoint.fingerprint(region).equals(fingerprint))throw SnapshotJson.invalid("conteúdo da região alterado");
        String id=SnapshotJson.text(root,"id"),characterId=SnapshotJson.text(root,"characterId");
        java.util.UUID.fromString(id);java.util.UUID.fromString(characterId);
        if(!SnapshotJson.flag(root,"characterBound"))throw SnapshotJson.invalid("personagem da tentativa");
        Profession profession=SnapshotJson.enumValue(root,"profession",Profession.class);
        GameDifficulty difficulty=SnapshotJson.enumValue(root,"difficulty",GameDifficulty.class);
        JsonValue bonuses=SnapshotJson.required(root,"permanent");SnapshotJson.object(bonuses,"vitality","strength","agility","endurance","recovery","armor");
        int max=PermanentAttribute.MAX_RANK;
        PermanentBonuses permanent=new PermanentBonuses(SnapshotJson.integer(bonuses,"vitality",0,max),SnapshotJson.integer(bonuses,"strength",0,max),
            SnapshotJson.integer(bonuses,"agility",0,max),SnapshotJson.integer(bonuses,"endurance",0,max),SnapshotJson.integer(bonuses,"recovery",0,max),SnapshotJson.integer(bonuses,"armor",0,max));
        PlayerState player=PlayerState.fromCheckpointState(SnapshotJson.required(root,"player"));
        if(player.getProfession()!=profession||player.estaMorto())throw SnapshotJson.invalid("profissão ou vida");
        RunSession result=new RunSession(region,profession,permanent,difficulty,new RunRandom(0),id,player);
        result.bindCharacter(characterId);result.attemptSequence=SnapshotJson.longInteger(root,"attemptSequence",0,Long.MAX_VALUE);
        result.arenaId=SnapshotJson.text(root,"arenaId");region.arena(result.arenaId);
        result.arenaElapsed=SnapshotJson.nonNegative(root,"arenaElapsed");result.arenaComplete=SnapshotJson.flag(root,"arenaComplete");
        result.completedArenas=SnapshotJson.integer(root,"completedArenas",0,region.arenas().size());result.arenaRewardClaimed=SnapshotJson.flag(root,"arenaRewardClaimed");
        result.chestObols=SnapshotJson.integer(root,"chestObols",0,Integer.MAX_VALUE);result.soulHundredths=SnapshotJson.longInteger(root,"soulHundredths",0,(long)Integer.MAX_VALUE*100);
        result.pendingForgeMaterials=SnapshotJson.integer(root,"pendingForgeMaterials",0,1);result.farmerQuest=SnapshotJson.enumValue(root,"farmerQuest",FarmerQuest.class);
        result.progress.restoreCheckpointState(SnapshotJson.required(root,"progress"));result.statistics.restoreCheckpointState(SnapshotJson.required(root,"statistics"));
        result.encounters.restoreCheckpointState(SnapshotJson.required(root,"encounters"));result.necromancy.restoreCheckpointState(SnapshotJson.required(root,"necromancy"));
        result.fury.restoreCheckpointState(SnapshotJson.required(root,"fury"));result.farmerRandom.restoreCheckpointState(SnapshotJson.required(root,"farmerRandom"));
        result.rewardRandom.restoreCheckpointState(SnapshotJson.required(root,"rewardRandom"));result.boonRandom.restoreCheckpointState(SnapshotJson.required(root,"boonRandom"));
        JsonValue ranks=SnapshotJson.required(root,"ranks");if(!ranks.isObject()||ranks.size>Upgrade.values().length)throw SnapshotJson.invalid("dádivas");
        result.ranks.clear();java.util.Set<String> seen=new java.util.HashSet<>();
        for(JsonValue rank:ranks){
            if(!seen.add(rank.name))throw SnapshotJson.invalid("dádiva repetida");Upgrade upgrade;
            try{upgrade=Upgrade.valueOf(rank.name);}catch(IllegalArgumentException invalid){throw SnapshotJson.invalid("dádiva");}
            result.ranks.put(upgrade,SnapshotJson.integer(ranks,rank.name,1,upgrade.maxRank));
        }
        JsonValue kills=SnapshotJson.required(root,"arenaKills");if(!kills.isObject()||kills.size>EnemyType.values().length)throw SnapshotJson.invalid("abates");
        result.arenaKills.clear();seen.clear();
        for(JsonValue count:kills){
            if(!seen.add(count.name))throw SnapshotJson.invalid("abate repetido");EnemyType type;
            try{type=EnemyType.valueOf(count.name);}catch(IllegalArgumentException invalid){throw SnapshotJson.invalid("tipo de inimigo");}
            result.arenaKills.put(type,SnapshotJson.integer(kills,count.name,1,Integer.MAX_VALUE));
        }
        JsonValue draws=SnapshotJson.required(root,"variations");if(!draws.isObject()||draws.size>region.arenas().size())throw SnapshotJson.invalid("variações");
        result.variations.clear();seen.clear();
        for(JsonValue draw:draws){
            if(!seen.add(draw.name)||region.arena(draw.name).kind()!=ArenaDefinition.Kind.COMBAT)throw SnapshotJson.invalid("variação da sala");
            result.variations.put(draw.name,SnapshotJson.enumValue(draws,draw.name,ArenaVariation.class));
        }
        for(ArenaDefinition arena:region.arenas())if(arena.kind()==ArenaDefinition.Kind.COMBAT&&!result.variations.containsKey(arena.id()))throw SnapshotJson.invalid("variação ausente");
        JsonValue choices=SnapshotJson.required(root,"rewardChoices");
        if(choices.isNull())result.rewardChoices=null;
        else{
            if(!choices.isArray()||choices.size>3)throw SnapshotJson.invalid("escolhas de recompensa");
            java.util.ArrayList<Upgrade> parsed=new java.util.ArrayList<>();
            for(JsonValue choice:choices){
                if(!choice.isString())throw SnapshotJson.invalid("escolha de recompensa");Upgrade upgrade;
                try{upgrade=Upgrade.valueOf(choice.asString());}catch(IllegalArgumentException invalid){throw SnapshotJson.invalid("escolha de recompensa");}
                if(parsed.contains(upgrade)||upgrade.rarity.ordinal()<BoonRarity.RARE.ordinal())throw SnapshotJson.invalid("recompensa repetida ou inválida");parsed.add(upgrade);
            }
            result.rewardChoices=java.util.List.copyOf(parsed);
        }
        result.validateCheckpointState();return result;
    }
    private void validateCheckpointState(){
        if(statistics.checkpointEnded()||!statistics.checkpointRoomId().equals(arenaId)||statistics.checkpointKills()!=progress.kills()
            ||Math.abs(statistics.checkpointElapsed()-progress.elapsed())>Math.max(.05f,progress.elapsed()*.0001f)
            ||arenaElapsed>progress.elapsed()+.05f)throw SnapshotJson.invalid("estatísticas da tentativa");
        int roomKills=0;for(int count:arenaKills.values())roomKills=Math.addExact(roomKills,count);
        if(roomKills>progress.kills()||arenaComplete!=goalsMet()||arenaRewardClaimed&&!arenaComplete
            ||!arenaComplete&&rewardChoices!=null||rewardChoices!=null&&arena().reward()!=ArenaReward.RARE_BOON)
            throw SnapshotJson.invalid("progresso da sala");
        if(!questEnabled()&&farmerQuest!=FarmerQuest.NOT_STARTED)throw SnapshotJson.invalid("promessa fora da região");
    }
}
