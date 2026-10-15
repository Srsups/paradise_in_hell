package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;

public class Main extends Game {
    private final SaveSlots saves;
    private final ProfileService[] profiles = new ProfileService[SaveSlots.COUNT];
    private int activeSlot;
    private SharedProgressService shared;
    private final AudioSettingsStore audioStore;
    private final AudioBackend suppliedAudio;
    private AudioSettingsService audioSettings;
    private GameAudio audio;
    public Main() { this(SaveSlots.files(FileProfileStore.defaultPath()),
        new FileAudioSettingsStore(FileProfileStore.defaultPath().resolveSibling("audio-settings.json")),null); }
    public Main(ProfileStore first) { this(new SaveSlots(first)); }
    public Main(SaveSlots saves) { this(saves,AudioSettingsStore.memory(),null); }
    public Main(SaveSlots saves,AudioSettingsStore audioStore,AudioBackend output) {
        this.saves=saves;this.audioStore=audioStore;this.suppliedAudio=output;
    }
    public AudioSettingsService audioSettings() {
        if(audioSettings==null)audioSettings=new AudioSettingsService(audioStore);return audioSettings;
    }
    public GameAudio audio() {
        if(audio==null)audio=new GameAudio(audioSettings(),suppliedAudio==null?new GdxAudioBackend():suppliedAudio);
        return audio;
    }
    @Override public void render() {audio().advance(Gdx.graphics.getDeltaTime());super.render();}
    @Override public void create() { profileAt(0); audio(); setScreen(new MenuScreen(this)); }
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
            }
        }
        return shared;
    }
    public boolean necromancerUnlocked() { return shared().progress().necromancerUnlocked(); }
    public boolean unlockNecromancer() { return shared().unlockNecromancer(); }
    public RunReport latestReport() { return shared().progress().latest(activeSlot, profiles().profile().characterId()); }
    public boolean deleteSave(int index) {
        ProfileService target = profileAt(index);
        SharedProgressService global = shared(); // Preserve an older character's legendary access before resetting it.
        CampaignProfile old = target.profile();
        if ((old.necromancerUnlocked() || old.professionChosen() && old.profession() == Profession.NECROMANTE)
            && !global.error().isEmpty() && !global.retrySave()) return false;
        return target.reset();
    }
    public void openLastReport() {
        RunReport report=latestReport();
        if(report!=null) setScreen(new RunReportScreen(this,report,()->new RefugeScreen(this)));
    }
    public boolean chooseProfession(Profession profession) {
        return profiles().chooseProfession(profession, necromancerUnlocked());
    }
    public boolean createCharacter(Profession profession, GameDifficulty difficulty) {
        return profiles().createCharacter(profession, difficulty, necromancerUnlocked());
    }
    public int activeSlot() { return activeSlot; }
    public RegionDefinition campaignRegion() {
        return RegionDefinition.fromJson(Gdx.files.internal(RegionDefinition.PROTOTYPE_ASSET).readString("UTF-8"));
    }
    public void openCampaign() { setScreen(new SaveSelectionScreen(this, false)); }
    public void openDemo() { setScreen(new SaveSelectionScreen(this, true)); }
    public void selectSave(int index, boolean demo) {
        profileAt(index); activeSlot = index;
        if (!profiles().profile().professionChosen()) setScreen(new ProfessionScreen(this, demo ? null : campaignRegion()));
        else if (demo) startDemo();
        else setScreen(new RefugeScreen(this));
    }
    public void startDemo() {
        if (!profiles().profile().professionChosen()) throw new IllegalStateException("Choose a save profession first");
        setScreen(new GameScreen(this, RunSession.demo(profiles().profile().profession(), profiles().profile().difficulty())));
    }
    public void startCampaign() {
        CampaignProfile profile = profiles().profile();
        if (!profile.professionChosen()) throw new IllegalStateException("Choose a save profession first");
        setScreen(new GameScreen(this, RunSession.campaign(campaignRegion(), profile.profession(), profile.bonuses(), profile.difficulty())));
    }
    public void offerRoute(RunSession session) {
        if (session.isDemo() || session.finished() || !session.arenaComplete()) throw new IllegalStateException("No safe exit");
        if (session.canClaimArenaReward()) { setScreen(new ArenaRewardScreen(this, session)); return; }
        setScreen(session.encounters().openBlacksmith()
            ? new BlacksmithScreen(this, session) : new RouteScreen(this, session));
    }
    public void finishRun(RunSession session, RunOutcome outcome) {
        boolean firstFinish = !session.finished();
        session.end();
        if (!session.isDemo()) profiles().settle(session, outcome);
        shared().settle(session,outcome,activeSlot);
        setScreen(outcome == RunOutcome.ABANDONED
            ? session.isDemo() ? new MenuScreen(this) : new RefugeScreen(this)
            : new GameOverScreen(this, outcome == RunOutcome.VICTORY, session));
        if(firstFinish && outcome!=RunOutcome.ABANDONED)audio().play(outcome==RunOutcome.VICTORY?SoundCue.VICTORY:SoundCue.DEATH);
    }
    @Override public void setScreen(Screen next) {
        Screen previous = getScreen();
        if (previous == next) return;
        super.setScreen(next);
        if (previous != null) previous.dispose();
        if(audio!=null) {
            audio.stopEffects();
            if(next!=null)audio.music(next instanceof GameScreen || next instanceof BossRoomScreen ? AudioBackend.Track.COMBAT : AudioBackend.Track.REFUGE);
        }
    }
    @Override public void dispose() { setScreen(null); if(audio!=null)audio.dispose(); }
}
