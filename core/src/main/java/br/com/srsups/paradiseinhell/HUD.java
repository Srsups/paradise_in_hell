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

    // Calcula tamanhos proporcionais baseados na viewport da HUD
    float barX = viewport.getWorldWidth() * 0.02f; // 2% da largura
    float barWidth = viewport.getWorldWidth() * 0.25f; // 25% da largura
    float barHeight = viewport.getWorldHeight() * 0.035f; // 3.5% da altura
    float padding = viewport.getWorldHeight() * 0.015f;
    float barYVida = viewport.getWorldHeight() - barHeight - padding;

    // Barra de Vida (suavizada)
    shapeRenderer.setColor(Color.DARK_GRAY);
    float vidaReal = jogador.getVida();
    vidaExibida = MathUtils.lerp(vidaExibida, vidaReal, 0.12f); // suaviza
    float pct = vidaExibida / jogador.getVidaMaxima();

    // Fundo da barra
    shapeRenderer.setColor(Color.DARK_GRAY);
    shapeRenderer.rect(barX, barYVida, barWidth, barHeight);

    // Barra suavizada (efeito visual leve indicando mudança)
    shapeRenderer.setColor(new Color(1f, 0.85f, 0f, 0.35f));
    shapeRenderer.rect(barX, barYVida, barWidth * pct, barHeight);

    // Barra real (vida atual)
    shapeRenderer.setColor(Color.RED);
    shapeRenderer.rect(barX, barYVida, barWidth * (jogador.getVida() / jogador.getVidaMaxima()), barHeight);

    // Barra de Estamina (abaixo da barra de vida)
    float barYEstamina = barYVida - barHeight - padding;
    shapeRenderer.setColor(Color.DARK_GRAY);
    shapeRenderer.rect(barX, barYEstamina, barWidth, barHeight);
    shapeRenderer.setColor(jogador.isExausto() ? Color.FIREBRICK : Color.GREEN);
    shapeRenderer.rect(barX, barYEstamina, barWidth * (jogador.getEstaminaAtual() / jogador.getEstaminaMaxima()), barHeight);

    shapeRenderer.end();

        // --- 2. Lógica de desenhar os textos ---
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        // Posiciona os textos próximos às barras, em unidades da viewport
        float textoX = barX + barWidth + (viewport.getWorldWidth() * 0.02f);
        float textoY = barYVida + (barHeight * 0.75f);
        font.draw(batch, "Nível: " + jogador.getNivel(), textoX, textoY);
        font.draw(batch, "XP: " + (int)jogador.getXpAtual() + " / " + jogador.getXpParaProximoNivel(), textoX, textoY - (barHeight + padding));
        batch.end();
    }

    public Viewport getViewport() {
        return this.viewport;
    }

    public void dispose() {
        // Não descartar objetos que foram passados pelo construtor (são geralmente compartilhados pelo jogo)
        // Se HUD criar seus próprios recursos no futuro, então devem ser descartados aqui.
    }

    public void resize(int width, int height) {
        viewport.update(width, height, true); // O 'true' centraliza a câmera
        // Ajusta escala da fonte para manter proporção relativa à resolução base (1920x1080)
        float scale = viewport.getWorldWidth() / 1920f;
        font.getData().setScale(scale);
    }
}
