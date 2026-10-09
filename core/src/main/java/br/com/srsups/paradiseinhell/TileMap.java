package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class TileMap {
    private final Map<String, Tile> tiles = new HashMap<>();
    private final TextureRegion[] floors;
    private final TextureRegion obstacle;
    private final long seed = new Random().nextLong();
    private int safeX, safeY, safeRadius;
    private int left, right, bottom, top;
    private Rectangle bounds;

    public TileMap(Texture atlas) {
        floors = new TextureRegion[] { Art.cell(atlas, 0, 2), Art.cell(atlas, 1, 2), Art.cell(atlas, 2, 2) };
        obstacle = Art.cell(atlas, 3, 2);
    }
    private Tile tileAt(int x, int y) {
        String key = x + "," + y;
        return tiles.computeIfAbsent(key, ignored -> {
            // Coordinate-based randomness preserves scenery after eviction from the cache.
            Random random = new Random(seed ^ (x * 73856093L) ^ (y * 19349663L));
            TextureRegion floor = floors[random.nextInt(floors.length)];
            boolean safe = Math.abs((long) x - safeX) <= safeRadius && Math.abs((long) y - safeY) <= safeRadius;
            boolean solid = !safe && random.nextFloat() < 0.012f;
            return new Tile(x, y, solid ? obstacle : floor, solid);
        });
    }
    public boolean ehSolido(float x, float y) {
        if (bounds != null && !bounds.contains(x, y)) return true;
        return tileAt((int) Math.floor(x / Tile.TILE_SIZE), (int) Math.floor(y / Tile.TILE_SIZE)).solido;
    }
    public void setBounds(float x, float y, float width, float height) {
        bounds = new Rectangle(x, y, width, height);
    }
    public void update(Camera camera) {
        left = (int) Math.floor((camera.position.x - camera.viewportWidth / 2) / Tile.TILE_SIZE) - 2;
        right = (int) Math.floor((camera.position.x + camera.viewportWidth / 2) / Tile.TILE_SIZE) + 2;
        bottom = (int) Math.floor((camera.position.y - camera.viewportHeight / 2) / Tile.TILE_SIZE) - 2;
        top = (int) Math.floor((camera.position.y + camera.viewportHeight / 2) / Tile.TILE_SIZE) + 2;
        tiles.values().removeIf(tile -> tile.gridX < left - 8 || tile.gridX > right + 8 || tile.gridY < bottom - 8 || tile.gridY > top + 8);
        for (int x = left; x <= right; x++) for (int y = bottom; y <= top; y++) tileAt(x, y);
    }
    public void draw(SpriteBatch batch) {
        for (int x = left; x <= right; x++) for (int y = bottom; y <= top; y++) {
            // Obstacles sit over a floor tile so their transparent corners are filled.
            Tile tile = tileAt(x, y);
            if (tile.solido) batch.draw(floors[0], x * Tile.TILE_SIZE, y * Tile.TILE_SIZE, Tile.TILE_SIZE, Tile.TILE_SIZE);
            tile.draw(batch);
        }
    }
    public void gerarAreaInicialSegura(float x, float y, int radius) {
        safeX = (int) Math.floor(x / Tile.TILE_SIZE);
        safeY = (int) Math.floor(y / Tile.TILE_SIZE);
        safeRadius = radius;
        tiles.clear();
    }
    public void dispose() { tiles.clear(); }
}
