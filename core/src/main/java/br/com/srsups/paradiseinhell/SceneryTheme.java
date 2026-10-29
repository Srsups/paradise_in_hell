package br.com.srsups.paradiseinhell;

import java.util.Objects;

/** Presentation choices only: no textures, collision changes or gameplay random draws. */
public enum SceneryTheme {
    REFUGE("refuge", "Refúgio do Estige", 0x524546554745L),
    STYX("styx", "Margem do Estige", 0x53545958L),
    ASHES("ashes", "Caminho das Cinzas", 0x4153484553L),
    SHADOWS("shadows", "Pátio das Sombras", 0x534841444F5753L),
    GUARDIAN("guardian", "Sala do Guardião", 0x475541524449414EL);

    private final String key, title;
    private final long salt;
    SceneryTheme(String key, String title, long salt) {
        this.key = key;
        this.title = title;
        this.salt = salt;
    }
    public String key() { return key; }
    public String title() { return title; }
    public static SceneryTheme forSession(RunSession session) {
        Objects.requireNonNull(session);
        return session.isDemo() ? STYX : forArena(session.arena());
    }
    public static SceneryTheme forArena(ArenaDefinition arena) {
        Objects.requireNonNull(arena);
        if (arena.kind() == ArenaDefinition.Kind.BOSS_ROOM) return GUARDIAN;
        return switch (arena.id()) {
            case "cinzas" -> ASHES;
            case "sombras" -> SHADOWS;
            default -> STYX;
        };
    }
    /** Stable across eviction, visit order, negative coordinates and enum reordering. */
    public int variant(long seed, int tileX, int tileY, int count) {
        if (count < 1) throw new IllegalArgumentException("Invalid scenery variant count");
        long mixed = seed ^ salt ^ ((long) tileX * 0x9E3779B97F4A7C15L)
            ^ ((long) tileY * 0xC2B2AE3D27D4EB4FL);
        mixed = (mixed ^ (mixed >>> 30)) * 0xBF58476D1CE4E5B9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94D049BB133111EBL;
        mixed ^= mixed >>> 31;
        return (int) Math.floorMod(mixed, (long) count);
    }
    /** Three quiet base tiles; the other three are accents with a 4% selection chance. */
    public int floorVariant(long seed, int tileX, int tileY) {
        if (variant(seed ^ 0xA0761D6478BD642FL, tileX, tileY, 1000) < 40)
            return 3 + variant(seed ^ 0xE7037ED1A0B428DBL, tileX, tileY, 3);
        return variant(seed ^ 0x8EBC6AF09C88C6E3L, tileX, tileY, 3);
    }
    public int obstacleVariant(long seed, int tileX, int tileY) {
        return variant(seed ^ 0x589965CC75374CC3L, tileX, tileY, 3);
    }
}
