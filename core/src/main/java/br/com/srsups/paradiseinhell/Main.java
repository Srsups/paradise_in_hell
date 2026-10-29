package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;

public class Main extends Game {
    private PilotArt visuals;
    private boolean instantDialogue;
    public boolean instantDialogue() { return instantDialogue; }
    public void instantDialogue(boolean value) { instantDialogue=value; }
    public PilotArt visuals() { if(visuals==null)visuals=new PilotArt();return visuals; }
    private final SaveSlots saves;
    private final ProfileService[] profiles = new ProfileService[SaveSlots.COUNT];
    private int activeSlot;
    private SharedProgressService shared;
    private final AudioSettingsStore audioStore;
    private final AudioBackend suppliedAudio;
    private AudioSettingsService audioSettings;
    private GameAudio audio;
    private final VideoSettingsStore videoStore;
    private final VideoBackend suppliedVideo;
    private VideoSettingsService video;
    private final ControlSettingsStore controlStore;
    private ControlSettingsService controlSettings;
    private GameControls controls;
    private final HudSettingsStore hudStore;
    private HudSettingsService hudSettings;
    private boolean closing;
    public Main() { this(new GdxVideoBackend()); }
    public Main(VideoBackend desktopVideo) { this(SaveSlots.files(FileProfileStore.defaultPath()),
        new FileAudioSettingsStore(FileProfileStore.defaultPath().resolveSibling("audio-settings.json")),null,
        new FileVideoSettingsStore(FileProfileStore.defaultPath().resolveSibling("video-settings.json")),desktopVideo,
        new FileControlSettingsStore(FileProfileStore.defaultPath().resolveSibling("control-settings.json")),
        new FileHudSettingsStore(FileProfileStore.defaultPath().resolveSibling("hud-settings.json"))); }
    public Main(ProfileStore first) { this(new SaveSlots(first)); }
    public Main(SaveSlots saves) { this(saves,AudioSettingsStore.memory(),null); }
    public Main(SaveSlots saves,AudioSettingsStore audioStore,AudioBackend output) {
        this(saves,audioStore,output,VideoSettingsStore.memory(),VideoBackend.memory());
    }
    public Main(SaveSlots saves,AudioSettingsStore audioStore,AudioBackend output,VideoSettingsStore videoStore,VideoBackend videoBackend) {
        this(saves,audioStore,output,videoStore,videoBackend,ControlSettingsStore.memory());
    }
    public Main(SaveSlots saves,AudioSettingsStore audioStore,AudioBackend output,VideoSettingsStore videoStore,VideoBackend videoBackend,
                ControlSettingsStore controlStore) {
        this(saves,audioStore,output,videoStore,videoBackend,controlStore,HudSettingsStore.memory());
    }
    public Main(SaveSlots saves,AudioSettingsStore audioStore,AudioBackend output,VideoSettingsStore videoStore,VideoBackend videoBackend,
                ControlSettingsStore controlStore,HudSettingsStore hudStore) {
        this.saves=saves;this.audioStore=audioStore;this.suppliedAudio=output;
        this.videoStore=java.util.Objects.requireNonNull(videoStore);this.suppliedVideo=java.util.Objects.requireNonNull(videoBackend);
        this.controlStore=java.util.Objects.requireNonNull(controlStore);
        this.hudStore=java.util.Objects.requireNonNull(hudStore);
    }
    public HudSettingsService hudSettings() {
        if(hudSettings==null)hudSettings=new HudSettingsService(hudStore);return hudSettings;
    }
    public ControlSettingsService controlSettings() {
        if(controlSettings==null)controlSettings=new ControlSettingsService(controlStore);return controlSettings;
    }
    public GameControls controls() {
        if(controls==null)controls=new GameControls(controlSettings());return controls;
    }
    public VideoSettingsService video() {
        if(video==null)video=new VideoSettingsService(videoStore,suppliedVideo);return video;
    }
    public AudioSettingsService audioSettings() {
        if(audioSettings==null)audioSettings=new AudioSettingsService(audioStore);return audioSettings;
    }
    public GameAudio audio() {
        if(audio==null)audio=new GameAudio(audioSettings(),suppliedAudio==null?new GdxAudioBackend():suppliedAudio);
        return audio;
    }
    @Override public void render() {
        if(closing || video!=null && video.busy())return;
        if(controls!=null)controls.poll();
        if(video!=null)video.tick();
        audio().advance(Gdx.graphics.getDeltaTime());super.render();
    }
    @Override public void create() { profileAt(0); audio(); setScreen(new MenuScreen(this));video().initialize(); }
    @Override public void pause(){if(video!=null)video.cancelPreview();super.pause();}
    public ProfileService profiles() { return profileAt(activeSlot); }
    public ProfileService profileAt(int index) {
        ProfileStore store = saves.store(index);
        if (profiles[index] == null) profiles[index] = new ProfileService(store);
        return profiles[index];
    }
    public SharedProgressService shared() {
        if(shared==null) {
            shared=new SharedProgressService(saves.sharedStore());
            for(int i=0;i<SaveSlots.COUNT;i++) {
                CampaignProfile old=profileAt(i).profile();
                if(old.necromancerUnlocked() || old.professionChosen() && old.profession()==Profession.NECROMANTE)
                    shared.preserveLegacyUnlock();
                if(old.farmerQuest()==FarmerQuest.COMPLETED || old.professionChosen() && old.profession()==Profession.FAZENDEIRO)
                    shared.preserveFarmerUnlock();
            }
        }
        return shared;
    }
    public boolean necromancerUnlocked() { return shared().progress().necromancerUnlocked(); }
    public boolean unlockNecromancer() { return shared().unlockNecromancer(); }
    public boolean farmerUnlocked() { return shared().progress().farmerUnlocked(); }
    public boolean professionUnlocked(Profession profession) {
        return profession == Profession.NECROMANTE ? necromancerUnlocked()
            : profession != Profession.FAZENDEIRO || farmerUnlocked();
    }
    public RunReport latestReport() { return shared().progress().latest(activeSlot, profiles().profile().characterId()); }
    public boolean deleteSave(int index) {
        ProfileService target = profileAt(index);
        SharedProgressService global = shared(); // Preserve an older character's legendary access before resetting it.
        if(!recoverPendingSettlement(target))return false;
        CampaignProfile old = target.profile();
        if ((old.necromancerUnlocked() || old.professionChosen() && old.profession() == Profession.NECROMANTE
            || old.farmerQuest()==FarmerQuest.COMPLETED || old.professionChosen() && old.profession()==Profession.FAZENDEIRO)
            && !global.error().isEmpty() && !global.retrySave()) return false;
        return target.reset();
    }
    public void openLastReport() {
        RunReport report=latestReport();
        if(report!=null) setScreen(new RunReportScreen(this,report,()->new RefugeScreen(this)));
    }
    public boolean hasUnderworldEpilogue() {
        return profiles().profile().professionChosen()
            && profiles().profile().has(CampaignProfile.Event.INSURGENCE_SPARK_OBTAINED);
    }
    public void openUnderworldEpilogue(java.util.function.Supplier<Screen> returnTo) {
        setScreen(new UnderworldEndingScreen(this,profiles().profile().characterId(),returnTo));
    }
    public boolean chooseProfession(Profession profession) {
        return profiles().chooseProfession(profession, necromancerUnlocked(), farmerUnlocked());
    }
    public boolean createCharacter(Profession profession, GameDifficulty difficulty) {
        return profiles().createCharacter(profession, difficulty, necromancerUnlocked(), farmerUnlocked());
    }
    public int activeSlot() { return activeSlot; }
    public RegionDefinition campaignRegion() {
        return RegionDefinition.fromJson(Gdx.files.internal(RegionDefinition.PROTOTYPE_ASSET).readString("UTF-8"));
    }
    public void openCampaign() { setScreen(new SaveSelectionScreen(this, false)); }
    public void openDemo() { setScreen(new SaveSelectionScreen(this, true)); }
    public void openOpening(java.util.function.Supplier<Screen> returnTo,boolean recordCompletion) {
        CampaignProfile profile=profiles().profile();
        if(recordCompletion && !profile.professionChosen())throw new IllegalStateException("Choose a character before the campaign introduction");
        setScreen(new OpeningScreen(this,profile.characterId(),recordCompletion,returnTo));
    }
    public void enterCampaign() {
        if(!profiles().profile().professionChosen())throw new IllegalStateException("Choose a character first");
        if(!recoverPendingSettlement()) { settlementRetry(this::enterCampaign); return; }
        if(profiles().profile().checkpoint()!=null) { setScreen(new ResumeRunScreen(this)); return; }
        if(profiles().profile().openingPending())openOpening(()->new RefugeScreen(this),true);
        else setScreen(new RefugeScreen(this));
    }
    public void selectSave(int index, boolean demo) {
        profileAt(index); activeSlot = index;
        if (!profiles().profile().professionChosen()) setScreen(new ProfessionScreen(this, demo ? null : campaignRegion()));
        else if (demo) startDemo();
        else enterCampaign();
    }
    public void startDemo() {
        if (!profiles().profile().professionChosen()) throw new IllegalStateException("Choose a save profession first");
        setScreen(new GameScreen(this, RunSession.demo(profiles().profile().profession(), profiles().profile().difficulty())));
    }
    public void startCampaign() {
        CampaignProfile profile = profiles().profile();
        if (!profile.professionChosen()) throw new IllegalStateException("Choose a save profession first");
        if(!recoverPendingSettlement() || !profiles().ensureCharacterIdentity()) { settlementRetry(this::startCampaign); return; }
        profile=profiles().profile();
        if(profile.checkpoint()!=null) { setScreen(new ResumeRunScreen(this)); return; }
        RunSession run=RunSession.campaign(campaignRegion(),profile.profession(),profile.bonuses(),profile.difficulty());
        profiles().prepareRun(run);
        reachSafePoint(run,RunCheckpoint.Stage.ROOM_START,()->combatScreen(run));
    }
    /** Finish the two-file settlement transaction before another attempt can spend its rewards. */
    public boolean recoverPendingSettlement(){return recoverPendingSettlement(profiles());}
    private boolean recoverPendingSettlement(ProfileService profile) {
        RunSettlement receipt=profile.pendingSettlement();
        if(receipt!=null) {
            if(!profile.settlementDurable() && !profile.retrySave())return false;
            if(!shared().settleReceipt(receipt) || !profile.acknowledgeSettlement(receipt))return false;
        }
        if(!profile.profile().professionChosen())return true;
        long completed=shared().progress().highestCompletedSequence(profile.profile().characterId());
        if(profile.profile().checkpoint()!=null && profile.profile().checkpoint().sequence()<=completed)
            if(!profile.discardCompletedCheckpoint(completed))return false;
        return profile.profile().checkpoint()!=null || profile.syncAttemptSequence(completed);
    }
    private void settlementRetry(Runnable retry) {
        setScreen(new CheckpointSaveErrorScreen(this,"Não foi possível concluir o registro do personagem.",retry));
    }
    private Screen combatScreen(RunSession run) {
        return run.arena().kind()==ArenaDefinition.Kind.BOSS_ROOM?new BossRoomScreen(this,run):new GameScreen(this,run);
    }
    private void reachSafePoint(RunSession run,RunCheckpoint.Stage stage,java.util.function.Supplier<Screen> next) {
        if(profiles().saveCheckpoint(run,stage))setScreen(next.get());
        else setScreen(new CheckpointSaveErrorScreen(this,"O checkpoint ainda não foi gravado.",()->reachSafePoint(run,stage,next)));
    }
    private void safeScreen(RunSession run,RunCheckpoint.Stage stage,java.util.function.Supplier<Screen> next) {
        if(run.attemptSequence()>0)reachSafePoint(run,stage,next);else setScreen(next.get());
    }
    public void enterNextArena(RunSession run,String destination) {
        run.enterArena(destination);
        safeScreen(run,RunCheckpoint.Stage.ROOM_START,()->combatScreen(run));
    }
    public void saveAndExit(RunSession run,RunCheckpoint.Stage stage) {
        if(run.isDemo()||run.attemptSequence()==0)throw new IllegalStateException("No campaign checkpoint");
        reachSafePoint(run,stage,()->new MenuScreen(this));
    }
    public void resumeCampaign() {
        if(!recoverPendingSettlement()) { settlementRetry(this::resumeCampaign);return; }
        RunCheckpoint checkpoint=profiles().profile().checkpoint();
        if(checkpoint==null) { enterCampaign();return; }
        final RunSession run;
        try { run=checkpoint.restore(campaignRegion());profiles().prepareRun(run); }
        catch(IllegalArgumentException | IllegalStateException invalid) { setScreen(new ResumeRunScreen(this));return; }
        setScreen(switch(checkpoint.stage()) {
            case ROOM_START -> combatScreen(run);
            case ARENA_REWARD -> new ArenaRewardScreen(this,run);
            case BLACKSMITH -> new BlacksmithScreen(this,run);
            case FARMER -> new FarmerQuestScreen(this,run);
            case ROUTE -> new RouteScreen(this,run);
        });
    }
    /** Called only after the resume screen's explicit confirmation. */
    public void abandonSavedAttempt() {
        if(!recoverPendingSettlement()) { settlementRetry(this::abandonSavedAttempt);return; }
        RunCheckpoint checkpoint=profiles().profile().checkpoint();
        if(checkpoint==null) { enterCampaign();return; }
        RunSession run;
        try { run=checkpoint.restore(campaignRegion());profiles().prepareRun(run); }
        catch(IllegalArgumentException | IllegalStateException invalid) {
            if(profiles().discardIncompatibleCheckpoint())enterCampaign();else settlementRetry(this::abandonSavedAttempt);
            return;
        }
        finishRun(run,RunOutcome.ABANDONED);
    }
    public void offerRoute(RunSession session) {
        if (session.isDemo() || session.finished() || !session.arenaComplete()) throw new IllegalStateException("No safe exit");
        if (session.canClaimArenaReward()) {
            if(session.arena().reward()==ArenaReward.RARE_BOON)session.arenaRewardChoices();
            safeScreen(session,RunCheckpoint.Stage.ARENA_REWARD,()->new ArenaRewardScreen(this,session));return;
        }
        if (session.encounters().blacksmithOpen() || session.encounters().openBlacksmith()) {
            safeScreen(session,RunCheckpoint.Stage.BLACKSMITH,()->new BlacksmithScreen(this,session));return;
        }
        boolean eligible = !farmerUnlocked() && profiles().profile().farmerQuest()==FarmerQuest.NOT_STARTED
            && session.characterId().equals(profiles().profile().characterId());
        if (session.encounters().farmerOpen() || session.openFarmerEncounter(eligible)) {
            safeScreen(session,RunCheckpoint.Stage.FARMER,()->new FarmerQuestScreen(this,session));return;
        }
        safeScreen(session,RunCheckpoint.Stage.ROUTE,()->new RouteScreen(this,session));
    }
    public void finishRun(RunSession session, RunOutcome outcome) {
        finishRun(session,outcome,java.util.function.Function.identity());
    }
    /** Settle immediately; a cosmetic ending may briefly present the completed attempt. */
    public void finishRun(RunSession session, RunOutcome outcome,java.util.function.Function<Screen,Screen> presentation) {
        boolean firstFinish = !session.finished();
        session.end();
        if (!session.isDemo()) profiles().settle(session, outcome,activeSlot);
        if (!session.isDemo() && profiles().profile().farmerQuest()==FarmerQuest.COMPLETED) shared().preserveFarmerUnlock();
        if(session.attemptSequence()>0)recoverPendingSettlement();else shared().settle(session,outcome,activeSlot);
        boolean epiloguePending=firstFinish && outcome==RunOutcome.VICTORY && !session.isDemo()
            && session.region().id().equals(RegionDefinition.PROTOTYPE_ID)
            && session.characterId().equals(profiles().profile().characterId()) && hasUnderworldEpilogue()
            && !profiles().profile().has(CampaignProfile.Event.UNDERWORLD_EPILOGUE_READ);
        Screen destination=outcome == RunOutcome.ABANDONED
            ? session.isDemo() ? new MenuScreen(this) : new RefugeScreen(this)
            : epiloguePending?new UnderworldEndingScreen(this,session.characterId(),()->new GameOverScreen(this,true,session))
                : new GameOverScreen(this, outcome == RunOutcome.VICTORY, session);
        setScreen(firstFinish && outcome!=RunOutcome.ABANDONED?presentation.apply(destination):destination);
        if(firstFinish && outcome!=RunOutcome.ABANDONED)audio().play(outcome==RunOutcome.VICTORY?SoundCue.VICTORY:SoundCue.DEATH);
    }
    @Override public void setScreen(Screen next) {
        Screen previous = getScreen();
        if (previous == next) return;
        if(video!=null)video.cancelPreview();
        super.setScreen(next);
        if (previous != null) previous.dispose();
        if(audio!=null) {
            audio.stopEffects();
            if(next!=null)audio.music(next instanceof GameScreen || next instanceof BossRoomScreen ? AudioBackend.Track.COMBAT : AudioBackend.Track.REFUGE);
        }
    }
    @Override public void dispose() {
        if(closing)return;closing=true;if(video!=null)video.cancelPreview();
        setScreen(null);if(audio!=null)audio.dispose();if(visuals!=null)visuals.dispose();
    }
}
