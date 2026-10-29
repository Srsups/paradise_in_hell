package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.util.List;

/** A safe choice between arenas. Holds the session, never a combat screen or texture. */
public final class RouteScreen extends ScreenAdapter {
    private final Main game;
    private final RunSession session;
    private final List<String> exits;
    private final FitViewport viewport = new FitViewport(960, 540);
    private final SpriteBatch batch = new SpriteBatch();
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont font = UiTheme.bodyFont(),titleFont=UiTheme.titleFont();
    private final Rectangle[] cards;
    private final TextureRegion[] previews;
    private final SafeRunExit safeExit;
    private float inputDelay = 0.2f;
    private boolean mouseReleased;
    public static final float BODY_SCALE=.90f,TITLE_SCALE=1.12f,TEXT_GAP=11;

    public RouteScreen(Main game, RunSession session) {
        this.game = game;
        this.session = session;
        safeExit=new SafeRunExit(game,session,RunCheckpoint.Stage.ROUTE,new Rectangle(650,15,282,36));
        exits = session.arena().exits();
        if (!session.arenaComplete() || exits.isEmpty()) throw new IllegalStateException("No route available");
        cards = new Rectangle[exits.size()];
        previews=new TextureRegion[cards.length];
        float width=Math.min(400,(904f-16*(cards.length-1))/cards.length);
        float start=(960-width*cards.length-16*(cards.length-1))/2;
        for(int index=0;index<cards.length;index++) {
            cards[index]=new Rectangle(start+index*(width+16),94,width,380);
            previews[index]=SceneryVisuals.cover(game.visuals().scenery().preview(SceneryTheme.forArena(session.region().arena(exits.get(index)))),
                width-24,previewHeight());
        }
    }
    public float previewHeight(){return cards.length==1?140:104;}
    public float textTop(){return 474-previewHeight()-34;}
    public static List<String> details(RunSession session,ArenaDefinition arena) {
        String event=arena.kind()==ArenaDefinition.Kind.COMBAT
            ? "Evento: "+session.variation(arena.id()).label+"\n"+session.variation(arena.id()).description : "Chefe da região";
        String reward=arena.reward().description.isEmpty()
            ? arena.kind()==ArenaDefinition.Kind.BOSS_ROOM?"Vitória: conclui a travessia":"Recompensa da travessia" : arena.reward().description;
        return List.of(arena.title(),arena.description(),event,reward,arena.goalDescription());
    }
    @Override public void show() { Gdx.input.setInputProcessor(null); }
    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void render(float delta) {
        inputDelay = Math.max(0,inputDelay-Math.max(0,delta));
        mouseReleased|=!Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        Vector2 pointer = viewport.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        if(safeExit.input(delta,pointer))return;
        if (inputDelay <= 0) {
            for (int index = 0; index < exits.size(); index++) {
                boolean key = Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + index)
                    || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1 + index);
                if (key || mouseReleased&&Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && cards[index].contains(pointer)) {
                    game.enterNextArena(session,exits.get(index));
                    return;
                }
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                game.finishRun(session, RunOutcome.ABANDONED);
                return;
            }
        }
        ScreenUtils.clear(UiTheme.INK);
        viewport.apply();
        shapes.setProjectionMatrix(viewport.getCamera().combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for(Rectangle card:cards) {
            UiTheme.panel(shapes,card.x,card.y,card.width,card.height,card.contains(pointer));
            shapes.setColor(UiTheme.BRONZE);shapes.rect(card.x+14,card.y+13,card.width-28,30);
        }
        shapes.end();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        batch.setColor(Color.WHITE);titleFont.setColor(UiTheme.IVORY);titleFont.getData().setScale(.92f);
        var heading=new GlyphLayout(titleFont,"ESCOLHA O PRÓXIMO DESTINO");
        titleFont.draw(batch,heading,(960-heading.width)/2,520);
        font.getData().setScale(.86f);font.setColor(UiTheme.TEAL);
        font.draw(batch,"Saindo de "+session.arena().title(),28,490);
        for (int index = 0; index < exits.size(); index++) {
            ArenaDefinition arena = session.region().arena(exits.get(index));
            Rectangle card = cards[index];
            batch.draw(previews[index],card.x+12,card.y+card.height-previewHeight()-14,card.width-24,previewHeight());
            var copy=details(session,arena);float y=textTop();
            for(int section=0;section<copy.size();section++)
                y=paragraph(copy.get(section),card,y,section==0?TITLE_SCALE:BODY_SCALE,
                    section==0?UiTheme.IVORY:section==2?UiTheme.TEAL:section==3?Color.GOLD:Color.LIGHT_GRAY);
            font.getData().setScale(.95f);font.setColor(UiTheme.INK);
            String action=(index+1)+"  —  Seguir caminho";var label=new GlyphLayout(font,action);
            font.draw(batch,label,card.x+(card.width-label.width)/2,card.y+34);
        }
        font.getData().setScale(.9f);font.setColor(UiTheme.IVORY);
        font.draw(batch,"Vida: "+CombatNumbers.format(session.player().getVida())+"   |   Nível: "+session.player().getNivel()
            +"   |   Arenas concluídas: "+session.completedArenas(),28,72);
        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch,"Clique ou use 1, 2, 3. | ESC: abandonar e voltar ao refúgio",28,39,safeExit.visible()?595:904,8,true);
        font.getData().setScale(1);font.setColor(Color.WHITE);
        batch.end();
        safeExit.draw(batch,shapes,font,pointer);
    }
    private float paragraph(String text,Rectangle card,float y,float scale,Color color) {
        font.getData().setScale(scale);font.setColor(color);
        var layout=new GlyphLayout(font,text,color,card.width-28,8,true);
        font.draw(batch,layout,card.x+14,y);return y-layout.height-TEXT_GAP;
    }
    @Override public void dispose() { batch.dispose(); shapes.dispose(); font.dispose();titleFont.dispose(); }
}
