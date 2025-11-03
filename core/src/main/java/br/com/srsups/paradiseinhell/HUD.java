package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class HUD {
    private final ShapeRenderer shapeRenderer;
    private final OrthographicCamera hudCamera;
    private final Viewport hudViewport;
    private float time = 0f;

    public HUD() {
        shapeRenderer = new ShapeRenderer();
        hudCamera = new OrthographicCamera();
        // ScreenViewport faz world units = pixels da tela por padrão (útil para GUI)
        hudViewport = new ScreenViewport(hudCamera);
        // Inicializa o viewport e configure a câmera HUD para unidades em pixels
        float startW = Gdx.graphics.getWidth();
        float startH = Gdx.graphics.getHeight();
        // Atualiza o viewport para as dimensões atuais (define worldWidth/worldHeight)
        hudViewport.update((int) startW, (int) startH, true);
        // Garanta que a câmera ortográfica cubra exatamente a área em pixels
        hudCamera.setToOrtho(false, startW, startH);
        hudCamera.update();
    }

    public void resize(int width, int height) {
        // atualiza viewport do HUD para o novo tamanho da janela/tela
        hudViewport.update(width, height, true);
        // ajuste a câmera ortográfica para corresponder a pixels
        hudCamera.setToOrtho(false, width, height);
        hudCamera.update();
    }

    /**
     * hpFill e staminaFill variam entre 0..1
     */
    public void render(float hpFill, float staminaFill, float xpLevel) {
        time += Gdx.graphics.getDeltaTime();

        // 1) aplica a viewport do HUD (muito importante)
        hudViewport.apply();

        // 2) usa a câmera do HUD (hudCamera) — aqui usamos as dimensões reais da tela (pixels)
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        shapeRenderer.setProjectionMatrix(hudCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Cores
        Color gold = new Color(0.92f, 0.78f, 0.45f, 0.9f);
        Color whiteTrans = new Color(1f, 1f, 1f, 0.25f);
        Color whiteFull = new Color(1f, 1f, 1f, 0.9f);
        Color staminaColor = new Color(0.75f, 0.75f, 1f, 0.8f);

        // Margens e tamanhos proporcionais à tela vista (agora corretos)
        float marginX = w * 0.04f; // 4% da largura
        float marginY = h * 0.04f; // 4% da altura
        float barWidth = w * 0.20f; // 20% da largura da área visível
        float barHeight = h * 0.02f; // 2% da altura
        float spacing = h * 0.012f; // espaçamento

        // Posicionar o círculo NO CANTO SUPERIOR ESQUERDO
        float circleRadius = h * 0.045f;
        float circleX = marginX + circleRadius;           // distância da borda esquerda
        float circleY = h - marginY - circleRadius;       // distância da borda superior

        // contorno dourado e interior
        shapeRenderer.setColor(gold);
        shapeRenderer.circle(circleX, circleY, circleRadius);
        shapeRenderer.setColor(whiteFull);
        shapeRenderer.circle(circleX, circleY, circleRadius * 0.75f);

        // Barras ao lado do círculo
        float barX = circleX + circleRadius + (w * 0.02f);
        float hpBarY = circleY - (barHeight * 0.5f); // alinhamento mais natural
        float staminaBarY = hpBarY - (barHeight + spacing);

        // HP
        shapeRenderer.setColor(whiteTrans);
        shapeRenderer.rect(barX, hpBarY, barWidth, barHeight);
        shapeRenderer.setColor(gold);
        shapeRenderer.rect(barX, hpBarY, barWidth * hpFill, barHeight);

        // Stamina
        shapeRenderer.setColor(whiteTrans);
        shapeRenderer.rect(barX, staminaBarY, barWidth, barHeight);
        shapeRenderer.setColor(staminaColor);
        shapeRenderer.rect(barX, staminaBarY, barWidth * staminaFill, barHeight);

        // Orbe de XP no canto inferior direito (pulsante)
        float xpRadius = h * 0.035f;
        float xpX = w - marginX - xpRadius;
        float xpY = marginY + xpRadius;
        float pulse = 1f + 0.05f * MathUtils.sin(time * 2.5f);

        shapeRenderer.setColor(whiteTrans);
        shapeRenderer.circle(xpX, xpY, xpRadius * pulse);
        shapeRenderer.setColor(gold);
        shapeRenderer.circle(xpX, xpY, xpRadius * 0.75f * pulse);

        shapeRenderer.end();
    }

    public void dispose() {
        shapeRenderer.dispose();
    }

    public Viewport getViewport() {
        return hudViewport;
    }
}
