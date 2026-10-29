package br.com.srsups.paradiseinhell.lwjgl3;

import br.com.srsups.paradiseinhell.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Set;
import static br.com.srsups.paradiseinhell.lwjgl3.DemoSmokeTest.*;

/** Exercises all three display choices without switching the user's real window or monitor. */
public final class DisplayModeSmoke {
    private DisplayModeSmoke() {}

    private static final class Store implements VideoSettingsStore {
        VideoSettings saved=VideoSettings.DEFAULT;
        int writes;
        boolean fail;
        @Override public VideoSettings load(){return saved;}
        @Override public void save(VideoSettings value)throws IOException {
            writes++;
            if(fail)throw new IOException("display fixture save failure");
            saved=value;
        }
    }
    private record State(VideoSettings settings,int x,int y,int width,int height,boolean decorated,boolean resizable)
        implements VideoBackend.Snapshot {}

    /** Window geometry is deliberately separate from the preferred normal-window dimensions. */
    private static final class Backend implements VideoBackend {
        VideoSettings actual=VideoSettings.DEFAULT;
        int calls,restores,x=120,y=80,width=960,height=540;
        boolean decorated=true,resizable=true,fullscreenSupported=true,smallWorkArea,failNext;
        @Override public VideoSettings current(){return actual;}
        @Override public boolean supportsFullscreen(){return fullscreenSupported;}
        @Override public String monitorDescription(){return "Monitor de teste — 1920 × 1080 | área útil 1920 × 1040";}
        @Override public VideoSettings adapt(VideoSettings requested) {
            if(!smallWorkArea||requested.mode()!=VideoSettings.Mode.WINDOWED)return requested;
            double factor=Math.min(1,Math.min(1000d/requested.width(),650d/requested.height()));
            return requested.withSize(new VideoSettings.Size((int)Math.round(requested.width()*factor),(int)Math.round(requested.height()*factor)));
        }
        @Override public Snapshot snapshot(){return new State(actual,x,y,width,height,decorated,resizable);}
        @Override public boolean restore(Snapshot snapshot) {
            restores++;
            if(snapshot instanceof State state) {
                actual=state.settings();x=state.x();y=state.y();width=state.width();height=state.height();
                decorated=state.decorated();resizable=state.resizable();return true;
            }
            return apply(snapshot.settings());
        }
        @Override public boolean apply(VideoSettings settings) {
            calls++;actual=settings;
            if(settings.mode()==VideoSettings.Mode.WINDOWED) {
                x=120;y=80;width=settings.width();height=settings.height();decorated=true;resizable=true;
            } else {
                x=-1920;y=0;width=1920;height=1080;decorated=false;resizable=false;
            }
            if(failNext){failNext=false;return false;}
            return true;
        }
    }
    private static final class Fixture implements AutoCloseable {
        final Store store=new Store();
        final Backend backend=new Backend();
        final Main game=new Main(SaveSlots.memory(),AudioSettingsStore.memory(),new RecordedAudio(),store,backend);
        final long[] nanos={0};
        Fixture()throws Exception {
            game.create();
            VideoSettingsService service=new VideoSettingsService(store,backend,()->nanos[0]);
            service.initialize();set(game,"video",service);
        }
        void character(){check(game.createCharacter(Profession.CACADOR,GameDifficulty.MEDIUM),"Display fixture creates a memory-only character");}
        void seconds(int seconds){nanos[0]+=seconds*1_000_000_000L;}
        @Override public void close(){game.dispose();}
    }

    public static void verify()throws Exception {
        int width=Gdx.graphics.getWidth(),height=Gdx.graphics.getHeight();boolean fullscreen=Gdx.graphics.isFullscreen();
        verifyChoicesAndPersistence();
        verifyRollbackAndAdaptation();
        verifyIndependentFullscreenSupport();
        verifyBorderlessInCombat();
        check(Gdx.graphics.getWidth()==width&&Gdx.graphics.getHeight()==height&&Gdx.graphics.isFullscreen()==fullscreen,
            "Display-mode UI tests never switch the real native window or monitor");
        System.out.println("M7_DISPLAY_MODES_OK: three modes, fitted windows, protected confirmation and exact rollback");
    }

    private static void verifyChoicesAndPersistence()throws Exception {
        try(Fixture fixture=new Fixture()) {
            Main game=fixture.game;key(game.getScreen(),Input.Keys.F2);
            Screen screen=game.getScreen();VideoPanel panel=panel(screen);release(screen,.25f);
            check(panel.draft().mode()==VideoSettings.Mode.WINDOWED,"Video opens with a distinct normal-window mode");
            captureNpcDialogue(screen,"m7-display-windowed");verifyLabels(screen,panel);

            clickReleased(screen,panel.borderlessBounds());
            VideoSettings borderlessDraft=panel.draft();
            check(borderlessDraft.mode()==VideoSettings.Mode.BORDERLESS&&!borderlessDraft.fullscreen()
                &&fixture.backend.calls==0&&fixture.store.writes==0,
                "Borderless is a separate draft mode, not a fullscreen boolean alias");
            keyReleased(screen,Input.Keys.NUM_5);clickReleased(screen,panel.presetBounds(4));
            check(panel.draft().equals(borderlessDraft),"Number and mouse window presets are disabled in borderless mode");
            captureNpcDialogue(screen,"m7-display-borderless");

            keyReleased(screen,Input.Keys.RIGHT);
            check(panel.draft().mode()==VideoSettings.Mode.FULLSCREEN,"Right on the mode row moves from borderless to fullscreen");
            VideoSettings fullDraft=panel.draft();keyReleased(screen,Input.Keys.NUM_3);
            check(panel.draft().equals(fullDraft),"Fullscreen also disables normal-window preset shortcuts");
            captureNpcDialogue(screen,"m7-display-fullscreen");
            keyReleased(screen,Input.Keys.LEFT);
            check(panel.draft().mode()==VideoSettings.Mode.BORDERLESS,"Left on the mode row returns to borderless");
            keyReleased(screen,Input.Keys.LEFT);
            check(panel.draft().mode()==VideoSettings.Mode.WINDOWED,"Left can select a decorated window again");

            keyReleased(screen,Input.Keys.NUM_3);
            check(panel.draft().width()==1280&&panel.draft().height()==720,"Window mode retains selectable preferred dimensions");
            clickReleased(screen,panel.borderlessBounds());clickReleased(screen,panel.fullscreenBounds());
            check(panel.draft().width()==1280&&panel.draft().height()==720,
                "Switching between monitor-sized modes retains the draft's normal-window size");
            clickReleased(screen,panel.windowedBounds());
            check(panel.draft().width()==1280&&panel.draft().height()==720,
                "Returning to window mode restores the same preferred size");

            clickReleased(screen,panel.borderlessBounds());clickReleased(screen,panel.applyBounds());
            check(game.video().previewing()&&fixture.backend.actual.mode()==VideoSettings.Mode.BORDERLESS
                &&fixture.backend.width==1920&&fixture.backend.height==1080&&!fixture.backend.decorated
                &&fixture.backend.actual.width()==1280&&fixture.store.writes==0,
                "Borderless Apply fills the simulated monitor while retaining an unsaved preferred window size");
            captureNpcDialogue(screen,"m7-display-preview");
            input(screen,.3f,Set.of(Input.Keys.ENTER),Set.of(Input.Keys.ENTER),Set.of(Input.Buttons.LEFT),Set.of(),null);
            check(game.video().previewing()&&fixture.store.writes==0,
                "A mouse button held through Apply cannot accidentally confirm a borderless preview");
            clickReleased(screen,panel.keepBounds());
            check(!game.video().previewing()&&fixture.store.writes==1&&fixture.store.saved.mode()==VideoSettings.Mode.BORDERLESS
                &&fixture.store.saved.width()==1280,
                "Confirmation saves the distinct borderless mode and preferred window size exactly once");
            VideoSettingsService reloaded=new VideoSettingsService(fixture.store,VideoBackend.memory());
            check(reloaded.value().equals(fixture.store.saved),"The confirmed borderless preference reloads independently of characters");
            release(screen,.25f);clickReleased(screen,panel.fullscreenBounds());key(screen,Input.Keys.F2);
            check(game.getScreen() instanceof MenuScreen&&fixture.store.saved.mode()==VideoSettings.Mode.BORDERLESS,
                "Closing an unapplied fullscreen draft keeps the confirmed borderless setting");
        }
    }

    private static void verifyRollbackAndAdaptation()throws Exception {
        try(Fixture fixture=new Fixture()) {
            Main game=fixture.game;key(game.getScreen(),Input.Keys.F2);Screen screen=game.getScreen();VideoPanel panel=panel(screen);
            clickReleased(screen,panel.borderlessBounds());clickReleased(screen,panel.applyBounds());confirm(screen,panel);
            State before=(State)fixture.backend.snapshot();int writes=fixture.store.writes,restores=fixture.backend.restores;
            clickReleased(screen,panel.windowedBounds());clickReleased(screen,panel.applyBounds());key(screen,Input.Keys.ESCAPE);
            check(!game.video().previewing()&&fixture.backend.snapshot().equals(before)&&fixture.backend.restores==restores+1
                &&fixture.store.writes==writes,"Escape restores the preceding borderless geometry, decorations and settings without saving");

            clickReleased(screen,panel.fullscreenBounds());clickReleased(screen,panel.applyBounds());
            fixture.seconds(16);mainFrame(game);
            check(!game.video().previewing()&&fixture.backend.snapshot().equals(before)&&fixture.store.writes==writes,
                "The wall-clock timeout restores a borderless snapshot after a fullscreen preview");

            clickReleased(screen,panel.windowedBounds());fixture.backend.failNext=true;clickReleased(screen,panel.applyBounds());
            check(!game.video().previewing()&&fixture.backend.snapshot().equals(before)&&fixture.store.writes==writes
                &&!game.video().error().isEmpty(),"A partially applied refused mode rolls back the exact preceding borderless state");

            clickReleased(screen,panel.fullscreenBounds());clickReleased(screen,panel.applyBounds());
            game.pause();
            check(!game.video().previewing()&&fixture.backend.snapshot().equals(before)&&fixture.store.writes==writes,
                "Application pause cancels a pending fullscreen preview back to borderless");

            clickReleased(screen,panel.fullscreenBounds());clickReleased(screen,panel.applyBounds());fixture.store.fail=true;confirm(screen,panel);
            check(game.video().pendingSave()&&fixture.backend.actual.mode()==VideoSettings.Mode.FULLSCREEN
                &&fixture.store.saved.mode()==VideoSettings.Mode.BORDERLESS,
                "A failed confirmation save keeps the applied fullscreen mode in memory and the old saved mode intact");
            fixture.store.fail=false;keyReleased(screen,Input.Keys.S);
            check(!game.video().pendingSave()&&fixture.store.saved.mode()==VideoSettings.Mode.FULLSCREEN,
                "Retry saves the confirmed third mode without applying another native change");

            fixture.backend.smallWorkArea=true;clickReleased(screen,panel.windowedBounds());keyReleased(screen,Input.Keys.NUM_5);
            clickReleased(screen,panel.applyBounds());
            check(game.video().previewing()&&fixture.backend.actual.mode()==VideoSettings.Mode.WINDOWED
                &&fixture.backend.actual.width()==1000&&fixture.backend.actual.height()==563,
                "An oversized window request is fitted proportionally before the service compares the applied preview");
            confirm(screen,panel);
            check(fixture.store.saved.width()==1000&&fixture.store.saved.height()==563,
                "Confirming an adapted window persists the actually fitted logical dimensions");
            clickReleased(screen,panel.borderlessBounds());clickReleased(screen,panel.applyBounds());confirm(screen,panel);
            check(fixture.backend.width==1920&&fixture.backend.height==1080&&fixture.store.saved.width()==1000,
                "Borderless fills the whole monitor rather than the work area while preserving the fitted normal-window size");
        }
    }

    private static void verifyIndependentFullscreenSupport()throws Exception {
        try(Fixture fixture=new Fixture()) {
            fixture.backend.fullscreenSupported=false;Main game=fixture.game;
            key(game.getScreen(),Input.Keys.F2);Screen screen=game.getScreen();VideoPanel panel=panel(screen);
            clickReleased(screen,panel.borderlessBounds());clickReleased(screen,panel.applyBounds());
            check(game.video().previewing()&&fixture.backend.actual.mode()==VideoSettings.Mode.BORDERLESS,
                "A platform without exclusive fullscreen can still preview borderless window mode");
            confirm(screen,panel);int calls=fixture.backend.calls,writes=fixture.store.writes;
            clickReleased(screen,panel.fullscreenBounds());clickReleased(screen,panel.applyBounds());
            check(!game.video().previewing()&&fixture.backend.actual.mode()==VideoSettings.Mode.BORDERLESS
                &&fixture.backend.calls==calls&&fixture.store.writes==writes&&!game.video().error().isEmpty(),
                "An unsupported exclusive mode leaves the available borderless mode and preferences unchanged");
        }
    }

    private static void verifyBorderlessInCombat()throws Exception {
        try(Fixture fixture=new Fixture()) {
            fixture.character();Main game=fixture.game;RunSession run=RunSession.campaign(game.campaignRegion(),Profession.CACADOR);
            GameScreen room=new GameScreen(game,run);game.setScreen(room);
            Jogador actor=(Jogador)field(room,"player");float x=actor.x,y=actor.y,clock=run.progress().elapsed();
            Object map=field(room,"map"),atlas=field(room,"atlas");
            key(room,Input.Keys.F2);VideoPanel panel=panel(room);
            clickReleased(room,panel.borderlessBounds());clickReleased(room,panel.applyBounds());confirm(room,panel);
            check(game.getScreen()==room&&field(room,"map")==map&&field(room,"atlas")==atlas
                &&run.progress().elapsed()==clock&&actor.x==x&&actor.y==y,
                "A confirmed borderless change preserves the same paused arena, actor and resources");
            clickReleased(room,panel.fullscreenBounds());clickReleased(room,panel.applyBounds());key(room,Input.Keys.ESCAPE);
            check(fixture.backend.actual.mode()==VideoSettings.Mode.BORDERLESS&&run.progress().elapsed()==clock,
                "Fullscreen rollback in combat returns to borderless without advancing the run");
            clickReleased(room,panel.hudButtonBounds());
            check(panel.hudPage(),"The HUD settings page remains available with borderless display active");
            key(room,Input.Keys.ESCAPE);check(!panel.hudPage()&&panel.open(),"HUD Escape returns to the unchanged display-mode page");
            key(room,Input.Keys.F2);
            check(!panel.open()&&game.getScreen()==room&&actor.x==x&&run.progress().elapsed()==clock,
                "Closing borderless video settings consumes its frame and preserves the original combat state");
            release(room,.05f);check(run.progress().elapsed()>clock,"Combat resumes normally after borderless settings close");
        }
    }

    private static void verifyLabels(Screen screen,VideoPanel panel)throws Exception {
        BitmapFont font=(BitmapFont)field(screen,"font");float sx=font.getData().scaleX,sy=font.getData().scaleY;
        try {
            font.getData().setScale(1);
            List<Rectangle> bounds=List.of(panel.windowedBounds(),panel.borderlessBounds(),panel.fullscreenBounds());
            VideoSettings.Mode[] modes=VideoSettings.Mode.values();
            for(int index=0;index<modes.length;index++) {
                GlyphLayout label=new GlyphLayout(font,modes[index].label());Rectangle button=bounds.get(index);
                check(label.width<=button.width-20&&label.height<=button.height-12,
                    "Every localized display-mode label fits its actual clickable button");
                for(int other=index+1;other<bounds.size();other++)
                    check(!button.overlaps(bounds.get(other)),"The three display-mode buttons have separate click areas");
            }
        } finally {font.getData().setScale(sx,sy);}
    }
    private static void confirm(Screen screen,VideoPanel panel)throws Exception {clickReleased(screen,panel.keepBounds());}
    private static VideoPanel panel(Screen screen)throws Exception {return (VideoPanel)field(screen,screen instanceof VideoSettingsScreen?"panel":"videoPanel");}
    private static void keyReleased(Screen screen,int key)throws Exception {release(screen,.25f);key(screen,key);}
    private static void clickReleased(Screen screen,Rectangle bounds)throws Exception {release(screen,.25f);input(screen,0,Set.of(),Set.of(),Set.of(Input.Buttons.LEFT),Set.of(Input.Buttons.LEFT),bounds);}
    private static void key(Screen screen,int key)throws Exception {input(screen,0,Set.of(key),Set.of(key),Set.of(),Set.of(),null);}
    private static void release(Screen screen,float delta)throws Exception {input(screen,delta,Set.of(),Set.of(),Set.of(),Set.of(),null);}
    private static void mainFrame(Main game){withInput(game::render,Set.of(),Set.of(),Set.of(),Set.of(),700,270);}
    private static void input(Screen screen,float delta,Set<Integer> heldKeys,Set<Integer> keys,Set<Integer> heldButtons,Set<Integer> buttons,Rectangle target)throws Exception {
        FitViewport viewport=(FitViewport)field(screen,screen instanceof GameScreen?"hud":"viewport");
        Vector2 point=viewport.project(new Vector2(target==null?700:target.x+target.width/2,target==null?270:target.y+target.height/2));
        withInput(()->screen.render(delta),heldKeys,keys,heldButtons,buttons,Math.round(point.x),Gdx.graphics.getHeight()-Math.round(point.y));
    }
    private static void withInput(Runnable render,Set<Integer> heldKeys,Set<Integer> keys,Set<Integer> heldButtons,Set<Integer> buttons,int x,int y) {
        Input original=Gdx.input;
        Gdx.input=(Input)Proxy.newProxyInstance(Input.class.getClassLoader(),new Class<?>[]{Input.class},(proxy,method,args)->switch(method.getName()) {
            case "isKeyPressed" -> (int)args[0]==Input.Keys.ANY_KEY?!heldKeys.isEmpty():heldKeys.contains((int)args[0]);
            case "isKeyJustPressed" -> (int)args[0]==Input.Keys.ANY_KEY?!keys.isEmpty():keys.contains((int)args[0]);
            case "isButtonPressed" -> heldButtons.contains((int)args[0]);
            case "isButtonJustPressed" -> buttons.contains((int)args[0]);
            case "getX" -> x;
            case "getY" -> y;
            default -> method.invoke(original,args);
        });
        try{render.run();}finally{Gdx.input=original;}
    }
}
