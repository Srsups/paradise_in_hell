package br.com.srsups.paradiseinhell;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** No total wave/enemy budget. Campaign types are available from the first wave. */
public final class EnemyWaves {
    private final Random random;
    public EnemyWaves() { this(new Random()); }
    public EnemyWaves(Random random) { this.random = random; }
    public List<EnemyType> next(int size, float difficulty, boolean campaign) {
        if (size <= 0) throw new IllegalArgumentException("Empty wave");
        List<EnemyType> wave = new ArrayList<>(size);
        boolean sameType = random.nextFloat() < 0.35f;
        EnemyType groupType = choose(difficulty, campaign);
        for (int index = 0; index < size; index++) wave.add(sameType ? groupType : choose(difficulty, campaign));
        return wave;
    }
    private EnemyType choose(float difficulty, boolean campaign) {
        if (!campaign && difficulty < 60) return EnemyType.COMMON;
        if (!campaign && difficulty < 120) return random.nextBoolean() ? EnemyType.COMMON : EnemyType.FAST;
        float value = random.nextFloat();
        return value < 0.5f ? EnemyType.COMMON : value < 0.85f ? EnemyType.FAST : EnemyType.TANK;
    }
}
