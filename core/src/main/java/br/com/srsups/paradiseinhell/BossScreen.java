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
    private HUD hud;
    private Texture spritesheet;
    private ChefeCerbero chefe;
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
        viewport = new ExtendViewport(704, 480, camera);
        viewport.apply();
        camera.position.set(viewport.getWorldWidth()/2f, viewport.getWorldHeight()/2f, 0);
        camera.update();
        hud = new HUD();

        texturaProjetil = new TextureRegion(spritesheet, 178, 209, 5, 16);

        // Cria um mapa específico para a arena do chefe
        mapaDaArena = new TileMap(spritesheet);
        // Coloca o jogador no centro da arena
        // Converte a posição do centro da câmera para a grade de tiles
        int centroArenaX = (int) (viewport.getWorldWidth() / 2 / Tile.TILE_SIZE);
        int centroArenaY = (int) (viewport.getWorldHeight() / 2 / Tile.TILE_SIZE);

        // Gera uma arena segura e vazia
        // Gera uma arena fechada de 64x32 tiles internos
        mapaDaArena.gerarArenaFechada(centroArenaX, centroArenaY, 64, 32);

        // Coloca o jogador no centro da arena (posiciona pelo canto superior esquerdo do sprite)
        // Usa o centro real do mapa para evitar que o jogador seja posicionado fora da arena.
        float arenaCenterX = mapaDaArena.mapPixelLeft + mapaDaArena.mapPixelWidth * 0.5f;
        float arenaCenterY = mapaDaArena.mapPixelBottom + mapaDaArena.mapPixelHeight * 0.5f;

        // Tenta encontrar um tile não-sólido próximo ao centro para posicionar o jogador
        Vector2 posJog = mapaDaArena.findNearestFreePosition(arenaCenterX, arenaCenterY, 8);
        if (posJog != null) {
            jogador.x = posJog.x - jogador.getWidth() / 2f;
            jogador.y = posJog.y - jogador.getHeight() / 2f;
        } else {
            // fallback: centro exato
            jogador.x = arenaCenterX - jogador.getWidth() / 2f;
            jogador.y = arenaCenterY - jogador.getHeight() / 2f;
        }

        // Instancia o chefe próximo ao jogador mas garante o spawn em tile livre
        float preferBossX = arenaCenterX + 100f;
        float preferBossY = arenaCenterY;
        Vector2 posChefe = mapaDaArena.findNearestFreePosition(preferBossX, preferBossY, 8);
        if (posChefe != null) {
            chefe = new ChefeCerbero(posChefe.x - 8f, posChefe.y - 8f, spritesheet); // ajusta para bottom-left (assume 16x16)
        } else {
            chefe = new ChefeCerbero(arenaCenterX + 100f, arenaCenterY, spritesheet);
        }
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
        hud.render(jogador.getVida(), jogador.getEstaminaAtual(), jogador.getXpAtual());


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
        // 1. Centraliza a câmera no centro do jogador
        camera.position.set(jogador.getX() + jogador.getWidth() / 2f, jogador.getY() + jogador.getHeight() / 2f, 0);

        // 2. Calcula os limites efetivos da câmera (leva em conta o zoom)
        float camHalfWidth = (camera.viewportWidth * camera.zoom) * 0.5f;
        float camHalfHeight = (camera.viewportHeight * camera.zoom) * 0.5f;

        // 3. Calcula os limites do mapa usando o offset real fornecido pelo TileMap
        float mapLeft = mapaDaArena.mapPixelLeft;
        float mapRight = mapaDaArena.mapPixelLeft + mapaDaArena.mapPixelWidth;
        float mapBottom = mapaDaArena.mapPixelBottom;
        float mapTop = mapaDaArena.mapPixelBottom + mapaDaArena.mapPixelHeight;

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

        hud.resize(width, height);

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
        hud.dispose();
    }
}
