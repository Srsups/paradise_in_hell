package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class OrbeXP {
    public float x, y;
    public final int valorXP;
    private final TextureRegion textura;
    public OrbeXP(float x, float y, TextureRegion textura) { this(x, y, textura, 1); }
    public OrbeXP(float x, float y, TextureRegion textura, int valorXP) {
        if (valorXP <= 0) throw new IllegalArgumentException("XP must be positive");
        this.x = x; this.y = y; this.textura = textura; this.valorXP = valorXP;
    }
    public void draw(SpriteBatch batch) { batch.draw(textura, x, y, 6, 6); }
}
