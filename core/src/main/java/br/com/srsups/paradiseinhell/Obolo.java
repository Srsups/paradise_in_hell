package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Obolo {
    public float x, y;
    private TextureRegion textura;
    public int valor = 1; // Cada Óbolo vale 1 por padrão

    public Obolo(float x, float y, TextureRegion textura) {
        this.x = x;
        this.y = y;
        this.textura = textura;
    }

    public void draw(SpriteBatch batch) {
        // Pinta o batch de dourado antes de desenhar, para diferenciar do orbe de XP
        batch.setColor(Color.GOLD);
        batch.draw(textura, x, y);
        batch.setColor(Color.WHITE); // CRUCIAL: Reseta a cor para não afetar outros sprites
    }
}
