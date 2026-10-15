package br.com.srsups.paradiseinhell;

public enum Upgrade {
    FORCA("Força de Hércules", "+20% do dano base da profissão.", 6, BoonRarity.COMMON),
    CADENCIA("Celeridade de Ártemis", "+12% da velocidade de ataque base.", 6, BoonRarity.COMMON),
    VIDA("Saúde do Minotauro", "+20% da vida base, máxima e atual.", 4, BoonRarity.COMMON),
    VELOCIDADE("Sandálias de Hermes", "+10% da velocidade base da profissão.", 4, BoonRarity.COMMON),
    RESISTENCIA("Resistência de Ares", "Reduz o dano recebido em 10%.", 5, BoonRarity.RARE),
    INTELIGENCIA("Sabedoria de Atena", "+150% de XP coletada (2,5x).\nUma vez por tentativa; XP futura.", 1, BoonRarity.EPIC),
    MAGNETISMO("Colheita de Deméter", "+32 de raio e +40 de atração dos cristais de XP.", 3, BoonRarity.RARE),
    CURA("Cura de Apolo", "Recupera 40 de vida.", Integer.MAX_VALUE, BoonRarity.COMMON),
    VAMPIRISMO("Favor de Hades", "+1 de vida a cada abate.", 3, BoonRarity.RARE),
    EGIDE("Égide", "Escudo periódico: 2s ativo, 10s recarga.", 1, BoonRarity.EPIC),
    POSEIDON("Correnteza de Poseidon", "Aura de dano; repetições a fortalecem.", 4, BoonRarity.EPIC),
    CHOQUE("Faísca de Zeus", "Raio em cadeia; recarga própria de 0,8s.", 3, BoonRarity.RARE),
    QUEIMADURA("Brasa de Héstia", "Ataques queimam por 3s; recarga própria de 0,8s.", 3, BoonRarity.RARE),
    IMPACTO("Martelo de Hefesto", "Ataques empurram; a aura reforça o impacto.", 3, BoonRarity.RARE),
    RESSURREICAO("Favor de Asclépio", "Uma segunda chance com 50% da vida.", 1, BoonRarity.MYTHOLOGICAL);

    public static final float ATHENA_XP_BONUS = 1.5f;

    public final String title;
    public final String description;
    public final int maxRank;
    public final BoonRarity rarity;
    Upgrade(String title, String description, int maxRank, BoonRarity rarity) {
        this.title = title;
        this.description = description;
        this.maxRank = maxRank; this.rarity = rarity;
    }
    public String description(PlayerState player) {
        return this == CADENCIA ? description + "\n" + CombatNumbers.formatRate(player.attacksPerSecond())
            + " para " + CombatNumbers.formatRate(player.attacksPerSecondAfterCadence()) + " ataques/s." : description;
    }
    public void apply(PlayerState player) {
        switch (this) {
            case FORCA -> player.aumentarDano(0.20f);
            case CADENCIA -> player.aumentarCadencia();
            case VIDA -> player.aumentarVidaMaxima(0.20f);
            case VELOCIDADE -> player.aumentarVelocidadeMovimento(0.1f);
            case RESISTENCIA -> player.aumentarResistencia(0.1f);
            case INTELIGENCIA -> player.aumentarInteligencia(ATHENA_XP_BONUS);
            case MAGNETISMO -> player.aumentarAtracaoXp();
            case CURA -> player.curar(40);
            case VAMPIRISMO -> player.ativarCuraPorAbate(player.getCuraPorAbate() + 1);
            case EGIDE -> player.ativarHabilidadeEgide();
            case POSEIDON -> player.ativarAuraDePoseidon();
            case CHOQUE -> player.addShock();
            case QUEIMADURA -> player.addBurn();
            case IMPACTO -> player.addImpact();
            case RESSURREICAO -> player.ganharRessurreicao();
        }
    }
}
