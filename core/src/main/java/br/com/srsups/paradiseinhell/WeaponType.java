package br.com.srsups.paradiseinhell;

public enum WeaponType {
    BOW("Arco e flecha", 5, 0.65f, 0.15f, 300, 5, 16),
    SWORD("Espada", 8, 0.6f, 0.15f, 0, 0, 0),
    PISTOL("Pistola", 2, 0.25f, 0.08f, 420, 3, 7),
    FISTS("Socos", 4, 0.30f, 0.08f, 0, 0, 0);

    public boolean isMelee() { return this == SWORD || this == FISTS; }
    public float halfAngle() { return this == FISTS ? 40 : WeaponAttack.HALF_ANGLE_DEGREES; }
    public final String label;
    public final int damage;
    public final float interval, minimumInterval, projectileSpeed, projectileWidth, projectileHeight;
    WeaponType(String label, int damage, float interval, float minimumInterval,
               float projectileSpeed, float projectileWidth, float projectileHeight) {
        this.label = label; this.damage = damage; this.interval = interval; this.minimumInterval = minimumInterval;
        this.projectileSpeed = projectileSpeed; this.projectileWidth = projectileWidth; this.projectileHeight = projectileHeight;
    }
}
