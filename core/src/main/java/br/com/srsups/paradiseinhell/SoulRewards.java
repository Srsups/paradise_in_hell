package br.com.srsups.paradiseinhell;

/** Initial account-wide economy; independent of XP, luck and the character's Obols. */
public final class SoulRewards {
    public static final int MINI_BOSS=20, FLOOR_BOSS=100;
    private SoulRewards() { }
    public static int enemy(EnemyType type) { return switch(type) {case COMMON->1;case FAST->2;case TANK->5;}; }
}

