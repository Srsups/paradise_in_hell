package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
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
    private GlyphLayout scoreLayout;

    public GameOverScreen(Game game, int obolosDaPartida) {
        this.game = game;
        this.batch = new SpriteBatch();
        this.font = new BitmapFont();
        font.setColor(Color.WHITE);
        font.getData().setScale(2); // Deixa a fonte maior

        this.layout = new GlyphLayout(font, "Você morreu");
        this.scoreLayout = new GlyphLayout(font, "Óbolos Coletados: " + obolosDaPartida);

        // --- LÓGICA DE SALVAR ---
        Preferences prefs = Gdx.app.getPreferences("ParadiseInHellSave");
        int totalMoedas = prefs.getInteger("total_moedas", 0);
        prefs.putInteger("total_moedas", totalMoedas + obolosDaPartida);
        prefs.flush(); // Salva as alterações no disco
    }

    @Override
    public void show() {}

    @Override
    public void render(float delta) {
        if (Gdx.input.isTouched()) {
            game.setScreen(new MenuScreen(game));
            return;
        }

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        // Desenha o texto "Você morreu"
        font.draw(batch, layout,
            Gdx.graphics.getWidth() / 2f - layout.width / 2f,
            Gdx.graphics.getHeight() / 2f + layout.height / 2f + 50 // Um pouco mais para cima
        );

        // Desenha o placar de moedas
        font.draw(batch, scoreLayout,
            Gdx.graphics.getWidth() / 2f - scoreLayout.width / 2f,
            Gdx.graphics.getHeight() / 2f - scoreLayout.height / 2f
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
