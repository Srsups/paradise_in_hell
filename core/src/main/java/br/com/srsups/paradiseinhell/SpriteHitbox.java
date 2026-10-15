package br.com.srsups.paradiseinhell;

import java.util.function.IntBinaryOperator;

/** Cached opacity mask mapped to the same rectangle and flips used to draw a frame. */
public final class SpriteHitbox {
    private static final int GRID = 64;
    public static final class Shape {
        private final long[] rows = new long[GRID];
        private int minX = GRID, minY = GRID, maxX, maxY;
        public static Shape fromAlpha(int width, int height, IntBinaryOperator alpha) {
            Shape shape = new Shape();
            for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
                if (alpha.applyAsInt(x, y) < 96) continue;
                int gx = x * GRID / width, gy = y * GRID / height;
                int ex = Math.min(GRID, ((x + 1) * GRID + width - 1) / width);
                int ey = Math.min(GRID, ((y + 1) * GRID + height - 1) / height);
                for (int my = gy; my < ey; my++) for (int mx = gx; mx < ex; mx++) shape.rows[my] |= 1L << mx;
                shape.minX = Math.min(shape.minX, gx); shape.maxX = Math.max(shape.maxX, ex);
                shape.minY = Math.min(shape.minY, gy); shape.maxY = Math.max(shape.maxY, ey);
            }
            return shape;
        }
    }
    private final Shape shape;
    private float x, y, width, height;
    private boolean flipX, flipY;
    public SpriteHitbox(Shape shape) { this.shape = shape; }
    public SpriteHitbox place(float x, float y, float width, float height, boolean flipX, boolean flipY) {
        this.x = x; this.y = y; this.width = width; this.height = height;
        this.flipX = flipX; this.flipY = flipY;
        return this;
    }
    public float left() { return x + width * (flipX ? GRID - shape.maxX : shape.minX) / GRID; }
    public float right() { return x + width * (flipX ? GRID - shape.minX : shape.maxX) / GRID; }
    public float bottom() { return y + height * (flipY ? shape.minY : GRID - shape.maxY) / GRID; }
    public float top() { return y + height * (flipY ? shape.maxY : GRID - shape.minY) / GRID; }
    public boolean contains(float px, float py) {
        float u = (px - x) / width, v = (py - y) / height;
        if (u < 0 || u >= 1 || v < 0 || v >= 1) return false;
        int gx = Math.min(GRID - 1, (int) (u * GRID)), gy = Math.min(GRID - 1, (int) (v * GRID));
        if (flipX) gx = GRID - 1 - gx;
        if (!flipY) gy = GRID - 1 - gy;
        return (shape.rows[gy] & (1L << gx)) != 0;
    }
    /** True only if an opaque mask cell intersects this visible world rectangle. */
    public boolean visibleWithin(float left,float bottom,float right,float top) {
        if(width<=0 || height<=0 || right<=left || top<=bottom
                || right<=left() || left>=right() || top<=bottom() || bottom>=top())return false;
        int fromX=Math.max(0,(int)Math.floor((left-x)/width*GRID));
        int toX=Math.min(GRID,(int)Math.ceil((right-x)/width*GRID));
        int fromY=Math.max(0,(int)Math.floor((bottom-y)/height*GRID));
        int toY=Math.min(GRID,(int)Math.ceil((top-y)/height*GRID));
        for(int gy=fromY;gy<toY;gy++) {
            int imageY=flipY?gy:GRID-1-gy;
            for(int gx=fromX;gx<toX;gx++)
                if((shape.rows[imageY] & (1L << (flipX?GRID-1-gx:gx)))!=0)return true;
        }
        return false;
    }
    public boolean overlaps(SpriteHitbox other) {
        float left = Math.max(left(), other.left()), right = Math.min(right(), other.right());
        float bottom = Math.max(bottom(), other.bottom()), top = Math.min(top(), other.top());
        if (left >= right || bottom >= top) return false;
        // Subpixel sampling only after the cheap bounds check; transparent corners never deal contact damage.
        for (float py = bottom + Math.min(.125f, (top - bottom) / 2); py < top; py += .25f)
            for (float px = left + Math.min(.125f, (right - left) / 2); px < right; px += .25f)
                if (contains(px, py) && other.contains(px, py)) return true;
        return false;
    }
    public boolean sweptHits(float startX, float startY, float endX, float endY, float radius) {
        if (Math.max(startX, endX) + radius < left() || Math.min(startX, endX) - radius > right()
            || Math.max(startY, endY) + radius < bottom() || Math.min(startY, endY) - radius > top()) return false;
        int steps = Math.max(1, (int) Math.ceil(Math.hypot(endX - startX, endY - startY) / .5));
        for (int i = 0; i <= steps; i++) {
            float px = startX + (endX - startX) * i / steps, py = startY + (endY - startY) * i / steps;
            if (contains(px, py) || contains(px + radius, py) || contains(px - radius, py)
                || contains(px, py + radius) || contains(px, py - radius)
                || contains(px + radius * .7071f, py + radius * .7071f)
                || contains(px - radius * .7071f, py + radius * .7071f)
                || contains(px + radius * .7071f, py - radius * .7071f)
                || contains(px - radius * .7071f, py - radius * .7071f)) return true;
        }
        return false;
    }
}

