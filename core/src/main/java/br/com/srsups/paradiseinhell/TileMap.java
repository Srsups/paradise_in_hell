package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.Camera;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class TileMap {
    private Map<String, Tile> tiles;
    private Texture spritesheet;
    private TextureRegion[] grassTiles;
    private Random random;

    public TileMap() {
        tiles = new HashMap<>();
        random = new Random();
        spritesheet = new Texture("spritesheet.png");

        // Cria os 3 tipos de grama
        grassTiles = new TextureRegion[3];
        grassTiles[0] = new TextureRegion(spritesheet, 278, 209, 16, 16);
        grassTiles[1] = new TextureRegion(spritesheet, 295, 209, 16, 16);
        grassTiles[2] = new TextureRegion(spritesheet, 244, 226, 16, 16);
    }

    public void update(Camera camera) {
        int camLeft = (int)(camera.position.x - camera.viewportWidth / 2) / Tile.TILE_SIZE - 1;
        int camRight = (int)(camera.position.x + camera.viewportWidth / 2) / Tile.TILE_SIZE + 1;
        int camBottom = (int)(camera.position.y - camera.viewportHeight / 2) / Tile.TILE_SIZE - 1;
        int camTop = (int)(camera.position.y + camera.viewportHeight / 2) / Tile.TILE_SIZE + 1;

        for (int x = camLeft; x <= camRight; x++) {
            for (int y = camBottom; y <= camTop; y++) {
                String key = x + "," + y;
                if (!tiles.containsKey(key)) {
                    // Sorteia um dos 3 tipos de tile
                    TextureRegion tile = grassTiles[random.nextInt(grassTiles.length)];
                    tiles.put(key, new Tile(x, y, tile));
                }
            }
        }
    }

    public void draw(SpriteBatch batch) {
        for (Tile tile : tiles.values()) {
            tile.draw(batch);
        }
    }

    public void dispose() {
        spritesheet.dispose();
    }
}
