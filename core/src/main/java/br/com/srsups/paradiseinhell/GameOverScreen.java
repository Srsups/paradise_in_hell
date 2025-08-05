package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class GameOverScreen implements Screen {
    private Game game;
    private SpriteBatch batch;
    private BitmapFont font;
    private GlyphLayout layout;

    public GameOverScreen(Game game) {
        this.game = game;
        this.batch = new SpriteBatch();
        this.font = new BitmapFont();
        font.setColor(Color.WHITE);
        font.getData().setScale(3); // Deixa a fonte maior

        this.layout = new GlyphLayout(font, "Você morreu");
    }

    @Override
    public void show() {}

    @Override
    public void render(float delta) {
        if (Gdx.input.isTouched()) {
            game.setScreen(new MenuScreen(game));
        }

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        // Desenha o texto centralizado na tela
        font.draw(batch, layout,
            Gdx.graphics.getWidth() / 2f - layout.width / 2f,
            Gdx.graphics.getHeight() / 2f + layout.height / 2f
        );
        batch.end();
    }

    @Override
    public void resize(int i, int i1) {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }


    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
    }
}
