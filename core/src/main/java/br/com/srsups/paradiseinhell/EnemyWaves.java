package br.com.srsups.paradiseinhell;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** No total wave/enemy budget. Campaign types are available from the first wave. */
public final class EnemyWaves {
    public static final int MAX_FAST = 24, MAX_FAST_PER_WAVE = 2;
    public static final int MAX_ARCHERS = 6, MAX_ARCHERS_PER_WAVE = 2;
    public static final float ARCHER_CHANCE = .20f;
    private final Random random;
    public EnemyWaves() { this(new Random()); }
    public EnemyWaves(Random random) { this.random = random; }
    public List<EnemyType> next(int size, float difficulty, boolean campaign) {
        return next(size, difficulty, campaign, ArenaVariation.BALANCED);
    }
    public List<EnemyType> next(int size, float difficulty, boolean campaign, ArenaVariation variation) {
        return next(size, difficulty, campaign, variation, 0);
    }
    public List<EnemyType> next(int size, float difficulty, boolean campaign, ArenaVariation variation, int activeFast) {
        if (activeFast < 0) throw new IllegalArgumentException("Invalid active enemies");
        if (variation == null) throw new IllegalArgumentException("Missing variation");
        if (size <= 0) throw new IllegalArgumentException("Empty wave");
        List<EnemyType> wave = new ArrayList<>(size);
        boolean sameType = random.nextFloat() < 0.35f;
        EnemyType groupType = choose(difficulty, campaign, variation);
        int fast = 0;
        for (int index = 0; index < size; index++) {
            EnemyType type = sameType ? groupType : choose(difficulty, campaign, variation);
            if (type == EnemyType.FAST && (fast >= MAX_FAST_PER_WAVE || activeFast + fast >= MAX_FAST)) {
                // Redistribute surplus spawns instead of discarding the horde or changing the goals.
                type = !campaign && difficulty < 120 ? EnemyType.COMMON
                    : variation.chooseWithoutFast(random.nextFloat());
            }
            if (type == EnemyType.FAST) fast++;
            wave.add(type);
        }
        return wave;
    }
    /** Converts only common enemies; the existing three kill-goal groups remain intact. */
    public boolean archer(EnemyType type, float elapsed, boolean campaign, int active, int inWave) {
        return type == EnemyType.COMMON && elapsed >= (campaign ? 15 : 60)
            && active < MAX_ARCHERS && inWave < MAX_ARCHERS_PER_WAVE && random.nextFloat() < ARCHER_CHANCE;
    }
    private EnemyType choose(float difficulty, boolean campaign, ArenaVariation variation) {
        if (!campaign && difficulty < 60) return EnemyType.COMMON;
        if (!campaign && difficulty < 120) return random.nextBoolean() ? EnemyType.COMMON : EnemyType.FAST;
        float value = random.nextFloat();
        return (!campaign ? ArenaVariation.BALANCED : variation).choose(value);
    }
}
