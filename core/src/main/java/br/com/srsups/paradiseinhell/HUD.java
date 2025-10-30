package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class HUD {
    private Viewport viewport;
    private ShapeRenderer shapeRenderer;
    private SpriteBatch batch;
    private BitmapFont font;
    private float vidaExibida;

    public HUD(SpriteBatch batch, ShapeRenderer shapeRenderer, BitmapFont font) {
        // Defina a resolução "ideal" ou "virtual" para a sua HUD.
        // Todos os seus desenhos serão baseados nessas dimensões.
        float hudWidth = 1920;
        float hudHeight = 1080;

        // A FitViewport vai escalar essa resolução para caber na tela
        // sem distorcer, adicionando barras pretas se necessário.
        viewport = new FitViewport(hudWidth, hudHeight, new OrthographicCamera());

        this.shapeRenderer = shapeRenderer;
        this.batch = batch;
        this.font = font;
    }

    // O método draw agora recebe o Jogador para saber o que desenhar
    public void draw(Jogador jogador) {
        // --- 1. Lógica de desenhar as barras (antigo desenharUI) ---
        // Aplica a viewport. Isso ajusta a câmera para o tamanho da tela atual.
        viewport.apply();
        shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);
        batch.setProjectionMatrix(viewport.getCamera().combined);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Barra de Vida
        float barX = 20f, barWidth = 400f, barHeight = 40f;
        float barYVida = viewport.getWorldHeight() - 50f;
        shapeRenderer.setColor(Color.DARK_GRAY);
        float vidaReal = jogador.getVida();
        vidaExibida = MathUtils.lerp(vidaExibida, vidaReal, 0.12f); // suaviza
        float pct = vidaExibida / jogador.getVidaMaxima();
        shapeRenderer.rect(barX, barYVida, barWidth * pct, barHeight);
        shapeRenderer.setColor(Color.RED);
        shapeRenderer.rect(barX, barYVida, barWidth * (jogador.getVida() / jogador.getVidaMaxima()), barHeight);

        // Barra de Estamina
        float barYEstamina = barYVida - 30f;
        shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(barX, barYEstamina, barWidth, barHeight);
        shapeRenderer.setColor(jogador.isExausto() ? Color.FIREBRICK : Color.GREEN);
        shapeRenderer.rect(barX, barYEstamina, barWidth * (jogador.getEstaminaAtual() / jogador.getEstaminaMaxima()), barHeight);

        shapeRenderer.end();

        // --- 2. Lógica de desenhar os textos ---
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        float textoY = viewport.getWorldHeight() - 80f; // Posição Y inicial
        font.draw(batch, "Nível: " + jogador.getNivel(), 25, textoY);
        font.draw(batch, "XP: " + (int)jogador.getXpAtual() + " / " + jogador.getXpParaProximoNivel(), 25, textoY - 40); // 40 pixels abaixo
        batch.end();
    }

    public Viewport getViewport() {
        return this.viewport;
    }

    public void dispose() {
        shapeRenderer.dispose();
        batch.dispose();
        font.dispose();
    }

    public void resize(int width, int height) {
        viewport.update(width, height, true); // O 'true' centraliza a câmera
    }
}
