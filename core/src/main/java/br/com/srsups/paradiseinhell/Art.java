package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.TextureData;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/** Cells of the original 4 x 4 demo atlas. */
public final class Art {
    public static final String ATLAS = "demo-atlas.png";
    private Art() {}
    private record RegionKey(int x, int y, int width, int height) { }
    private static final Map<Texture, Map<RegionKey, SpriteHitbox.Shape>> BODY_SHAPES = new WeakHashMap<>();

    public static SpriteHitbox hitbox(TextureRegion frame) {
        Texture atlas = frame.getTexture();
        Map<RegionKey, SpriteHitbox.Shape> shapes = BODY_SHAPES.get(atlas);
        if (shapes == null) {
            shapes = new HashMap<>();
            TextureData data = atlas.getTextureData();
            if (!data.isPrepared()) data.prepare();
            Pixmap pixels = data.consumePixmap();
            try {
                for (int row = 0; row < 2; row++) for (int column = 0; column < 4; column++) {
                    TextureRegion cell = cell(atlas, column, row);
                    int x = cell.getRegionX(), y = cell.getRegionY();
                    RegionKey key = new RegionKey(x, y, cell.getRegionWidth(), cell.getRegionHeight());
                    shapes.put(key, SpriteHitbox.Shape.fromAlpha(key.width, key.height,
                        (px, py) -> pixels.getPixel(x + px, y + py) & 255));
                }
            } finally { if (data.disposePixmap()) pixels.dispose(); }
            BODY_SHAPES.put(atlas, shapes);
        }
        RegionKey key = new RegionKey(frame.getRegionX() - (frame.isFlipX() ? frame.getRegionWidth() : 0),
            frame.getRegionY() - (frame.isFlipY() ? frame.getRegionHeight() : 0), frame.getRegionWidth(), frame.getRegionHeight());
        SpriteHitbox.Shape shape = shapes.get(key);
        if (shape == null) throw new IllegalArgumentException("Not an actor frame");
        return new SpriteHitbox(shape);
    }

    public static TextureRegion cell(Texture texture, int column, int row) {
        int x = column * texture.getWidth() / 4;
        int y = row * texture.getHeight() / 4;
        int width = (column + 1) * texture.getWidth() / 4 - x;
        int height = (row + 1) * texture.getHeight() / 4 - y;
        // Trim the thin border of floor illustrations to keep tiles continuous.
        if (row == 2 && column < 3) {
            int inset = Math.max(1, width / 40);
            return new TextureRegion(texture, x + inset, y + inset, width - inset * 2, height - inset * 2);
        }
        return new TextureRegion(texture, x, y, width, height);
    }
}
