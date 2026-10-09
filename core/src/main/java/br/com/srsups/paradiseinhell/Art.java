package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/** Cells of the original 4 x 4 demo atlas. */
public final class Art {
    public static final String ATLAS = "demo-atlas.png";
    private Art() {}

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
