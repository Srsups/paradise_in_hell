package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class MenuScreen extends ScreenAdapter {
    private final Main game;
    private final FitViewport viewport = new FitViewport(960, 540);
    private final SpriteBatch batch = new SpriteBatch();
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont font = new BitmapFont();
    private final Rectangle play = new Rectangle(280, 265, 400, 55);
    private final Rectangle campaign = new Rectangle(280, 195, 400, 55);
    private final Rectangle audio = new Rectangle(280, 78, 400, 40);
    private final Rectangle exit = new Rectangle(280, 130, 400, 45);

    public MenuScreen(Main game) { this.game = game; }
    @Override public void show() { Gdx.input.setInputProcessor(null); }
    @Override public void resize(int width, int height) { viewport.update(width, height, true); }

    @Override public void render(float delta) {
        boolean clicked = Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);
        Vector2 pointer = viewport.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || clicked && play.contains(pointer)) {
            game.openDemo();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.C) || clicked && campaign.contains(pointer)) {
            game.openCampaign();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.O) || clicked && audio.contains(pointer)) {
            game.setScreen(new AudioSettingsScreen(game,()->new MenuScreen(game))); return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || clicked && exit.contains(pointer)) {
            Gdx.app.exit();
            return;
        }
        ScreenUtils.clear(0.045f, 0.055f, 0.085f, 1);
        viewport.apply();
        shapes.setProjectionMatrix(viewport.getCamera().combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.72f, 0.54f, 0.22f, 1);
        shapes.rect(play.x, play.y, play.width, play.height);
        shapes.setColor(0.24f, 0.38f, 0.48f, 1);
        shapes.rect(campaign.x, campaign.y, campaign.width, campaign.height);
        shapes.setColor(0.13f, 0.16f, 0.23f, 1);
        shapes.rect(exit.x, exit.y, exit.width, exit.height);
        shapes.rect(audio.x, audio.y, audio.width, audio.height);
        shapes.end();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        centered("PARADISE IN HELL", 440, 3, Color.GOLD);
        centered("Demo de sobrevivência e protótipo da campanha", 385, 1.2f, Color.WHITE);
        centered("DEMO: 5 MINUTOS  [ENTER]", 300, 1.2f, Color.BLACK);
        centered("CAMPANHA: PROTÓTIPO  [C]", 230, 1.2f, Color.WHITE);
        centered("SAIR  [ESC]", 158, 1.2f, Color.WHITE);
        centered("ÁUDIO  [O]", 106, 1.1f, Color.WHITE);
        centered("WASD / setas: mover   |   SHIFT: dash", 57, 1.1f, Color.LIGHT_GRAY);
        centered("Mouse: mirar   |   Segure o botão esquerdo: atacar", 33, 1.1f, Color.LIGHT_GRAY);
        centered("ESC: pausar   |   1, 2, 3: escolher uma dádiva", 11, 1.1f, Color.LIGHT_GRAY);
        batch.end();
    }
    private void centered(String text, float y, float scale, Color color) {
        font.getData().setScale(scale);
        font.setColor(color);
        GlyphLayout layout = new GlyphLayout(font, text);
        font.draw(batch, layout, (960 - layout.width) / 2, y);
    }
    @Override public void dispose() { batch.dispose(); shapes.dispose(); font.dispose(); }
}
