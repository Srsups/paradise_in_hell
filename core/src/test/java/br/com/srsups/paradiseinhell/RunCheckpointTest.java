package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RunCheckpointTest {
    private static RegionDefinition region(){return region("Submundo de teste");}
    private static RegionDefinition region(String title){
        return new RegionDefinition(RegionDefinition.PROTOTYPE_ID,title,"start",List.of(
            new ArenaDefinition("start","Estige","",ArenaDefinition.Kind.COMBAT,0,
                Map.of(EnemyType.COMMON,2,EnemyType.FAST,1,EnemyType.TANK,1),List.of("side","recovery","boss")),
            new ArenaDefinition("side","Sombras","",ArenaDefinition.Kind.COMBAT,60,Map.of(EnemyType.COMMON,1),List.of("boss"),ArenaReward.RARE_BOON,1),
            new ArenaDefinition("recovery","Cinzas","",ArenaDefinition.Kind.COMBAT,60,Map.of(EnemyType.COMMON,1),List.of("boss"),ArenaReward.RECOVERY_CHEST,1),
            new ArenaDefinition("boss","Guardião","",ArenaDefinition.Kind.BOSS_ROOM,120,Map.of(),List.of())));
    }
    private static RunSession run(Profession profession){return run(profession,GameDifficulty.HARD,new RunRandom(12345));}
    private static RunSession run(Profession profession,GameDifficulty difficulty,java.util.random.RandomGenerator random){
        RunSession run=RunSession.campaign(region(),profession,new PermanentBonuses(2,3,1,2,4,1),difficulty,random);
        run.bindCharacter(UUID.randomUUID().toString());return run;
    }
    private static void complete(RunSession run){
        for(var goal:run.arena().killGoals().entrySet())while(run.kills(goal.getKey())<goal.getValue())run.recordKill(goal.getKey(),1);
    }
    private static String json(JsonValue value){return value.toJson(JsonWriter.OutputType.json);}
    private static RunSession copy(RunSession run,RunCheckpoint.Stage stage){return RunCheckpoint.capture(run,stage,7,1).restore(region());}

    @Test void everyProfessionRestoresActualBuildWithoutReplayingHealingOrConsumedResurrection(){
        for(Profession profession:Profession.values()){
            RunSession original=run(profession);PlayerState player=original.player();
            original.applyUpgrade(Upgrade.FORCA);original.applyUpgrade(Upgrade.VIDA);original.applyUpgrade(Upgrade.CURA);
            original.applyUpgrade(Upgrade.CADENCIA);original.applyUpgrade(Upgrade.RESSURREICAO);original.applyUpgrade(Upgrade.AMPARO);
            original.applyUpgrade(Upgrade.RASTRO);original.applyUpgrade(Upgrade.EMBOSCADA);original.applyUpgrade(Upgrade.CHOQUE);
            original.applyUpgrade(Upgrade.POSEIDON);player.forgeWeapon();
            if(profession==Profession.FAZENDEIRO)player.switchTool();
            player.sofrerDano(10000);player.usarRessurreicao();player.tryShoot();player.tryShock();player.tryDash();
            original.bindAttemptSequence(7);
            RunSession restored=copy(original,RunCheckpoint.Stage.ROOM_START);
            assertEquals(original.id(),restored.id());assertEquals(original.characterId(),restored.characterId());
            assertEquals(json(original.player().checkpointState()),json(restored.player().checkpointState()),profession.name());
            assertEquals(player.getVida(),restored.player().getVida());assertFalse(restored.player().possuiRessurreicao());
            assertEquals(player.getWeapon(),restored.player().getWeapon());assertEquals(WeaponRarity.RARE,restored.player().getWeaponRarity());
            assertEquals(json(original.checkpointState()),json(restored.checkpointState()),profession.name());
        }
    }
    @Test void captureDoesNotBindOrMutateTheLiveAttemptBeforeTheProfileCommit(){
        RunSession run=run(Profession.CACADOR);assertEquals(0,run.attemptSequence());
        RunCheckpoint checkpoint=RunCheckpoint.capture(run,RunCheckpoint.Stage.ROOM_START,12,4);
        assertEquals(0,run.attemptSequence());assertEquals(12,checkpoint.sequence());assertEquals(4,checkpoint.revision());
        RunSession restored=checkpoint.restore(region());assertEquals(12,restored.attemptSequence());
        assertThrows(IllegalArgumentException.class,()->restored.bindAttemptSequence(13));
        assertThrows(IllegalArgumentException.class,()->RunCheckpoint.capture(restored,RunCheckpoint.Stage.ROOM_START,13,5));
    }
    @Test void exactRandomStatePreservesFutureDrawsAndIsIndependentOfJavaSerialization(){
        RunRandom random=new RunRandom(423),javaCompatible=new RunRandom(423);java.util.Random reference=new java.util.Random(423);
        for(int i=0;i<50;i++)assertEquals(reference.nextFloat(),javaCompatible.nextFloat());
        for(int i=0;i<15;i++)random.nextDouble();
        RunRandom restored=RunRandom.fromCheckpointState(random.checkpointState());
        for(int i=0;i<100;i++){assertEquals(random.nextInt(),restored.nextInt());assertEquals(random.nextDouble(),restored.nextDouble());}
        RunSession run=run(Profession.CACADOR);RunSession copy=copy(run,RunCheckpoint.Stage.ROOM_START);
        for(int i=0;i<20;i++)assertEquals(run.boonRandom().nextInt(),copy.boonRandom().nextInt());
        JsonValue bad=random.checkpointState();bad.get("state").set(-1L,null);
        assertThrows(IllegalArgumentException.class,()->RunRandom.fromCheckpointState(bad));
    }
    @Test void fractionalCurrencyRoomStatisticsAndDamageContinueWithoutAnExtraRoomOrCredit(){
        RunSession original=run(Profession.CACADOR,GameDifficulty.EASY,new RunRandom(2));
        original.advance(17.25f);Target target=new Target(100);
        original.statistics().deal(target,12,DamageSource.WEAPON);original.player().sofrerDano(10);complete(original);
        assertEquals(3,original.earnedSouls());
        RunSession restored=copy(original,RunCheckpoint.Stage.ROUTE);
        restored.enterArena("side");restored.advance(3);restored.recordKill(EnemyType.COMMON,1);
        assertEquals(4,restored.earnedSouls());
        restored.end();RunReport report=restored.report(RunOutcome.ABANDONED,0);
        assertEquals(2,report.rooms().size());assertEquals(20.25f,report.duration());assertEquals(5,report.kills());
        assertEquals(12,report.damageDealt());assertTrue(report.received()>0);assertEquals(17.25f,report.rooms().getFirst().duration());
    }
    @Test void storedRewardChoicesAndTheirConsumedStatusDoNotRerollOrApplyTwice(){
        RunSession run=run(Profession.CACADOR);complete(run);run.enterArena("side");complete(run);
        List<Upgrade> offered=List.copyOf(run.arenaRewardChoices());assertFalse(offered.isEmpty());
        RunSession restored=copy(run,RunCheckpoint.Stage.ARENA_REWARD);assertEquals(offered,restored.arenaRewardChoices());
        Upgrade chosen=offered.getFirst();restored.claimBoonReward(chosen);
        assertThrows(IllegalStateException.class,()->restored.claimBoonReward(chosen));
        RunSession consumed=copy(restored,RunCheckpoint.Stage.ROUTE);
        assertFalse(consumed.canClaimArenaReward());assertEquals(1,consumed.rank(chosen));
    }
    @Test void alreadyRolledNpcAndRegionalFragmentFactsSurviveSuspension(){
        java.util.Random forced=new java.util.Random(){@Override public float nextFloat(){return 0;}};
        RunSession run=run(Profession.APOSTADOR,GameDifficulty.MEDIUM,forced);run.prepareFarmerQuest(FarmerQuest.ACCEPTED);
        run.advance(45);run.markMiniBossSpawned();assertTrue(run.recordMiniBossDefeat());complete(run);
        assertTrue(run.encounters().openBlacksmith());RunSession restored=copy(run,RunCheckpoint.Stage.BLACKSMITH);
        assertEquals(1,restored.pendingForgeMaterials());assertTrue(restored.encounters().regionMaterialClaimed());
        assertEquals(0,restored.encounters().materialChance());assertEquals(FarmerQuest.SEED_FOUND,restored.farmerQuest());
        restored.encounters().closeBlacksmith();assertFalse(restored.encounters().openBlacksmith());
        assertTrue(restored.encounters().openFarmer(true,forced));
        RunSession farmer=copy(restored,RunCheckpoint.Stage.FARMER);assertTrue(farmer.encounters().farmerOpen());
        farmer.encounters().closeFarmer();assertFalse(farmer.encounters().openFarmer(true,forced));
    }
    @Test void necromancerAlliesKeepTheirDamageLifetimeAndCooldownAcrossRooms(){
        RunSession run=run(Profession.NECROMANTE);
        JsonValue deterministic=run.necromancy().checkpointState();deterministic.get("random").get("state").set(new RunRandom(0).state(),null);
        run.necromancy().restoreCheckpointState(deterministic);
        for(int i=0;i<30;i++)run.necromancy().resurrect(run.player(),new Target(0));
        assertFalse(run.necromancy().allies().isEmpty());
        run.necromancy().update(.3f,0,0,List.of(new Target(100)),(x,y)->false,(x,y,tx,ty)->true);complete(run);
        JsonValue before=run.necromancy().checkpointState();RunSession restored=copy(run,RunCheckpoint.Stage.ROUTE);
        assertEquals(json(before),json(restored.necromancy().checkpointState()));
        float remaining=restored.necromancy().allies().getFirst().remaining;
        restored.necromancy().enterRoom(100,200);
        assertEquals(92,restored.necromancy().allies().getFirst().x);assertEquals(192,restored.necromancy().allies().getFirst().y);
        assertEquals(remaining,restored.necromancy().allies().getFirst().remaining);
    }
    @Test void wrongContentIdentityStageFutureSchemaAndDamagedDeepStateAreRejected(){
        RunSession run=run(Profession.CACADOR);RunCheckpoint checkpoint=RunCheckpoint.capture(run,RunCheckpoint.Stage.ROOM_START,7,1);
        assertThrows(IllegalArgumentException.class,()->checkpoint.restore(region("Conteúdo modificado")));
        JsonValue bad=checkpoint.json();bad.get("state").get("characterId").set(UUID.randomUUID().toString());
        assertThrows(IllegalArgumentException.class,()->RunCheckpoint.fromJson(bad));
        JsonValue future=checkpoint.json();future.get("version").set(2L,null);future.addChild("newField",new JsonValue(true));
        assertThrows(RunCheckpoint.UnsupportedVersion.class,()->RunCheckpoint.fromJson(future));
        JsonValue wrongStage=checkpoint.json();wrongStage.get("stage").set(RunCheckpoint.Stage.ROUTE.name());
        assertThrows(IllegalArgumentException.class,()->RunCheckpoint.fromJson(wrongStage).restore(region()));
        JsonValue negative=checkpoint.json();negative.get("state").get("arenaElapsed").set(-1d,null);
        assertThrows(IllegalArgumentException.class,()->RunCheckpoint.fromJson(negative).restore(region()));
        assertEquals(run.characterId(),checkpoint.restore(region()).characterId());
    }
    @Test void liveCombatPendingLevelsFinishedRunsAndDemoCannotProduceAnEligibleCheckpoint(){
        RunSession live=run(Profession.CACADOR);live.advance(1);
        assertThrows(IllegalArgumentException.class,()->RunCheckpoint.capture(live,RunCheckpoint.Stage.ROOM_START,1,1));
        RunSession level=run(Profession.CACADOR);level.player().ganharXP(10);
        assertThrows(IllegalArgumentException.class,()->RunCheckpoint.capture(level,RunCheckpoint.Stage.ROOM_START,1,1));
        RunSession finished=run(Profession.CACADOR);finished.end();
        assertThrows(IllegalArgumentException.class,()->RunCheckpoint.capture(finished,RunCheckpoint.Stage.ROOM_START,1,1));
        assertThrows(IllegalArgumentException.class,()->RunCheckpoint.capture(RunSession.demo(),RunCheckpoint.Stage.ROOM_START,1,1));
    }
    @Test void furyPressureAndPreselectedArenaVariationsArePreservedUntilNormalRoomTransition(){
        RunSession original=run(Profession.CACADOR);complete(original);original.advance(70);
        RunSession restored=copy(original,RunCheckpoint.Stage.ROUTE);
        assertEquals(json(original.fury().checkpointState()),json(restored.fury().checkpointState()));
        for(ArenaDefinition arena:region().arenas())assertEquals(original.variation(arena.id()),restored.variation(arena.id()));
        restored.enterArena("side");assertEquals(0,restored.fury().farmingSeconds());assertEquals(0,restored.fury().level());
    }
    private static final class Target implements CombatTarget {
        float health;Target(float health){this.health=health;}
        public float centerX(){return 0;}public float centerY(){return 0;}public float health(){return health;}
        public float maxHealth(){return 100;}public float strength(){return 1;}public void hurt(float damage){health=Math.max(0,health-damage);}
    }
}
