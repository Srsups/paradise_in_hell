package br.com.srsups.paradiseinhell;

/** Legacy procedural obstacles, independent of artwork and cached tile objects. */
public final class TileLayout {
    private static final long MULTIPLIER = 0x5DEECE66DL, ADDEND = 11, MASK = (1L << 48) - 1;
    private TileLayout() {}

    /** Reproduces new Random(coordinateSeed).nextInt(3); nextFloat() < .012f without allocation. */
    public static boolean solid(long seed, int tileX, int tileY) {
        long coordinateSeed = seed ^ (tileX * 73856093L) ^ (tileY * 19349663L);
        long state = (coordinateSeed ^ MULTIPLIER) & MASK;
        int bits, value;
        // Random.nextInt(3) can reject its first draw; skipping exactly one LCG step is not equivalent.
        do {
            state = (state * MULTIPLIER + ADDEND) & MASK;
            bits = (int) (state >>> 17);
            value = bits % 3;
        } while (bits - value + 2 < 0);
        state = (state * MULTIPLIER + ADDEND) & MASK;
        return ((int) (state >>> 24) / 0x1.0p24f) < .012f;
    }
}
