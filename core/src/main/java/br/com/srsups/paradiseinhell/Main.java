package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;

public class Main extends Game {
    private final SaveSlots saves;
    private final ProfileService[] profiles = new ProfileService[SaveSlots.COUNT];
    private int activeSlot;
    public Main() { this(SaveSlots.files(FileProfileStore.defaultPath())); }
    public Main(ProfileStore first) { this(new SaveSlots(first, ProfileStore.memory(), ProfileStore.memory(), ProfileStore.memory())); }
    public Main(SaveSlots saves) { this.saves = saves; }
    @Override public void create() { profileAt(0); setScreen(new MenuScreen(this)); }
    public ProfileService profiles() { return profileAt(activeSlot); }
    public ProfileService profileAt(int index) {
        ProfileStore store = saves.store(index);
        if (profiles[index] == null) profiles[index] = new ProfileService(store);
        return profiles[index];
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
        setScreen(new GameScreen(this, RunSession.demo(profiles().profile().profession())));
    }
    public void startCampaign() {
        CampaignProfile profile = profiles().profile();
        if (!profile.professionChosen()) throw new IllegalStateException("Choose a save profession first");
        setScreen(new GameScreen(this, RunSession.campaign(campaignRegion(), profile.profession(), profile.bonuses())));
    }
    public void offerRoute(RunSession session) {
        if (session.isDemo() || session.finished() || !session.arenaComplete()) throw new IllegalStateException("No safe exit");
        setScreen(session.encounters().openBlacksmith()
            ? new BlacksmithScreen(this, session) : new RouteScreen(this, session));
    }
    public void finishRun(RunSession session, RunOutcome outcome) {
        session.end();
        if (!session.isDemo()) profiles().settle(session, outcome);
        setScreen(outcome == RunOutcome.ABANDONED
            ? session.isDemo() ? new MenuScreen(this) : new RefugeScreen(this)
            : new GameOverScreen(this, outcome == RunOutcome.VICTORY, session));
    }
    @Override public void setScreen(Screen next) {
        Screen previous = getScreen();
        if (previous == next) return;
        super.setScreen(next);
        if (previous != null) previous.dispose();
    }
    @Override public void dispose() { setScreen(null); }
}
