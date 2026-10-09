package br.com.srsups.paradiseinhell;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.random.RandomGenerator;

/** Draw tier first, then a boon within it, without replacement or capped boons. */
public final class UpgradeChoices {
    private UpgradeChoices() {}
    public static List<Upgrade> draw(RunSession session, int count, RandomGenerator random) {
        if (count < 1 || count > 3) throw new IllegalArgumentException("Invalid choice count");
        EnumMap<BoonRarity, List<Upgrade>> pool = new EnumMap<>(BoonRarity.class);
        for (Upgrade boon : Upgrade.values()) {
            if (session.rank(boon) >= boon.maxRank) continue;
            if (boon == Upgrade.CURA && session.player().getVida() >= session.player().getVidaMaxima()) continue;
            pool.computeIfAbsent(boon.rarity, key -> new ArrayList<>()).add(boon);
        }
        if (pool.isEmpty()) return List.of(Upgrade.CURA);
        List<Upgrade> result = new ArrayList<>();
        while (result.size() < count && !pool.isEmpty()) {
            float sum = 0, luck = session.player().getLuckMultiplier();
            for (BoonRarity tier : pool.keySet()) sum += tier.weight(luck);
            float roll = random.nextFloat() * sum;
            BoonRarity selected = null;
            for (BoonRarity tier : pool.keySet()) {
                selected = tier; roll -= tier.weight(luck);
                if (roll < 0) break;
            }
            List<Upgrade> options = pool.get(selected);
            result.add(options.remove(random.nextInt(options.size())));
            if (options.isEmpty()) pool.remove(selected);
        }
        return List.copyOf(result);
    }
}
