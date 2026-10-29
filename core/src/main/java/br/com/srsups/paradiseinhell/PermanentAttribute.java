package br.com.srsups.paradiseinhell;

/** Limited progression; separate from temporary divine upgrades. Values are provisional. */
public enum PermanentAttribute {
    VITALITY("Vitalidade", "+10% da vida base por nível."),
    STRENGTH("Força", "+5% do dano base por nível."),
    AGILITY("Agilidade", "+3% da velocidade base por nível."),
    ENDURANCE("Reserva de estamina", "+5% da capacidade base por nível."),
    RECOVERY("Recuperação de estamina", "+8% da regeneração base por nível; sem regenerar defendendo."),
    ARMOR("Armadura", "-3% de dano recebido por nível; até 15% no altar.");

    public static final int MAX_RANK = 5;
    public static final float ARMOR_REDUCTION_PER_RANK = .03f;
    public final String title, description;
    PermanentAttribute(String title, String description) { this.title = title; this.description = description; }
    public int price(int rank) {
        if (rank < 0 || rank >= MAX_RANK) throw new IllegalArgumentException("Invalid rank");
        return switch(this){case ENDURANCE->6+rank*5;case RECOVERY->7+rank*5;default->5+rank*4;};
    }
    public boolean optionalInLegacyProfile(){return this==ENDURANCE||this==RECOVERY||this==ARMOR;}
    public String preview(PlayerState player,int rank){
        float current=switch(this){
            case VITALITY->player.getVidaMaxima();case STRENGTH->player.getDano();case AGILITY->player.normalWalkingSpeed();
            case ENDURANCE->player.getEstaminaMaxima();case RECOVERY->player.staminaRegeneration();case ARMOR->player.damageReduction()*100;
        };
        float increase=switch(this){
            case VITALITY->player.getProfession().baseHealth*.10f;
            case STRENGTH->player.getWeapon().damage*player.getWeaponRarity().damageMultiplier*.05f;
            case AGILITY->player.getProfession().baseSpeed*.03f;case ENDURANCE->5;case RECOVERY->.8f;case ARMOR->ARMOR_REDUCTION_PER_RANK*100;
        };
        if(this==ARMOR)
            return CombatNumbers.formatRate(current)+"%"+(rank>=MAX_RANK?" (máximo)":" para "
                +CombatNumbers.formatRate(Math.min(75,current+increase))+"%")+" de redução de dano";
        String unit=switch(this){case VITALITY->" vida";case STRENGTH->" dano";case AGILITY->" movimento";
            case ENDURANCE->" estamina";case RECOVERY->" estamina/s";case ARMOR->throw new AssertionError();};
        return CombatNumbers.format(current)+(rank>=MAX_RANK?" (máximo)":" para "+CombatNumbers.format(current+increase))+unit;
    }
}
