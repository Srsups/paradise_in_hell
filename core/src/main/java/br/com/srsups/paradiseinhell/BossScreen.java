package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import java.util.ArrayList;
import java.util.Iterator;

public class BossScreen implements Screen, WorldController {

    private Game game;
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private Jogador jogador;
    private Texture spritesheet;
    private ChefeCerbero chefe; // Nosso futuro chefe
    private TileMap mapaDaArena;
    private ArrayList<Projetil> projeteis = new ArrayList<>();
    private TextureRegion texturaProjetil;

    // Recebemos o jogador da tela anterior para manter o progresso
    public BossScreen(Game game, Jogador jogador, Texture spritesheet) {
        this.game = game;
        this.jogador = jogador;
        this.spritesheet = spritesheet;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 480, 270); // Uma visão menor para a arena
        texturaProjetil = new TextureRegion(spritesheet, 178, 209, 5, 16);

        // Cria um mapa específico para a arena do chefe
        mapaDaArena = new TileMap(spritesheet);
        // Coloca o jogador no centro da arena
        // Converte a posição do centro da câmera para a grade de tiles
        int centroArenaX = (int) (camera.viewportWidth / 2 / Tile.TILE_SIZE);
        int centroArenaY = (int) (camera.viewportHeight / 2 / Tile.TILE_SIZE);

        // Gera uma arena segura e vazia
        // Gera uma arena fechada de 28x15 tiles internos
        mapaDaArena.gerarArenaFechada(centroArenaX, centroArenaY, 28, 15);

        // Coloca o jogador no centro da arena
        jogador.x = camera.viewportWidth / 2f;
        jogador.y = camera.viewportHeight / 2f;

        // Instancia o chefe
        chefe = new ChefeCerbero(jogador.x + 100, jogador.y, spritesheet);
    }

    public void criarProjetil(Jogador atirador, float x, float y, OrthographicCamera camera) {
        Vector3 mousePosTela = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        Vector3 mousePosMundo = camera.unproject(mousePosTela);
        Vector2 direcao = new Vector2(mousePosMundo.x - x, mousePosMundo.y - y).nor();
        float danoFinal = atirador.getDano();
        projeteis.add(new Projetil(x, y, direcao, texturaProjetil, danoFinal));
    }

    @Override
    public void render(float delta) {
        // Lógica de Update
        jogador.update(delta, mapaDaArena, camera, this);
        chefe.update(delta, jogador, mapaDaArena);

        Iterator<Projetil> projetilIterator = projeteis.iterator();
        while (projetilIterator.hasNext()) {
            Projetil p = projetilIterator.next();
            p.update(delta, mapaDaArena);
            if (p.deveSerRemovido) {
                projetilIterator.remove();
                continue;
            }

            // Checa colisão com o CHEFE
            if (p.x < chefe.x + 16 && p.x + 8 > chefe.x && p.y < chefe.y + 16 && p.y + 8 > chefe.y) {
                chefe.sofrerDano(p.dano);
                projetilIterator.remove();
            }
        }

        // Câmera segue o jogador
        camera.position.set(jogador.x, jogador.y, 0);
        camera.update();

        // Lógica de Desenho
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        mapaDaArena.draw(batch);
        jogador.draw(batch);
        chefe.draw(batch);
        for (Projetil p : projeteis) {
            p.draw(batch);
        }
        batch.end();

        // Lógica para fim da batalha (placeholder)
        if (chefe.estaMorto()) {
            System.out.println("CHEFE DERROTADO!");
            // Aqui você transicionaria para a próxima fase do jogo ou de volta ao menu
            game.setScreen(new MenuScreen(game));
        }
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

    }
}
