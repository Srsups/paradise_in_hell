package br.com.srsups.paradiseinhell.lwjgl3;

import br.com.srsups.paradiseinhell.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import static br.com.srsups.paradiseinhell.lwjgl3.DemoSmokeTest.*;

/** Live spawn pacing and authored flight sprites, with all game progress and audio kept in memory. */
public final class HordeSmoke {
    private HordeSmoke() {}
    private static final Method UPDATE;
    static {
        try {UPDATE=GameScreen.class.getDeclaredMethod("updatePlaying",float.class);UPDATE.setAccessible(true);}
        catch(ReflectiveOperationException error){throw new ExceptionInInitializerError(error);}
    }
    private static final class Fixture implements AutoCloseable {
        final Main game=new Main(SaveSlots.memory(),AudioSettingsStore.memory(),new RecordedAudio());
        final RunSession run;
        final GameScreen room;
        final List<Inimigo> enemies;
        final HordeSchedule hordes;
        final FitViewport world;
        final Input actual=Gdx.input;
        @SuppressWarnings("unchecked") Fixture(boolean demo)throws Exception {
            game.create();check(game.createCharacter(Profession.CACADOR,GameDifficulty.MEDIUM),"The horde fixture creates a character in memory");
            String character=game.profiles().profile().characterId();game.profiles().markOpening(character,true);
            game.profiles().setTutorialEnabled(character,false);
            run=demo?RunSession.demo(Profession.CACADOR):RunSession.campaign(game.campaignRegion(),Profession.CACADOR);
            room=new GameScreen(game,run);game.setScreen(room);
            enemies=(List<Inimigo>)field(room,"enemies");hordes=(HordeSchedule)field(room,"hordes");world=(FitViewport)field(room,"world");
            Jogador actor=(Jogador)field(room,"player");((TileMap)field(room,"map")).gerarAreaInicialSegura(actor.x,actor.y,60);
            // Install the quiet input only after construction succeeds, so an asset-load failure cannot leak a proxy.
            Gdx.input=(Input)Proxy.newProxyInstance(Input.class.getClassLoader(),new Class<?>[]{Input.class},(proxy,method,args)->switch(method.getName()) {
                case "isKeyJustPressed","isKeyPressed","isButtonPressed","isButtonJustPressed" -> false;
                default -> method.invoke(actual,args);
            });
        }
        void tick(float delta)throws Exception {
            // A valid remaining protection window isolates pacing from a deliberately stationary test actor.
            set(run.player(),"protecaoRessurreicao",2f);UPDATE.invoke(room,delta);
            check(game.getScreen()==room,"The pacing fixture stays on its live combat screen");
            check(enemies.stream().filter(enemy->enemy.getType()==EnemyType.FAST&&!enemy.estaMorto()).count()<=EnemyWaves.MAX_FAST,
                "Ordinary waves and hordes share the simultaneous fast-enemy limit");
        }
        void until(float target)throws Exception {
            for(int frame=0;run.progress().elapsed()<target-.0001f;frame++) {
                if(frame>20_000)throw new AssertionError("The live room did not reach its horde time");
                tick(Math.min(.05f,target-run.progress().elapsed()));
            }
        }
        @Override public void close(){try{game.dispose();}finally{Gdx.input=actual;}}
    }

    public static void verify()throws Exception {
        verifyAuthoredFlight();
        verifyQuietWindowsAndFormation();
        verifyDemoAndContinuousSpawning();
        System.out.println("HORDE_SMOKE_OK: authored bat flight, quiet warning/recovery, homogeneous offscreen formations, frozen overlays, shared fast caps and continued spawning");
    }

    private static void verifyAuthoredFlight()throws Exception {
        try(Fixture fixture=new Fixture(false)) {
            FlyerArt art=fixture.game.visuals().flyer();var pixels=new IdentityHashMap<Texture,Pixmap>();
            try {
                for(ActorAnimation.Direction direction:ActorAnimation.Direction.values()) {
                    Set<Long> hashes=new java.util.HashSet<>();
                    for(int frame=0;frame<FlyerArt.FLIGHT_FRAMES;frame++) {
                        TextureRegion region=art.frame(direction,frame);Pixmap source=pixels.get(region.getTexture());
                        if(source==null){var data=region.getTexture().getTextureData();if(!data.isPrepared())data.prepare();source=data.consumePixmap();pixels.put(region.getTexture(),source);}
                        check(region.getRegionWidth()==64&&region.getRegionHeight()==64,"Bat flight keeps the authored sprite grid");
                        check(region.isFlipX()==direction.mirrored(),"Right-facing bats mirror their authored left views");
                        long hash=1;int visible=0;
                        for(int y=0;y<64;y++)for(int x=0;x<64;x++) {
                            int left=Math.round(Math.min(region.getU(),region.getU2())*source.getWidth());
                            int top=Math.round(Math.min(region.getV(),region.getV2())*source.getHeight());
                            int color=source.getPixel(left+(region.isFlipX()?63-x:x),top+(region.isFlipY()?63-y:y));hash=hash*31+color;
                            if((color&255)>=96)visible++;
                        }
                        check(visible>30,"Every wingbeat has a visible body and wings");hashes.add(hash);
                    }
                    check(hashes.size()>3,"Flight is an authored wingbeat sequence instead of a static tinted enemy");
                }
            } finally {for(var entry:pixels.entrySet())if(entry.getKey().getTextureData().disposePixmap())entry.getValue().dispose();}
            FlightCatalog catalog=new FlightCatalog(art);fixture.game.setScreen(catalog);captureNpcDialogue(catalog,"m7-flyer-flight");
        }
    }

    private static void verifyQuietWindowsAndFormation()throws Exception {
        try(Fixture fixture=new Fixture(false)) {
            fixture.until(32);
            check(fixture.hordes.phase()==HordeSchedule.Phase.WARNING&&fixture.hordes.kind()==HordeSchedule.Kind.FLYER,
                "The first campaign horde announces its distinct flying role three seconds ahead");
            int quietCount=fixture.enemies.size();
            check((float)field(fixture.room,"spawnTimer")==0,"The warning discards the ordinary spawn timer");
            verifyFrozenOverlays(fixture);captureNpcDialogue(fixture.room,"m7-horde-warning");
            fixture.until(34.99f);
            check(fixture.enemies.size()==quietCount&&fixture.hordes.phase()==HordeSchedule.Phase.WARNING,
                "No ordinary enemies spawn during the pre-horde quiet window");
            Set<Inimigo> known=Collections.newSetFromMap(new IdentityHashMap<>());known.addAll(fixture.enemies);
            List<Vector2> origins=new ArrayList<>();List<Inimigo> group=new ArrayList<>();
            while(fixture.run.arenaElapsed()<39.75f-.0001f) {
                fixture.tick(Math.min(.025f,39.75f-fixture.run.arenaElapsed()));
                for(Inimigo enemy:fixture.enemies)if(known.add(enemy)) {
                    check(enemy.isFlyer()&&!enemy.isArcher()&&enemy.getType()==EnemyType.COMMON,
                        "The horde emits one enemy role and keeps the original three-goal identity");
                    check(enemy.getXpDrop()==StygianFlyer.XP&&enemy.getSoulDrop()==StygianFlyer.SOULS,
                        "The new enemy supplies its own XP and Soul amounts");
                    check(!VisibleArea.sprite(fixture.world,enemy.hitbox()),"A grouped unit appears outside the visible camera");
                    origins.add(new Vector2(enemy.x,enemy.y));group.add(enemy);
                }
            }
            check(group.size()==HordeSchedule.Kind.FLYER.size(),"The full flying band arrives over the five-second horde interval");
            float minX=Float.POSITIVE_INFINITY,maxX=Float.NEGATIVE_INFINITY,minY=minX,maxY=maxX;
            for(Vector2 point:origins){minX=Math.min(minX,point.x);maxX=Math.max(maxX,point.x);minY=Math.min(minY,point.y);maxY=Math.max(maxY,point.y);}
            check(Math.min(maxX-minX,maxY-minY)<=33&&Math.max(maxX-minX,maxY-minY)<=91,
                "One horde retains a compact approach band rather than scattering over every map edge");
            check(field(fixture.room,"hordeFormation") instanceof EnemySpawner.GroupFormation,"The real screen retains the formation across its batches");
            // Show the approaching flying band while the live horde caption is still active.
            fixture.room.render(0);captureNpcDialogue(fixture.room,"m7-horde-flyers");
            fixture.until(40);
            int recoveryCount=fixture.enemies.size();
            check(fixture.hordes.phase()==HordeSchedule.Phase.RECOVERY,"The horde enters its post-spawn respite");
            fixture.until(43.99f);
            check(fixture.enemies.size()==recoveryCount&&(float)field(fixture.room,"spawnTimer")==0,
                "Recovery stops ordinary spawns and does not accumulate a delayed regular wave");
            fixture.until(44.04f);
            check(fixture.hordes.phase()==HordeSchedule.Phase.NORMAL&&fixture.enemies.size()==recoveryCount
                &&(float)field(fixture.room,"spawnTimer")<=.051f,"Normal pacing resumes with a fresh timer and no backlog burst");
        }
    }

    private static void verifyFrozenOverlays(Fixture fixture)throws Exception {
        for(int key:new int[]{Input.Keys.ESCAPE,Input.Keys.TAB,Input.Keys.F2}) {
            float time=fixture.run.arenaElapsed(),remaining=fixture.hordes.secondsRemaining();int count=fixture.enemies.size();
            press(fixture.room,key);fixture.room.render(1);
            check(fixture.run.arenaElapsed()==time&&fixture.hordes.secondsRemaining()==remaining&&fixture.enemies.size()==count,
                "Pause, boon list and video all freeze horde timing and batches");
            press(fixture.room,key);
            if(key==Input.Keys.ESCAPE)check(fixture.run.arenaElapsed()>time,"Unpausing resumes ordinary combat time");
            else check(fixture.run.arenaElapsed()==time,"Closing an overlay consumes its input frame");
        }
    }

    private static void verifyDemoAndContinuousSpawning()throws Exception {
        try(Fixture fixture=new Fixture(true)) {
            fixture.until(35);
            check(fixture.hordes.phase()==HordeSchedule.Phase.HORDE&&fixture.hordes.kind()==HordeSchedule.Kind.COMMON,
                "The demo's first horde preserves its beginner common-enemy introduction");
            int total=fixture.enemies.size();
            for(Inimigo enemy:fixture.enemies)enemy.dispose();fixture.enemies.clear();
            while(fixture.run.progress().elapsed()<180) {
                fixture.tick(.05f);total+=fixture.enemies.size();
                for(Inimigo enemy:fixture.enemies)enemy.dispose();fixture.enemies.clear();
            }
            check(total>160&&fixture.game.getScreen()==fixture.room,"The simultaneous workload cap does not limit the total enemies generated in a run");
            check(fixture.run.progress().kills()==0,"Removing test entities to measure supply does not invent gameplay kills or rewards");
        }
    }

    private static final class FlightCatalog extends ScreenAdapter {
        private final FlyerArt art;
        private final FitViewport viewport=new FitViewport(960,540);
        private final SpriteBatch batch=new SpriteBatch();
        private final BitmapFont font=UiTheme.bodyFont();
        FlightCatalog(FlyerArt art){this.art=art;}
        @Override public void resize(int w,int h){viewport.update(w,h,true);}
        @Override public void render(float delta) {
            ScreenUtils.clear(UiTheme.INK);viewport.apply();batch.setProjectionMatrix(viewport.getCamera().combined);batch.begin();
            font.setColor(UiTheme.IVORY);font.getData().setScale(1.2f);font.draw(batch,"MORCEGO DO ESTIGE — BATIDAS DE ASAS",35,515);
            font.getData().setScale(.9f);font.setColor(Color.LIGHT_GRAY);
            for(ActorAnimation.Direction direction:ActorAnimation.Direction.values()) {
                float y=445-direction.ordinal()*53;font.draw(batch,direction.key(),35,y+24);
                for(int frame=0;frame<FlyerArt.FLIGHT_FRAMES;frame++)batch.draw(art.frame(direction,frame),190+frame*120,y,53,53);
            }
            batch.end();
        }
        @Override public void dispose(){batch.dispose();font.dispose();}
    }
}
