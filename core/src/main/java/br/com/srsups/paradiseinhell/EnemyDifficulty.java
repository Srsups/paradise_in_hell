package br.com.srsups.paradiseinhell;

/** Linear growth at spawn/room entry. No healing of enemies already in combat. */
public record EnemyDifficulty(float healthMultiplier, float damageMultiplier) {
    public static final EnemyDifficulty BASE = new EnemyDifficulty(1, 1);
    public EnemyDifficulty {
        if (!Float.isFinite(healthMultiplier) || !Float.isFinite(damageMultiplier) || healthMultiplier <= 0 || damageMultiplier <= 0)
            throw new IllegalArgumentException("Invalid difficulty");
    }
    public static EnemyDifficulty at(float elapsedSeconds, float roomDifficultySeconds) {
        if (!Float.isFinite(elapsedSeconds) || !Float.isFinite(roomDifficultySeconds) || elapsedSeconds < 0 || roomDifficultySeconds < 0)
            throw new IllegalArgumentException("Invalid difficulty time");
        return new EnemyDifficulty(1 + elapsedSeconds / 60 * 0.06f + roomDifficultySeconds / 60 * 0.12f,
            1 + elapsedSeconds / 60 * 0.03f + roomDifficultySeconds / 60 * 0.06f);
    }
}
