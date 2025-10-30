package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class GameOverScreen implements Screen {
    private Main game;
    private SpriteBatch batch;
    private BitmapFont font;
    private GlyphLayout layout;
    private GlyphLayout scoreLayout;

    private OrthographicCamera uiCamera;
    private Viewport viewport;

    public GameOverScreen(Main game, int obolosDaPartida) {
        this.game = game;
        this.batch = game.batch;
        this.font = game.font;

        uiCamera = new OrthographicCamera();
        viewport = new FitViewport(1920, 1080, uiCamera);

        font.setColor(Color.WHITE);
        font.getData().setScale(2);

        this.layout = new GlyphLayout(font, "Você morreu");
        this.scoreLayout = new GlyphLayout(font, "Óbolos Coletados: " + obolosDaPartida);

        Preferences prefs = Gdx.app.getPreferences("ParadiseInHellSave");
        int totalMoedas = prefs.getInteger("total_moedas", 0);
        prefs.putInteger("total_moedas", totalMoedas + obolosDaPartida);
        prefs.flush();
    }

    @Override
    public void render(float delta) {
        viewport.apply();

        if (Gdx.input.isTouched()) {
            game.setScreen(new MenuScreen(game));
            return;
        }

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        font.draw(batch, layout,
            viewport.getWorldWidth() / 2f - layout.width / 2f,
            viewport.getWorldHeight() / 2f + layout.height / 2f + 50
        );
        font.draw(batch, scoreLayout,
            viewport.getWorldWidth() / 2f - scoreLayout.width / 2f,
            viewport.getWorldHeight() / 2f - scoreLayout.height / 2f
        );
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void show() {

    }

    @Override
    public void hide() {

    }
}
