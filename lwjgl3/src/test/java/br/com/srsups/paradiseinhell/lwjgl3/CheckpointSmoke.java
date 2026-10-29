package br.com.srsups.paradiseinhell.lwjgl3;

import br.com.srsups.paradiseinhell.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.List;
import static br.com.srsups.paradiseinhell.lwjgl3.DemoSmokeTest.*;

/** Real checkpoint screens and rendering; even reloads use JSON, without touching player files. */
public final class CheckpointSmoke {
    private CheckpointSmoke() {}

    private static final class Store implements ProfileStore {
        String saved=new CampaignProfile().toJson();
        int writes;
        boolean fail;
        @Override public CampaignProfile load(){return CampaignProfile.fromJson(saved);}
        @Override public void save(CampaignProfile profile)throws IOException {
            writes++;
            if(fail)throw new IOException("checkpoint fixture save failure");
            saved=profile.toJson();
        }
    }
    private static final class Fixture implements AutoCloseable {
        final Store store=new Store();
        final SaveSlots saves=new SaveSlots(store);
        Main game;
        Fixture(){open();}
        private void open(){game=new Main(saves,AudioSettingsStore.memory(),new RecordedAudio());game.create();}
        void character(){
            check(game.createCharacter(Profession.CACADOR,GameDifficulty.MEDIUM),"Checkpoint fixture creates a real character");
            String id=game.profiles().profile().characterId();
            check(game.profiles().markOpening(id,true),"Checkpoint fixture acknowledges its introduction");
            check(game.profiles().setTutorialEnabled(id,false),"Checkpoint fixture disables tutorial overlays");
        }
        RunSession start()throws Exception {
            character();game.startCampaign();
            check(game.getScreen() instanceof GameScreen,"A persisted first room enters real combat");
            return session(game);
        }
        void reload(){game.dispose();open();}
        RunSession resume(Class<? extends Screen> expected)throws Exception {
            game.enterCampaign();check(game.getScreen() instanceof ResumeRunScreen,"A loaded checkpoint offers resume before entering the refuge");
            key(game,Input.Keys.ENTER);
            check(expected.isInstance(game.getScreen()),"Resume returns to the saved screen: "+expected.getSimpleName());
            return session(game);
        }
        @Override public void close(){game.dispose();}
    }

    public static void verify()throws Exception {
        verifyRoomStartReloadAndCancel();
        verifySafeExitAndBossStart();
        verifyBoonRewardDoesNotRepeat();
        verifyRecoveryDoesNotRepeat();
        verifyNpcStages();
        verifyConfirmedAbandonment();
        verifyFailedCheckpointRetry();
        verifyRemappedShortcutKeepsTheButton();
        verifyDeathIsTerminalAndOldBackupCannotResume();
        verifyFailedDeathSettlementRecovery();
        System.out.println("CHECKPOINT_SMOKE_OK: JSON reload, room starts, safe exits, all safe stages, protected resume, confirmed abandonment, exact rewards, remapped shortcut, terminal death, consumed backups and save retry");
    }

    private static void verifyRoomStartReloadAndCancel()throws Exception {
        try(Fixture fixture=new Fixture()) {
            RunSession live=fixture.start();RunCheckpoint first=fixture.game.profiles().profile().checkpoint();
            check(first!=null && first.stage()==RunCheckpoint.Stage.ROOM_START && first.sequence()==1 && first.revision()==1,
                "Starting a campaign durably stores its first room before gameplay");
            check(first.runId().equals(live.id()) && live.attemptSequence()==1,"The live attempt receives the committed identity");
            float originalLife=live.player().getVida();
            live.applyUpgrade(Upgrade.FORCA);live.player().sofrerDano(17);live.advance(12.5f);
            fixture.reload();fixture.game.enterCampaign();Screen resume=fixture.game.getScreen();
            check(resume instanceof ResumeRunScreen,"Closing during combat keeps the last room-start checkpoint");
            resume.render(.3f);captureNpcDialogue(resume,"m8-checkpoint-resume");
            int writes=fixture.store.writes;key(fixture.game,Input.Keys.ESCAPE);
            check(fixture.game.getScreen() instanceof MenuScreen && fixture.store.writes==writes
                && fixture.game.profiles().profile().checkpoint().runId().equals(first.runId()),
                "Cancelling resume returns to the menu without discarding or settling the attempt");
            RunSession restored=fixture.resume(GameScreen.class);
            check(restored!=live && restored.id().equals(first.runId()) && restored.characterId().equals(first.characterId())
                && restored.attemptSequence()==1 && restored.arenaElapsed()==0 && restored.progress().elapsed()==0,
                "Reload reconstructs the same attempt at the beginning of the saved room");
            check(restored.player().getVida()==originalLife && restored.rank(Upgrade.FORCA)==0,
                "Unsaved live combat does not replace the room-start health or build");
            check(fixture.game.profiles().profile().attempts()==0 && fixture.game.latestReport()==null
                && fixture.game.shared().progress().souls()==0,"Reloading and cancelling do not create results or shared rewards");
        }
    }

    private static void verifySafeExitAndBossStart()throws Exception {
        try(Fixture fixture=new Fixture()) {
            Main game=fixture.game;RunSession live=fixture.start();forceEncounters(live,null);
            live.applyUpgrade(Upgrade.FORCA);live.player().sofrerDano(21);live.addChestObols(7);live.advance(11.25f);complete(live);
            game.offerRoute(live);check(game.getScreen() instanceof RouteScreen,"A completed arena reaches a safe route screen");
            check(game.profiles().profile().checkpoint().stage()==RunCheckpoint.Stage.ROUTE,"The route itself is a persisted safe stage");
            String state=json(live.checkpointState());int obols=game.profiles().profile().obols(),attempts=game.profiles().profile().attempts();
            int souls=game.shared().progress().souls();float elapsed=live.progress().elapsed();
            game.getScreen().render(.3f);captureNpcDialogue(game.getScreen(),"m8-checkpoint-route");key(game,Input.Keys.F5);
            check(game.getScreen() instanceof MenuScreen && !live.finished() && live.progress().elapsed()==elapsed,
                "F5 on a safe route saves and exits without ending the attempt or advancing its clocks");
            check(game.profiles().profile().obols()==obols && game.profiles().profile().attempts()==attempts
                && game.shared().progress().souls()==souls && game.latestReport()==null,"Saving and exiting grants no terminal rewards or statistics");
            fixture.reload();RunSession restored=fixture.resume(RouteScreen.class);game=fixture.game;
            check(json(restored.checkpointState()).equals(state),"Safe resume preserves the complete build, current life, pending currencies and statistics");
            key(game,Input.Keys.NUM_1);
            check(game.getScreen() instanceof BossRoomScreen,"The route continues into the actual boss arena");
            RunCheckpoint bossStart=game.profiles().profile().checkpoint();RunSession bossRun=session(game);
            check(bossStart.stage()==RunCheckpoint.Stage.ROOM_START && bossStart.runId().equals(live.id())
                && bossStart.sequence()==1 && bossStart.revision()>1 && bossRun.arenaElapsed()==0,
                "Entering the boss commits a new room start while retaining attempt identity");
            fixture.reload();RunSession resumedBoss=fixture.resume(BossRoomScreen.class);
            BossEncounter boss=(BossEncounter)field(fixture.game.getScreen(),"boss");
            check(resumedBoss.arena().kind()==ArenaDefinition.Kind.BOSS_ROOM && resumedBoss.player().getVida()==restored.player().getVida()
                && boss.elapsed()==0 && boss.health()==boss.maxHealth(),"A boss-room checkpoint reconstructs a fresh encounter without healing the player");
        }
    }

    private static void verifyBoonRewardDoesNotRepeat()throws Exception {
        try(Fixture fixture=new Fixture()) {
            RunSession run=optionalArena(fixture,"sombras");complete(run);fixture.game.offerRoute(run);
            check(fixture.game.getScreen() instanceof ArenaRewardScreen,"The rare arena reward is offered before routes");
            List<Upgrade> choices=List.copyOf(run.arenaRewardChoices());check(!choices.isEmpty(),"The checkpoint fixture has eligible rare rewards");
            check(fixture.game.profiles().profile().checkpoint().stage()==RunCheckpoint.Stage.ARENA_REWARD,"Pending rewards have their own checkpoint stage");
            key(fixture.game,Input.Keys.F5);check(fixture.game.getScreen() instanceof MenuScreen,"F5 preserves the pending boon before leaving");
            fixture.reload();RunSession restored=fixture.resume(ArenaRewardScreen.class);
            check(restored.arenaRewardChoices().equals(choices),"Reload cannot reroll the persisted reward choices");
            captureNpcDialogue(fixture.game.getScreen(),"m8-checkpoint-reward");key(fixture.game,Input.Keys.NUM_1);
            check(fixture.game.getScreen() instanceof RouteScreen && restored.rank(choices.getFirst())==1 && !restored.canClaimArenaReward(),
                "A chosen reward applies once and stores the consumed route stage");
            key(fixture.game,Input.Keys.F5);fixture.reload();RunSession consumed=fixture.resume(RouteScreen.class);
            check(consumed.rank(choices.getFirst())==1 && !consumed.canClaimArenaReward(),"A second reload neither offers nor applies the boon again");
        }
    }

    private static void verifyRecoveryDoesNotRepeat()throws Exception {
        try(Fixture fixture=new Fixture()) {
            RunSession run=optionalArena(fixture,"cinzas");run.player().sofrerDano(60);complete(run);fixture.game.offerRoute(run);
            float before=run.player().getVida();key(fixture.game,Input.Keys.F5);fixture.reload();RunSession pending=fixture.resume(ArenaRewardScreen.class);
            check(pending.player().getVida()==before,"Resuming an unopened recovery chest does not heal");
            key(fixture.game,Input.Keys.NUM_1);float healed=pending.player().getVida();
            check(healed==Math.min(pending.player().getVidaMaxima(),before+40) && fixture.game.getScreen() instanceof RouteScreen,
                "Choosing recovery heals exactly once before showing routes");
            key(fixture.game,Input.Keys.F5);fixture.reload();RunSession consumed=fixture.resume(RouteScreen.class);
            check(consumed.player().getVida()==healed && !consumed.canClaimArenaReward(),"The consumed recovery checkpoint never repeats its heal");
        }
    }

    private static void verifyNpcStages()throws Exception {
        for(RunCheckpoint.Stage stage:List.of(RunCheckpoint.Stage.BLACKSMITH,RunCheckpoint.Stage.FARMER)) {
            try(Fixture fixture=new Fixture()) {
                RunSession live=fixture.start();forceEncounters(live,stage);complete(live);fixture.game.offerRoute(live);
                Class<? extends Screen> expected=stage==RunCheckpoint.Stage.BLACKSMITH?BlacksmithScreen.class:FarmerQuestScreen.class;
                check(expected.isInstance(fixture.game.getScreen()) && fixture.game.profiles().profile().checkpoint().stage()==stage,
                    "The NPC encounter persists its exact safe stage: "+stage);
                key(fixture.game,Input.Keys.F5);check(fixture.game.getScreen() instanceof MenuScreen,"F5 exits the NPC without consuming the encounter");
                fixture.reload();RunSession restored=fixture.resume(expected);
                check(restored.id().equals(live.id()) && (stage==RunCheckpoint.Stage.BLACKSMITH?restored.encounters().blacksmithOpen():restored.encounters().farmerOpen()),
                    "An already rolled NPC remains open instead of being rerolled after reload");
                key(fixture.game,Input.Keys.ESCAPE);
                check(fixture.game.getScreen() instanceof RouteScreen,"Leaving a resumed NPC continues the same safe route");
                key(fixture.game,Input.Keys.F5);fixture.reload();RunSession closed=fixture.resume(RouteScreen.class);
                check(!closed.encounters().blacksmithOpen() && !closed.encounters().farmerOpen(),"A consumed NPC stays closed after another reload");
            }
        }
    }

    private static void verifyConfirmedAbandonment()throws Exception {
        try(Fixture fixture=new Fixture()) {
            RunSession run=fixture.start();forceEncounters(run,null);complete(run);run.addChestObols(5);fixture.game.offerRoute(run);
            int earnedObols=run.earnedObols(),earnedSouls=run.earnedSouls();key(fixture.game,Input.Keys.F5);
            fixture.game.enterCampaign();key(fixture.game,Input.Keys.DEL);
            check(fixture.game.getScreen() instanceof ResumeRunScreen && fixture.game.profiles().profile().checkpoint()!=null
                && fixture.game.profiles().profile().attempts()==0,"Delete only asks for abandonment confirmation");
            captureNpcDialogue(fixture.game.getScreen(),"m8-checkpoint-abandon");key(fixture.game,Input.Keys.ESCAPE);
            check(fixture.game.getScreen() instanceof ResumeRunScreen && fixture.game.profiles().profile().checkpoint()!=null,
                "Cancelling abandonment keeps the saved attempt available");
            key(fixture.game,Input.Keys.DEL);key(fixture.game,Input.Keys.ENTER);
            check(fixture.game.getScreen() instanceof RefugeScreen && fixture.game.profiles().profile().checkpoint()==null
                && fixture.game.profiles().profile().attempts()==1,"Only confirmed abandonment ends and clears the saved attempt");
            check(fixture.game.profiles().profile().obols()==earnedObols && fixture.game.shared().progress().souls()==earnedSouls
                && fixture.game.latestReport()!=null && fixture.game.latestReport().id().equals(run.id()),
                "Confirmed abandonment settles the saved reward totals and report once");
            fixture.reload();fixture.game.enterCampaign();
            check(fixture.game.getScreen() instanceof RefugeScreen && fixture.game.profiles().profile().attempts()==1
                && fixture.game.profiles().profile().obols()==earnedObols && fixture.game.shared().progress().souls()==earnedSouls,
                "Reload after abandonment cannot revive the checkpoint or duplicate its rewards");
            fixture.game.startCampaign();check(fixture.game.profiles().profile().checkpoint().sequence()==2,"A later attempt receives the next durable sequence");
        }
    }

    private static void verifyFailedCheckpointRetry()throws Exception {
        try(Fixture fixture=new Fixture()) {
            fixture.character();fixture.store.fail=true;fixture.game.startCampaign();
            check(fixture.game.getScreen() instanceof CheckpointSaveErrorScreen && fixture.game.profiles().profile().checkpoint()==null
                && new ProfileService(fixture.store).profile().checkpoint()==null,"A failed first checkpoint blocks combat and preserves the last durable profile");
            key(fixture.game,Input.Keys.ENTER);check(fixture.game.getScreen() instanceof CheckpointSaveErrorScreen,"A failed retry remains safely on the error screen");
            fixture.store.fail=false;key(fixture.game,Input.Keys.ENTER);
            check(fixture.game.getScreen() instanceof GameScreen && fixture.game.profiles().profile().checkpoint().sequence()==1,
                "A successful retry commits once before allowing the first combat screen");
            RunSession run=session(fixture.game);forceEncounters(run,null);complete(run);
            RunCheckpoint original=fixture.game.profiles().profile().checkpoint();fixture.store.fail=true;fixture.game.offerRoute(run);
            check(fixture.game.getScreen() instanceof CheckpointSaveErrorScreen && fixture.game.profiles().profile().checkpoint().revision()==original.revision()
                && !run.finished() && fixture.game.profiles().profile().attempts()==0,"Failed safe-stage saving neither replaces the old checkpoint nor settles the run");
            fixture.game.getScreen().render(.3f);captureNpcDialogue(fixture.game.getScreen(),"m8-checkpoint-save-error");
            fixture.store.fail=false;key(fixture.game,Input.Keys.ENTER);
            RunCheckpoint saved=fixture.game.profiles().profile().checkpoint();
            check(fixture.game.getScreen() instanceof RouteScreen && saved.stage()==RunCheckpoint.Stage.ROUTE
                && saved.runId().equals(original.runId()) && saved.sequence()==original.sequence() && saved.revision()==original.revision()+1,
                "Retry writes the intended safe stage once using the original attempt identity");
        }
    }

    private static void verifyRemappedShortcutKeepsTheButton()throws Exception {
        try(Fixture fixture=new Fixture()) {
            RunSession run=fixture.start();forceEncounters(run,null);complete(run);fixture.game.offerRoute(run);
            Screen route=fixture.game.getScreen();check(route instanceof RouteScreen,"The remapping fixture reaches a checkpointed route");
            check(fixture.game.controlSettings().apply(ControlSettings.DEFAULT.withBinding(ControlAction.BOONS,0,InputBinding.key(Input.Keys.F5))),
                "Existing custom controls may use F5 for the boon reference action");
            long revision=fixture.game.profiles().profile().checkpoint().revision();key(fixture.game,Input.Keys.F5);
            check(fixture.game.getScreen()==route && fixture.game.profiles().profile().checkpoint().revision()==revision,
                "F5 assigned to a gameplay action never also triggers save and exit");
            Rectangle button=(Rectangle)field(field(route,"safeExit"),"button");
            route.render(.31f);click(route,button);
            check(fixture.game.getScreen() instanceof MenuScreen && !run.finished()
                && fixture.game.profiles().profile().checkpoint().revision()==revision+1,
                "The visible safe-exit button remains usable when its optional shortcut conflicts with custom controls");
        }
    }

    private static void verifyDeathIsTerminalAndOldBackupCannotResume()throws Exception {
        try(Fixture fixture=new Fixture()) {
            RunSession run=fixture.start();String olderActiveBackup=fixture.store.saved;
            complete(run);run.addChestObols(7);run.player().sofrerDano(10000);
            int obols=run.earnedObols(),souls=run.earnedSouls();String identity=run.characterId();
            fixture.game.finishRun(run,RunOutcome.DEATH);
            check(fixture.game.getScreen() instanceof GameOverScreen && fixture.game.profiles().profile().checkpoint()==null
                && fixture.game.profiles().profile().pendingSettlement()==null && fixture.game.profiles().profile().attempts()==1
                && fixture.game.profiles().profile().deaths()==1,"A real Main death settles and durably clears the active checkpoint");
            check(fixture.game.profiles().profile().obols()==obols && fixture.game.shared().progress().souls()==souls
                && fixture.game.shared().progress().history().size()==1 && fixture.game.latestReport().outcome()==RunOutcome.DEATH
                && fixture.game.shared().progress().highestCompletedSequence(identity)==1,
                "Death credits personal currency, shared souls and one report with its consumed sequence");
            fixture.game.finishRun(run,RunOutcome.DEATH);
            check(fixture.game.profiles().profile().attempts()==1 && fixture.game.profiles().profile().obols()==obols
                && fixture.game.shared().progress().souls()==souls && fixture.game.shared().progress().history().size()==1,
                "A repeated terminal callback cannot credit the same death twice");
            fixture.reload();fixture.game.enterCampaign();
            check(fixture.game.getScreen() instanceof RefugeScreen && fixture.game.profiles().profile().checkpoint()==null
                && fixture.game.profiles().profile().attempts()==1 && fixture.game.profiles().profile().obols()==obols
                && fixture.game.shared().progress().souls()==souls,
                "Restarting after a recorded death returns to the refuge instead of offering the old room");

            fixture.store.saved=olderActiveBackup;fixture.reload();fixture.game.enterCampaign();
            check(fixture.game.getScreen() instanceof RefugeScreen && fixture.game.profiles().profile().checkpoint()==null
                && fixture.game.profiles().profile().attemptSequence()==1 && fixture.game.profiles().profile().lastSettledSequence()==1,
                "A restored old profile backup is reconciled against the shared consumed-sequence ledger before resume");
            check(fixture.game.shared().progress().souls()==souls && fixture.game.shared().progress().history().size()==1
                && fixture.game.profiles().profile().obols()==0 && fixture.game.profiles().profile().attempts()==0,
                "Discarding a consumed backup never invents or repeats personal rewards, shared rewards or history");
            fixture.game.startCampaign();
            check(fixture.game.getScreen() instanceof GameScreen && fixture.game.profiles().profile().checkpoint().sequence()==2,
                "The next attempt advances beyond a consumed sequence even when the profile came from an older backup");
        }
    }

    private static void verifyFailedDeathSettlementRecovery()throws Exception {
        try(Fixture fixture=new Fixture()) {
            RunSession run=fixture.start();complete(run);run.addChestObols(9);run.player().sofrerDano(10000);
            int obols=run.earnedObols(),souls=run.earnedSouls();fixture.store.fail=true;
            fixture.game.finishRun(run,RunOutcome.DEATH);
            check(fixture.game.profiles().profile().checkpoint()==null && fixture.game.profiles().pendingSettlement()!=null
                && !fixture.game.profiles().settlementDurable() && fixture.game.profiles().profile().attempts()==1,
                "A failed terminal profile write retains the settled result and receipt in memory for recovery");
            check(new ProfileService(fixture.store).profile().checkpoint()!=null && fixture.game.shared().progress().souls()==0
                && fixture.game.shared().progress().history().isEmpty() && !fixture.game.recoverPendingSettlement(),
                "Shared rewards wait until the terminal profile receipt is durable, including when a retry also fails");
            fixture.store.fail=false;check(fixture.game.recoverPendingSettlement(),"Recovering retries the terminal profile, shared credit and acknowledgement");
            check(fixture.game.profiles().settlementDurable() && fixture.game.profiles().pendingSettlement()==null
                && fixture.game.profiles().profile().checkpoint()==null && fixture.game.profiles().profile().obols()==obols
                && fixture.game.shared().progress().souls()==souls && fixture.game.shared().progress().history().size()==1,
                "A successful terminal retry preserves its earned totals and consumes the receipt exactly once");
            check(fixture.game.recoverPendingSettlement() && fixture.game.profiles().profile().attempts()==1
                && fixture.game.shared().progress().souls()==souls && fixture.game.shared().progress().history().size()==1,
                "Recovery is idempotent after its receipt has been acknowledged");
            fixture.reload();fixture.game.enterCampaign();
            check(fixture.game.getScreen() instanceof RefugeScreen && fixture.game.profiles().profile().checkpoint()==null
                && fixture.game.profiles().profile().attempts()==1 && fixture.game.profiles().profile().obols()==obols
                && fixture.game.shared().progress().souls()==souls,"The recovered terminal transaction remains final after a JSON reload");
        }
    }

    private static RunSession optionalArena(Fixture fixture,String destination)throws Exception {
        RunSession run=fixture.start();forceEncounters(run,null);complete(run);fixture.game.offerRoute(run);
        int index=run.arena().exits().indexOf(destination);check(index>=0,"The optional route exists in the campaign");
        key(fixture.game,Input.Keys.NUM_1+index);check(fixture.game.getScreen() instanceof GameScreen && run.arena().id().equals(destination),"The real route UI enters "+destination);
        return run;
    }
    private static RunSession session(Main game)throws Exception{return (RunSession)field(game.getScreen(),"session");}
    private static void complete(RunSession run){
        for(var goal:run.arena().killGoals().entrySet())while(run.kills(goal.getKey())<goal.getValue())run.recordKill(goal.getKey());
    }
    private static void forceEncounters(RunSession run,RunCheckpoint.Stage stage){
        JsonValue state=run.encounters().checkpointState();
        state.get("blacksmithScheduled").set(stage==RunCheckpoint.Stage.BLACKSMITH);
        state.get("blacksmithVisited").set(false);state.get("blacksmithOpen").set(false);
        state.get("farmerRolled").set(true);state.get("farmerScheduled").set(stage==RunCheckpoint.Stage.FARMER);
        state.get("farmerVisited").set(false);state.get("farmerOpen").set(false);
        run.encounters().restoreCheckpointState(state);
    }
    private static void key(Main game,int key)throws Exception {game.getScreen().render(.31f);press(game.getScreen(),key);}
    private static void click(Screen screen,Rectangle target)throws Exception {
        FitViewport viewport=(FitViewport)field(screen,"viewport");
        Vector2 pointer=viewport.project(new Vector2(target.x+target.width/2,target.y+target.height/2));
        int x=Math.round(pointer.x),y=Gdx.graphics.getHeight()-Math.round(pointer.y);Input original=Gdx.input;
        Gdx.input=(Input)Proxy.newProxyInstance(Input.class.getClassLoader(),new Class<?>[]{Input.class},(proxy,method,args)->switch(method.getName()) {
            case "isKeyPressed", "isKeyJustPressed" -> false;
            case "isButtonPressed", "isButtonJustPressed" -> (int)args[0]==Input.Buttons.LEFT;
            case "getX" -> x;
            case "getY" -> y;
            default -> method.invoke(original,args);
        });
        try {screen.render(0);}finally {Gdx.input=original;}
    }
    private static String json(JsonValue state){return state.toJson(JsonWriter.OutputType.json);}
}
