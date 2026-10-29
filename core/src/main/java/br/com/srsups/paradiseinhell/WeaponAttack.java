package br.com.srsups.paradiseinhell;

/** Melee geometry shared by real combat and headless tests. */
public final class WeaponAttack {
    public static final float SWORD_REACH = 36, HALF_ANGLE_DEGREES = 55;
    public static final float RAKE_HALF_WIDTH = 4;
    private WeaponAttack() {}
    /** The rake is a narrow thrust; the scythe reaches every direction in one swing. */
    public static boolean hits(WeaponType weapon, float x, float y, float directionX, float directionY,
                               float targetX, float targetY, float reach, float targetRadius) {
        if (!weapon.isMelee()) return false;
        if (weapon == WeaponType.RAKE)
            return rakeHits(x, y, directionX, directionY, targetX, targetY, reach, targetRadius);
        if (weapon == WeaponType.SCYTHE) {
            float dx = targetX - x, dy = targetY - y, radius = reach + targetRadius;
            return dx * dx + dy * dy <= radius * radius;
        }
        return meleeHits(x, y, directionX, directionY, targetX, targetY, reach + targetRadius, weapon.halfAngle());
    }
    public static boolean hits(WeaponType weapon, float x, float y, float directionX, float directionY,
                               float targetX, float targetY, float reach, float targetRadius, CombatEffects.LineOfSight line) {
        return hits(weapon, x, y, directionX, directionY, targetX, targetY, reach, targetRadius)
            && line.clear(x, y, targetX, targetY);
    }
    public static boolean rakeHits(float x, float y, float directionX, float directionY,
                                   float targetX, float targetY, float reach, float targetRadius) {
        float length = (float) Math.hypot(directionX, directionY);
        if (length <= .0001f || reach < RAKE_HALF_WIDTH * 2 || targetRadius < 0) return false;
        float ux = directionX / length, uy = directionY / length;
        float dx = targetX - x, dy = targetY - y;
        // The round ends stay within the advertised reach instead of extending it by the tool width.
        float along = Math.max(RAKE_HALF_WIDTH, Math.min(reach - RAKE_HALF_WIDTH, dx * ux + dy * uy));
        float closestX = dx - along * ux, closestY = dy - along * uy;
        float radius = RAKE_HALF_WIDTH + targetRadius;
        return closestX * closestX + closestY * closestY <= radius * radius;
    }
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
