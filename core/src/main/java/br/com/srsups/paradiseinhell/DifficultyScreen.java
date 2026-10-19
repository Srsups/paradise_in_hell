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

/** Pending choices are local to this screen until the player confirms and disk saving succeeds. */
public final class DifficultyScreen extends ScreenAdapter {
    private final Main game;
    private final Profession profession;
    private final RegionDefinition region;
    private GameDifficulty selected = GameDifficulty.MEDIUM;
    private String notice = "";
    private float confirmDelay = .2f;
    private final FitViewport viewport = new FitViewport(960, 540);
    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font = UiTheme.bodyFont();
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final Rectangle[] cards = {
        new Rectangle(35, 200, 285, 185), new Rectangle(337, 200, 285, 185), new Rectangle(639, 200, 285, 185)
    };
    private final Rectangle back = new Rectangle(215, 78, 220, 50);
    private final Rectangle confirm = new Rectangle(495, 78, 250, 50);
    public DifficultyScreen(Main game, Profession profession, RegionDefinition region) {
        this.game = game; this.profession = profession; this.region = region;
    }
    public GameDifficulty selected() { return selected; }
    @Override public void show() { Gdx.input.setInputProcessor(null); }
    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void render(float delta) {
        confirmDelay = Math.max(0, confirmDelay - delta);
        Vector2 pointer = viewport.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        boolean click = Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || click && back.contains(pointer)) {
            game.setScreen(new ProfessionScreen(game, region)); return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) selected = GameDifficulty.values()[Math.max(0, selected.ordinal() - 1)];
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) selected = GameDifficulty.values()[Math.min(2, selected.ordinal() + 1)];
        for (int i = 0; i < cards.length; i++) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + i) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1 + i)
                || click && cards[i].contains(pointer)) selected = GameDifficulty.values()[i];
        }
        if (confirmDelay == 0 && (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || click && confirm.contains(pointer))) {
            if (game.createCharacter(profession, selected)) {
                if (region == null) game.startDemo(); else game.setScreen(new RefugeScreen(game));
                return;
            }
            notice = game.profiles().error().isEmpty() ? "Personagem indisponível. Volte e escolha outra profissão."
                : "Não foi possível criar o personagem. Verifique o save e tente novamente.";
        }
        draw();
    }
    private void draw() {
        ScreenUtils.clear(.045f, .055f, .085f, 1); viewport.apply();
        shapes.setProjectionMatrix(viewport.getCamera().combined); shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < cards.length; i++) {
            shapes.setColor(selected.ordinal() == i ? new Color(.30f, .27f, .14f, 1) : new Color(.13f, .16f, .23f, 1));
            Rectangle c = cards[i]; shapes.rect(c.x, c.y, c.width, c.height);
        }
        shapes.setColor(.19f, .23f, .31f, 1); shapes.rect(back.x, back.y, back.width, back.height);
        shapes.setColor(.32f, .29f, .13f, 1); shapes.rect(confirm.x, confirm.y, confirm.width, confirm.height);
        shapes.end(); batch.setProjectionMatrix(viewport.getCamera().combined); batch.begin();
        text("ESCOLHA A DIFICULDADE", 260, 492, 1.65f, Color.GOLD);
        text("Save " + (game.activeSlot() + 1) + " | " + profession.title + " | " + profession.weapon.label, 35, 440, 1.15f, Color.WHITE);
        for (int i = 0; i < cards.length; i++) {
            GameDifficulty d = GameDifficulty.values()[i]; Rectangle c = cards[i];
            text(d.label + (selected == d ? "  (selecionada)" : ""), c.x + 18, c.y + 156, 1.15f, Color.GOLD);
            text("Vida dos inimigos: " + Math.round(d.healthMultiplier * 100) + "%", c.x + 18, c.y + 119, 1, Color.WHITE);
            text("Dano dos inimigos: " + Math.round(d.damageMultiplier * 100) + "%", c.x + 18, c.y + 91, 1, Color.WHITE);
            text("Óbolos e Almas: " + d.rewardPercent + "%", c.x + 18, c.y + 63, 1, Color.WHITE);
            text(i == 0 ? "Para conhecer a jornada." : i == 1 ? "O desafio padrão." : "Maior risco e recompensa.", c.x + 18, c.y + 30, .95f, Color.LIGHT_GRAY);
        }
        text("Profissão e dificuldade ficam fixas neste save. Para mudar, crie outro personagem.", 70, 164, 1, Color.GOLD);
        text("Voltar", back.x + 83, back.y + 32, 1, Color.WHITE);
        text("Criar personagem", confirm.x + 54, confirm.y + 32, 1, Color.WHITE);
        text("Clique ou setas: escolher | ENTER: criar | ESC: voltar", 205, 40, 1, Color.LIGHT_GRAY);
        if (!notice.isEmpty()) text(notice, 35, 410, 1, Color.SALMON);
        batch.end();
    }
    private void text(String value, float x, float y, float scale, Color color) {
        font.getData().setScale(scale); font.setColor(color); font.draw(batch, value, x, y);
    }
    @Override public void dispose() { batch.dispose(); font.dispose(); shapes.dispose(); }
}

