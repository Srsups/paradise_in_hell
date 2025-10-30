package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

public class Main extends Game {

    public SpriteBatch batch;
    public ShapeRenderer shapeRenderer;
    public BitmapFont font;
    public HUD hud;

    // O método create agora só tem uma função: definir a primeira tela.
    @Override
    public void create() {
        // Inicialize-os UMA VEZ quando o jogo começar
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();
        hud = new HUD(batch, shapeRenderer, font);
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

        // É importante chamar o dispose da tela atual também
        super.dispose();
    }

}
