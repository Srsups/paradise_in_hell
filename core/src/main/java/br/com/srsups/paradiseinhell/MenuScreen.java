package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;

public class MenuScreen implements Screen {
    private Game game;
    private SpriteBatch batch;
    private Texture background;
    private TextureRegion playButtonTexture, exitButtonTexture;
    private Rectangle playButtonBounds, exitButtonBounds;
    private Texture Buttonspritesheet;

    public MenuScreen(Game game) {
        this.game = game;
        batch = new SpriteBatch();
        Buttonspritesheet = new Texture("button_spritesheet.png");
        background = new Texture("Foto_BG.jpg"); // Crie uma imagem de fundo para o menu
        playButtonTexture = new TextureRegion(Buttonspritesheet, 433, 104, 238, 58); // Crie o sprite do botão de jogar
        exitButtonTexture = new TextureRegion(Buttonspritesheet, 701, 367, 238, 58); // Crie o sprite do botão de sair

        // Define a posição e o tamanho dos retângulos de colisão dos botões
        playButtonBounds = new Rectangle(
            Gdx.graphics.getWidth() / 2f - playButtonTexture.getRegionWidth() / 2f,
            Gdx.graphics.getHeight() / 2f,
            playButtonTexture.getRegionWidth(),
            playButtonTexture.getRegionHeight()
        );

        exitButtonBounds = new Rectangle(
            Gdx.graphics.getWidth() / 2f - exitButtonTexture.getRegionWidth() / 2f,
            Gdx.graphics.getHeight() / 2f - 100, // 100 pixels abaixo do botão de jogar
            exitButtonTexture.getRegionWidth(),
            exitButtonTexture.getRegionHeight()
        );
    }

    @Override
    public void show() { }

    @Override
    public void render(float delta) {
        // Lógica de Input
        if (Gdx.input.isTouched()) {
            Vector3 touchPos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            // Checa o clique no botão de jogar
            if (playButtonBounds.contains(touchPos.x, Gdx.graphics.getHeight() - touchPos.y)) {
                game.setScreen(new GameScreen(game)); // Inicia a tela de jogo
            }
            // Checa o clique no botão de sair
            if (exitButtonBounds.contains(touchPos.x, Gdx.graphics.getHeight() - touchPos.y)) {
                Gdx.app.exit(); // Fecha o jogo
            }
        }

        // Lógica de Desenho
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        batch.draw(background, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.draw(playButtonTexture, playButtonBounds.x, playButtonBounds.y);
        batch.draw(exitButtonTexture, exitButtonBounds.x, exitButtonBounds.y);
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
        background.dispose();
        Buttonspritesheet.dispose();
    }
}
