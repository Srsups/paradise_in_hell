package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.ArrayList;
import java.util.Iterator;

public class BossScreen implements Screen, WorldController {

    private Main game;
    private OrthographicCamera camera;
    private Viewport viewport;
    private Jogador jogador;
    private Texture spritesheet;
    private ChefeCerbero chefe; // Nosso futuro chefe
    private TileMap mapaDaArena;
    private ArrayList<Projetil> projeteis = new ArrayList<>();
    private TextureRegion texturaProjetil;

    // Recebemos o jogador da tela anterior para manter o progresso
    public BossScreen(Main game, Jogador jogador, Texture spritesheet) {
        this.game = game;
        this.jogador = jogador;
        this.spritesheet = spritesheet;
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new ExtendViewport(480, 270, camera);
        viewport.apply();
        camera.position.set(viewport.getWorldWidth()/2f, viewport.getWorldHeight()/2f, 0);
        camera.update();

        texturaProjetil = new TextureRegion(spritesheet, 178, 209, 5, 16);

        // Cria um mapa específico para a arena do chefe
        mapaDaArena = new TileMap(spritesheet);
        // Coloca o jogador no centro da arena
        // Converte a posição do centro da câmera para a grade de tiles
        int centroArenaX = (int) (camera.viewportWidth / 2 / Tile.TILE_SIZE);
        int centroArenaY = (int) (camera.viewportHeight / 2 / Tile.TILE_SIZE);

        // Gera uma arena segura e vazia
        // Gera uma arena fechada de 28x15 tiles internos
        mapaDaArena.gerarArenaFechada(centroArenaX, centroArenaY, 40, 22);

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

        // --- LÓGICA DA CÂMERA (MODIFICADA) ---
        // 1. Centraliza a câmera no jogador
        camera.position.set(jogador.x, jogador.y, 0);

        // 2. Calcula os limites da câmera
        float camHalfWidth = camera.viewportWidth * 0.5f;
        float camHalfHeight = camera.viewportHeight * 0.5f;

        // 3. Calcula os limites do mapa (assumindo que o mapa começa em 0,0)
        float mapLeft = 0;
        float mapRight = mapaDaArena.mapPixelWidth;
        float mapBottom = 0;
        float mapTop = mapaDaArena.mapPixelHeight;

        // 4. "Fixa" a posição da câmera (Clamp) para que ela não ultrapasse os limites
        camera.position.x = Math.max(mapLeft + camHalfWidth, Math.min(mapRight - camHalfWidth, camera.position.x));
        camera.position.y = Math.max(mapBottom + camHalfHeight, Math.min(mapTop - camHalfHeight, camera.position.y));

        // 5. Atualiza a câmera APÓS todas as modificações
        camera.update();

        // --- LÓGICA DE DANO DO CHEFE ---
        if (chefe.estaAtacando() && chefe.getSolidArea().overlaps(jogador.getSolidArea())) {
            jogador.sofrerDano(chefe.getDanoAtaqueCarga());
        }

        // Lógica de Desenho
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();
        mapaDaArena.draw(game.batch);
        jogador.draw(game.batch);
        chefe.draw(game.batch);
        for (Projetil p : projeteis) {
            p.draw(game.batch);
        }
        game.batch.end();

        // Lógica de Desenho da HUD (fora do batch do mundo)
        game.hud.draw(jogador); // <-- HUD é desenhada aqui

        // Lógica para fim da batalha
        if (chefe.estaMorto()) {
            System.out.println("CHEFE DERROTADO!");
            game.setScreen(new MenuScreen(game));
        }
    }

    @Override
    public void resize(int width, int height) {
        if (viewport != null)
            viewport.update(width, height, true);
        if (game.hud != null)
            game.hud.resize(width, height);
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
