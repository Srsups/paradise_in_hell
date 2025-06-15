package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Tile {
    public static final int TILE_SIZE = 16; // agora é 16x16

    public int gridX, gridY;
    private TextureRegion tileRegion;
    public boolean solido;

    public Tile(int gridX, int gridY, TextureRegion region, boolean solido) {
        this.gridX = gridX;
        this.gridY = gridY;
        this.tileRegion = region;
        this.solido = solido;
    }

    public void draw(SpriteBatch batch) {
        batch.draw(tileRegion, gridX * TILE_SIZE, gridY * TILE_SIZE);
    }
}
