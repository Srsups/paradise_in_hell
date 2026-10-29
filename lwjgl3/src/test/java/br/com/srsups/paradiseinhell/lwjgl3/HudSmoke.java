package br.com.srsups.paradiseinhell.lwjgl3;

import br.com.srsups.paradiseinhell.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import static br.com.srsups.paradiseinhell.lwjgl3.DemoSmokeTest.*;

/** Real HUD settings and scaled rendering with memory preferences and no native monitor changes. */
public final class HudSmoke {
    private HudSmoke() {}

    private static final class Store implements HudSettingsStore {
        HudSettings saved=HudSettings.DEFAULT;
        int writes;
        boolean fail;
        @Override public HudSettings load(){return saved;}
        @Override public void save(HudSettings next)throws IOException {
            writes++;
            if(fail)throw new IOException("HUD fixture save failure");
            saved=next;
        }
    }
    private static final class Backend implements VideoBackend {
        VideoSettings actual=VideoSettings.DEFAULT;
        int calls;
        @Override public VideoSettings current(){return actual;}
        @Override public boolean apply(VideoSettings next){calls++;actual=next;return true;}
    }
    private static final class Fixture implements AutoCloseable {
        final Store store=new Store();
        final Backend backend=new Backend();
        final Main game=new Main(SaveSlots.memory(),AudioSettingsStore.memory(),new RecordedAudio(),
            VideoSettingsStore.memory(),backend,ControlSettingsStore.memory(),store);
        Fixture(){game.create();}
        void character(){check(game.createCharacter(Profession.CACADOR,GameDifficulty.MEDIUM),"HUD fixture creates a campaign character");}
        @Override public void close(){game.dispose();}
    }

    public static void verify()throws Exception {
        int width=Gdx.graphics.getWidth(),height=Gdx.graphics.getHeight();
        boolean fullscreen=Gdx.graphics.isFullscreen();
        verifyMenuAndPersistence();
        verifyArenaAndGraphics();
        verifySuspendedDragging();
        verifyNestedChoices();
        verifyRefuge();
        verifyBoss();
        check(Gdx.graphics.getWidth()==width && Gdx.graphics.getHeight()==height && Gdx.graphics.isFullscreen()==fullscreen,
            "HUD tests preserve native window dimensions and display mode");
        System.out.println("M7_HUD_OK: scaled anchors, protected settings, global persistence and unchanged world geometry");
    }

    private static void verifyMenuAndPersistence()throws Exception {
        try(Fixture fixture=new Fixture()) {
            Main game=fixture.game;
            key(game.getScreen(),Input.Keys.F2);
            Screen screen=game.getScreen();
            check(screen instanceof VideoSettingsScreen,"HUD settings reuse the existing menu video entry");
            VideoPanel video=video(screen);
            check(!video.hudPage(),"F2 still opens the image settings first");
            HudPanel panel=openHud(screen);
            check(panel.draft().equals(HudSettings.DEFAULT) && !game.profiles().profile().professionChosen(),
                "The HUD preference can be configured before creating a save");
            input(screen,.3f,Set.of(Input.Keys.ENTER),Set.of(Input.Keys.ENTER),Set.of(Input.Buttons.LEFT),Set.of(Input.Buttons.LEFT),panel.presetBounds(0));
            check(panel.draft().equals(HudSettings.DEFAULT) && fixture.store.writes==0,
                "A held click from opening HUD settings cannot change a preset or save it");
            release(screen,.25f);captureNpcDialogue(screen,"m7-hud-menu");verifyTextAndBounds(screen,panel,"");

            clickReleased(screen,panel.presetBounds(0));
            check(panel.draft().percent()==80 && game.hudSettings().value().percent()==100 && fixture.store.writes==0,
                "A smaller HUD is only a preview draft until Save");
            captureNpcDialogue(screen,"m7-hud-80");
            release(screen,.25f);key(screen,Input.Keys.LEFT);
            check(panel.draft().percent()==80,"Keyboard adjustment cannot go below the supported HUD size");
            keyReleased(screen,Input.Keys.RIGHT);
            check(panel.draft().percent()==85,"The HUD keyboard adjustment uses five percentage points");
            clickReleased(screen,panel.plusBounds());
            check(panel.draft().percent()==90,"The HUD plus button increases the percentage by five");
            clickReleased(screen,panel.minusBounds());
            check(panel.draft().percent()==85,"The HUD minus button decreases the percentage by five");
            Rectangle track=panel.trackBounds();
            clickReleased(screen,new Rectangle(track.x+track.width*.4f,track.y+track.height*.5f,0,0));
            check(panel.draft().percent()==98,"The slider supports intermediate percentages rather than only presets");
            input(screen,.05f,Set.of(),Set.of(),Set.of(Input.Buttons.LEFT),Set.of(),
                new Rectangle(track.x+track.width*.75f,track.y+track.height*.5f,0,0));
            check(panel.draft().percent()==114,"Dragging the held slider continues adjusting without another click");
            clickReleased(screen,panel.presetBounds(2));
            check(panel.draft().percent()==125,"The largest preset selects 125 percent");
            release(screen,.25f);key(screen,Input.Keys.RIGHT);
            check(panel.draft().percent()==125,"Keyboard adjustment cannot exceed the supported HUD size");
            captureNpcDialogue(screen,"m7-hud-125");

            clickReleased(screen,panel.defaultsBounds());
            check(panel.draft().equals(HudSettings.DEFAULT) && fixture.store.writes==0,
                "Restoring the HUD default changes only the draft");
            clickReleased(screen,panel.presetBounds(2));clickReleased(screen,panel.applyBounds());
            check(game.hudSettings().value().percent()==125 && fixture.store.saved.percent()==125 && fixture.store.writes==1,
                "The HUD Save action applies and persists one global preference");
            check(new HudSettingsService(fixture.store).value().percent()==125,
                "The chosen HUD scale reloads independently of character data");
            check(game.deleteSave(1) && game.hudSettings().value().percent()==125 && fixture.store.writes==1,
                "Resetting a character slot preserves the global HUD preference");
            check(fixture.backend.calls==0 && !game.video().previewing(),
                "Changing HUD size never starts a physical video mode preview");

            clickReleased(screen,panel.presetBounds(0));key(screen,Input.Keys.ESCAPE);
            check(!video.hudPage() && video.open() && game.hudSettings().value().percent()==125 && fixture.store.writes==1,
                "Escape discards an unapplied HUD draft and returns to image settings");
            key(screen,Input.Keys.ESCAPE);
            check(game.getScreen() instanceof MenuScreen,"A second Escape returns from video to the menu");
            key(game.getScreen(),Input.Keys.F2);screen=game.getScreen();panel=openHud(screen);
            check(panel.draft().percent()==125,"Reopening HUD settings starts from the saved preference");

            clickReleased(screen,panel.presetBounds(1));fixture.store.fail=true;clickReleased(screen,panel.applyBounds());
            check(game.hudSettings().value().percent()==100 && fixture.store.saved.percent()==125
                && game.hudSettings().pendingSave() && !game.hudSettings().error().isEmpty(),
                "A HUD save failure keeps the chosen size in memory and preserves the previous saved preference");
            captureNpcDialogue(screen,"m7-hud-save-error");verifyTextAndBounds(screen,panel,game.hudSettings().error());
            fixture.store.fail=false;keyReleased(screen,Input.Keys.S);
            check(!game.hudSettings().pendingSave() && fixture.store.saved.percent()==100,
                "S retries a failed HUD preference write");
            int writes=fixture.store.writes;
            for(int frame=0;frame<12;frame++)release(screen,.05f);
            check(fixture.store.writes==writes && fixture.backend.calls==0,
                "Drawing a settled HUD panel neither rewrites preferences nor changes display mode");
            key(screen,Input.Keys.F2);
            check(game.getScreen() instanceof MenuScreen,"F2 closes both HUD and video pages");
        }
    }

    @SuppressWarnings("unchecked")
    private static void verifyArenaAndGraphics()throws Exception {
        try(Fixture fixture=new Fixture()) {
            fixture.character();Main game=fixture.game;
            game.controlSettings().apply(ControlSettings.DEFAULT.withBinding(ControlAction.ATTACK,0,InputBinding.key(Input.Keys.SPACE)));
            RunSession run=RunSession.campaign(game.campaignRegion(),Profession.CACADOR);
            GameScreen room=new GameScreen(game,run);game.setScreen(room);release(room,0);
            Jogador actor=(Jogador)field(room,"player");PlayerState stats=run.player();
            List<Projetil> shots=(List<Projetil>)field(room,"projectiles");
            FitViewport world=(FitViewport)field(room,"world"),hud=(FitViewport)field(room,"hud");
            OrthographicCamera camera=(OrthographicCamera)world.getCamera();
            Matrix4 worldMatrix=new Matrix4(camera.combined);
            float viewportWidth=world.getWorldWidth(),viewportHeight=world.getWorldHeight(),zoom=camera.zoom;
            float x=actor.x,y=actor.y,clock=run.progress().elapsed(),health=stats.getVida();
            Object map=field(room,"map"),atlas=field(room,"atlas");
            int centerBefore=centerPixels();
            game.hudSettings().apply(new HudSettings(125));release(room,0);
            check(Arrays.equals(worldMatrix.val,camera.combined.val) && camera.zoom==zoom
                && world.getWorldWidth()==viewportWidth && world.getWorldHeight()==viewportHeight
                && actor.x==x && actor.y==y && stats.getVida()==health && run.progress().elapsed()==clock,
                "Scaling HUD does not change the world camera, actor geometry, health or combat time");
            check(centerPixels()==centerBefore,"The arena center renders identically while only HUD size changes");
            captureNpcDialogue(room,"m7-hud-combat-125");
            verifyRendererRestoration((SpriteBatch)field(room,"batch"),(ShapeRenderer)field(room,"shapes"));
            check(hud.getWorldWidth()==960 && hud.getWorldHeight()==540,
                "HUD scaling keeps menu/input coordinates in the existing logical viewport");

            key(room,Input.Keys.F2);HudPanel panel=openHud(room);
            input(room,2,Set.of(Input.Keys.D,Input.Keys.SHIFT_LEFT,Input.Keys.SPACE),Set.of(Input.Keys.SHIFT_LEFT),
                Set.of(Input.Buttons.LEFT),Set.of(),null);
            check(actor.x==x && actor.y==y && run.progress().elapsed()==clock && stats.getVida()==health && shots.isEmpty(),
                "The HUD page freezes movement, dash, remapped attack, health and run time");
            input(room,.05f,Set.of(Input.Keys.F2,Input.Keys.SPACE),Set.of(Input.Keys.F2),Set.of(),Set.of(),null);
            check(!video(room).open() && !panel.open() && game.getScreen()==room && field(room,"map")==map && field(room,"atlas")==atlas,
                "Closing HUD settings consumes its frame and preserves the exact arena resources");
            input(room,0,Set.of(Input.Keys.SPACE),Set.of(),Set.of(),Set.of(),null);
            check(shots.isEmpty(),"An attack held through HUD closing remains blocked until physically released");
            release(room,0);key(room,Input.Keys.SPACE);
            check(!shots.isEmpty(),"A new attack after release resumes the configured weapon");
            release(room,.05f);
            check(run.progress().elapsed()>clock,"The arena advances again only after HUD settings close");
        }
    }

    private static void verifySuspendedDragging()throws Exception {
        for(int location=0;location<3;location++) {
            try(Fixture fixture=new Fixture()) {
                fixture.character();Main game=fixture.game;
                Screen screen;
                if(location==0)screen=new GameScreen(game,RunSession.campaign(game.campaignRegion(),Profession.CACADOR));
                else if(location==1)screen=new RefugeScreen(game);
                else {
                    RunSession run=bossSession(Profession.CACADOR);game.profiles().prepareRun(run);
                    screen=new BossRoomScreen(game,run);
                }
                game.setScreen(screen);key(screen,Input.Keys.F2);HudPanel panel=openHud(screen);
                Rectangle track=panel.trackBounds();
                clickReleased(screen,trackPoint(track,.4f));
                int selected=panel.draft().percent();
                check(selected==98,"The suspended-drag fixture starts a genuine slider drag");
                input(screen,0,Set.of(Input.Keys.F3),Set.of(Input.Keys.F3),Set.of(Input.Buttons.LEFT),Set.of(),trackPoint(track,.4f));
                ControlsPanel controls=(ControlsPanel)field(screen,"controlsPanel");
                check(controls.open(),"Controls can temporarily take input while the HUD slider is being dragged");
                release(screen,.25f);
                Rectangle outside=new Rectangle(940,500,0,0);
                input(screen,0,Set.of(Input.Keys.F3),Set.of(Input.Keys.F3),Set.of(Input.Buttons.LEFT),Set.of(Input.Buttons.LEFT),outside);
                check(!controls.open() && video(screen).hudPage() && panel.draft().percent()==selected,
                    "Returning from controls consumes the mouse press and preserves the exact HUD draft");
                input(screen,.5f,Set.of(),Set.of(),Set.of(Input.Buttons.LEFT),Set.of(),outside);
                check(panel.draft().percent()==selected && fixture.store.writes==0,
                    "A drag suspended by controls cannot resume from an unrelated held mouse button outside the slider");
                release(screen,.25f);click(screen,trackPoint(track,.75f));
                check(panel.draft().percent()==114,"A fresh slider click works normally after the suspended drag is released");
                key(screen,Input.Keys.F2);
            }
        }
        try(Fixture fixture=new Fixture()) {
            Main game=fixture.game;key(game.getScreen(),Input.Keys.F2);Screen screen=game.getScreen();
            HudPanel panel=openHud(screen);Rectangle track=panel.trackBounds();
            clickReleased(screen,trackPoint(track,.4f));int selected=panel.draft().percent();
            game.pause();
            input(screen,.5f,Set.of(),Set.of(),Set.of(Input.Buttons.LEFT),Set.of(),new Rectangle(940,500,0,0));
            check(video(screen).hudPage() && panel.draft().percent()==selected && fixture.store.writes==0,
                "Application pause cancels a HUD drag without changing its draft or saving");
            release(screen,.25f);click(screen,trackPoint(track,.75f));
            check(panel.draft().percent()==114,"After application pause a fresh released slider press still works");
        }
    }

    private static Rectangle trackPoint(Rectangle track,float fraction) {
        return new Rectangle(track.x+track.width*fraction,track.y+track.height*.5f,0,0);
    }

    private static void verifyNestedChoices()throws Exception {
        try(Fixture fixture=new Fixture()) {
            fixture.character();Main game=fixture.game;
            RunSession run=RunSession.campaign(game.campaignRegion(),Profession.CACADOR);
            GameScreen room=new GameScreen(game,run);game.setScreen(room);
            key(room,Input.Keys.TAB);BoonPanel boons=(BoonPanel)field(room,"boonPanel");float clock=run.progress().elapsed();
            key(room,Input.Keys.F2);openHud(room);release(room,2);key(room,Input.Keys.F2);
            check(boons.open() && run.progress().elapsed()==clock,"HUD settings preserve a paused boon reference");key(room,Input.Keys.ESCAPE);
            key(room,Input.Keys.F1);TutorialPanel guide=(TutorialPanel)field(room,"tutorialPanel");clock=run.progress().elapsed();
            key(room,Input.Keys.F2);openHud(room);release(room,2);key(room,Input.Keys.F2);
            check(guide.open() && run.progress().elapsed()==clock,"HUD settings preserve the active tutorial page");key(room,Input.Keys.ESCAPE);
            key(room,Input.Keys.ESCAPE);clock=run.progress().elapsed();
            key(room,Input.Keys.F2);openHud(room);release(room,2);key(room,Input.Keys.F2);
            check(field(room,"state").toString().equals("PAUSED") && run.progress().elapsed()==clock,
                "Leaving HUD settings does not release an existing explicit pause");key(room,Input.Keys.ESCAPE);

            run.player().ganharXP(10);release(room,0);
            check(field(room,"state").toString().equals("LEVEL_UP"),"HUD fixture opens a real pending level choice");
            ChoiceInputGuard guard=(ChoiceInputGuard)field(room,"choiceGuard");float waiting=guard.remaining();
            Rectangle[] cards=(Rectangle[])field(room,"cards");Rectangle firstCard=new Rectangle(cards[0]);
            key(room,Input.Keys.F2);HudPanel panel=openHud(room);clickReleased(room,panel.presetBounds(2));clickReleased(room,panel.applyBounds());
            input(room,2,Set.of(Input.Keys.NUM_1),Set.of(Input.Keys.NUM_1),Set.of(Input.Buttons.LEFT),Set.of(Input.Buttons.LEFT),firstCard);
            check(field(room,"state").toString().equals("LEVEL_UP") && guard.remaining()==waiting && cards[0].equals(firstCard),
                "Editing HUD cannot choose a boon, age its protection or resize the choice hit rectangles");
            input(room,.05f,Set.of(Input.Keys.F2),Set.of(Input.Keys.F2),Set.of(Input.Buttons.LEFT),Set.of(),null);
            input(room,1.6f,Set.of(Input.Keys.NUM_1),Set.of(Input.Keys.NUM_1),Set.of(Input.Buttons.LEFT),Set.of(),null);
            check(field(room,"state").toString().equals("LEVEL_UP"),"A held attack still cannot select the underlying boon after HUD closing");
            release(room,0);key(room,Input.Keys.NUM_1);
            check(field(room,"state").toString().equals("PLAYING"),"Fresh input resolves the preserved boon choice normally");
        }
    }

    private static void verifyRefuge()throws Exception {
        try(Fixture fixture=new Fixture()) {
            fixture.character();Main game=fixture.game;RefugeScreen refuge=new RefugeScreen(game);game.setScreen(refuge);
            Jogador actor=(Jogador)field(refuge,"player");float x=actor.x,y=actor.y,visual=(float)field(refuge,"visualTime");
            key(refuge,Input.Keys.F2);HudPanel panel=openHud(refuge);
            clickReleased(refuge,panel.presetBounds(2));clickReleased(refuge,panel.applyBounds());
            input(refuge,2,Set.of(Input.Keys.D,Input.Keys.ENTER),Set.of(Input.Keys.ENTER),Set.of(Input.Buttons.LEFT),Set.of(),null);
            check(actor.x==x && actor.y==y && (float)field(refuge,"visualTime")==visual && game.getScreen()==refuge,
                "HUD settings in the refuge freeze movement and cannot start a run");
            key(refuge,Input.Keys.F2);
            check(game.getScreen()==refuge && !video(refuge).open() && actor.x==x,"Closing HUD preserves the refuge and consumes its input frame");
            key(refuge,Input.Keys.C);DialoguePlayback dialogue=(DialoguePlayback)field(refuge,"playback");String text=dialogue.visibleText();
            key(refuge,Input.Keys.F2);openHud(refuge);release(refuge,2);key(refuge,Input.Keys.F2);
            check(field(refuge,"state").toString().equals("DIALOGUE") && dialogue.visibleText().equals(text),
                "Changing HUD preferences does not advance or resize a refuge conversation");
        }
    }

    @SuppressWarnings("unchecked")
    private static void verifyBoss()throws Exception {
        try(Fixture fixture=new Fixture()) {
            fixture.character();Main game=fixture.game;game.hudSettings().apply(new HudSettings(125));
            RunSession run=bossSession(Profession.CACADOR);game.profiles().prepareRun(run);
            BossRoomScreen room=new BossRoomScreen(game,run);game.setScreen(room);release(room,0);
            BossEncounter boss=(BossEncounter)field(room,"boss");Jogador actor=(Jogador)field(room,"player");
            FitViewport world=(FitViewport)field(room,"world");Matrix4 projection=new Matrix4(world.getCamera().combined);
            captureNpcDialogue(room,"m7-hud-boss-125");
            verifyRendererRestoration((SpriteBatch)field(room,"batch"),(ShapeRenderer)field(room,"shapes"));
            float x=actor.x,y=actor.y,clock=run.progress().elapsed(),bossTime=boss.elapsed(),health=run.player().getVida();
            List<Projetil> shots=(List<Projetil>)field(room,"projectiles");
            key(room,Input.Keys.F2);HudPanel panel=openHud(room);
            input(room,3,Set.of(Input.Keys.D,Input.Keys.SHIFT_LEFT),Set.of(Input.Keys.SHIFT_LEFT),Set.of(Input.Buttons.LEFT),Set.of(Input.Buttons.LEFT),null);
            check(actor.x==x && actor.y==y && run.progress().elapsed()==clock && boss.elapsed()==bossTime
                && run.player().getVida()==health && shots.isEmpty(),"HUD settings freeze the boss pattern and incoming/outgoing combat");
            clickReleased(room,panel.presetBounds(0));clickReleased(room,panel.applyBounds());
            check(game.hudSettings().value().percent()==80 && Arrays.equals(projection.val,world.getCamera().combined.val),
                "A smaller HUD leaves the boss world camera unchanged");
            key(room,Input.Keys.F2);release(room,.05f);
            check(game.getScreen()==room && boss.elapsed()>bossTime,"Boss simulation resumes in the same room after HUD settings close");
        }
    }

    private static void verifyRendererRestoration(SpriteBatch batch,ShapeRenderer shapes) {
        Matrix4 oldBatch=new Matrix4(batch.getTransformMatrix()),oldShapes=new Matrix4(shapes.getTransformMatrix());
        Matrix4 batchProjection=new Matrix4(batch.getProjectionMatrix()),shapeProjection=new Matrix4(shapes.getProjectionMatrix());
        Matrix4 customBatch=new Matrix4().translate(3,5,0),customShapes=new Matrix4().translate(7,11,0);
        HudScaleRenderer renderer=new HudScaleRenderer();
        try {
            batch.setTransformMatrix(customBatch);shapes.setTransformMatrix(customShapes);
            for(HudLayout.Anchor anchor:HudLayout.Anchor.values()) {
                renderer.begin(batch,shapes,new HudSettings(125),anchor);
                try {
                    check(!Arrays.equals(customBatch.val,batch.getTransformMatrix().val),"HUD scale applies its transform to sprites");
                    check(!Arrays.equals(customShapes.val,shapes.getTransformMatrix().val),"HUD scale applies its transform to shapes");
                    check(Arrays.equals(batchProjection.val,batch.getProjectionMatrix().val)
                        && Arrays.equals(shapeProjection.val,shapes.getProjectionMatrix().val),"HUD scale never changes either projection matrix");
                } finally {renderer.end();}
                check(Arrays.equals(customBatch.val,batch.getTransformMatrix().val)
                    && Arrays.equals(customShapes.val,shapes.getTransformMatrix().val),
                    "HUD renderer restores independent prior matrices before menus and further world drawing");
            }
        } finally {batch.setTransformMatrix(oldBatch);shapes.setTransformMatrix(oldShapes);}
    }

    private static int centerPixels() {
        int width=Gdx.graphics.getBackBufferWidth(),height=Gdx.graphics.getBackBufferHeight();
        return Arrays.hashCode(ScreenUtils.getFrameBufferPixels(width*2/5,height/3,width/5,height/4,true));
    }
    private static void verifyTextAndBounds(Screen screen,HudPanel panel,String error)throws Exception {
        BitmapFont font=(BitmapFont)field(screen,"font");float x=font.getData().scaleX,y=font.getData().scaleY;
        try {
            for(Rectangle bounds:List.of(panel.minusBounds(),panel.plusBounds(),panel.trackBounds(),panel.applyBounds(),panel.defaultsBounds(),panel.backBounds()))
                check(bounds.x>=0 && bounds.y>=0 && bounds.x+bounds.width<=960 && bounds.y+bounds.height<=540
                    && bounds.width>0 && bounds.height>0,"HUD controls fit the unchanged logical UI rectangle");
            font.getData().setScale(.95f);
            for(int index=0;index<3;index++) {
                Rectangle bounds=panel.presetBounds(index);String label=List.of("80%","100%","125%").get(index);
                GlyphLayout text=new GlyphLayout(font,label);
                check(text.width<=bounds.width-12 && text.height<=bounds.height-8,"HUD percentage labels fit their preset hit areas");
            }
            font.getData().setScale(.88f);GlyphLayout notice=new GlyphLayout(font,error,Color.WHITE,890,Align.left,true);
            check(notice.width<=890 && notice.height<=40,"A HUD save failure fits its footer without hiding buttons");
        } finally {font.getData().setScale(x,y);}
    }
    private static VideoPanel video(Screen screen)throws Exception {
        return (VideoPanel)field(screen,screen instanceof VideoSettingsScreen?"panel":"videoPanel");
    }
    private static HudPanel openHud(Screen screen)throws Exception {
        VideoPanel video=video(screen);check(video.open(),"The video panel must be open before entering HUD settings");
        clickReleased(screen,video.hudButtonBounds());
        HudPanel panel=(HudPanel)field(video,"hudPanel");
        check(video.hudPage() && panel.open(),"The HUD button opens its dedicated settings page");
        return panel;
    }
    private static void keyReleased(Screen screen,int key)throws Exception {release(screen,.25f);key(screen,key);}
    private static void clickReleased(Screen screen,Rectangle target)throws Exception {release(screen,.25f);click(screen,target);}
    private static void click(Screen screen,Rectangle target)throws Exception {input(screen,0,Set.of(),Set.of(),Set.of(Input.Buttons.LEFT),Set.of(Input.Buttons.LEFT),target);}
    private static void key(Screen screen,int key)throws Exception {input(screen,0,Set.of(key),Set.of(key),Set.of(),Set.of(),null);}
    private static void release(Screen screen,float delta)throws Exception {input(screen,delta,Set.of(),Set.of(),Set.of(),Set.of(),null);}
    private static void input(Screen screen,float delta,Set<Integer> heldKeys,Set<Integer> keys,Set<Integer> heldButtons,Set<Integer> buttons,Rectangle target)throws Exception {
        FitViewport viewport=(FitViewport)field(screen,screen instanceof GameScreen || screen instanceof BossRoomScreen || screen instanceof RefugeScreen?"hud":"viewport");
        Vector2 pointer=viewport.project(new Vector2(target==null?700:target.x+target.width/2,target==null?270:target.y+target.height/2));
        int x=Math.round(pointer.x),y=Gdx.graphics.getHeight()-Math.round(pointer.y);Input original=Gdx.input;
        Gdx.input=(Input)Proxy.newProxyInstance(Input.class.getClassLoader(),new Class<?>[]{Input.class},(proxy,method,args)->switch(method.getName()) {
            case "isKeyPressed" -> (int)args[0]==Input.Keys.ANY_KEY?!heldKeys.isEmpty():heldKeys.contains((int)args[0]);
            case "isKeyJustPressed" -> (int)args[0]==Input.Keys.ANY_KEY?!keys.isEmpty():keys.contains((int)args[0]);
            case "isButtonPressed" -> heldButtons.contains((int)args[0]);
            case "isButtonJustPressed" -> buttons.contains((int)args[0]);
            case "getX" -> x;
            case "getY" -> y;
            default -> method.invoke(original,args);
        });
        try {screen.render(delta);}finally {Gdx.input=original;}
    }
}
