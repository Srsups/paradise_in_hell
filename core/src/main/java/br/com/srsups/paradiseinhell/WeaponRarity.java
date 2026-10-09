package br.com.srsups.paradiseinhell;

/** Temporary weapon forging. Divine/permanent percentages use the forged weapon base. */
public enum WeaponRarity {
    COMMON("Comum", 1, 1),
    RARE("Rara", 1.25f, 1.10f);
    public final String label;
    public final float damageMultiplier, attackRateMultiplier;
    WeaponRarity(String label, float damageMultiplier, float attackRateMultiplier) {
        this.label = label; this.damageMultiplier = damageMultiplier; this.attackRateMultiplier = attackRateMultiplier;
    }
    public static String power(WeaponType weapon) {
        return switch (weapon) {
            case BOW -> "Flechas atravessam um alvo adicional.";
            case PISTOL -> "Tiros ricocheteiam em um alvo próximo.";
            case SWORD -> "Golpes alcançam 48 unidades, mantendo o cone.";
            case FISTS -> "Socos alcançam 34 unidades e continuam em um alvo.";
        };
    }
}
