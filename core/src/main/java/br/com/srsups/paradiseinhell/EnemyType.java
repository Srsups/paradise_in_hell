package br.com.srsups.paradiseinhell;

/** Shared combat, XP and kill-goal identity. XP values are initial balance settings. */
public enum EnemyType {
    COMMON("Comuns", 10, 30, 8, 16, 1),
    FAST("Rápidos", 6, 70, 6, 16, 2),
    TANK("Tanks", 30, 22, 16, 21, 5);

    public final String label;
    public final float health, speed, size;
    public final int damage, xp;
    EnemyType(String label, float health, float speed, int damage, float size, int xp) {
        this.label = label; this.health = health; this.speed = speed;
        this.damage = damage; this.size = size; this.xp = xp;
    }
}
