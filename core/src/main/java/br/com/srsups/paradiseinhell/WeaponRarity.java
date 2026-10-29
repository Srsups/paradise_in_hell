package br.com.srsups.paradiseinhell;

/** Forging persists in the profile. Attribute percentages use the forged weapon base. */
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
            case WAND -> "Explosões de feitiços alcançam mais inimigos.";
            case CURSE -> "Maldições duram 5s em vez de 3s.";
            case SOUL -> "Chance de reviver +25%; aliados duram 75s.";
            case SHIELD -> "35% de chance de revidar um ataque bloqueado.";
            case FISTS -> "Socos alcançam 34 unidades e continuam em um alvo.";
            case RAKE -> "Estocadas de 64 unidades. A forja melhora as duas ferramentas.";
            case SCYTHE -> "Giros de 44 unidades. A forja melhora as duas ferramentas.";
        };
    }
}
