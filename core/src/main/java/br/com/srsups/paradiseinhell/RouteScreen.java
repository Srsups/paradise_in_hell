package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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
    private final BitmapFont font = UiTheme.bodyFont();
    private final Rectangle[] cards;
    private float inputDelay = 0.2f;

    public RouteScreen(Main game, RunSession session) {
        this.game = game;
        this.session = session;
        exits = session.arena().exits();
        if (!session.arenaComplete() || exits.isEmpty()) throw new IllegalStateException("No route available");
        cards = new Rectangle[exits.size()];
        float width = (890f - 20 * (exits.size() - 1)) / exits.size();
        for (int index = 0; index < cards.length; index++) cards[index] = new Rectangle(35 + index * (width + 20), 150, width, 285);
    }
    @Override public void show() { Gdx.input.setInputProcessor(null); }
    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void render(float delta) {
        inputDelay -= delta;
        Vector2 pointer = viewport.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        if (inputDelay <= 0) {
            for (int index = 0; index < exits.size(); index++) {
                boolean key = Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + index)
                    || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1 + index);
                if (key || Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && cards[index].contains(pointer)) {
                    session.enterArena(exits.get(index));
                    game.setScreen(session.arena().kind() == ArenaDefinition.Kind.BOSS_ROOM
                        ? new BossRoomScreen(game, session) : new GameScreen(game, session));
                    return;
                }
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                game.finishRun(session, RunOutcome.ABANDONED);
                return;
            }
        }
        ScreenUtils.clear(0.045f, 0.055f, 0.085f, 1);
        viewport.apply();
        shapes.setProjectionMatrix(viewport.getCamera().combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.13f, 0.16f, 0.23f, 1);
        for (Rectangle card : cards) shapes.rect(card.x, card.y, card.width, card.height);
        shapes.end();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        font.setColor(Color.GOLD);
        font.getData().setScale(1.6f);
        font.draw(batch, "ESCOLHA O PRÓXIMO DESTINO", 190, 475);
        font.getData().setScale(1);
        font.setColor(Color.WHITE);
        for (int index = 0; index < exits.size(); index++) {
            ArenaDefinition arena = session.region().arena(exits.get(index));
            Rectangle card = cards[index];
            font.draw(batch, "[" + (index + 1) + "] " + arena.title(), card.x + 18, card.y + 262, card.width - 36, 8, true);
            font.draw(batch, arena.description(), card.x + 18, card.y + 216, card.width - 36, 8, true);
            font.setColor(Color.CYAN);
            font.draw(batch, arena.kind() == ArenaDefinition.Kind.COMBAT
                ? "Evento: " + session.variation(arena.id()).label + "\n" + session.variation(arena.id()).description : "Chefe da região", card.x + 18, card.y + 148, card.width - 36, 8, true);
            font.setColor(Color.GOLD);
            font.draw(batch, arena.reward().description, card.x + 18, card.y + 112, card.width - 36, 8, true);
            font.setColor(Color.WHITE);
            font.draw(batch, arena.goalDescription(), card.x + 18, card.y + 65, card.width - 36, 8, true);
        }
        font.draw(batch, "Vida: " + CombatNumbers.format(session.player().getVida()) + "   |   Nível: " + session.player().getNivel()
            + "   |   Arenas concluídas: " + session.completedArenas(), 180, 120);
        font.draw(batch, "Clique ou use 1, 2, 3.   |   ESC: abandonar e voltar ao refúgio", 235, 65);
        batch.end();
    }
    @Override public void dispose() { batch.dispose(); shapes.dispose(); font.dispose(); }
}
