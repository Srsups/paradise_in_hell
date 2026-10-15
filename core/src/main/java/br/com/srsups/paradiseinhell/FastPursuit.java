package br.com.srsups.paradiseinhell;

/** Short, staggered bursts create a chase rhythm without requiring movement boons. */
public final class FastPursuit {
    public static final float BURST_SECONDS = 1, RECOVERY_SECONDS = 1, RECOVERY_SPEED = 24;
    private FastPursuit() { }
    public static float speed(float elapsed, float offset) {
        float cycle = (elapsed + offset) % (BURST_SECONDS + RECOVERY_SECONDS);
        return cycle < BURST_SECONDS ? EnemyType.FAST.speed : RECOVERY_SPEED;
    }
    public static float offset(float x, float y) {
        int hash = Float.floatToIntBits(x) * 31 + Float.floatToIntBits(y);
        hash ^= hash >>> 16;
        return Integer.toUnsignedLong(hash) % 2000 / 1000f;
    }
}

