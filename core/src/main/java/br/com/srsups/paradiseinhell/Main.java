package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private Jogador jogador;
    private TileMap tileMap;
    private Texture spritesheet;
    private ArrayList<Inimigo> inimigos = new ArrayList<>();
    private float spawnTimer = 0f;
    private float spawnInterval = 3f; // Spawn de um novo inimigo a cada 3 segundos
    private TextureRegion texturaProjetil;
    private ArrayList<Projetil> projeteis = new ArrayList<>();
    public static Main instance;

    @Override
    public void create() {
        batch = new SpriteBatch();

        spritesheet = new Texture("spritesheet.png");

        // Cria uma câmera com a mesma dimensão da tela
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        texturaProjetil = new TextureRegion(spritesheet, 178, 209, 5, 16);

        tileMap = new TileMap(spritesheet);

        jogador = new Jogador(100, 100, spritesheet);

        instance = this;

        tileMap.gerarAreaInicialSegura(jogador.x, jogador.y, 5);
    }

    public void criarProjetil(float x, float y, OrthographicCamera camera) {
        // ... (cálculo da direção do mouse, sem alterações) ...
        Vector3 mousePosTela = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        Vector3 mousePosMundo = camera.unproject(mousePosTela);
        Vector2 direcao = new Vector2(mousePosMundo.x - x, mousePosMundo.y - y).nor();

        // Modificação: Passe a TextureRegion que já foi carregada e recortada
        projeteis.add(new Projetil(x, y, direcao, texturaProjetil));
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);


        jogador.update(delta, tileMap, camera);

        for (Inimigo inimigo : inimigos) {
            inimigo.update(delta, jogador, tileMap);
        }

        for (Projetil p : projeteis) {
            p.update(delta, tileMap);
        }

        // Atualiza a posição da câmera para seguir o jogador
        camera.position.set(jogador.x + 8, jogador.y + 8, 0);
        camera.update();
        tileMap.update(camera); // atualiza o mapa baseado na câmera

        spawnTimer += delta;
        if (spawnTimer >= spawnInterval) {
            // A Main agora é responsável por adicionar o inimigo à lista
            inimigos.add(EnemySpawner.spawnInimigoForaDaTela(camera, spritesheet));
            spawnTimer = 0f;
        }

        // Agora, fazemos a checagem de colisões e remoções com um Iterator
        Iterator<Projetil> projetilIterator = projeteis.iterator();
        while (projetilIterator.hasNext()) {
            Projetil p = projetilIterator.next();

            // Checa se o projétil já deve ser removido (por ter atingido uma parede)
            if (p.deveSerRemovido) {
                projetilIterator.remove(); // Remove e vai para o próximo projétil
                continue;
            }

            // Se não atingiu uma parede, checa se atingiu um inimigo
            Iterator<Inimigo> inimigoIterator = inimigos.iterator();
            while (inimigoIterator.hasNext()) {
                Inimigo i = inimigoIterator.next();

                // Checa colisão entre projétil e inimigo
                if (p.x < i.x + 16 && p.x + 8 > i.x && p.y < i.y + 16 && p.y + 8 > i.y) {
                    i.sofrerDano(p.dano);
                    p.deveSerRemovido = true; // Marca o projétil para remoção

                    if (i.estaMorto()) {
                        inimigoIterator.remove();
                    }
                    break; // Sai do loop de inimigos, pois o projétil já atingiu seu alvo
                }
            }

            // Após checar contra todos os inimigos, vemos se o projétil foi marcado para remoção
            if (p.deveSerRemovido) {
                projetilIterator.remove();
            }
        }

        Iterator<Inimigo> inimigoIterator = inimigos.iterator();
        while (inimigoIterator.hasNext()) {
            Inimigo i = inimigoIterator.next();
            if (jogador.x < i.x + 16 && jogador.x + 16 > i.x && jogador.y < i.y + 16 && jogador.y + 16 > i.y) {
                jogador.sofrerDano(10); // Jogador sofre 10 de dano por toque
                inimigoIterator.remove(); // Remove o inimigo para não dar dano contínuo
            }
        }

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        tileMap.draw(batch);    // desenha os tiles primeiro (fundo)
        jogador.draw(batch);    // desenha o jogador por cima

        for (Inimigo inimigo : inimigos) {
            inimigo.draw(batch);
        }

        for (Projetil projetil : projeteis) {
            projetil.draw(batch);
        }

        batch.end();
    }

    @Override
    public void dispose() {
        batch.dispose();
        tileMap.dispose();
        jogador.dispose();
        spritesheet.dispose();
    }
}
