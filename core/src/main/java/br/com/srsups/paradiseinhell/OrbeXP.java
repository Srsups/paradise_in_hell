package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class OrbeXP {
    public float x, y;
    public int valorXP = 1; // Cada orbe vale 1 de XP
    private TextureRegion textura;

    public OrbeXP(float x, float y, TextureRegion textura) {
        this.x = x;
        this.y = y;
        this.textura = textura;
    }

    public void draw(SpriteBatch batch) {
        batch.draw(textura, x, y);
    }
}
