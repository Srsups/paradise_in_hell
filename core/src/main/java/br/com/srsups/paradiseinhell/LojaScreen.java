// Em LojaScreen.java
package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class LojaScreen implements Screen {

    private Main game;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private OrthographicCamera uiCamera;
    private Viewport viewport;

    private int totalObolos;
    private int nivelUpgradeVida;
    private int custoUpgradeVida;
    private int nivelUpgradeDano;
    private int custoUpgradeDano;
    private int nivelUpgradeVelocidade;
    private int custoUpgradeVelocidade;

    private Rectangle botaoComprarVida;
    private Rectangle botaoComprarDano;
    private Rectangle botaoComprarVelocidade;
    private Rectangle botaoVoltar;

    public LojaScreen(Main game) {
        this.game = game;
        // Pega as referências dos recursos compartilhados
        this.batch = game.batch;
        this.shapeRenderer = game.shapeRenderer;
        this.font = game.font;

        // Configura a Viewport para a UI
        uiCamera = new OrthographicCamera();
        viewport = new FitViewport(1920, 1080, uiCamera);
    }

    @Override
    public void show() {
        font.getData().setScale(2f); // Aumenta a escala da fonte para a nova resolução

        // Carrega os dados salvos
        Preferences prefs = Gdx.app.getPreferences("ParadiseInHellSave");
        totalObolos = prefs.getInteger("total_moedas", 0);
        nivelUpgradeVida = prefs.getInteger("upgrade_vida_nivel", 0);
        nivelUpgradeDano = prefs.getInteger("upgrade_dano_nivel", 0);
        nivelUpgradeVelocidade = prefs.getInteger("upgrade_velocidade_nivel", 0);

        atualizarCustos();

        // Define os botões da UI usando as coordenadas da Viewport
        float centerX = viewport.getWorldWidth() / 2f;
        float startY = viewport.getWorldHeight() * 0.7f;
        float buttonWidth = 600f;
        float buttonHeight = 100f;
        float spacing = 40f;

        botaoComprarVida = new Rectangle(centerX - buttonWidth / 2f, startY, buttonWidth, buttonHeight);
        botaoComprarDano = new Rectangle(centerX - buttonWidth / 2f, startY - (buttonHeight + spacing), buttonWidth, buttonHeight);
        botaoComprarVelocidade = new Rectangle(centerX - buttonWidth / 2f, startY - 2 * (buttonHeight + spacing), buttonWidth, buttonHeight);
        botaoVoltar = new Rectangle(50, 50, 200, 80);
    }

    private void atualizarCustos() {
        custoUpgradeVida = 100 + (nivelUpgradeVida * 150);
        custoUpgradeDano = 100 + (nivelUpgradeDano * 150);
        custoUpgradeVelocidade = 100 + (nivelUpgradeVelocidade * 150);
    }

    private void tentarCompra(String tipo) {
        Preferences prefs = Gdx.app.getPreferences("ParadiseInHellSave");
        boolean comprou = false;

        if (tipo.equals("vida") && totalObolos >= custoUpgradeVida) {
            totalObolos -= custoUpgradeVida;
            nivelUpgradeVida++;
            prefs.putInteger("upgrade_vida_nivel", nivelUpgradeVida);
            comprou = true;
        } else if (tipo.equals("dano") && totalObolos >= custoUpgradeDano) {
            totalObolos -= custoUpgradeDano;
            nivelUpgradeDano++;
            prefs.putInteger("upgrade_dano_nivel", nivelUpgradeDano);
            comprou = true;
        } else if (tipo.equals("velocidade") && totalObolos >= custoUpgradeVelocidade) {
            totalObolos -= custoUpgradeVelocidade;
            nivelUpgradeVelocidade++;
            prefs.putInteger("upgrade_velocidade_nivel", nivelUpgradeVelocidade);
            comprou = true;
        }

        if (comprou) {
            prefs.putInteger("total_moedas", totalObolos);
            prefs.flush();
            atualizarCustos();
            System.out.println("Upgrade " + tipo + " comprado!");
        } else {
            System.out.println("Óbolos insuficientes para " + tipo + "!");
        }
    }

    @Override
    public void render(float delta) {
        viewport.apply();

        // Lógica de Input
        if (Gdx.input.justTouched()) {
            Vector3 touchPos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            viewport.unproject(touchPos); // Converte as coordenadas do clique

            if (botaoVoltar.contains(touchPos.x, touchPos.y)) {
                game.setScreen(new MenuScreen(game));
            }
            if (botaoComprarVida.contains(touchPos.x, touchPos.y)) {
                tentarCompra("vida");
            }
            if (botaoComprarDano.contains(touchPos.x, touchPos.y)) {
                tentarCompra("dano");
            }
            if (botaoComprarVelocidade.contains(touchPos.x, touchPos.y)) {
                tentarCompra("velocidade");
            }
        }

        // Lógica de Desenho
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(uiCamera.combined);
        shapeRenderer.setProjectionMatrix(uiCamera.combined);

        // Desenha os retângulos dos botões
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.GRAY);
        shapeRenderer.rect(botaoComprarVida.x, botaoComprarVida.y, botaoComprarVida.width, botaoComprarVida.height);
        shapeRenderer.rect(botaoComprarDano.x, botaoComprarDano.y, botaoComprarDano.width, botaoComprarDano.height);
        shapeRenderer.rect(botaoComprarVelocidade.x, botaoComprarVelocidade.y, botaoComprarVelocidade.width, botaoComprarVelocidade.height);
        shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(botaoVoltar.x, botaoVoltar.y, botaoVoltar.width, botaoVoltar.height);
        shapeRenderer.end();

        // Desenha os textos
        batch.begin();
        font.setColor(Color.GOLD);
        font.draw(batch, "Óbolos: " + totalObolos, viewport.getWorldWidth() - 400, viewport.getWorldHeight() - 50);

        font.setColor(Color.WHITE);
        font.draw(batch, "Loja de Melhorias", viewport.getWorldWidth() / 2f - 250, viewport.getWorldHeight() - 50);

        String textoUpgradeVida = String.format("Vida Máxima Nvl %d (Custo: %d)", nivelUpgradeVida + 1, custoUpgradeVida);
        String textoUpgradeDano = String.format("Dano Base Nvl %d (Custo: %d)", nivelUpgradeDano + 1, custoUpgradeDano);
        String textoUpgradeVelocidade = String.format("Velocidade Nvl %d (Custo: %d)", nivelUpgradeVelocidade + 1, custoUpgradeVelocidade);

        font.draw(batch, textoUpgradeVida, botaoComprarVida.x + 40, botaoComprarVida.y + 65);
        font.draw(batch, textoUpgradeDano, botaoComprarDano.x + 40, botaoComprarDano.y + 65);
        font.draw(batch, textoUpgradeVelocidade, botaoComprarVelocidade.x + 40, botaoComprarVelocidade.y + 65);
        font.draw(batch, "Voltar", botaoVoltar.x + 45, botaoVoltar.y + 55);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    // ... (pause, resume, hide, dispose) ...
    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }
    @Override public void dispose() { }
}
