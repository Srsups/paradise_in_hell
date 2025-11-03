package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class Main extends Game {

    public SpriteBatch batch;
    public ShapeRenderer shapeRenderer;
    public BitmapFont font;
    public HUD hud;
    public Viewport viewport;
    public OrthographicCamera camera;

    // O método create agora só tem uma função: definir a primeira tela.
    @Override
    public void create() {
        // Inicialize-os UMA VEZ quando o jogo começar
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();
        // Cria câmera e viewport que a HUD espera
        camera = new OrthographicCamera();
        viewport = new FitViewport(1920f, 1080f, camera);
        hud = new HUD();
        Gdx.graphics.setVSync(true);

        // Inicia o jogo com a primeira tela (ex: MenuScreen ou GameScreen)
        this.setScreen(new MenuScreen(this));
    }

    @Override
    public void dispose() {
        // Libere os recursos UMA VEZ quando o jogo fechar
        batch.dispose();
        shapeRenderer.dispose();
        font.dispose();
        if (hud != null) hud.dispose();

        // É importante chamar o dispose da tela atual também
        super.dispose();
    }

}
