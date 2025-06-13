package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;

public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private Jogador jogador;
    private TileMap tileMap;

    @Override
    public void create() {
        batch = new SpriteBatch();

        // Cria uma câmera com a mesma dimensão da tela
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        tileMap = new TileMap();

        jogador = new Jogador(100, 100);
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);


        jogador.update(delta);
        tratarInput(jogador, delta);

        // Atualiza a posição da câmera para seguir o jogador
        camera.position.set(jogador.x, jogador.y, 0);
        camera.update();

        tileMap.update(camera); // atualiza o mapa baseado na câmera

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        tileMap.draw(batch);    // desenha os tiles primeiro (fundo)
        jogador.draw(batch);    // desenha o jogador por cima
        batch.end();
    }

    private void tratarInput(Jogador jogador, float delta) {
        float velocidade = 100f;

        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            jogador.y += velocidade * delta;
            jogador.setEstado(Jogador.Estado.COSTAS);
        } else if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            jogador.y -= velocidade * delta;
            jogador.setEstado(Jogador.Estado.FRENTE);
        } else if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            jogador.x -= velocidade * delta;
            jogador.setEstado(Jogador.Estado.LADO_ESQUERDO);
        } else if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            jogador.x += velocidade * delta;
            jogador.setEstado(Jogador.Estado.LADO_DIREITO);
        } else {
            jogador.setEstado(Jogador.Estado.PARADO);
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        tileMap.dispose();
        jogador.dispose();
    }
}
