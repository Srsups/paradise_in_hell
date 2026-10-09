package br.com.srsups.paradiseinhell;

/** Swept collision for projectiles and a moving boss; independent of graphics. */
public final class CombatGeometry {
    private CombatGeometry() {}
    public static float segmentDistanceSquared(float x, float y, float startX, float startY, float endX, float endY) {
        float dx = endX - startX, dy = endY - startY, lengthSquared = dx * dx + dy * dy;
        float amount = lengthSquared == 0 ? 0 : Math.max(0, Math.min(1, ((x - startX) * dx + (y - startY) * dy) / lengthSquared));
        float offsetX = x - (startX + dx * amount), offsetY = y - (startY + dy * amount);
        return offsetX * offsetX + offsetY * offsetY;
    }
    public static boolean sweptCircleHits(float startX, float startY, float endX, float endY,
                                          float targetX, float targetY, float radius) {
        return segmentDistanceSquared(targetX, targetY, startX, startY, endX, endY) <= radius * radius;
    }
}
