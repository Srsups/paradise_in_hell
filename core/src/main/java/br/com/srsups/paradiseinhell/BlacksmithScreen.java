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

/** Rare, safe encounter. Material and permanent rarity commit before changing the run. */
public final class BlacksmithScreen extends ScreenAdapter {
    private final Main game;
    private final RunSession session;
    private final FitViewport viewport = new FitViewport(960, 540);
    private final SpriteBatch batch = new SpriteBatch();
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont font = new BitmapFont();
    private final Rectangle forge = new Rectangle(120, 95, 350, 55), leave = new Rectangle(490, 95, 350, 55);
    private String notice = "";
    private float inputDelay = 0.2f, noticeTimer;
    public BlacksmithScreen(Main game, RunSession session) {
        if (session.isDemo() || session.finished() || !session.encounters().blacksmithOpen())
            throw new IllegalStateException("No blacksmith encounter");
        this.game = game; this.session = session;
    }
    @Override public void show() { Gdx.input.setInputProcessor(null); }
    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void render(float delta) {
        inputDelay -= delta; noticeTimer = Math.max(0, noticeTimer - delta);
        if (noticeTimer == 0) notice = "";
        Vector2 pointer = viewport.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        boolean click = Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);
        if (inputDelay <= 0) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1)
                || Gdx.input.isKeyJustPressed(Input.Keys.E) || click && forge.contains(pointer)) {
                notice = !session.player().canForgeWeapon() ? "Sua arma já tem a melhoria permanente disponível."
                    : game.profiles().profile().forgeMaterials() == 0 ? "Falta um Fragmento de Forja. Ele vem dos mini-bosses."
                    : game.profiles().forgeWeapon(session) ? "Arma rara salva permanentemente! Zeus vai adorar."
                    : "A forja não foi concluída; seu material foi preservado.";
                noticeTimer = 3.5f;
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_2)
                || Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
                || click && leave.contains(pointer)) {
                session.encounters().closeBlacksmith();
                game.setScreen(new RouteScreen(game, session)); return;
            }
        }
        ScreenUtils.clear(0.045f, 0.055f, 0.085f, 1); viewport.apply();
        shapes.setProjectionMatrix(viewport.getCamera().combined); shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.13f, 0.16f, 0.23f, 1); shapes.rect(80, 180, 800, 220);
        shapes.setColor(0.55f, 0.35f, 0.12f, 1); shapes.rect(forge.x, forge.y, forge.width, forge.height);
        shapes.setColor(0.12f, 0.25f, 0.33f, 1); shapes.rect(leave.x, leave.y, leave.width, leave.height);
        // Provisional anvil, drawn without adding another external asset.
        shapes.setColor(Color.SLATE); shapes.rect(690, 265, 100, 14); shapes.triangle(790, 265, 820, 275, 790, 279);
        shapes.rect(725, 240, 25, 25); shapes.rect(710, 225, 55, 15);
        shapes.end();
        batch.setProjectionMatrix(viewport.getCamera().combined); batch.begin();
        font.setColor(Color.GOLD); font.getData().setScale(1.6f);
        font.draw(batch, "FERREIRO ERRANTE", 300, 475); font.getData().setScale(1);
        font.setColor(Color.WHITE);
        font.draw(batch, "Não conserto casamentos. Armas, talvez.", 110, 370);
        PlayerState player = session.player();
        font.draw(batch, player.getProfession().title + " | " + player.getWeapon().label + " | Raridade: " + player.getWeaponRarity().label, 110, 330);
        font.draw(batch, "Fragmentos guardados: " + game.profiles().profile().forgeMaterials(), 110, 300);
        font.draw(batch, "Custo: 1 Fragmento de Forja\nRara: +25% do dano base da arma e +10% de cadência.\n"
            + WeaponRarity.power(player.getWeapon()), 110, 265, 540, 8, true);
        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "Melhoria permanente: continua após morte, novas tentativas e ao fechar o jogo.", 110, 195);
        font.setColor(Color.WHITE); font.draw(batch, "[1 / E] Melhorar arma", 200, 128);
        font.draw(batch, "[2 / ENTER] Continuar", 565, 128);
        if (!notice.isEmpty()) { font.setColor(Color.GOLD); font.draw(batch, notice, 110, 75, 740, 8, true); }
        if (!game.profiles().error().isEmpty()) {
            font.setColor(Color.SALMON); font.draw(batch, game.profiles().error(), 80, 45, 800, 8, true);
        }
        batch.end();
    }
    @Override public void dispose() { batch.dispose(); shapes.dispose(); font.dispose(); }
}
