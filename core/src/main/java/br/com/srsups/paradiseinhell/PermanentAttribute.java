package br.com.srsups.paradiseinhell;

/** Limited progression; separate from temporary divine upgrades. Values are provisional. */
public enum PermanentAttribute {
    VITALITY("Vitalidade", "+10% da vida base por nível."),
    STRENGTH("Força", "+5% do dano base por nível."),
    AGILITY("Agilidade", "+3% da velocidade base por nível.");

    public static final int MAX_RANK = 5;
    public final String title, description;
    PermanentAttribute(String title, String description) { this.title = title; this.description = description; }
    public int price(int rank) {
        if (rank < 0 || rank >= MAX_RANK) throw new IllegalArgumentException("Invalid rank");
        return 5 + rank * 4;
    }
}
