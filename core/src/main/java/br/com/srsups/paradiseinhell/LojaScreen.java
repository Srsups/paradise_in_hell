package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;

public class LojaScreen implements Screen {

    private Game game;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;

    private int totalObolos;
    private int nivelUpgradeVida;
    private int custoUpgradeVida;

    private Rectangle botaoComprarVida;
    private Rectangle botaoVoltar;

    public LojaScreen(Game game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();
        font.getData().setScale(1.5f);

        // Carrega os dados salvos
        Preferences prefs = Gdx.app.getPreferences("ParadiseInHellSave");
        totalObolos = prefs.getInteger("total_moedas", 0);
        nivelUpgradeVida = prefs.getInteger("upgrade_vida_nivel", 0);

        // Lógica de custo (ex: 100, 250, 500...)
        custoUpgradeVida = 100 + (nivelUpgradeVida * 150);

        // Define os botões da UI
        botaoComprarVida = new Rectangle(Gdx.graphics.getWidth() / 2f - 150, Gdx.graphics.getHeight() / 2f, 300, 50);
        botaoVoltar = new Rectangle(20, 20, 100, 40);
    }

    @Override
    public void render(float delta) {
        // Lógica de Input
        if (Gdx.input.justTouched()) {
            Vector3 touchPos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            // A câmera da UI não é invertida, então não precisa ajustar o Y

            if (botaoVoltar.contains(touchPos.x, Gdx.graphics.getHeight() - touchPos.y)) {
                game.setScreen(new MenuScreen(game));
            }

            if (botaoComprarVida.contains(touchPos.x, Gdx.graphics.getHeight() - touchPos.y)) {
                // Tenta comprar o upgrade
                if (totalObolos >= custoUpgradeVida) {
                    // Deduz o custo
                    totalObolos -= custoUpgradeVida;
                    nivelUpgradeVida++;

                    // Salva o progresso
                    Preferences prefs = Gdx.app.getPreferences("ParadiseInHellSave");
                    prefs.putInteger("total_moedas", totalObolos);
                    prefs.putInteger("upgrade_vida_nivel", nivelUpgradeVida);
                    prefs.flush();

                    // Atualiza o custo para o próximo nível
                    custoUpgradeVida = 100 + (nivelUpgradeVida * 150);
                    System.out.println("Upgrade comprado! Novo nível: " + nivelUpgradeVida);
                } else {
                    System.out.println("Óbolos insuficientes!");
                }
            }
        }

        // Lógica de Desenho
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        font.setColor(Color.GOLD);
        font.draw(batch, "Óbolos: " + totalObolos, Gdx.graphics.getWidth() - 200, Gdx.graphics.getHeight() - 30);

        font.setColor(Color.WHITE);
        font.draw(batch, "Loja de Melhorias", Gdx.graphics.getWidth() / 2f - 100, Gdx.graphics.getHeight() - 50);
        batch.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.GRAY);
        shapeRenderer.rect(botaoComprarVida.x, botaoComprarVida.y, botaoComprarVida.width, botaoComprarVida.height);
        shapeRenderer.rect(botaoVoltar.x, botaoVoltar.y, botaoVoltar.width, botaoVoltar.height);
        shapeRenderer.end();

        batch.begin();
        String textoUpgrade = String.format("Vida Máxima Nvl %d (Custo: %d)", nivelUpgradeVida + 1, custoUpgradeVida);
        font.draw(batch, textoUpgrade, botaoComprarVida.x + 20, botaoComprarVida.y + 35);
        font.draw(batch, "Voltar", botaoVoltar.x + 20, botaoVoltar.y + 30);
        batch.end();
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
        batch.dispose();
        shapeRenderer.dispose();
        font.dispose();
    }
}
