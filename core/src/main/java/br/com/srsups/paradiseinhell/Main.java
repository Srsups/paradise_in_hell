package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;

import java.util.ArrayList;
import java.util.Random;

public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private Jogador jogador;
    private TileMap tileMap;
    private Texture spritesheet;
    private ArrayList<Inimigo> inimigos = new ArrayList<>();
    private Random random = new Random();
    private float spawnTimer = 0f;
    private float spawnInterval = 3f; // Spawn de um novo inimigo a cada 3 segundos

    @Override
    public void create() {
        batch = new SpriteBatch();

        spritesheet = new Texture("spritesheet.png");

        // Cria uma câmera com a mesma dimensão da tela
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        tileMap = new TileMap(spritesheet);

        jogador = new Jogador(100, 100, spritesheet);

        tileMap.gerarAreaInicialSegura(jogador.x, jogador.y, 5);
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);


        jogador.update(delta, tileMap);

        for (Inimigo inimigo : inimigos) {
            inimigo.update(delta, jogador, tileMap);
        }

        // Atualiza a posição da câmera para seguir o jogador
        camera.position.set(jogador.x + 8, jogador.y + 8, 0);
        camera.update();

        tileMap.update(camera); // atualiza o mapa baseado na câmera

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        tileMap.draw(batch);    // desenha os tiles primeiro (fundo)
        jogador.draw(batch);    // desenha o jogador por cima
        for (Inimigo inimigo : inimigos) {
            inimigo.draw(batch);
        }
        batch.end();

        spawnTimer += delta;
        if (spawnTimer >= spawnInterval) {
            // A Main agora é responsável por adicionar o inimigo à lista
            inimigos.add(EnemySpawner.spawnInimigoForaDaTela(camera, spritesheet));
            spawnTimer = 0f;
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        tileMap.dispose();
        jogador.dispose();
        spritesheet.dispose();
    }
}
