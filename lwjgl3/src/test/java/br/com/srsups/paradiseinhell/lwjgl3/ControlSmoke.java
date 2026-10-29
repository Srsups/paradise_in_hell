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

/** Exercises real remapping UI and world input with memory-only preferences and profiles. */
public final class ControlSmoke {
    private ControlSmoke() {}
    private static final class Store implements ControlSettingsStore {
        ControlSettings saved=ControlSettings.DEFAULT;
        int writes;
        boolean fail;
        @Override public ControlSettings load(){return saved;}
        @Override public void save(ControlSettings value)throws IOException {
            writes++;
            if(fail)throw new IOException("controls fixture save failure");
            saved=value;
        }
    }
    private static final class Fixture implements AutoCloseable {
        final Store store=new Store();
        final SaveSlots saves=SaveSlots.memory();
        final Main game=new Main(saves,AudioSettingsStore.memory(),new RecordedAudio(),
            VideoSettingsStore.memory(),VideoBackend.memory(),store);
        Fixture(){game.create();}
        void character(){check(game.createCharacter(Profession.CACADOR,GameDifficulty.MEDIUM),"Controls fixture creates a character");}
        @Override public void close(){game.dispose();}
    }
    public static void verify()throws Exception {
        verifyMenuDraftCaptureAndPersistence();
        verifyRemappedCombatAndOverlay();
        verifyNestedPanelsAndChoices();
        verifyRefuge();
        verifyBoss();
        System.out.println("M7_CONTROLS_OK: capture, persistence, world input and paused overlays");
    }

    private static void verifyMenuDraftCaptureAndPersistence()throws Exception {
        try(Fixture fixture=new Fixture()) {
            Main game=fixture.game;
            key(game.getScreen(),Input.Keys.F3);
            Screen screen=game.getScreen();
            check(screen.getClass().getSimpleName().equals("ControlsSettingsScreen"),"Menu F3 opens the dedicated controls screen without creating a character");
            ControlsPanel panel=panel(screen);
            check(panel.open() && !game.profiles().profile().professionChosen(),"Control configuration is available before save creation");
            release(screen,.25f);
            captureNpcDialogue(screen,"m7-controls-menu");verifyBindingText(screen,panel);

            Rectangle up=panel.bindingBounds(0,0);
            click(screen,up);
            check(panel.capturing(),"Clicking a binding starts capture rather than changing a live action");
            input(screen,.3f,Set.of(),Set.of(),Set.of(Input.Buttons.LEFT),Set.of(),up);
            check(panel.capturing() && panel.draft().equals(ControlSettings.DEFAULT),"The click held from selecting a cell cannot immediately bind left mouse");
            release(screen,.25f);key(screen,Input.Keys.Z);
            check(panel.draft().bindings(ControlAction.MOVE_UP).getFirst().equals(InputBinding.key(Input.Keys.Z))
                && game.controlSettings().value().equals(ControlSettings.DEFAULT) && fixture.store.writes==0,
                "A fresh key changes only the local draft before Save");

            ControlSettings beforeConflict=panel.draft();
            beginCapture(screen,panel.bindingBounds(1,0));key(screen,Input.Keys.Z);
            check(panel.draft().equals(beforeConflict) && fixture.store.writes==0,
                "A duplicate key cannot silently replace another action or change saved preferences");
            cancelCapture(screen,panel);

            beginCapture(screen,panel.bindingBounds(4,0));
            mouse(screen,Input.Buttons.MIDDLE);
            check(panel.draft().bindings(ControlAction.ATTACK).equals(List.of(InputBinding.mouse(Input.Buttons.MIDDLE))),
                "Mouse buttons can be captured for attack without changing cursor aiming");

            beginCapture(screen,panel.bindingBounds(0,1));key(screen,Input.Keys.DEL);
            check(panel.draft().bindings(ControlAction.MOVE_UP).equals(List.of(InputBinding.key(Input.Keys.Z))),
                "Delete removes an alternate association while retaining a usable action");
            beginCapture(screen,panel.bindingBounds(0,0));key(screen,Input.Keys.DEL);
            check(panel.draft().bindings(ControlAction.MOVE_UP).equals(List.of(InputBinding.key(Input.Keys.Z))),
                "The UI refuses to delete the last association of an action");
            cancelCapture(screen,panel);

            clickReleased(screen,panel.defaultsBounds());
            check(panel.draft().equals(ControlSettings.DEFAULT) && fixture.store.writes==0,"Restore defaults edits the draft until Save is chosen");
            captureKey(screen,panel.bindingBounds(0,0),Input.Keys.Z);
            beginCapture(screen,panel.bindingBounds(4,0));mouse(screen,Input.Buttons.MIDDLE);
            clickReleased(screen,panel.applyBounds());
            check(game.controlSettings().value().equals(panel.draft()) && fixture.store.saved.equals(panel.draft()) && fixture.store.writes==1,
                "The Save button applies and persists keyboard and mouse associations exactly once");
            check(new ControlSettingsService(fixture.store).value().equals(panel.draft()),"Global controls reload independently of character state");

            clickReleased(screen,panel.nextBounds());
            check(panel.page()==1,"The second page exposes guard, interaction, chest, tool and boon actions");
            verifyBindingText(screen,panel);captureNpcDialogue(screen,"m7-controls-page-two");
            ControlSettings saved=fixture.store.saved;
            captureKey(screen,panel.bindingBounds(1,0),Input.Keys.U);
            key(screen,Input.Keys.ESCAPE);
            check(game.getScreen() instanceof MenuScreen && fixture.store.saved.equals(saved) && game.controlSettings().value().equals(saved)
                && fixture.store.writes==1,"Leaving an unapplied draft restores the menu without saving or changing live controls");

            key(game.getScreen(),Input.Keys.F3);screen=game.getScreen();panel=panel(screen);release(screen,.25f);
            captureKey(screen,panel.bindingBounds(1,0),Input.Keys.X);
            fixture.store.fail=true;clickReleased(screen,panel.applyBounds());
            check(game.controlSettings().pendingSave() && !game.controlSettings().error().isEmpty()
                && game.controlSettings().value().equals(panel.draft()) && fixture.store.saved.equals(saved),
                "A failed controls save retains working choices in memory and preserves the prior disk snapshot");
            captureNpcDialogue(screen,"m7-controls-save-error");
            fixture.store.fail=false;clickReleased(screen,panel.applyBounds());
            check(!game.controlSettings().pendingSave() && fixture.store.saved.equals(game.controlSettings().value()),
                "Saving again retries a pending control preference without requiring a character");
            int writes=fixture.store.writes;
            for(int frame=0;frame<12;frame++)release(screen,.05f);
            check(fixture.store.writes==writes,"Drawing stable controls does not repeatedly write preferences");
            key(screen,Input.Keys.F3);
            check(game.getScreen() instanceof MenuScreen,"F3 closes the standalone settings screen");
        }
    }

    @SuppressWarnings("unchecked")
    private static void verifyRemappedCombatAndOverlay()throws Exception {
        try(Fixture fixture=new Fixture()) {
            fixture.character();Main game=fixture.game;
            check(game.controlSettings().apply(custom()),"Custom keyboard and mouse configuration is accepted");
            RunSession run=RunSession.campaign(game.campaignRegion(),Profession.CACADOR);
            GameScreen room=new GameScreen(game,run);game.setScreen(room);
            Jogador actor=(Jogador)field(room,"player");PlayerState stats=run.player();
            List<Projetil> shots=(List<Projetil>)field(room,"projectiles");
            float x=actor.x,y=actor.y;
            holdKey(room,.05f,Input.Keys.K);
            check(actor.x>x && actor.y==y,"A custom movement key moves the actual arena actor");
            x=actor.x;holdKey(room,.05f,Input.Keys.D);
            check(actor.x==x,"The replaced primary movement key no longer moves the actor");
            holdMouse(room,0,Input.Buttons.MIDDLE);
            check(!shots.isEmpty(),"The remapped attack mouse button emits a real projectile");
            int count=shots.size();set(stats,"cooldownTiro",0f);holdMouse(room,0,Input.Buttons.LEFT);
            check(shots.size()==count,"The old left attack binding no longer fires after remapping");
            input(room,.05f,Set.of(Input.Keys.SPACE,Input.Keys.K),Set.of(Input.Keys.SPACE),Set.of(),Set.of(),null);
            check(stats.isDashing(),"Dash uses its remapped key together with the configured movement direction");

            x=actor.x;y=actor.y;float clock=run.progress().elapsed(),shotX=shots.getFirst().x;
            float stamina=stats.getEstaminaAtual(),dash=(float)field(stats,"dashTimer");
            Object map=field(room,"map"),atlas=field(room,"atlas");
            input(room,.05f,Set.of(Input.Keys.F3,Input.Keys.K),Set.of(Input.Keys.F3),Set.of(Input.Buttons.MIDDLE),Set.of(),null);
            ControlsPanel panel=panel(room);
            check(panel.open(),"F3 opens the controls overlay over the current arena");
            input(room,2,Set.of(Input.Keys.K,Input.Keys.SPACE),Set.of(Input.Keys.SPACE),Set.of(Input.Buttons.MIDDLE),Set.of(),null);
            check(actor.x==x && actor.y==y && run.progress().elapsed()==clock && shots.getFirst().x==shotX
                && stats.getEstaminaAtual()==stamina && (float)field(stats,"dashTimer")==dash,
                "The controls overlay freezes actor, projectiles, dash, stamina and the run clock");
            captureNpcDialogue(room,"m7-controls-combat");
            input(room,.05f,Set.of(Input.Keys.F3),Set.of(Input.Keys.F3),Set.of(Input.Buttons.MIDDLE),Set.of(),null);
            check(!panel.open() && actor.x==x && run.progress().elapsed()==clock && game.getScreen()==room
                && field(room,"map")==map && field(room,"atlas")==atlas,"Closing controls consumes its frame and preserves arena ownership");
            set(stats,"cooldownTiro",0f);count=shots.size();
            holdMouse(room,0,Input.Buttons.MIDDLE);
            check(shots.size()==count,"An attack held while closing the overlay remains blocked until physical release");
            release(room,0);holdMouse(room,0,Input.Buttons.MIDDLE);
            check(shots.size()==count+1,"A fresh press after release resumes the configured attack");
            release(room,.05f);
            check(run.progress().elapsed()>clock,"The arena simulation resumes after the overlay closes");

            // Isolate utility actions from the dash started above.
            set(stats,"dashTimer",0f);
            key(room,Input.Keys.Y);BoonPanel boons=(BoonPanel)field(room,"boonPanel");
            check(boons.open(),"The remapped boon reference key opens the existing panel");
            key(room,Input.Keys.TAB);check(boons.open(),"The replaced Tab binding cannot close the remapped boon panel");
            key(room,Input.Keys.Y);check(!boons.open(),"The same remapped reference key closes the boon panel");
            RunChests chests=(RunChests)field(room,"chests");List<RunChests.Chest> contents=(List<RunChests.Chest>)field(chests,"chests");
            contents.add(new RunChests.Chest(actor.x+8,actor.y+8,RunChests.Reward.OBOLS));
            key(room,Input.Keys.F);check(chests.chests().size()==1,"The previous chest shortcut no longer opens supplies");
            key(room,Input.Keys.B);check(chests.chests().isEmpty(),"A nearby real chest opens with its configured interaction key");
        }
    }

    private static void verifyNestedPanelsAndChoices()throws Exception {
        try(Fixture fixture=new Fixture()) {
            fixture.character();Main game=fixture.game;
            RunSession run=RunSession.campaign(game.campaignRegion(),Profession.CACADOR);
            GameScreen room=new GameScreen(game,run);game.setScreen(room);
            key(room,Input.Keys.TAB);BoonPanel boons=(BoonPanel)field(room,"boonPanel");float clock=run.progress().elapsed();
            key(room,Input.Keys.F3);release(room,2);key(room,Input.Keys.F3);
            check(boons.open() && run.progress().elapsed()==clock,"Controls preserve an already open boon reference");key(room,Input.Keys.ESCAPE);
            key(room,Input.Keys.O);AudioPanel audio=(AudioPanel)field(room,"audioPanel");clock=run.progress().elapsed();
            key(room,Input.Keys.F3);input(room,2,Set.of(Input.Keys.M),Set.of(Input.Keys.M),Set.of(),Set.of(),null);key(room,Input.Keys.ESCAPE);
            check(audio.open() && !game.audioSettings().value().muted() && run.progress().elapsed()==clock,
                "Controls preserve audio settings without forwarding its mute shortcut");key(room,Input.Keys.ESCAPE);
            key(room,Input.Keys.F1);TutorialPanel guide=(TutorialPanel)field(room,"tutorialPanel");clock=run.progress().elapsed();
            key(room,Input.Keys.F3);release(room,2);key(room,Input.Keys.F3);
            check(guide.open() && run.progress().elapsed()==clock,"Controls preserve the current guide page and paused simulation");key(room,Input.Keys.ESCAPE);
            key(room,Input.Keys.ESCAPE);clock=run.progress().elapsed();
            key(room,Input.Keys.F3);release(room,2);key(room,Input.Keys.ESCAPE);
            check(field(room,"state").toString().equals("PAUSED") && run.progress().elapsed()==clock,"Closing controls does not cancel an existing explicit pause");key(room,Input.Keys.ESCAPE);

            run.player().ganharXP(10);release(room,0);
            check(field(room,"state").toString().equals("LEVEL_UP"),"The fixture opens a genuine pending boon choice");
            ChoiceInputGuard guard=(ChoiceInputGuard)field(room,"choiceGuard");float waiting=guard.remaining();clock=run.progress().elapsed();
            key(room,Input.Keys.F3);
            input(room,3,Set.of(Input.Keys.NUM_1),Set.of(Input.Keys.NUM_1),Set.of(Input.Buttons.LEFT),Set.of(Input.Buttons.LEFT),null);
            check(guard.remaining()==waiting && field(room,"state").toString().equals("LEVEL_UP") && run.progress().elapsed()==clock,
                "Control configuration cannot spend a level or age its input-protection timer");
            input(room,.05f,Set.of(Input.Keys.F3),Set.of(Input.Keys.F3),Set.of(Input.Buttons.LEFT),Set.of(),null);
            check(!panel(room).open() && guard.remaining()==waiting && field(room,"state").toString().equals("LEVEL_UP"),
                "Closing controls consumes the held attack and leaves the original pending choice intact");
            input(room,1.6f,Set.of(Input.Keys.NUM_1),Set.of(Input.Keys.NUM_1),Set.of(Input.Buttons.LEFT),Set.of(),null);
            check(field(room,"state").toString().equals("LEVEL_UP"),"A held attack still cannot choose an underlying boon after the settings panel closes");
            release(room,0);key(room,Input.Keys.NUM_1);
            check(field(room,"state").toString().equals("PLAYING"),"A released fresh selection completes the original boon choice normally");
        }
    }

    private static void verifyRefuge()throws Exception {
        try(Fixture fixture=new Fixture()) {
            fixture.character();Main game=fixture.game;game.controlSettings().apply(custom());
            RefugeScreen refuge=new RefugeScreen(game);game.setScreen(refuge);
            Jogador actor=(Jogador)field(refuge,"player");float x=actor.x;
            holdKey(refuge,.05f,Input.Keys.K);check(actor.x>x,"The configured movement is also used in the refuge");
            x=actor.x;float y=actor.y,visual=(float)field(refuge,"visualTime");
            key(refuge,Input.Keys.F3);input(refuge,2,Set.of(Input.Keys.K,Input.Keys.ENTER),Set.of(Input.Keys.ENTER),Set.of(Input.Buttons.MIDDLE),Set.of(),null);
            check(panel(refuge).open() && actor.x==x && actor.y==y && (float)field(refuge,"visualTime")==visual && game.getScreen()==refuge,
                "Refuge controls freeze movement and ignore departure even when gameplay inputs are held");
            captureNpcDialogue(refuge,"m7-controls-refuge");key(refuge,Input.Keys.ESCAPE);
            check(!panel(refuge).open() && game.getScreen()==refuge && actor.x==x,"Closing refuge controls consumes Escape without returning to the menu");
            key(refuge,Input.Keys.C);DialoguePlayback dialogue=(DialoguePlayback)field(refuge,"playback");
            String visible=dialogue.visibleText();key(refuge,Input.Keys.F3);release(refuge,2);key(refuge,Input.Keys.F3);
            check(field(refuge,"state").toString().equals("DIALOGUE") && dialogue.visibleText().equals(visible),
                "Consulting controls preserves the exact revealed text of an active refuge conversation");
        }
    }

    @SuppressWarnings("unchecked")
    private static void verifyBoss()throws Exception {
        try(Fixture fixture=new Fixture()) {
            fixture.character();Main game=fixture.game;game.controlSettings().apply(custom());
            RunSession run=bossSession(Profession.GUERREIRO);game.profiles().prepareRun(run);
            BossRoomScreen room=new BossRoomScreen(game,run);game.setScreen(room);
            BossEncounter boss=(BossEncounter)field(room,"boss");Jogador actor=(Jogador)field(room,"player");
            holdKey(room,0,Input.Keys.G);
            check(run.player().guarding(),"The warrior raises the shield using a keyboard association");
            float x=actor.x,y=actor.y,clock=run.progress().elapsed(),bossClock=boss.elapsed();
            key(room,Input.Keys.F3);input(room,3,Set.of(Input.Keys.K,Input.Keys.SPACE),Set.of(Input.Keys.SPACE),Set.of(Input.Buttons.MIDDLE),Set.of(),null);
            check(actor.x==x && actor.y==y && boss.elapsed()==bossClock && run.progress().elapsed()==clock,
                "Controls stop the boss pattern and run clock as well as player movement");
            captureNpcDialogue(room,"m7-controls-boss");
            input(room,.05f,Set.of(Input.Keys.F3),Set.of(Input.Keys.F3),Set.of(Input.Buttons.MIDDLE),Set.of(),null);
            List<Projetil> shots=(List<Projetil>)field(room,"projectiles");
            int count=shots.size();holdMouse(room,0,Input.Buttons.MIDDLE);
            check(!panel(room).open() && game.getScreen()==room && boss.elapsed()==bossClock && shots.size()==count,
                "Closing boss controls preserves the same arena and blocks an attack held through closing");
            release(room,.05f);check(boss.elapsed()>bossClock,"Boss patterns resume after leaving controls");
        }
        try(Fixture fixture=new Fixture()) {
            fixture.character();Main game=fixture.game;game.controlSettings().apply(custom());
            RunSession run=RunSession.campaign(game.campaignRegion(),Profession.FAZENDEIRO);
            GameScreen room=new GameScreen(game,run);game.setScreen(room);WeaponType before=run.player().getWeapon();
            key(room,Input.Keys.Q);check(run.player().getWeapon()==before,"The farmer's replaced tool shortcut is inactive");
            key(room,Input.Keys.X);check(run.player().getWeapon()!=before,"The farmer switches tools with the configured key");
        }
    }

    private static ControlSettings custom() {
        return ControlSettings.DEFAULT
            .withBinding(ControlAction.MOVE_UP,0,InputBinding.key(Input.Keys.Z))
            .withBinding(ControlAction.MOVE_RIGHT,0,InputBinding.key(Input.Keys.K))
            .withBinding(ControlAction.ATTACK,0,InputBinding.mouse(Input.Buttons.MIDDLE))
            .withBinding(ControlAction.DASH,0,InputBinding.key(Input.Keys.SPACE))
            .withBinding(ControlAction.GUARD,0,InputBinding.key(Input.Keys.G))
            .withBinding(ControlAction.INTERACT,0,InputBinding.key(Input.Keys.U))
            .withBinding(ControlAction.OPEN_CHEST,0,InputBinding.key(Input.Keys.B))
            .withBinding(ControlAction.SWITCH_TOOL,0,InputBinding.key(Input.Keys.X))
            .withBinding(ControlAction.BOONS,0,InputBinding.key(Input.Keys.Y));
    }
    private static ControlsPanel panel(Screen screen)throws Exception {
        return (ControlsPanel)field(screen,screen instanceof GameScreen || screen instanceof BossRoomScreen || screen instanceof RefugeScreen?"controlsPanel":"panel");
    }
    private static void verifyBindingText(Screen screen,ControlsPanel panel)throws Exception {
        BitmapFont font=(BitmapFont)field(screen,"font");float sx=font.getData().scaleX,sy=font.getData().scaleY;
        try {
            font.getData().setScale(.9f);
            int start=panel.page()*6;
            for(int row=0;row<6 && start+row<ControlAction.values().length;row++) {
                List<InputBinding> bindings=panel.draft().bindings(ControlAction.values()[start+row]);
                for(int slot=0;slot<bindings.size();slot++) {
                    Rectangle bounds=panel.bindingBounds(row,slot);GlyphLayout text=new GlyphLayout(font,bindings.get(slot).label());
                    check(text.width<=bounds.width-20 && text.height<=bounds.height-10,"The actual localized binding label fits its clickable card");
                }
            }
        } finally {font.getData().setScale(sx,sy);}
    }
    private static void beginCapture(Screen screen,Rectangle bounds)throws Exception {
        clickReleased(screen,bounds);check(panel(screen).capturing(),"The requested control cell enters capture");release(screen,.25f);
    }
    private static void captureKey(Screen screen,Rectangle bounds,int key)throws Exception {beginCapture(screen,bounds);key(screen,key);}
    private static void cancelCapture(Screen screen,ControlsPanel panel)throws Exception {if(panel.capturing())key(screen,Input.Keys.ESCAPE);}
    private static void clickReleased(Screen screen,Rectangle bounds)throws Exception {release(screen,.25f);click(screen,bounds);}
    private static void click(Screen screen,Rectangle bounds)throws Exception {input(screen,0,Set.of(),Set.of(),Set.of(Input.Buttons.LEFT),Set.of(Input.Buttons.LEFT),bounds);}
    private static void key(Screen screen,int key)throws Exception {input(screen,0,Set.of(key),Set.of(key),Set.of(),Set.of(),null);}
    private static void mouse(Screen screen,int button)throws Exception {input(screen,0,Set.of(),Set.of(),Set.of(button),Set.of(button),null);}
    private static void holdKey(Screen screen,float delta,int key)throws Exception {input(screen,delta,Set.of(key),Set.of(),Set.of(),Set.of(),null);}
    private static void holdMouse(Screen screen,float delta,int button)throws Exception {input(screen,delta,Set.of(),Set.of(),Set.of(button),Set.of(),null);}
    private static void release(Screen screen,float delta)throws Exception {input(screen,delta,Set.of(),Set.of(),Set.of(),Set.of(),null);}
    private static void input(Screen screen,float delta,Set<Integer> heldKeys,Set<Integer> keys,Set<Integer> heldButtons,Set<Integer> buttons,Rectangle target)throws Exception {
        FitViewport viewport=(FitViewport)field(screen,screen instanceof GameScreen || screen instanceof BossRoomScreen || screen instanceof RefugeScreen?"hud":"viewport");
        Vector2 pointer=viewport.project(new Vector2(target==null?700:target.x+target.width/2,target==null?270:target.y+target.height/2));
        int x=Math.round(pointer.x),y=Gdx.graphics.getHeight()-Math.round(pointer.y);
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
        try {screen.render(delta);}finally {Gdx.input=original;}
    }
}
