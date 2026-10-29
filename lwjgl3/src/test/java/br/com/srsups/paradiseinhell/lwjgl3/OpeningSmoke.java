package br.com.srsups.paradiseinhell.lwjgl3;

import br.com.srsups.paradiseinhell.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static br.com.srsups.paradiseinhell.lwjgl3.DemoSmokeTest.*;

/** Real layered rendering, lifecycle and input; all profiles, audio and video preferences stay in memory. */
public final class OpeningSmoke {
    private OpeningSmoke() {}
    private static final class Store implements ProfileStore {
        CampaignProfile saved=new CampaignProfile();
        int writes;
        boolean fail;
        @Override public CampaignProfile load(){return saved.copy();}
        @Override public void save(CampaignProfile profile)throws IOException {
            writes++;
            if(fail)throw new IOException("opening fixture save failure");
            saved=profile.copy();
        }
    }
    private static final class Fixture implements AutoCloseable {
        final Store store=new Store();
        final RecordedAudio audio=new RecordedAudio();
        final Main game=new Main(new SaveSlots(store),AudioSettingsStore.memory(),audio);
        Fixture(){game.create();}
        void character(){check(game.createCharacter(Profession.CACADOR,GameDifficulty.MEDIUM),"Opening fixture creates a real character");}
        OpeningScreen open(boolean record,AtomicInteger destinations) {
            OpeningScreen screen=new OpeningScreen(game,game.profiles().profile().characterId(),record,()->{
                destinations.incrementAndGet();return record?new RefugeScreen(game):new MenuScreen(game);
            });
            game.setScreen(screen);return screen;
        }
        @Override public void close(){game.dispose();}
    }
    public static void verify()throws Exception {
        verifyShotsAssetsAndControls();
        verifyNaturalCompletion();
        verifySkipAndDisposal();
        verifyCampaignEntryAndReplay();
        verifyMenuPreviewAndSaveFailure();
        verifyMutedAudio();
    }
    private static void verifyShotsAssetsAndControls()throws Exception {
        try(Fixture fixture=new Fixture()) {
            fixture.character();AtomicInteger destinations=new AtomicInteger();OpeningScreen screen=fixture.open(false,destinations);
            OpeningArt art=(OpeningArt)field(screen,"art");
            verifyAssets(art);verifyCopy(screen);verifyLayerGeometry(screen,art);
            int writes=fixture.store.writes;input(screen,0,Input.Keys.ENTER,false,false,null);
            check(screen.playback().current().id()==OpeningStory.ShotId.HOME && destinations.get()==0,
                "Fresh entry input cannot skip the opening's minimum subtitle reading time");
            advance(screen,2.2f);
            captureNpcDialogue(screen,"m7-opening-home");long first=frameHash();
            advance(screen,.6f);
            check(frameHash()!=first,"Layer interpolation and breathing visibly animate the opening without swapping a static poster");
            input(screen,0,Input.Keys.SPACE,false,false,null);float paused=screen.playback().elapsedTotal();long still=frameHash();
            int sounds=fixture.audio.played.size();
            for(int frame=0;frame<8;frame++)input(screen,.1f,0,false,false,null);
            check(screen.playback().paused() && screen.playback().elapsedTotal()==paused && frameHash()==still && fixture.audio.played.size()==sounds,
                "Space pauses the exact layer pose, timeline and sound queue");
            input(screen,0,Input.Keys.ENTER,false,false,null);
            check(screen.playback().current().id()==OpeningStory.ShotId.HOME,"Manual next cannot silently unpause the story");
            input(screen,0,Input.Keys.F2,true,true,null);VideoPanel video=(VideoPanel)field(screen,"videoPanel");
            input(screen,2,Input.Keys.ENTER,true,true,null);
            check(video.open() && screen.playback().elapsedTotal()==paused && fixture.audio.played.size()==sounds,
                "Opening video controls consume held clicks and freeze the story and cues");
            captureNpcDialogue(screen,"m7-opening-video");input(screen,0,Input.Keys.F2,true,true,null);
            check(!video.open() && screen.playback().paused() && screen.playback().elapsedTotal()==paused,
                "Closing F2 consumes the frame and preserves a pre-existing story pause");
            input(screen,0,Input.Keys.SPACE,false,false,null);screen.pause();
            check(screen.playback().paused(),"Application pause also freezes the opening timeline");
            input(screen,0,Input.Keys.SPACE,false,false,null);

            float[] captureTimes={2.2f,2.4f,4f,3f,2f,6.5f};
            for(int index=1;index<OpeningStory.shots().size();index++) {
                advanceLocalTo(screen,1.6f);input(screen,0,Input.Keys.ENTER,false,false,null);
                check(screen.playback().shotIndex()==index,"A fresh Enter advances exactly one eligible shot");
                advanceLocalTo(screen,captureTimes[index]);
                verifyLayerGeometry(screen,art);
                check(screen.playback().subtitle()!=null && !screen.playback().text().isBlank(),"The current timed subtitle appears in its scene");
                captureNpcDialogue(screen,"m7-opening-"+screen.playback().current().id().name().toLowerCase(java.util.Locale.ROOT));
                int afterCue=fixture.audio.played.size();screen.render(0);screen.resize(800,600);screen.render(0);screen.resize(960,540);screen.render(0);
                check(fixture.audio.played.size()==afterCue,"Drawing and resizing cannot replay a shot's sound cue");
            }
            check(destinations.get()==0 && fixture.store.writes==writes,"Watching and resizing a preview never constructs the destination or writes a profile");
            input(screen,0,Input.Keys.ENTER,false,false,null);
            check(fixture.game.getScreen() instanceof MenuScreen && destinations.get()==1 && art.disposed(),
                "Finishing a preview creates its destination lazily and releases the screen-owned atlas");
            check(fixture.store.writes==writes && fixture.game.profiles().profile().openingPending(),
                "A menu-style preview does not claim the character's pending introduction");
        }
    }
    private static void verifyNaturalCompletion()throws Exception {
        try(Fixture fixture=new Fixture()) {
            fixture.character();AtomicInteger destinations=new AtomicInteger();OpeningScreen screen=fixture.open(true,destinations);
            OpeningArt art=(OpeningArt)field(screen,"art");CampaignProfile before=fixture.game.profiles().profile().copy();
            int writes=fixture.store.writes;
            for(int frame=0;frame<600 && fixture.game.getScreen()==screen;frame++)input(screen,.1f,0,false,false,null);
            check(fixture.game.getScreen() instanceof RefugeScreen && destinations.get()==1 && screen.playback().finished()
                && !screen.playback().skipped() && Math.abs(screen.playback().elapsedTotal()-45)<.001f,
                "All six shots naturally complete the 45-second opening and enter the refuge once");
            check(fixture.game.profiles().profile().openingState()==CampaignProfile.CinematicState.COMPLETED && fixture.store.writes==writes+1,
                "Only actual completion persists the character's completed opening ledger");
            List<SoundCue> expected=OpeningStory.shots().stream().flatMap(shot->shot.cues().stream()).map(OpeningStory.Cue::sound).toList();
            check(fixture.audio.played.equals(expected),"The real screen emits every scripted sound once in timeline order");
            verifyNoProgressRewards(fixture.game,before);
            check(art.disposed() && (boolean)field(screen,"disposed"),"Natural delivery disposes the opening atlas and renderer");
            screen.render(1);screen.dispose();
            check(destinations.get()==1 && fixture.store.writes==writes+1,"Repeated callbacks after disposal cannot redeliver or write completion again");
        }
    }
    private static void verifySkipAndDisposal()throws Exception {
        try(Fixture fixture=new Fixture()) {
            fixture.character();AtomicInteger destinations=new AtomicInteger();OpeningScreen screen=fixture.open(true,destinations);
            int writes=fixture.store.writes;advance(screen,.5f);float at=screen.playback().elapsedTotal();
            input(screen,0,Input.Keys.ESCAPE,false,false,null);
            check(screen.confirmingSkip() && fixture.game.profiles().profile().openingPending(),"ESC asks before skipping instead of immediately claiming the opening");
            input(screen,.3f,Input.Keys.ENTER,true,true,screen.confirmSkipBounds());
            check(screen.confirmingSkip() && screen.playback().elapsedTotal()==at && destinations.get()==0 && fixture.store.writes==writes,
                "Skip confirmation freezes time and rejects held attack, a click and Enter before mouse release");
            input(screen,0,Input.Keys.ESCAPE,false,false,null);
            check(!screen.confirmingSkip() && screen.playback().elapsedTotal()==at && destinations.get()==0,
                "Cancelling the skip restores the same shot without advancing or acknowledging it");
            OpeningArt first=(OpeningArt)field(screen,"art");fixture.game.setScreen(new MenuScreen(fixture.game));
            check(first.disposed() && destinations.get()==0 && fixture.game.profiles().profile().openingPending() && fixture.store.writes==writes,
                "Replacing or disposing an unfinished opening releases its atlas without marking or invoking its destination");

            screen=fixture.open(true,destinations);CampaignProfile before=fixture.game.profiles().profile().copy();
            input(screen,0,Input.Keys.ESCAPE,false,false,null);input(screen,.3f,0,false,false,null);
            input(screen,0,0,true,true,screen.confirmSkipBounds());
            check(fixture.game.getScreen() instanceof RefugeScreen && destinations.get()==1
                && fixture.game.profiles().profile().openingState()==CampaignProfile.CinematicState.SKIPPED,
                "An explicitly released skip-button click acknowledges skipped state and creates its destination once");
            verifyNoProgressRewards(fixture.game,before);
        }
    }
    private static void verifyMenuPreviewAndSaveFailure()throws Exception {
        try(Fixture fixture=new Fixture()) {
            press(fixture.game.getScreen(),Input.Keys.I);
            check(fixture.game.getScreen() instanceof OpeningScreen,"The actual menu I shortcut opens the introduction without a character");
            OpeningScreen screen=(OpeningScreen)fixture.game.getScreen();int writes=fixture.store.writes;
            input(screen,0,Input.Keys.ESCAPE,false,false,null);input(screen,.3f,0,false,false,null);input(screen,0,Input.Keys.ENTER,false,false,null);
            check(fixture.game.getScreen() instanceof MenuScreen && fixture.store.writes==writes
                && !fixture.game.profiles().profile().professionChosen(),"Menu introduction preview works without creating a character or writing a save");
        }
        try(Fixture fixture=new Fixture()) {
            fixture.character();AtomicInteger destinations=new AtomicInteger();OpeningScreen screen=fixture.open(true,destinations);
            fixture.store.fail=true;input(screen,0,Input.Keys.ESCAPE,false,false,null);input(screen,.3f,0,false,false,null);input(screen,0,Input.Keys.ENTER,false,false,null);
            check(destinations.get()==1 && fixture.game.getScreen() instanceof RefugeScreen
                && fixture.game.profiles().profile().openingState()==CampaignProfile.CinematicState.SKIPPED && !fixture.game.profiles().error().isEmpty(),
                "A failed acknowledgement autosave preserves the fact in memory without trapping the player in the opening");
            check(new ProfileService(fixture.store).profile().openingPending(),"A failed opening save leaves the last disk snapshot unchanged");
            fixture.store.fail=false;check(fixture.game.profiles().retrySave(),"The existing profile retry saves an acknowledged opening later");
            check(new ProfileService(fixture.store).profile().openingState()==CampaignProfile.CinematicState.SKIPPED,
                "Opening acknowledgement survives a successful retry and profile reload");
        }
    }
    private static void verifyCampaignEntryAndReplay()throws Exception {
        try(Fixture fixture=new Fixture()) {
            fixture.character();Main game=fixture.game;int writes=fixture.store.writes;
            game.selectSave(0,true);
            check(game.getScreen() instanceof GameScreen && game.profiles().profile().openingPending() && fixture.store.writes==writes,
                "Selecting a new character for the timed demo does not force or acknowledge the campaign introduction");
            game.selectSave(0,false);
            check(game.getScreen() instanceof OpeningScreen,"An unseen character automatically receives the introduction when entering the campaign");
            OpeningScreen opening=(OpeningScreen)game.getScreen();
            input(opening,0,Input.Keys.ESCAPE,false,false,null);input(opening,.3f,0,false,false,null);input(opening,0,Input.Keys.ENTER,false,false,null);
            check(game.getScreen() instanceof RefugeScreen && !game.profiles().profile().openingPending(),
                "Acknowledging the first campaign introduction enters the refuge and clears only its pending state");
            game.selectSave(0,false);
            check(game.getScreen() instanceof RefugeScreen,"Returning to an acknowledged character does not force the introduction again");
            writes=fixture.store.writes;CampaignProfile before=game.profiles().profile().copy();press(game.getScreen(),Input.Keys.I);
            check(game.getScreen() instanceof OpeningScreen && !(boolean)field(game.getScreen(),"recordCompletion"),
                "The refuge I shortcut replays the story with recording disabled");
            opening=(OpeningScreen)game.getScreen();input(opening,0,Input.Keys.ESCAPE,false,false,null);
            input(opening,.3f,0,false,false,null);input(opening,0,Input.Keys.ENTER,false,false,null);
            check(game.getScreen() instanceof RefugeScreen && fixture.store.writes==writes && game.profiles().profile().openingState()==before.openingState(),
                "Refuge replay returns safely without changing the existing cinematic ledger or autosaving it again");
            verifyNoProgressRewards(game,before);
        }
    }
    private static void verifyMutedAudio()throws Exception {
        try(Fixture fixture=new Fixture()) {
            AtomicInteger destinations=new AtomicInteger();OpeningScreen screen=fixture.open(false,destinations);
            fixture.game.audioSettings().change(new AudioSettings(.75f,.3f,true));fixture.game.audio().refreshVolume();
            advance(screen,8);
            check(screen.playback().current().id()==OpeningStory.ShotId.ARRIVAL && fixture.audio.played.isEmpty()
                && screen.playback().subtitle()!=null,"Muting existing audio preferences preserves subtitles and timeline without playing cinematic effects");
            fixture.game.audioSettings().change(new AudioSettings(.5f,.3f,false));fixture.game.audio().refreshVolume();
            advance(screen,8);
            check(!fixture.audio.played.isEmpty() && fixture.audio.effectsGain>0 && fixture.audio.effectsGain<=.5f,
                "Unmuted cinematic cues honor the existing effects volume instead of bypassing the mixer");
        }
    }
    private static void verifyNoProgressRewards(Main game,CampaignProfile before) {
        CampaignProfile after=game.profiles().profile();
        check(after.obols()==before.obols() && after.attempts()==before.attempts() && after.deaths()==before.deaths()
            && after.victories()==before.victories() && after.forgeMaterials()==before.forgeMaterials()
            && after.weaponRarity()==before.weaponRarity() && after.farmerQuest()==before.farmerQuest(),
            "The opening never changes currency, attempts, deaths, victories, forge state or the farmer mission");
        check(game.shared().progress().souls()==0 && game.latestReport()==null,"The opening creates neither shared Souls nor a run report");
    }
    private static void verifyCopy(OpeningScreen screen)throws Exception {
        BitmapFont font=(BitmapFont)field(screen,"font"),title=(BitmapFont)field(screen,"titleFont");Rectangle bounds=screen.subtitleBounds();
        float scaleX=font.getData().scaleX,scaleY=font.getData().scaleY,titleX=title.getData().scaleX,titleY=title.getData().scaleY;
        try {
            for(var shot:OpeningStory.shots())for(var subtitle:shot.subtitles()) {
                font.getData().setScale(OpeningScreen.SUBTITLE_SCALE);
                GlyphLayout text=new GlyphLayout(font,subtitle.text(),Color.WHITE,bounds.width,Align.left,true);
                check(text.width<=bounds.width+.1f && text.height<=bounds.height,"Every full timed subtitle fits within two visible lines: "+shot.id());
                title.getData().setScale(.65f);check(new GlyphLayout(title,subtitle.speaker()).width<=bounds.width,"Each cinematic speaker fits the caption panel");
            }
            font.getData().setScale(.9f);
            check(new GlyphLayout(font,"CONTINUAR [ESPAÇO]").width<=screen.pauseBounds().width-25,"Pause/resume text fits its always-visible button");
            font.getData().setScale(1);
            check(new GlyphLayout(font,"PULAR ABERTURA [ENTER]").width<=screen.confirmSkipBounds().width-40,"Explicit skip text fits its confirmation target");
        } finally {font.getData().setScale(scaleX,scaleY);title.getData().setScale(titleX,titleY);}
    }
    private static void verifyAssets(OpeningArt art) {
        List<String> names=List.of("background-home","background-styx","foreground-home","foreground-styx","hero-idle","hero-reach",
            "wife-idle","wife-resist","zeus-idle","zeus-cast","charon","portal-0","portal-1","portal-2","portal-3");
        Texture texture=art.region(names.getFirst()).getTexture();var data=texture.getTextureData();
        if(!data.isPrepared())data.prepare();Pixmap pixels=data.consumePixmap();
        try {
            for(String name:names) {
                TextureRegion region=art.region(name);check(region.getTexture()==texture,"All opening layers share a single owned atlas page");
                int expectedWidth=name.startsWith("background-")||name.startsWith("foreground-")?960:name.startsWith("portal-")?256:432;
                int expectedHeight=expectedWidth==960?540:expectedWidth==432?480:256;
                check(region.getRegionWidth()==expectedWidth && region.getRegionHeight()==expectedHeight,"Opening layer keeps its authored dimensions: "+name);
                int opaque=0,transparent=0;
                for(int y=0;y<region.getRegionHeight();y+=8)for(int x=0;x<region.getRegionWidth();x+=8) {
                    int alpha=pixels.getPixel(region.getRegionX()+x,region.getRegionY()+y)&255;
                    if(alpha>64)opaque++;if(alpha<32)transparent++;
                }
                check(opaque>10,"Every opening region has visible artwork: "+name);
                if(!name.startsWith("background-"))check(transparent>10,"Actors, foreground and portal retain transparent cutout surroundings: "+name);
            }
        } finally {if(data.disposePixmap())pixels.dispose();}
    }
    private static void verifyLayerGeometry(OpeningScreen screen,OpeningArt art) {
        float progress=com.badlogic.gdx.math.Interpolation.smooth.apply(screen.playback().shotProgress());
        var layers=screen.playback().current().layers();
        for(int index=0;index<layers.size();index++) {
            var layer=layers.get(index);Rectangle bounds=screen.layerBounds(index);
            float x=layer.x()+(layer.endX()-layer.x())*progress,y=layer.y()+(layer.endY()-layer.y())*progress;
            boolean actor=!layer.asset().startsWith("background-")&&!layer.asset().startsWith("foreground-")&&!layer.asset().equals("portal");
            if(actor) {
                TextureRegion region=art.region(layer.asset());
                float expectedWidth=layer.height()*region.getRegionWidth()/region.getRegionHeight();
                check(Math.abs(bounds.width-expectedWidth)<.001f,"Actor placement uses its actual 432-by-480 canvas ratio: "+layer.asset());
                check(Math.abs(bounds.x+bounds.width/2-x-layer.width()/2)<.001f,
                    "A wider actor canvas preserves the authored foot-center instead of shifting the body: "+layer.asset());
            } else check(Math.abs(bounds.x-x)<.001f && bounds.width==layer.width(),"Background, foreground and portal retain the authored shot framing");
            check(Math.abs(bounds.y-y)<.001f && Math.abs(bounds.height-layer.height())<=layer.breath()+.001f,
                "Breathing keeps the lower anchor fixed and stays within its authored pixel amplitude");
        }
    }
    private static void advanceLocalTo(OpeningScreen screen,float target)throws Exception {
        int guard=0;while(screen.playback().elapsedInShot()<target && !screen.playback().finished() && guard++<300)input(screen,.1f,0,false,false,null);
        check(guard<300,"Local cinematic fixture reaches its target while the timeline runs");
    }
    private static void advance(OpeningScreen screen,float seconds)throws Exception {
        float target=screen.playback().elapsedTotal()+seconds;int guard=0;
        while(screen.playback().elapsedTotal()<target && !screen.playback().finished() && guard++<700)input(screen,.1f,0,false,false,null);
        check(guard<700,"Cinematic fixture advances with the renderer's bounded UI clock");
    }
    private static void input(OpeningScreen screen,float delta,int key,boolean held,boolean clicked,Rectangle target)throws Exception {
        FitViewport viewport=(FitViewport)field(screen,"viewport");
        Vector2 point=viewport.project(new Vector2(target==null?50:target.x+target.width/2,target==null?250:target.y+target.height/2));
        Input original=Gdx.input;
        Gdx.input=(Input)Proxy.newProxyInstance(Input.class.getClassLoader(),new Class<?>[]{Input.class},(proxy,method,args)->switch(method.getName()) {
            case "isKeyJustPressed" -> key!=0 && (int)args[0]==key;
            case "isButtonPressed" -> held && (int)args[0]==Input.Buttons.LEFT;
            case "isButtonJustPressed" -> clicked && (int)args[0]==Input.Buttons.LEFT;
            case "getX" -> Math.round(point.x);
            case "getY" -> Gdx.graphics.getHeight()-Math.round(point.y);
            default -> method.invoke(original,args);
        });
        try {((Main)field(screen,"game")).audio().advance(Math.max(0,delta));screen.render(delta);}finally {Gdx.input=original;}
    }
    private static long frameHash() {
        Pixmap pixels=Pixmap.createFromFrameBuffer(0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
        try {
            long hash=0xcbf29ce484222325L;
            for(int y=0;y<pixels.getHeight();y+=5)for(int x=0;x<pixels.getWidth();x+=5)hash=(hash^pixels.getPixel(x,y))*0x100000001b3L;
            return hash;
        } finally {pixels.dispose();}
    }
}
