package br.com.srsups.paradiseinhell;

/** Sword geometry shared by real combat and headless tests. */
public final class WeaponAttack {
    public static final float SWORD_REACH = 36, HALF_ANGLE_DEGREES = 55;
    private WeaponAttack() {}
    public static boolean swordHits(float x, float y, float directionX, float directionY, float targetX, float targetY) {
        return swordHits(x, y, directionX, directionY, targetX, targetY, SWORD_REACH);
    }
    public static boolean swordHits(float x, float y, float directionX, float directionY, float targetX, float targetY, float reach) {
        return meleeHits(x, y, directionX, directionY, targetX, targetY, reach, HALF_ANGLE_DEGREES);
    }
    public static boolean meleeHits(float x, float y, float directionX, float directionY, float targetX, float targetY,
                                   float reach, float halfAngleDegrees) {
        float dx = targetX - x, dy = targetY - y;
        float distanceSquared = dx * dx + dy * dy;
        if (distanceSquared > reach * reach) return false;
        if (distanceSquared < 64) return true;
        float directionLength = (float) Math.sqrt(directionX * directionX + directionY * directionY);
        return directionLength > 0 && (dx * directionX + dy * directionY)
            / ((float) Math.sqrt(distanceSquared) * directionLength) >= (float) Math.cos(Math.toRadians(halfAngleDegrees));
    }
}
