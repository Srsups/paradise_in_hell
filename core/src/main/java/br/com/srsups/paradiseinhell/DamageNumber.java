package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;

public class DamageNumber {
    public float x, y;
    public String texto;
    private float tempoDeVida = 0.7f; // O número some após 0.7 segundos
    private float velocidadeY = 50f;  // Velocidade com que sobe

    public DamageNumber(String texto, float x, float y, Color yellow) {
        this.texto = texto;
        this.x = x;
        this.y = y;
    }

    public void update(float delta) {
        y += velocidadeY * delta; // Move para cima
        tempoDeVida -= delta;     // Diminui o tempo de vida
    }

    public boolean deveSerRemovido() {
        return tempoDeVida <= 0;
    }

    public void draw(SpriteBatch batch, BitmapFont font) {
        // Desenha o texto centralizado na posição X
        font.draw(batch, texto, x, y, 0, Align.center, false);
    }
}
