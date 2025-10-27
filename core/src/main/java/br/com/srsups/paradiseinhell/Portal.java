package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

public class Portal {
    public float x, y;
    private TextureRegion textura;
    private Rectangle bounds; // Retângulo para checar a colisão/interação

    public Portal(float x, float y, TextureRegion textura) {
        this.x = x;
        this.y = y;
        this.textura = textura;
        // O retângulo de colisão terá o mesmo tamanho da textura
        this.bounds = new Rectangle(x, y, textura.getRegionWidth(), textura.getRegionHeight());
    }

    public void draw(SpriteBatch batch) {
        batch.draw(textura, x, y);
    }

    public Rectangle getBounds() {
        return bounds;
    }
}
