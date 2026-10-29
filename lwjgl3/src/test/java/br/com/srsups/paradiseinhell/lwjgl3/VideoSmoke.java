package br.com.srsups.paradiseinhell.lwjgl3;

import br.com.srsups.paradiseinhell.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.List;
import static br.com.srsups.paradiseinhell.lwjgl3.DemoSmokeTest.*;

/** Real UI/GL with fake window operations: never changes the user's native display mode. */
public final class VideoSmoke {
    private VideoSmoke() {}
    private static final class Store implements VideoSettingsStore {
        VideoSettings saved=VideoSettings.DEFAULT;
        int writes;
        boolean fail;
        @Override public VideoSettings load(){return saved;}
        @Override public void save(VideoSettings next)throws IOException {
            writes++;
            if(fail)throw new IOException("video fixture save failure");
            saved=next;
        }
    }
    private static final class Backend implements VideoBackend {
        VideoSettings actual=VideoSettings.DEFAULT;
        int calls;
        boolean refuseNext;
        Runnable callback;
        @Override public VideoSettings current(){return actual;}
        @Override public boolean apply(VideoSettings next) {
            calls++;
            if(refuseNext){refuseNext=false;return false;}
            actual=next;
            Runnable once=callback;callback=null;
            if(once!=null)once.run();
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
        void character(){check(game.createCharacter(Profession.CACADOR,GameDifficulty.MEDIUM),"Video fixture creates a real character");}
        void seconds(int amount){nanos[0]+=amount*1_000_000_000L;}
        @Override public void close(){game.dispose();}
    }
    public static void verify()throws Exception {
        boolean fullscreen=Gdx.graphics.isFullscreen();int width=Gdx.graphics.getWidth(),height=Gdx.graphics.getHeight();
        verifyMenuAndPersistence();
        verifyCombatAndNestedPanels();
        verifyRefuge();
        verifyBoss();
        verifyLifecycleRollback();
        check(Gdx.graphics.isFullscreen()==fullscreen && Gdx.graphics.getWidth()==width && Gdx.graphics.getHeight()==height,
            "Video smoke only uses fake backends and never changes native window dimensions or fullscreen");
    }
    private static void verifyMenuAndPersistence()throws Exception {
        try(Fixture fixture=new Fixture()) {
            Main game=fixture.game;
            press(game.getScreen(),Input.Keys.F2);
            check(game.getScreen() instanceof VideoSettingsScreen,"Menu F2 opens the dedicated video screen");
            Screen screen=game.getScreen();VideoPanel panel=panel(screen);
            captureNpcDialogue(screen,"m7-video-menu");verifyText(panel,(BitmapFont)field(screen,"font"),"");
            release(screen);key(screen,Input.Keys.NUM_3);
            check(panel.draft().width()==1280 && fixture.backend.calls==0 && fixture.store.writes==0,
                "Choosing a draft preset cannot apply a native change or write preferences");
            click(screen,panel.applyBounds(),true,true,0);
            check(game.video().previewing() && fixture.backend.actual.width()==1280 && fixture.store.writes==0,
                "The real Apply button begins an unsaved preview");
            captureNpcDialogue(screen,"m7-video-preview");
            input(screen,.3f,Input.Keys.ENTER,false,true,false,null);
            check(game.video().previewing() && fixture.store.writes==0,"Held attack cannot confirm a preview before mouse release");
            input(screen,0,0,false,false,false,null);key(screen,Input.Keys.ENTER);
            check(!game.video().previewing() && fixture.store.writes==1 && fixture.store.saved.width()==1280,
                "A released fresh Enter confirms and persists the displayed mode once");
            VideoSettingsService reloaded=new VideoSettingsService(fixture.store,VideoBackend.memory());
            check(reloaded.value().equals(fixture.store.saved),"Confirmed video preferences reload independently of characters");
            int calls=fixture.backend.calls;release(screen);key(screen,Input.Keys.NUM_4);key(screen,Input.Keys.ESCAPE);
            check(game.getScreen() instanceof MenuScreen && fixture.backend.calls==calls && fixture.store.writes==1,
                "Leaving an unapplied draft discards it without touching the current image");

            press(game.getScreen(),Input.Keys.F2);screen=game.getScreen();panel=panel(screen);
            beginPreview(screen,1);check(fixture.backend.actual.width()==960,"A second draft can preview another window size");
            key(screen,Input.Keys.ESCAPE);
            check(panel.open() && !game.video().previewing() && fixture.backend.actual.width()==1280 && fixture.store.writes==1,
                "ESC reverts a preview to the exact preceding image and keeps the settings screen open");

            release(screen);key(screen,Input.Keys.NUM_1);fixture.backend.refuseNext=true;key(screen,Input.Keys.ENTER);
            check(!game.video().previewing() && fixture.backend.actual.width()==1280 && fixture.store.writes==1 && !game.video().error().isEmpty(),
                "A refused native mode restores the earlier image without saving or leaving the video screen");
            fixture.store.fail=true;release(screen);key(screen,Input.Keys.ENTER);
            check(game.video().previewing(),"A rejected attempt does not prevent a later valid preview");
            release(screen);key(screen,Input.Keys.ENTER);
            check(!game.video().previewing() && game.video().pendingSave() && fixture.backend.actual.width()==800
                && fixture.store.saved.width()==1280,"A save failure after confirmation keeps the chosen image in memory");
            verifyText(panel,(BitmapFont)field(screen,"font"),game.video().error());
            fixture.store.fail=false;release(screen);key(screen,Input.Keys.S);
            check(!game.video().pendingSave() && fixture.store.saved.width()==800,"S retries only the pending confirmed preference save");
            int writes=fixture.store.writes;calls=fixture.backend.calls;
            for(int frame=0;frame<20;frame++)screen.render(.05f);
            check(fixture.store.writes==writes && fixture.backend.calls==calls,"Rendering settled settings performs no repeated save or mode apply");
            click(screen,panel.fullscreenBounds(),true,true,0);
            VideoSettings fullDraft=panel.draft();key(screen,Input.Keys.NUM_3);
            check(fullDraft.fullscreen() && panel.draft().equals(fullDraft) && fixture.backend.calls==calls,
                "Fullscreen selection remains a draft and disables window-preset shortcuts");
            key(screen,Input.Keys.F2);check(game.getScreen() instanceof MenuScreen,"F2 closes the standalone video screen");
        }
    }
    @SuppressWarnings("unchecked")
    private static void verifyCombatAndNestedPanels()throws Exception {
        try(Fixture fixture=new Fixture()) {
            fixture.character();Main game=fixture.game;
            RunSession run=RunSession.campaign(game.campaignRegion(),Profession.CACADOR);
            GameScreen room=new GameScreen(game,run);game.setScreen(room);
            fire(room);input(room,.05f,Input.Keys.SHIFT_LEFT,true,false,false,null);
            Jogador actor=(Jogador)field(room,"player");PlayerState stats=run.player();
            List<Projetil> shots=(List<Projetil>)field(room,"projectiles");
            check(!shots.isEmpty() && stats.isDashing(),"Combat video fixture starts with a real projectile and accepted dash");
            float x=actor.x,y=actor.y,shotX=shots.getFirst().x,clock=run.progress().elapsed(),stamina=stats.getEstaminaAtual();
            float dash=(float)field(stats,"dashTimer"),cooldown=(float)field(stats,"cooldownTiro");
            Object map=field(room,"map"),atlas=field(room,"atlas");
            input(room,.05f,Input.Keys.F2,true,true,true,null);
            VideoPanel panel=panel(room);check(panel.open(),"F2 opens video over the same combat screen");
            input(room,2,Input.Keys.NUM_1,true,true,true,null);
            check(actor.x==x && actor.y==y && shots.getFirst().x==shotX && run.progress().elapsed()==clock
                && stats.getEstaminaAtual()==stamina && (float)field(stats,"dashTimer")==dash && (float)field(stats,"cooldownTiro")==cooldown,
                "Video input freezes movement, projectiles, stamina, dash, shooting cooldown and run time");
            captureNpcDialogue(room,"m7-video-combat");
            fixture.backend.callback=()->{game.resize(800,600);game.render();};
            beginPreview(room,2);
            check(game.video().previewing() && game.getScreen()==room && field(room,"map")==map && field(room,"atlas")==atlas
                && run.progress().elapsed()==clock && actor.x==x && shots.getFirst().x==shotX,
                "A resize callback and reentrant Main.render cannot advance or replace an arena during native apply");
            game.resize(960,540);release(room);key(room,Input.Keys.ENTER);
            check(!game.video().previewing() && fixture.store.writes==1 && game.getScreen()==room,"Confirming a video mode retains the same session and assets");
            release(room);click(room,panel.backBounds(),true,true,0,true);
            check(!panel.open() && actor.x==x && actor.y==y && run.progress().elapsed()==clock && shots.getFirst().x==shotX,
                "Closing video by click consumes that frame without movement or a stray attack");
            input(room,.05f,0,true,false,false,null);
            check(actor.x>x && run.progress().elapsed()>clock && shots.getFirst().x!=shotX,"Combat resumes normally on the following frame");
            verifyNestedPanels(room,run);

            stats.ganharXP(10);room.render(0);
            check(field(room,"state").toString().equals("LEVEL_UP"),"Video fixture opens a real pending boon selection");
            ChoiceInputGuard guard=(ChoiceInputGuard)field(room,"choiceGuard");float remaining=guard.remaining();clock=run.progress().elapsed();
            key(room,Input.Keys.F2);input(room,3,Input.Keys.NUM_1,true,true,true,null);
            check(guard.remaining()==remaining && run.progress().elapsed()==clock && field(room,"state").toString().equals("LEVEL_UP"),
                "Video time and number shortcuts cannot advance a boon guard or choose an underlying card");
            input(room,.05f,Input.Keys.F2,true,true,true,null);
            check(!panel.open() && guard.remaining()==remaining && field(room,"state").toString().equals("LEVEL_UP"),
                "F2 closing restores the same choice and consumes its held attack and number input");
            input(room,1.6f,0,false,true,false,null);input(room,0,Input.Keys.NUM_1,false,true,false,null);
            check(!guard.ready() && field(room,"state").toString().equals("LEVEL_UP"),"The original boon wait still requires releasing attack");
            input(room,0,0,false,false,false,null);key(room,Input.Keys.NUM_1);
            check(field(room,"state").toString().equals("PLAYING"),"The original boon guard accepts a fresh released choice after video closes");

            key(room,Input.Keys.F2);VideoSettings before=fixture.backend.actual;
            beginPreview(room,before.width()==960?2:1);clock=run.progress().elapsed();
            fixture.seconds(16);mainFrame(game);
            check(!game.video().previewing() && fixture.backend.actual.equals(before) && run.progress().elapsed()==clock && game.getScreen()==room,
                "Main's wall-clock timeout restores the old image while the combat simulation remains frozen");
            beginPreview(room,before.width()==960?2:1);game.pause();
            check(!game.video().previewing() && fixture.backend.actual.equals(before) && field(room,"state").toString().equals("PAUSED"),
                "Application pause cancels a preview and preserves the arena's normal paused state");
        }
    }
    private static void verifyNestedPanels(GameScreen room,RunSession run)throws Exception {
        key(room,Input.Keys.TAB);BoonPanel boons=(BoonPanel)field(room,"boonPanel");float clock=run.progress().elapsed();
        key(room,Input.Keys.F2);input(room,2,0,true,true,true,null);key(room,Input.Keys.F2);
        check(boons.open() && run.progress().elapsed()==clock,"Video preserves an already open Tab reference");key(room,Input.Keys.ESCAPE);
        key(room,Input.Keys.O);AudioPanel audio=(AudioPanel)field(room,"audioPanel");clock=run.progress().elapsed();
        key(room,Input.Keys.F2);input(room,2,Input.Keys.M,true,true,true,null);key(room,Input.Keys.ESCAPE);
        check(audio.open() && !gameAudioMuted(room) && run.progress().elapsed()==clock,"Video preserves audio without forwarding its mute shortcut");key(room,Input.Keys.ESCAPE);
        key(room,Input.Keys.F1);TutorialPanel tutorial=(TutorialPanel)field(room,"tutorialPanel");clock=run.progress().elapsed();
        key(room,Input.Keys.F2);input(room,2,Input.Keys.H,true,true,true,null);key(room,Input.Keys.F2);
        check(tutorial.open() && run.progress().elapsed()==clock,"Video preserves F1 without ticking or forwarding its preference shortcut");key(room,Input.Keys.ESCAPE);
        key(room,Input.Keys.ESCAPE);clock=run.progress().elapsed();
        key(room,Input.Keys.F2);key(room,Input.Keys.ESCAPE);
        check(field(room,"state").toString().equals("PAUSED") && run.progress().elapsed()==clock,"Video closing does not release an earlier explicit pause");
        key(room,Input.Keys.ESCAPE);
    }
    private static boolean gameAudioMuted(GameScreen room)throws Exception {return ((Main)field(room,"game")).audioSettings().value().muted();}
    private static void verifyRefuge()throws Exception {
        try(Fixture fixture=new Fixture()) {
            fixture.character();Main game=fixture.game;RefugeScreen refuge=new RefugeScreen(game);game.setScreen(refuge);
            Jogador actor=(Jogador)field(refuge,"player");float x=actor.x,y=actor.y,visual=(float)field(refuge,"visualTime");
            input(refuge,.05f,Input.Keys.F2,true,true,true,null);input(refuge,2,Input.Keys.ENTER,true,true,true,null);
            check(panel(refuge).open() && actor.x==x && actor.y==y && (float)field(refuge,"visualTime")==visual && game.getScreen()==refuge,
                "The refuge video overlay freezes movement and refuses accidental departure");
            captureNpcDialogue(refuge,"m7-video-refuge");
            input(refuge,.05f,Input.Keys.ESCAPE,true,true,true,null);
            check(!panel(refuge).open() && actor.x==x && game.getScreen()==refuge,"Closing refuge video consumes ESC without leaving for the menu");
            key(refuge,Input.Keys.C);DialoguePlayback playback=(DialoguePlayback)field(refuge,"playback");
            String visible=playback.visibleText();key(refuge,Input.Keys.F2);input(refuge,2,Input.Keys.ENTER,true,true,true,null);key(refuge,Input.Keys.F2);
            check(field(refuge,"state").toString().equals("DIALOGUE") && playback.visibleText().equals(visible),
                "Video consultation preserves an in-progress refuge conversation without revealing or accepting a line");
        }
    }
    @SuppressWarnings("unchecked")
    private static void verifyBoss()throws Exception {
        try(Fixture fixture=new Fixture()) {
            fixture.character();Main game=fixture.game;RunSession run=bossSession(Profession.CACADOR);game.profiles().prepareRun(run);
            BossRoomScreen room=new BossRoomScreen(game,run);game.setScreen(room);fire(room);
            BossEncounter boss=(BossEncounter)field(room,"boss");Jogador actor=(Jogador)field(room,"player");
            List<Projetil> shots=(List<Projetil>)field(room,"projectiles");check(!shots.isEmpty(),"Boss fixture fires a real projectile");
            float x=actor.x,y=actor.y,clock=run.progress().elapsed(),bossClock=boss.elapsed(),shotX=shots.getFirst().x;
            key(room,Input.Keys.F2);input(room,3,Input.Keys.SHIFT_LEFT,true,true,true,null);
            check(actor.x==x && actor.y==y && run.progress().elapsed()==clock && boss.elapsed()==bossClock && shots.getFirst().x==shotX,
                "Boss video freezes player, projectiles, encounter patterns and run time");
            captureNpcDialogue(room,"m7-video-boss");beginPreview(room,2);key(room,Input.Keys.ESCAPE);key(room,Input.Keys.F2);
            check(game.getScreen()==room && !panel(room).open() && !game.video().previewing() && boss.elapsed()==bossClock && actor.x==x,
                "Applying and reverting video never recreates the boss arena or consumes its frozen combat frame");
            input(room,.05f,0,false,false,false,null);check(boss.elapsed()>bossClock,"Boss patterns resume only after the overlay has closed");
        }
    }
    private static void verifyLifecycleRollback()throws Exception {
        Fixture fixture=new Fixture();
        try {
            Main game=fixture.game;VideoSettings before=fixture.backend.actual;
            check(game.video().preview(before.withSize(VideoSettings.PRESETS.get(2))),"Lifecycle fixture starts an unsaved mode preview");
            game.setScreen(new MenuScreen(game));
            check(!game.video().previewing() && fixture.backend.actual.equals(before),"Changing the current screen cancels a pending preview");
            game.video().preview(before.withVsync(false));game.dispose();
            check(!game.video().previewing() && fixture.backend.actual.equals(before) && fixture.store.writes==0,
                "Disposal rolls back an unconfirmed image without writing preferences");
        } finally {fixture.close();}
    }
    private static void beginPreview(Screen screen,int preset)throws Exception {
        release(screen);key(screen,Input.Keys.NUM_1+preset);key(screen,Input.Keys.ENTER);
        Main game=(Main)field(screen,"game");check(game.video().previewing(),"The requested real UI preset starts a preview");
    }
    private static VideoPanel panel(Screen screen)throws Exception {return (VideoPanel)field(screen,screen instanceof VideoSettingsScreen?"panel":"videoPanel");}
    private static void release(Screen screen)throws Exception {input(screen,.21f,0,false,false,false,null);}
    private static void key(Screen screen,int key)throws Exception {input(screen,0,key,false,false,false,null);}
    private static void click(Screen screen,Rectangle target,boolean held,boolean clicked,float delta)throws Exception {click(screen,target,held,clicked,delta,false);}
    private static void click(Screen screen,Rectangle target,boolean held,boolean clicked,float delta,boolean moving)throws Exception {
        input(screen,delta,0,moving,held,clicked,target);
    }
    private static void input(Screen screen,float delta,int key,boolean moving,boolean held,boolean clicked,Rectangle target)throws Exception {
        FitViewport viewport=(FitViewport)field(screen,screen instanceof GameScreen || screen instanceof BossRoomScreen || screen instanceof RefugeScreen?"hud":"viewport");
        Vector2 point=viewport.project(new Vector2(target==null?700:target.x+target.width/2,target==null?270:target.y+target.height/2));
        withInput(()->screen.render(delta),key,moving,held,clicked,Math.round(point.x),Gdx.graphics.getHeight()-Math.round(point.y));
    }
    private static void mainFrame(Main game) {
        withInput(game::render,0,false,false,false,700,270);
    }
    private static void withInput(Runnable render,int key,boolean moving,boolean held,boolean clicked,int x,int y) {
        Input original=Gdx.input;
        Gdx.input=(Input)Proxy.newProxyInstance(Input.class.getClassLoader(),new Class<?>[]{Input.class},(proxy,method,args)->switch(method.getName()) {
            case "isKeyJustPressed" -> key!=0 && (int)args[0]==key;
            case "isKeyPressed" -> moving && (int)args[0]==Input.Keys.D;
            case "isButtonPressed" -> held && (int)args[0]==Input.Buttons.LEFT;
            case "isButtonJustPressed" -> clicked && (int)args[0]==Input.Buttons.LEFT;
            case "getX" -> x;
            case "getY" -> y;
            default -> method.invoke(original,args);
        });
        try {render.run();}finally {Gdx.input=original;}
    }
    private static void verifyText(VideoPanel panel,BitmapFont font,String notice) {
        float scaleX=font.getData().scaleX,scaleY=font.getData().scaleY;
        try {
            font.getData().setScale(.95f);
            for(int index=0;index<VideoSettings.PRESETS.size();index++) {
                Rectangle bounds=panel.presetBounds(index);GlyphLayout text=new GlyphLayout(font,VideoSettings.PRESETS.get(index).label());
                check(text.width<=bounds.width-24 && text.height<=bounds.height-16,"Each preset label fits its actual clickable card");
            }
            font.getData().setScale(1);
            for(String label:List.of("APLICAR [ENTER]","ENTER / clique: manter","ESC / clique: reverter"))
                check(new GlyphLayout(font,label).width<=panel.keepBounds().width-40,"Video action text fits its button: "+label);
            font.getData().setScale(.88f);GlyphLayout error=new GlyphLayout(font,notice,Color.WHITE,890,Align.left,true);
            check(error.width<=890 && error.height<=29,"Pending-save notices remain readable inside the video footer");
            font.getData().setScale(1.5f);check(new GlyphLayout(font,"MANTER ESTA IMAGEM?").width<=700,"The preview heading fits its confirmation panel");
        } finally {font.getData().setScale(scaleX,scaleY);}
    }
}
