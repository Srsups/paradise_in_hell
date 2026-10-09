package br.com.srsups.paradiseinhell;

/** Pure pickup movement, shared by normal collection and the magnet upgrade. */
public final class XpAttraction {
    public record Motion(float x, float y, boolean collected) {}
    private XpAttraction() {}
    public static Motion pull(float playerX, float playerY, float orbX, float orbY, float delta, float radius, float speed) {
        float dx = playerX - orbX, dy = playerY - orbY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        if (distance <= radius && distance > 0) {
            float step = Math.min(distance, speed * delta);
            orbX += dx / distance * step; orbY += dy / distance * step;
        }
        dx = playerX - orbX; dy = playerY - orbY;
        return new Motion(orbX, orbY, dx * dx + dy * dy <= 14 * 14);
    }
}
