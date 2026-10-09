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

/** Select or create an independent character without replacing an existing save. */
public final class SaveSelectionScreen extends ScreenAdapter {
    private final Main game;
    private final boolean demo, fromRefuge;
    private final FitViewport viewport = new FitViewport(960, 540);
    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font = new BitmapFont();
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final Rectangle[] cards = {
        new Rectangle(35, 270, 435, 150), new Rectangle(490, 270, 435, 150),
        new Rectangle(35, 110, 435, 150), new Rectangle(490, 110, 435, 150)
    };
    public SaveSelectionScreen(Main game, boolean demo) { this(game, demo, false); }
    public SaveSelectionScreen(Main game, boolean demo, boolean fromRefuge) {
        this.game = game; this.demo = demo; this.fromRefuge = fromRefuge;
    }
    @Override public void show() { Gdx.input.setInputProcessor(null); }
    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void render(float delta) {
        Vector2 pointer = viewport.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        for (int index = 0; index < cards.length; index++) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + index) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1 + index)
                || Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && cards[index].contains(pointer)) {
                game.selectSave(index, demo); return;
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(fromRefuge ? new RefugeScreen(game) : new MenuScreen(game)); return;
        }
        ScreenUtils.clear(0.045f, 0.055f, 0.085f, 1); viewport.apply();
        shapes.setProjectionMatrix(viewport.getCamera().combined); shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.13f, 0.16f, 0.23f, 1);
        for (Rectangle card : cards) shapes.rect(card.x, card.y, card.width, card.height);
        shapes.end();
        batch.setProjectionMatrix(viewport.getCamera().combined); batch.begin();
        font.setColor(Color.GOLD); font.getData().setScale(1.8f); font.draw(batch, "ESCOLHA SEU SAVE", 310, 490);
        font.setColor(Color.WHITE); font.getData().setScale(1);
        for (int index = 0; index < cards.length; index++) {
            CampaignProfile profile = game.profileAt(index).profile(); Rectangle card = cards[index];
            font.setColor(Color.GOLD); font.draw(batch, "[" + (index + 1) + "] Save " + (index + 1), card.x + 18, card.y + 130);
            font.setColor(Color.WHITE);
            if (profile.professionChosen()) {
                font.draw(batch, profile.profession().title + " | " + profile.profession().weapon.label, card.x + 18, card.y + 105);
                font.draw(batch, "Óbolos: " + profile.obols() + " | Fragmentos: " + profile.forgeMaterials(), card.x + 18, card.y + 80);
                font.draw(batch, "Retornos: " + profile.attempts() + " | Vitórias: " + profile.victories(), card.x + 18, card.y + 58);
                font.setColor(Color.LIGHT_GRAY); font.draw(batch, "Continuar com este personagem", card.x + 18, card.y + 28);
            } else {
                font.draw(batch, "Novo personagem", card.x + 18, card.y + 95);
                font.draw(batch, "Escolha a profissão uma vez.\nSaldo e progresso são próprios deste personagem.", card.x + 18, card.y + 65);
            }
            if (!game.profileAt(index).error().isEmpty()) {
                font.setColor(Color.SALMON); font.draw(batch, "Perfil com erro de gravação/leitura", card.x + 12, card.y + 12);
            }
        }
        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "Profissão fixa por save. Para jogar com outra, use um slot vazio.", 210, 75);
        font.draw(batch, "Clique ou 1, 2, 3, 4: selecionar   |   ESC: voltar", 260, 40);
        batch.end();
    }
    @Override public void dispose() { batch.dispose(); font.dispose(); shapes.dispose(); }
}
