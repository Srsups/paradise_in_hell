package br.com.srsups.paradiseinhell.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import br.com.srsups.paradiseinhell.Main;
import br.com.srsups.paradiseinhell.ProfileStore;
import br.com.srsups.paradiseinhell.RefugeScreen;
import br.com.srsups.paradiseinhell.SaveSelectionScreen;
import br.com.srsups.paradiseinhell.BlacksmithScreen;
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
        createApplication(Arrays.asList(args).contains("--smoke-test"));
    }

    private static Lwjgl3Application createApplication(boolean smokeTest) {
        Main game = smokeTest ? new Main(ProfileStore.memory()) {
            private int frames;
            @Override public void render() {
                super.render();
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
                if (frames == 8) setScreen(new RefugeScreen(this));
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
                if (++frames >= 16) Gdx.app.exit();
            }
        } : new Main();
        Lwjgl3ApplicationConfiguration configuration = getDefaultConfiguration();
        if (smokeTest) {
            configuration.setInitialVisible(false);
            configuration.disableAudio(true);
        }
        return new Lwjgl3Application(game, configuration);
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
        //// You can change these files; they are in lwjgl3/src/main/resources/ .
        //// They can also be loaded from the root of assets/ .
        configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");
        return configuration;
    }
}
