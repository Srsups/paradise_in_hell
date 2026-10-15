package br.com.srsups.paradiseinhell;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/** Allies live for one attempt, carry between rooms, and never count as enemy kills or XP. */
public final class Necromancy {
    public static final int MAX_ALLIES = 6;
    public static final class Ally extends EnemyNavigation.Body {
        public final float strength, damage;
        public float remaining, cooldown;
        private EnemyNavigation navigation;
        private Ally(CombatTarget corpse, PlayerState player) {
            super(corpse.centerX() - 8, corpse.centerY() - 8);
            strength = corpse.strength();
            damage = Math.min(40, 4 * (float) Math.sqrt(strength)) * player.getWeaponRarity().damageMultiplier;
            remaining = player.getWeaponRarity() == WeaponRarity.RARE ? 75 : 60;
        }
    }
    private final List<Ally> allies = new ArrayList<>();
    private final RandomGenerator random;
    private final DamageSink damageSink;
    private final java.util.Set<CombatTarget> processed = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());
    public Necromancy() { this(new java.util.Random()); }
    public Necromancy(RandomGenerator random) { this(random,DamageSink.DIRECT); }
    public Necromancy(RandomGenerator random,DamageSink damageSink) { this.random=java.util.Objects.requireNonNull(random); this.damageSink=java.util.Objects.requireNonNull(damageSink); }
    public List<Ally> allies() { return java.util.Collections.unmodifiableList(allies); }
    public static double chance(PlayerState player, CombatTarget corpse) {
        if (player.getProfession() != Profession.NECROMANTE || corpse.floorBoss()) return 0;
        double chance = 0.35 / Math.max(1, corpse.strength());
        if (player.getWeaponRarity() == WeaponRarity.RARE) chance *= 1.25;
        return Math.min(0.5, chance);
    }
    public boolean resurrect(PlayerState player, CombatTarget corpse) {
        if (!corpse.dead() || chance(player, corpse) == 0 || !processed.add(corpse) || allies.size() >= MAX_ALLIES) return false;
        if (random.nextDouble() >= chance(player, corpse)) return false;
        allies.add(new Ally(corpse, player)); return true;
    }
    public void enterRoom(float x, float y) {
        for (int i = 0; i < allies.size(); i++) {
            Ally ally = allies.get(i);
            // Start on the same known-safe tile, then spread while pursuing targets.
            ally.x = x - 8; ally.y = y - 8; ally.navigation = null;
        }
    }
    public void update(float delta, float playerX, float playerY, List<? extends CombatTarget> targets,
                       EnemyNavigation.Collision collision, CombatEffects.LineOfSight line) {
        if (!Float.isFinite(delta) || delta < 0) throw new IllegalArgumentException("Invalid delta");
        for (Ally ally : allies) {
            ally.remaining -= delta; ally.cooldown = Math.max(0, ally.cooldown - delta);
            if (ally.remaining <= 0) continue;
            CombatTarget nearest = null; float best = 220 * 220;
            for (CombatTarget target : targets) {
                float dx = target.centerX() - ally.x - 8, dy = target.centerY() - ally.y - 8;
                float distance = dx * dx + dy * dy;
                if (target.targetable() && distance < best) { best = distance; nearest = target; }
            }
            float tx = nearest == null ? playerX : nearest.centerX(), ty = nearest == null ? playerY : nearest.centerY();
            if (ally.navigation == null) ally.navigation = new EnemyNavigation(collision);
            ally.navigation.update(delta, tx, ty);
            if (nearest != null && best <= 28 * 28 && line.clear(ally.x + 8, ally.y + 8, tx, ty)) {
                if (ally.cooldown == 0) { damageSink.deal(nearest,ally.damage,DamageSource.ALLY); ally.cooldown = 0.9f; }
            } else ally.navigation.move(ally, 55, delta);
            if ((ally.x + 8 - playerX) * (ally.x + 8 - playerX) + (ally.y + 8 - playerY) * (ally.y + 8 - playerY) > 700 * 700) {
                ally.x = playerX - 8; ally.y = playerY - 8; ally.navigation = null;
            }
        }
        allies.removeIf(ally -> ally.remaining <= 0);
    }
    public void clear() { allies.clear(); processed.clear(); }
}
