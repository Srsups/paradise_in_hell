package br.com.srsups.paradiseinhell.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import br.com.srsups.paradiseinhell.Main;
import br.com.srsups.paradiseinhell.ProfileStore;
import br.com.srsups.paradiseinhell.RefugeScreen;
import br.com.srsups.paradiseinhell.SaveSelectionScreen;
import br.com.srsups.paradiseinhell.BlacksmithScreen;
import br.com.srsups.paradiseinhell.ArenaRewardScreen;
import br.com.srsups.paradiseinhell.PermanentBonuses;
import br.com.srsups.paradiseinhell.Profession;
import br.com.srsups.paradiseinhell.GameScreen;
import br.com.srsups.paradiseinhell.BossRoomScreen;
import br.com.srsups.paradiseinhell.RegionDefinition;
import br.com.srsups.paradiseinhell.RunSession;
import com.badlogic.gdx.Gdx;
import java.util.Arrays;

/** Launches the desktop (LWJGL3) application. */
public class Lwjgl3Launcher {
    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) return; // This handles macOS support and helps on Windows.
        boolean audioSmoke=Arrays.asList(args).contains("--audio-smoke-test");
        boolean displaySmoke=Arrays.asList(args).contains("--display-smoke-test");
        createApplication(audioSmoke || displaySmoke || Arrays.asList(args).contains("--smoke-test"),audioSmoke,displaySmoke);
    }

    private static Lwjgl3Application createApplication(boolean smokeTest,boolean audioSmoke,boolean displaySmoke) {
        Main game = smokeTest ? new Main(ProfileStore.memory()) {
            private int frames;
            private boolean rendering;
            @Override public void render() {
                if(rendering)return;
                rendering=true;
                try {
                super.render();
                if(frames==1) {
                    Lwjgl3WindowPlacement placement=new Lwjgl3WindowPlacement();
                    var monitor=placement.display();var window=placement.window();
                    if(monitor.bounds().width()<=0||monitor.workArea().height()<=0||window.width()<=0)
                        throw new IllegalStateException("Native monitor geometry is unavailable");
                    System.out.println("PACKAGED_MONITOR_SMOKE_OK: native monitor, work area and logical window geometry");
                    if(displaySmoke)verifyDisplayModes();
                }
                if (frames == 2) setScreen(new GameScreen(this));
                if (frames == 4) {
                    RegionDefinition region = RegionDefinition.fromJson(
                        Gdx.files.internal(RegionDefinition.PROTOTYPE_ASSET).readString("UTF-8"));
                    setScreen(new GameScreen(this, RunSession.campaign(region)));
                }
                if (frames == 6) {
                    RegionDefinition region = RegionDefinition.fromJson(
                        Gdx.files.internal(RegionDefinition.PROTOTYPE_ASSET).readString("UTF-8"));
                    RunSession session = RunSession.campaign(region);
                    for (var goal : session.arena().killGoals().entrySet())
                        for (int count = 0; count < goal.getValue(); count++) session.recordKill(goal.getKey());
                    session.enterArena("portoes");
                    setScreen(new BossRoomScreen(this, session));
                }
                if (frames == 8) {
                    br.com.srsups.paradiseinhell.CaronteDialogue.select(profiles().profile());
                    setScreen(new RefugeScreen(this));
                }
                if (frames == 10) setScreen(new SaveSelectionScreen(this, false));
                if (frames == 12) {
                    RegionDefinition region = RegionDefinition.fromJson(
                        Gdx.files.internal(RegionDefinition.PROTOTYPE_ASSET).readString("UTF-8"));
                    RunSession session = RunSession.campaign(region, Profession.CACADOR, PermanentBonuses.NONE,
                        new java.util.Random() { @Override public float nextFloat() { return 0; } });
                    for (var goal : session.arena().killGoals().entrySet())
                        for (int count = 0; count < goal.getValue(); count++) session.recordKill(goal.getKey());
                    session.encounters().openBlacksmith();
                    setScreen(new BlacksmithScreen(this, session));
                }
                if (frames == 14 || frames == 16) {
                    RunSession session = RunSession.campaign(campaignRegion(), Profession.APOSTADOR);
                    for (var goal : session.arena().killGoals().entrySet())
                        for (int count = 0; count < goal.getValue(); count++) session.recordKill(goal.getKey());
                    session.enterArena(frames == 14 ? "cinzas" : "sombras");
                    for (var goal : session.arena().killGoals().entrySet())
                        for (int count = 0; count < goal.getValue(); count++) session.recordKill(goal.getKey());
                    setScreen(new ArenaRewardScreen(this, session));
                }
                if (frames == 18) setScreen(new br.com.srsups.paradiseinhell.ProfessionScreen(this, campaignRegion()));
                if (frames == 19) setScreen(new br.com.srsups.paradiseinhell.DifficultyScreen(this, Profession.CACADOR, campaignRegion()));
                if (frames == 20 || frames == 22 || frames == 24 || frames == 26) {
                    Profession profession = Profession.values()[4 + (frames - 20) / 2];
                    RunSession session = RunSession.campaign(campaignRegion(), profession);
                    session.applyUpgrade(br.com.srsups.paradiseinhell.Upgrade.CHOQUE);
                    session.applyUpgrade(br.com.srsups.paradiseinhell.Upgrade.QUEIMADURA);
                    session.applyUpgrade(br.com.srsups.paradiseinhell.Upgrade.IMPACTO);
                    setScreen(new GameScreen(this, session));
                }
                if(frames==27) setScreen(br.com.srsups.paradiseinhell.FarmerQuestScreen.journal(this));
                if(frames==28) {
                    RunSession session=RunSession.demo(Profession.SACERDOTE);
                    session.applyUpgrade(br.com.srsups.paradiseinhell.Upgrade.CADENCIA); session.advance(4); session.end();
                    br.com.srsups.paradiseinhell.RunReport report=shared().settle(session,br.com.srsups.paradiseinhell.RunOutcome.ABANDONED,0);
                    setScreen(new br.com.srsups.paradiseinhell.RunReportScreen(this,report,()->new RefugeScreen(this)));
                }
                if(frames==29) {
                    setScreen(new br.com.srsups.paradiseinhell.VideoSettingsScreen(this,
                        ()->new br.com.srsups.paradiseinhell.MenuScreen(this)));
                    getScreen().render(0); // Render bundled video UI without changing the diagnostic window's mode.
                    setScreen(new br.com.srsups.paradiseinhell.OpeningScreen(this,
                        profiles().profile().characterId(),false,()->new br.com.srsups.paradiseinhell.MenuScreen(this)));
                    getScreen().render(.05f);
                    System.out.println("PACKAGED_OPENING_SMOKE_OK: layered atlas, timeline and subtitles");
                    setScreen(new br.com.srsups.paradiseinhell.ControlsSettingsScreen(this,
                        ()->new br.com.srsups.paradiseinhell.MenuScreen(this)));
                    getScreen().render(0);
                    System.out.println("PACKAGED_CONTROLS_SMOKE_OK: settings panel, default bindings and memory preferences");
                    setScreen(new br.com.srsups.paradiseinhell.VideoSettingsScreen(this,
                        ()->new br.com.srsups.paradiseinhell.MenuScreen(this),true));
                    getScreen().render(0);
                    System.out.println("PACKAGED_HUD_SMOKE_OK: scale panel, sample indicators and memory preferences");
                    verifyCheckpoint();
                }
                if(audioSmoke && frames==31) {
                    if(!Gdx.audio.getClass().getSimpleName().contains("OpenAL") || !audio().available())
                        throw new IllegalStateException("Native audio device is unavailable");
                    for(br.com.srsups.paradiseinhell.SoundCue cue:br.com.srsups.paradiseinhell.SoundCue.values()) {
                        audio().stopEffects();
                        if(!audio().play(cue))throw new IllegalStateException("Audio clip failed: "+cue);
                    }
                    audio().stopEffects();
                    setScreen(new br.com.srsups.paradiseinhell.AudioSettingsScreen(this,()->new br.com.srsups.paradiseinhell.MenuScreen(this)));
                }
                if(audioSmoke && frames==115) {
                    if(!audio().musicPlaying() || audio().musicPosition()<.1f)
                        throw new IllegalStateException("Packaged streaming music did not advance");
                    System.out.println("PACKAGED_AUDIO_SMOKE_OK: 24 native effects, streaming music, audio menu, bundled assets");
                }
                if (++frames >= (audioSmoke?120:30)) Gdx.app.exit();
                } finally {rendering=false;}
            }
            private void verifyCheckpoint() {
                if(!createCharacter(Profession.CACADOR,br.com.srsups.paradiseinhell.GameDifficulty.MEDIUM))
                    throw new IllegalStateException("Checkpoint diagnostic character was not created");
                RunSession run=RunSession.campaign(campaignRegion(),Profession.CACADOR);
                profiles().prepareRun(run);
                if(!profiles().saveCheckpoint(run,br.com.srsups.paradiseinhell.RunCheckpoint.Stage.ROOM_START))
                    throw new IllegalStateException("Checkpoint diagnostic could not save its memory profile");
                var loaded=br.com.srsups.paradiseinhell.CampaignProfile.fromJson(profiles().profile().toJson());
                var restored=loaded.checkpoint().restore(campaignRegion());
                if(!restored.id().equals(run.id())||restored.attemptSequence()!=1)
                    throw new IllegalStateException("Checkpoint JSON did not round-trip");
                setScreen(new br.com.srsups.paradiseinhell.ResumeRunScreen(this));getScreen().render(0);
                resumeCampaign();getScreen().render(0);
                finishRun(restored,br.com.srsups.paradiseinhell.RunOutcome.ABANDONED);
                if(profiles().profile().checkpoint()!=null||profiles().profile().pendingSettlement()!=null
                    ||profiles().profile().attempts()!=1)
                    throw new IllegalStateException("Checkpoint diagnostic did not complete its settlement");
                setScreen(new br.com.srsups.paradiseinhell.CheckpointSaveErrorScreen(this,"Diagnóstico em memória",()->{}));
                getScreen().render(0);
                System.out.println("PACKAGED_CHECKPOINT_SMOKE_OK: memory JSON, resume screens and terminal settlement");
            }
        } : new Main(new br.com.srsups.paradiseinhell.GdxVideoBackend(new Lwjgl3WindowPlacement()));
        Lwjgl3ApplicationConfiguration configuration = getDefaultConfiguration();
        if (smokeTest) {
            configuration.setInitialVisible(false);
            configuration.disableAudio(!audioSmoke);
        }
        return new Lwjgl3Application(game, configuration);
    }

    /** Hidden diagnostic, with no preference writes. Fullscreen requests the desktop's existing native mode. */
    private static void verifyDisplayModes() {
        var backend=new br.com.srsups.paradiseinhell.GdxVideoBackend(new Lwjgl3WindowPlacement());
        var original=backend.snapshot();
        try {
            for(var mode:br.com.srsups.paradiseinhell.VideoSettings.Mode.values()) {
                var target=backend.adapt(original.settings().withMode(mode));
                if(!backend.apply(target)||!backend.matches(target))
                    throw new IllegalStateException("Native display-mode check failed: "+mode);
            }
        } finally {
            if(!backend.restore(original)||!backend.matches(original.settings()))
                throw new IllegalStateException("Native display-mode check could not restore the original window");
        }
        System.out.println("PACKAGED_DISPLAY_MODES_SMOKE_OK: windowed, borderless, native fullscreen and restored window");
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("Paradise in Hell - Em desenvolvimento");
        //// Vsync limits the frames per second to what your hardware can display, and helps eliminate
        //// screen tearing. This setting doesn't always work on Linux, so the line after is a safeguard.
        configuration.useVsync(true);
        //// Limits FPS to the refresh rate of the currently active monitor, plus 1 to try to match fractional
        //// refresh rates. The Vsync setting above should limit the actual FPS to match the monitor.
        configuration.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1);
        //// If you remove the above line and set Vsync to false, you can get unlimited FPS, which can be
        //// useful for testing performance, but can also be very stressful to some hardware.
        //// You may also need to configure GPU drivers to fully disable Vsync; this can cause screen tearing.

        configuration.setWindowedMode(960, 540);
        configuration.setWindowSizeLimits(640,360,-1,-1);
        //// You can change these files; they are in lwjgl3/src/main/resources/ .
        //// They can also be loaded from the root of assets/ .
        configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");
        return configuration;
    }
}
