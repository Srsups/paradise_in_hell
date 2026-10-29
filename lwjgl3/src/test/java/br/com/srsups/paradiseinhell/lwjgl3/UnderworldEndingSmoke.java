package br.com.srsups.paradiseinhell.lwjgl3;

import br.com.srsups.paradiseinhell.*;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import java.io.IOException;
import static br.com.srsups.paradiseinhell.lwjgl3.DemoSmokeTest.*;

/** The narrative reads already-settled facts and owns no living arena or future screen. */
final class UnderworldEndingSmoke {
    private UnderworldEndingSmoke() {}
    static void verify() throws Exception {
        Store store=new Store();Main game=new Main(new SaveSlots(store),AudioSettingsStore.memory(),new RecordedAudio());
        try {
            game.create();RunSession win=win(game);
            check(game.getScreen() instanceof RunEndingScreen,"The guardian keeps its full death animation before the epilogue");
            check(store.saved.has(CampaignProfile.Event.INSURGENCE_SPARK_OBTAINED)
                && !store.saved.has(CampaignProfile.Event.UNDERWORLD_EPILOGUE_READ),"The earned spark is durable before any narrative input");
            int money=game.profiles().profile().obols(),souls=game.shared().progress().souls(),victories=game.profiles().profile().victories();
            float elapsed=win.progress().elapsed();RunReport report=game.latestReport();
            finishVisualEnding(game);
            check(game.getScreen() instanceof UnderworldEndingScreen,"The first real Submundo victory opens its pending story");
            UnderworldEndingScreen ending=(UnderworldEndingScreen)game.getScreen();
            DialoguePanel panel=(DialoguePanel)field(ending,"dialoguePanel");DialoguePlayback playback=(DialoguePlayback)field(ending,"playback");
            npcPointer(ending,panel.bounds(),.1f,true,true,-1);
            check(playback.line()==0,"The attack that killed the guardian cannot advance the new dialogue");
            npcPointer(ending,panel.bounds(),1.6f,true,true,-1);
            check(playback.line()==0,"Holding attack stays blocked after the epilogue reading delay");
            verifyCopy(ending,panel);captureNpcDialogue(ending,"m7-centelha-charon");
            npcPointer(ending,panel.bounds(),0,false,false,-1);npcPointer(ending,panel.bounds(),0,true,true,-1);
            check(playback.line()==1 && playback.current().portrait()==DialoguePortrait.PROTAGONIST,
                "A released fresh click advances exactly one line and switches the speaking portrait");
            press(ending,Input.Keys.ENTER);String visible=playback.visibleText();
            press(ending,Input.Keys.T);press(ending,Input.Keys.T);
            check(playback.line()==1 && playback.visibleText().equals(visible),"Text speed never changes the speaking line or erases revealed copy");
            captureNpcDialogue(ending,"m7-centelha-protagonist");
            check(win.progress().elapsed()==elapsed && game.latestReport()==report && game.profiles().profile().obols()==money
                && game.shared().progress().souls()==souls,"Narrative reading leaves the finalized clock, report and currencies unchanged");
            press(ending,Input.Keys.ESCAPE);
            check(game.getScreen() instanceof GameOverScreen && (boolean)field(ending,"disposed")
                && !game.profiles().profile().has(CampaignProfile.Event.UNDERWORLD_EPILOGUE_READ),"Deferring releases the story without claiming it was read");
            game.getScreen().render(.5f);press(game.getScreen(),Input.Keys.C);
            check(game.getScreen() instanceof UnderworldEndingScreen,"The result can reopen a deferred epilogue");
            store.fail=true;finishReading(game);
            check(game.getScreen() instanceof GameOverScreen && game.profiles().profile().has(CampaignProfile.Event.UNDERWORLD_EPILOGUE_READ)
                && !store.saved.has(CampaignProfile.Event.UNDERWORLD_EPILOGUE_READ) && !game.profiles().error().isEmpty(),
                "Failed autosave retains the completed reading in memory and still allows leaving");
            check(game.profiles().profile().obols()==money && game.shared().progress().souls()==souls
                && game.profiles().profile().victories()==victories && game.latestReport()==report,"Completing the story cannot settle a victory twice");
            game.getScreen().render(.5f);press(game.getScreen(),Input.Keys.ENTER);store.fail=false;press(game.getScreen(),Input.Keys.S);
            check(store.saved.has(CampaignProfile.Event.UNDERWORLD_EPILOGUE_READ),"Refuge retry makes the completed reading durable");
            int writes=store.writes;press(game.getScreen(),Input.Keys.V);
            check(game.getScreen() instanceof UnderworldEndingScreen,"The refuge provides an explicit story replay");
            finishReading(game);
            check(game.getScreen() instanceof RefugeScreen && store.writes==writes && game.profiles().profile().obols()==money,
                "Replaying an already-read story performs no write or currency transaction");
            win(game);finishVisualEnding(game);
            check(game.getScreen() instanceof GameOverScreen,"Later guardian victories skip an already-completed epilogue");
        } finally {game.dispose();}
        verifyClosing();
        System.out.println("M7_STORY_OK: settled victory, earned spark, protected input, deferred reading, replay, save retry and cleanup");
    }
    private static RunSession win(Main game) {
        RunSession run=bossSession(Profession.CACADOR);game.profiles().prepareRun(run);
        BossRoomScreen room=new BossRoomScreen(game,run);game.setScreen(room);
        try {((BossEncounter)field(room,"boss")).hurt(100000);}
        catch(Exception error){throw new AssertionError(error);}
        room.render(0);return run;
    }
    private static void finishReading(Main game) throws Exception {
        game.getScreen().render(1.6f);
        for(int i=0;i<UnderworldStory.lines().size()*2+1 && game.getScreen() instanceof UnderworldEndingScreen;i++)
            press(game.getScreen(),Input.Keys.ENTER);
        check(!(game.getScreen() instanceof UnderworldEndingScreen),"Reading every line must deliver its destination");
    }
    private static void verifyCopy(UnderworldEndingScreen screen,DialoguePanel panel) throws Exception {
        BitmapFont font=(BitmapFont)field(screen,"font");font.getData().setScale(panel.bodyScale());
        for(DialogueLine line:UnderworldStory.lines()) {
            GlyphLayout layout=new GlyphLayout(font,line.text(),Color.WHITE,panel.textWidth(),8,true);
            check(layout.height<=panel.textHeight() && layout.width<=panel.textWidth()+.1f,"Every ending line fits above the reading controls");
        }
        font.getData().setScale(.95f);
        check(new GlyphLayout(font,UnderworldStory.nextObjective(),Color.WHITE,864,8,true).height<=39,
            "The next objective fits above the development notice");
        font.getData().setScale(1);
    }
    private static void verifyClosing() throws Exception {
        Store store=new Store();Main game=new Main(new SaveSlots(store),AudioSettingsStore.memory(),new RecordedAudio());
        try {
            game.create();win(game);
            RunEndingScreen pose=(RunEndingScreen)game.getScreen();
            UnderworldEndingScreen unshown=(UnderworldEndingScreen)field(pose,"destination");
            check(field(unshown,"batch")==null,"An unshown narrative does not allocate its rendering resources or result screen");
            game.setScreen(new MenuScreen(game));
            check((boolean)field(unshown,"disposed") && !store.saved.has(CampaignProfile.Event.UNDERWORLD_EPILOGUE_READ),
                "Closing during the death pose disposes its pending story without completing it");
            win(game);finishVisualEnding(game);UnderworldEndingScreen shown=(UnderworldEndingScreen)game.getScreen();
            BitmapFont font=(BitmapFont)field(shown,"font");
            game.setScreen(new MenuScreen(game));
            check((boolean)field(shown,"disposed") && font.getRegion().getTexture().getTextureObjectHandle()==0
                && !store.saved.has(CampaignProfile.Event.UNDERWORLD_EPILOGUE_READ),"Closing a shown story frees its fonts and preserves the pending reading");
            check(game.hasUnderworldEpilogue() && !game.visuals().scenery().disposed(),"Story disposal keeps the character's spark and shared atlas alive");
        } finally {game.dispose();}
    }
    private static final class Store implements ProfileStore {
        private CampaignProfile saved=new CampaignProfile();private boolean fail;private int writes;
        Store(){saved.createCharacter(Profession.CACADOR,GameDifficulty.MEDIUM);}
        public CampaignProfile load(){return saved.copy();}
        public void save(CampaignProfile profile) throws IOException {
            if(fail)throw new IOException("M7 narrative smoke fixture");saved=profile.copy();writes++;
        }
    }
}
