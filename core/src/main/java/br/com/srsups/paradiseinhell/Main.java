package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;

public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    // Agora a classe Main tem uma referência para um objeto Jogador
    private Jogador jogador;

    @Override
    public void create () {
        batch = new SpriteBatch();
        // Cria uma nova instância do jogador na posição x=200, y=150
        jogador = new Jogador(200, 150);
    }

    @Override
    public void render () {
        float delta = Gdx.graphics.getDeltaTime();

        // Limpa a tela
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Atualiza posição e estado do jogador
        jogador.update(delta);
        tratarInput(jogador, delta);

        // Desenha o jogador
        batch.begin();
        jogador.draw(batch);
        batch.end();
    }

    //Lógica de movimento
    private void tratarInput(Jogador jogador, float delta) {
        float velocidade = 100f;

        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            jogador.y += velocidade * delta;
            jogador.setEstado(Jogador.Estado.COSTAS);
        } else if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            jogador.y -= velocidade * delta;
            jogador.setEstado(Jogador.Estado.FRENTE);
        } else if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            jogador.x -= velocidade * delta;
            jogador.setEstado(Jogador.Estado.LADO_ESQUERDO);
        } else if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            jogador.x += velocidade * delta;
            jogador.setEstado(Jogador.Estado.LADO_DIREITO);
        } else {
            jogador.setEstado(Jogador.Estado.PARADO);
        }
    }

    @Override
    public void dispose () {
        batch.dispose();
        // Pede para o jogador liberar seus recursos (a textura)
        jogador.dispose();
    }
}
