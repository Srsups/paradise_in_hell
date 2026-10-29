package br.com.srsups.paradiseinhell.lwjgl3;

import br.com.srsups.paradiseinhell.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
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

/** Real input, graphics and memory-only profiles for the contextual tutorial. */
public final class TutorialSmoke {
    private TutorialSmoke() {}
    private static final class Store implements ProfileStore {
        CampaignProfile saved=new CampaignProfile();
        int writes;
        @Override public CampaignProfile load(){return saved.copy();}
        @Override public void save(CampaignProfile profile) throws IOException {writes++;saved=profile.copy();}
    }
    private static Main game(SaveSlots slots) {
        Main game=new Main(slots,AudioSettingsStore.memory(),new RecordedAudio());game.create();return game;
    }
    private static void create(Main game,Profession profession) {
        check(game.createCharacter(profession,GameDifficulty.MEDIUM),"Tutorial fixture creates a real character");
    }
    public static void verify() throws Exception {
        verifyRefugeAndPreferences();
        verifyDemoAndChoiceProtection();
        verifyCampaignActionsAndNewRooms();
        verifyGuardianReference();
    }

    private static void verifyRefugeAndPreferences() throws Exception {
        Store first=new Store(),second=new Store();SaveSlots slots=new SaveSlots(first,second,ProfileStore.memory(),ProfileStore.memory());
        Main game=game(slots);
        try {
            create(game,Profession.CACADOR);
            check(game.profileAt(1).createCharacter(Profession.FERREIRO,GameDifficulty.MEDIUM,false),"Second tutorial profile is independent");
            RefugeScreen refuge=new RefugeScreen(game);game.setScreen(refuge);refuge.render(0);
            TutorialPanel panel=(TutorialPanel)field(refuge,"tutorialPanel");TutorialGuide guide=(TutorialGuide)field(refuge,"tutorial");
            Jogador actor=(Jogador)field(refuge,"player");PlayerState stats=(PlayerState)field(refuge,"stats");
            float x=actor.x,y=actor.y,cooldown=(float)field(stats,"cooldownTiro"),reading=guide.readingSeconds(TutorialTopic.REFUGE);
            int writes=first.writes;
            input(refuge,.05f,Input.Keys.F1,true,true,false,700,270);
            check(panel.open() && panel.pageCount()==2,"F1 opens the campaign guide in the refuge");
            input(refuge,2,0,true,true,true,700,270);
            check(actor.x==x && actor.y==y && (float)field(stats,"cooldownTiro")==cooldown
                && guide.readingSeconds(TutorialTopic.REFUGE)==reading,"The refuge guide freezes movement, attack and contextual reading");
            check(first.writes==writes,"Opening and consulting the reference cannot mark lessons or write every frame");
            verifyProfessionCopy(game,(BitmapFont)field(refuge,"font"));
            verifyCopy(panel,(BitmapFont)field(refuge,"font"));captureNpcDialogue(refuge,"m7-guide-refuge");
            press(refuge,Input.Keys.RIGHT);verifyCopy(panel,(BitmapFont)field(refuge,"font"));
            check(panel.page()==1,"Campaign guide exposes the progression reference on its second page");
            captureNpcDialogue(refuge,"m7-guide-progression");
            input(refuge,.05f,Input.Keys.F1,true,true,true,700,270);
            check(!panel.open() && actor.x==x && actor.y==y && field(refuge,"state").toString().equals("WALKING"),
                "Closing F1 consumes the frame without moving, attacking or leaving the refuge");
            input(refuge,.05f,0,true,true,false,700,270);
            check(actor.x>x && !((PlayerAnimation)field(actor,"visual")).attacking(),"Refuge movement resumes while attacks remain disabled");

            press(refuge,Input.Keys.F1);x=actor.x;y=actor.y;
            input(refuge,.05f,Input.Keys.ESCAPE,true,true,false,700,270);
            check(!panel.open() && actor.x==x && actor.y==y && game.getScreen()==refuge,
                "ESC closes the guide without also executing the refuge's menu action");
            writes=first.writes;
            press(refuge,Input.Keys.F1);press(refuge,Input.Keys.H);
            check(!game.profiles().profile().tutorialEnabled() && !guide.enabled() && first.writes==writes+1,
                "H changes and persists only this character's hint preference");
            press(refuge,Input.Keys.F1);press(refuge,Input.Keys.F1);
            check(panel.open() && !guide.enabled(),"The full reference can reopen with contextual hints disabled");
            for(int i=0;i<30;i++)refuge.render(.05f);
            check(first.writes==writes+1,"A disabled hint and an open reference do not generate repeated autosaves");
            selectSave(game,1,false);
            check(game.profiles().profile().tutorialEnabled(),"Another save retains its own enabled hints");
            selectSave(game,0,false);
            check(!game.profiles().profile().tutorialEnabled(),"Returning to the original save restores its preference");
            press(game.getScreen(),Input.Keys.H);
            check(game.profiles().profile().tutorialEnabled(),"H also enables hints outside the reference overlay");
            press(game.getScreen(),Input.Keys.H);
            check(!game.profiles().profile().tutorialEnabled(),"H outside the guide persists the next disabled preference");
        } finally {game.dispose();}
        Main reloaded=game(slots);
        try {
            selectSave(reloaded,0,false);TutorialGuide guide=(TutorialGuide)field(reloaded.getScreen(),"tutorial");
            check(!guide.enabled() && !new ProfileService(first).profile().tutorialEnabled(),"Reloading the game keeps the saved H preference");
            selectSave(reloaded,1,false);
            check(((TutorialGuide)field(reloaded.getScreen(),"tutorial")).enabled(),"Reloading does not spread a preference between characters");
        } finally {reloaded.dispose();}
    }

    @SuppressWarnings("unchecked")
    private static void verifyDemoAndChoiceProtection() throws Exception {
        Store store=new Store();Main game=game(new SaveSlots(store));
        try {
            create(game,Profession.CACADOR);int writes=store.writes;
            RunSession run=RunSession.demo(Profession.CACADOR);GameScreen room=new GameScreen(game,run);game.setScreen(room);room.render(0);
            Jogador actor=(Jogador)field(room,"player");TutorialPanel panel=(TutorialPanel)field(room,"tutorialPanel");
            captureNpcDialogue(room,"m7-tutorial-combat");
            float x=actor.x,y=actor.y,elapsed=run.progress().elapsed();
            input(room,.05f,Input.Keys.F1,true,true,false,700,270);
            check(panel.open() && panel.pageCount()==1,"Demo reference contains its basic page only");
            input(room,3,0,true,true,true,700,270);
            check(actor.x==x && actor.y==y && run.progress().elapsed()==elapsed && ((List<?>)field(room,"projectiles")).isEmpty(),
                "Guide input cannot move, shoot or advance the horde clock");
            verifyCopy(panel,(BitmapFont)field(room,"font"));captureNpcDialogue(room,"m7-guide-combat");
            press(room,Input.Keys.RIGHT);check(panel.page()==0,"Demo guide cannot expose campaign-only progression");
            input(room,0,0,false,false,false,700,270); // Release the attack before clicking the close button.
            clickClose(room);
            check(!panel.open() && actor.x==x && actor.y==y && run.progress().elapsed()==elapsed && ((List<?>)field(room,"projectiles")).isEmpty(),
                "Clicking the reference close button never becomes an attack");
            input(room,.05f,0,true,true,false,700,270);dashRightM4(room);
            check(actor.x>x && run.tutorial().seen(TutorialTopic.MOVE) && run.tutorial().seen(TutorialTopic.ATTACK)
                && run.tutorial().seen(TutorialTopic.DASH),"Real demo movement, attack and dash complete their temporary lessons");

            ((List<Projetil>)field(room,"projectiles")).clear();addXp(room,1);room.render(0);
            check(run.player().getXpAtual()==1 && run.tutorial().seen(TutorialTopic.EXPERIENCE),"A collected demo crystal completes its actual XP lesson");
            addXp(room,9);room.render(0);check(field(room,"state").toString().equals("LEVEL_UP"),"Real crystals open the boon selection");
            ChoiceInputGuard guard=(ChoiceInputGuard)field(room,"choiceGuard");float remaining=guard.remaining();
            press(room,Input.Keys.F1);input(room,2,Input.Keys.NUM_1,false,true,true,700,270);
            check(panel.open() && guard.remaining()==remaining && !run.tutorial().seen(TutorialTopic.BOONS),
                "Guide time and clicks cannot advance the boon protection or select an upgrade");
            input(room,.05f,Input.Keys.ESCAPE,true,true,true,700,270);
            check(!panel.open() && guard.remaining()==remaining && field(room,"state").toString().equals("LEVEL_UP"),
                "Closing the guide preserves the same pending boon and consumes its input frame");
            input(room,1.6f,0,false,true,false,700,270);
            input(room,0,Input.Keys.NUM_1,false,true,false,700,270);
            check(!guard.ready() && field(room,"state").toString().equals("LEVEL_UP"),"The original 1.5-second guard still requires releasing held attack");
            input(room,0,0,false,false,false,700,270);input(room,0,Input.Keys.NUM_1,false,false,false,700,270);
            check(field(room,"state").toString().equals("PLAYING") && run.tutorial().seen(TutorialTopic.BOONS),
                "A released fresh choice applies a boon and completes its lesson");
            check(store.writes==writes,"Demo lessons must never write seen topics to the campaign profile");
            for(TutorialTopic topic:TutorialTopic.values())check(!store.saved.tutorialSeen(topic),"Demo seen state is local to its attempt: "+topic);
        } finally {game.dispose();}
    }

    @SuppressWarnings("unchecked")
    private static void verifyCampaignActionsAndNewRooms() throws Exception {
        Store store=new Store();Main game=game(new SaveSlots(store));
        try {
            create(game,Profession.CACADOR);
            RunSession run=RunSession.campaign(game.campaignRegion(),Profession.CACADOR);GameScreen room=new GameScreen(game,run);game.setScreen(room);
            int writes=store.writes;addXp(room,1);room.render(0);
            check(run.player().getXpAtual()==1 && store.saved.tutorialSeen(TutorialTopic.EXPERIENCE) && store.writes==writes+1,
                "Collecting a real campaign crystal persists exactly one XP lesson");
            addXp(room,9);room.render(0);
            check(field(room,"state").toString().equals("LEVEL_UP") && !store.saved.tutorialSeen(TutorialTopic.BOONS),
                "Opening the choice screen alone does not claim the boon lesson");
            input(room,1.51f,0,false,false,false,700,270);input(room,0,Input.Keys.NUM_1,false,false,false,700,270);
            check(store.saved.tutorialSeen(TutorialTopic.BOONS) && store.writes==writes+2,"Choosing an actual boon persists its lesson once");
            int afterChoice=store.writes;for(int i=0;i<20;i++)room.render(.05f);
            check(store.writes==afterChoice,"Completed action lessons are not saved again on every combat frame");
            verifyNestedPanels(room,run);
            TutorialGuide guide=run.tutorial();TutorialPanel panel=(TutorialPanel)field(room,"tutorialPanel");
            press(room,Input.Keys.F1);verifyCopy(panel,(BitmapFont)field(room,"font"));
            float clock=run.progress().elapsed(),reading=guide.readingSeconds(TutorialTopic.MOVE);
            for(int i=0;i<10;i++)input(room,.5f,0,true,true,true,700,270);
            check(run.progress().elapsed()==clock && guide.readingSeconds(TutorialTopic.MOVE)==reading && store.writes==afterChoice,
                "Manual consultation freezes both combat and the contextual reading clock without writes");
            press(room,Input.Keys.F1);
            meetGoals(run);room.render(0);captureNpcDialogue(room,"m7-tutorial-exit");
            check(run.arenaComplete() && !game.profiles().profile().tutorialSeen(TutorialTopic.EXIT),
                "Unlocking the passage displays an unread contextual exit lesson");
            ((List<OrbeXP>)field(room,"orbs")).clear();run.enterArena("cinzas");game.setScreen(new GameScreen(game,run));
            check(run.tutorial()==guide && guide.seen(TutorialTopic.EXPERIENCE) && guide.seen(TutorialTopic.BOONS),
                "Changing rooms keeps the same attempt's completed tutorial actions");
            check(!run.arenaComplete(),"A new room resets its own goals without clearing the tutorial");
            RunSession next=RunSession.campaign(game.campaignRegion(),Profession.CACADOR);GameScreen nextRoom=new GameScreen(game,next);game.setScreen(nextRoom);
            check(next.tutorial()!=guide && next.tutorial().seen(TutorialTopic.EXPERIENCE) && next.tutorial().seen(TutorialTopic.BOONS),
                "A fresh campaign attempt restores saved lessons into a fresh guide");
            RunSession demo=RunSession.demo();GameScreen demoRoom=new GameScreen(game,demo);game.setScreen(demoRoom);
            check(!demo.tutorial().seen(TutorialTopic.EXPERIENCE),"Campaign lessons do not silently mark a new demo's temporary guide");
        } finally {game.dispose();}
    }

    private static void verifyGuardianReference() throws Exception {
        Store store=new Store();Main game=game(new SaveSlots(store));
        try {
            create(game,Profession.CACADOR);RunSession run=bossSession(Profession.CACADOR);game.profiles().prepareRun(run);
            BossRoomScreen room=new BossRoomScreen(game,run);game.setScreen(room);room.render(0);
            BossEncounter boss=(BossEncounter)field(room,"boss");Jogador actor=(Jogador)field(room,"player");
            TutorialPanel panel=(TutorialPanel)field(room,"tutorialPanel");float x=actor.x,y=actor.y,clock=run.progress().elapsed(),bossClock=boss.elapsed();
            input(room,.05f,Input.Keys.F1,true,true,false,700,270);input(room,2,0,true,true,true,700,270);
            check(panel.open() && actor.x==x && actor.y==y && run.progress().elapsed()==clock && boss.elapsed()==bossClock
                && ((List<?>)field(room,"projectiles")).isEmpty(),"Guardian guide freezes its attack clock, player and weapons");
            verifyCopy(panel,(BitmapFont)field(room,"font"));press(room,Input.Keys.RIGHT);verifyCopy(panel,(BitmapFont)field(room,"font"));
            input(room,.05f,Input.Keys.ESCAPE,true,true,true,700,270);
            check(!panel.open() && !(boolean)field(room,"paused") && actor.x==x && actor.y==y && boss.elapsed()==bossClock,
                "Closing the guardian guide consumes ESC without also pausing or running the arena");
            input(room,.05f,0,false,true,false,700,270);
            check(boss.elapsed()>bossClock && !((List<?>)field(room,"projectiles")).isEmpty(),"Normal guardian combat resumes on the next frame");
        } finally {game.dispose();}
    }

    @SuppressWarnings("unchecked")
    private static void addXp(GameScreen room,int amount) throws Exception {
        Jogador actor=(Jogador)field(room,"player");Texture atlas=(Texture)field(room,"atlas");
        ((List<OrbeXP>)field(room,"orbs")).add(new OrbeXP(actor.x+5,actor.y+5,Art.cell(atlas,1,3),amount));
    }
    private static void clickClose(Screen screen) throws Exception {
        TutorialPanel panel=(TutorialPanel)field(screen,"tutorialPanel");Rectangle close=(Rectangle)field(panel,"close");
        FitViewport viewport=(FitViewport)field(screen,"hud");Vector2 point=viewport.project(new Vector2(close.x+close.width/2,close.y+close.height/2));
        input(screen,.05f,0,true,true,true,Math.round(point.x),Gdx.graphics.getHeight()-Math.round(point.y));
    }
    private static void verifyNestedPanels(GameScreen room,RunSession run) throws Exception {
        TutorialPanel guide=(TutorialPanel)field(room,"tutorialPanel");
        press(room,Input.Keys.ESCAPE);float at=run.progress().elapsed();
        press(room,Input.Keys.F1);press(room,Input.Keys.ESCAPE);
        check(!guide.open() && field(room,"state").toString().equals("PAUSED") && run.progress().elapsed()==at,
            "Guide consultation preserves a previous explicit pause");
        press(room,Input.Keys.ESCAPE);
        BoonPanel boons=(BoonPanel)field(room,"boonPanel");press(room,Input.Keys.TAB);at=run.progress().elapsed();
        press(room,Input.Keys.F1);press(room,Input.Keys.F1);
        check(boons.open() && !guide.open() && run.progress().elapsed()==at,"Closing the guide restores the still-open Tab reference");
        press(room,Input.Keys.ESCAPE);
        AudioPanel audio=(AudioPanel)field(room,"audioPanel");press(room,Input.Keys.O);at=run.progress().elapsed();
        press(room,Input.Keys.F1);press(room,Input.Keys.ESCAPE);
        check(audio.open() && !guide.open() && run.progress().elapsed()==at,"Guide consultation preserves an existing audio panel and its paused clock");
        press(room,Input.Keys.ESCAPE);
    }
    private static void verifyProfessionCopy(Main game,BitmapFont font) {
        for(Profession profession:new Profession[]{Profession.CACADOR,Profession.GUERREIRO,Profession.FAZENDEIRO}) {
            TutorialPanel panel=new TutorialPanel(game,new TutorialGuide(false),game.profiles().profile().characterId(),false,profession);
            verifyCopy(panel,font);panel.movePage(1);verifyCopy(panel,font);
            if(profession==Profession.GUERREIRO)check(panel.detail(TutorialTopic.ATTACK).contains("direito"),"Warrior reference explains its shield input");
            if(profession==Profession.FAZENDEIRO)check(panel.detail(TutorialTopic.ATTACK).contains("Q"),"Farmer reference explains its active-combat tool change");
        }
    }
    private static void verifyCopy(TutorialPanel panel,BitmapFont font) {
        float scaleX=font.getData().scaleX,scaleY=font.getData().scaleY;
        try {
            for(int row=0;row<panel.topics().size();row++) {
                TutorialTopic topic=panel.topics().get(row);Rectangle bounds=panel.bodyBounds(row);
                font.getData().setScale(TutorialPanel.DETAIL_SCALE);
                GlyphLayout detail=new GlyphLayout(font,panel.detail(topic),Color.WHITE,bounds.width,Align.left,true);
                check(detail.width<=bounds.width+.1f && detail.height<=bounds.height,
                    "Tutorial copy stays inside its own row: "+topic);
                font.getData().setScale(.98f);
                check(new GlyphLayout(font,TutorialGuide.title(topic)).width<=bounds.width,"Tutorial heading fits its row: "+topic);
            }
        } finally {font.getData().setScale(scaleX,scaleY);}
    }
    private static void input(Screen screen,float delta,int key,boolean moving,boolean attack,boolean click,int x,int y) {
        Input original=Gdx.input;
        Gdx.input=(Input)Proxy.newProxyInstance(Input.class.getClassLoader(),new Class<?>[]{Input.class},(proxy,method,args)->{
            return switch(method.getName()) {
                case "isKeyJustPressed" -> key!=0 && (int)args[0]==key;
                case "isKeyPressed" -> moving && (int)args[0]==Input.Keys.D;
                case "isButtonPressed" -> attack && (int)args[0]==Input.Buttons.LEFT;
                case "isButtonJustPressed" -> click && (int)args[0]==Input.Buttons.LEFT;
                case "getX" -> x;
                case "getY" -> y;
                default -> method.invoke(original,args);
            };
        });
        try {screen.render(delta);}finally {Gdx.input=original;}
    }
}
