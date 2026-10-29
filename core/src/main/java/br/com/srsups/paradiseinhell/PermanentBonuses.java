package br.com.srsups.paradiseinhell;

/** A snapshot taken when starting a run; later purchases cannot alter an active run. */
public record PermanentBonuses(int vitality, int strength, int agility,int endurance,int recovery,int armor) {
    public static final PermanentBonuses NONE = new PermanentBonuses(0, 0, 0);
    public PermanentBonuses(int vitality,int strength,int agility){this(vitality,strength,agility,0,0,0);}
    public PermanentBonuses {
        if (vitality < 0 || strength < 0 || agility < 0 || vitality > PermanentAttribute.MAX_RANK
            || strength > PermanentAttribute.MAX_RANK || agility > PermanentAttribute.MAX_RANK
            ||endurance<0||recovery<0||armor<0||endurance>PermanentAttribute.MAX_RANK
            ||recovery>PermanentAttribute.MAX_RANK||armor>PermanentAttribute.MAX_RANK)
            throw new IllegalArgumentException("Invalid permanent bonus");
    }
    public void apply(PlayerState player) {
        player.aumentarVidaMaxima(vitality * 0.10f);
        player.aumentarDano(strength * 0.05f);
        player.aumentarVelocidadeMovimento(agility * 0.03f);
        player.aumentarCapacidadeEstamina(endurance*.05f);
        player.aumentarRecuperacaoEstamina(recovery*.08f);
        player.aumentarResistencia(armor*PermanentAttribute.ARMOR_REDUCTION_PER_RANK);
    }
}
