package br.com.srsups.paradiseinhell;

/** Fixed when a character is created; reward percentages accumulate before conversion. */
public enum GameDifficulty {
    EASY("Fácil", .85f, .75f, 85),
    MEDIUM("Média", 1, 1, 100),
    HARD("Difícil", 1.25f, 1.20f, 125);

    public final String label;
    public final float healthMultiplier, damageMultiplier;
    public final int rewardPercent;
    GameDifficulty(String label, float health, float damage, int rewards) {
        this.label = label; healthMultiplier = health; damageMultiplier = damage; rewardPercent = rewards;
    }
    public EnemyDifficulty apply(EnemyDifficulty base) {
        return new EnemyDifficulty(base.healthMultiplier() * healthMultiplier, base.damageMultiplier() * damageMultiplier);
    }
    public long rewardHundredths(int base) {
        if (base < 0) throw new IllegalArgumentException("Negative reward");
        return Math.multiplyExact((long) base, rewardPercent);
    }
    public int reward(int base) { return wholeReward(rewardHundredths(base)); }
    static int wholeReward(long hundredths) { return Math.toIntExact(hundredths / 100); }
}

