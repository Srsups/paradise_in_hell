package br.com.srsups.paradiseinhell;

/** Origins of effective damage, excluding overkill and blocked hits. */
public enum DamageSource {
    WEAPON("Arma"), AURA("Aura"), SPELL("Explosão de feitiço"), SHOCK("Choque"),
    BURN("Queimadura"), CURSE("Maldição"), ALLY("Aliados"), RETALIATION("Revide"),
    AMBUSH("Emboscada"), CONVERGENCE("Convergência"), STYGIAN_ECHO("Eco do Estige");
    public final String label;
    DamageSource(String label) { this.label = label; }
}

