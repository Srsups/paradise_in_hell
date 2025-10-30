// Em MenuScreen.java
package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class MenuScreen implements Screen {
    private Main game;
    private SpriteBatch batch;

    // --- NOVOS CAMPOS PARA A UI ROBUSTA ---
    private OrthographicCamera uiCamera;
    private Viewport viewport;
    // ------------------------------------

    private Texture background;
    private TextureRegion playButtonTexture, exitButtonTexture, shopButtonTexture;
    private Rectangle playButtonBounds, exitButtonBounds, shopButtonBounds;
    private Texture Buttonspritesheet;

    public MenuScreen(Main game) {
        this.game = game;
        this.batch = game.batch;

        // --- CONFIGURAÇÃO DA VIEWPORT ---
        // 1. Crie uma câmera para a UI.
        uiCamera = new OrthographicCamera();
        // 2. Crie uma FitViewport com uma resolução virtual (ex: 1920x1080).
        // A UI será desenhada nesta resolução e a viewport a ajustará para a tela.
        viewport = new FitViewport(1920, 1080, uiCamera);
        // ------------------------------------

        Buttonspritesheet = new Texture("button_spritesheet.png");
        background = new Texture("Foto_BG.jpg");
        playButtonTexture = new TextureRegion(Buttonspritesheet, 433, 104, 238, 58);
        exitButtonTexture = new TextureRegion(Buttonspritesheet, 701, 367, 238, 58);
        shopButtonTexture = new TextureRegion(Buttonspritesheet, 433, 236, 238, 58);

        // --- POSICIONAMENTO BASEADO NA VIEWPORT ---
        // Use viewport.getWorldWidth() e getWorldHeight() para posicionar os elementos
        float centerX = viewport.getWorldWidth() / 2f;
        float centerY = viewport.getWorldHeight() / 2f;

        playButtonBounds = new Rectangle(
            centerX - playButtonTexture.getRegionWidth() / 2f,
            centerY,
            playButtonTexture.getRegionWidth(),
            playButtonTexture.getRegionHeight()
        );

        shopButtonBounds = new Rectangle(
            centerX - shopButtonTexture.getRegionWidth() / 2f,
            playButtonBounds.y - 120, // Aumente o espaçamento para a resolução maior
            shopButtonTexture.getRegionWidth(),
            shopButtonTexture.getRegionHeight()
        );

        exitButtonBounds = new Rectangle(
            centerX - exitButtonTexture.getRegionWidth() / 2f,
            shopButtonBounds.y - 120,
            exitButtonTexture.getRegionWidth(),
            exitButtonTexture.getRegionHeight()
        );
    }

    @Override
    public void render(float delta) {
        // --- ATUALIZA A CÂMERA E APLICA A VIEWPORT ---
        viewport.apply();
        // ------------------------------------------

        if (Gdx.input.justTouched()) {
            Vector3 touchPos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            // Converte as coordenadas do clique da tela para as coordenadas da viewport
            viewport.unproject(touchPos);

            // A checagem agora usa as coordenadas do mundo da viewport
            if (playButtonBounds.contains(touchPos.x, touchPos.y)) {
                game.setScreen(new GameScreen(game));
            }
            if (shopButtonBounds.contains(touchPos.x, touchPos.y)) {
                game.setScreen(new LojaScreen(game));
            }
            if (exitButtonBounds.contains(touchPos.x, touchPos.y)) {
                Gdx.app.exit();
            }
        }

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // --- DESENHO USANDO A CÂMERA DA VIEWPORT ---
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        // Desenha o background para preencher toda a viewport
        batch.draw(background, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
        batch.draw(playButtonTexture, playButtonBounds.x, playButtonBounds.y);
        batch.draw(shopButtonTexture, shopButtonBounds.x, shopButtonBounds.y);
        batch.draw(exitButtonTexture, exitButtonBounds.x, exitButtonBounds.y);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        // ATUALIZA A VIEWPORT QUANDO A JANELA É REDIMENSIONADA
        viewport.update(width, height, true); // O 'true' centraliza a câmera
    }

    @Override
    public void show() { }


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
        background.dispose();
        Buttonspritesheet.dispose();
    }

}
