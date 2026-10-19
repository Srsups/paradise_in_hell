package br.com.srsups.paradiseinhell;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Bounded status effects shared by arenas and guardians, independent of rendering. */
public final class CombatEffects {
    @FunctionalInterface public interface LineOfSight { boolean clear(float x, float y, float ox, float oy); }
    public enum Kind { FIRE, CURSE, SHOCK, SPELL, IMPACT }
    public static final class Flash {
        public final Kind kind;
        public final float x, y, ox, oy;
        private float life = 0.18f;
        public float progress() { return 1-life/.18f; }
        Flash(Kind kind, float x, float y, float ox, float oy) {
            this.kind = kind; this.x = x; this.y = y; this.ox = ox; this.oy = oy;
        }
    }
    private static final class Dot {
        float fireTime, fireDps, curseTime, curseDps;
    }
    public static final float CURSE_RADIUS = 32;
    private final DamageSink damageSink;
    public CombatEffects() { this(DamageSink.DIRECT); }
    public CombatEffects(DamageSink damageSink) { this.damageSink=java.util.Objects.requireNonNull(damageSink); }
    private final Map<CombatTarget, Dot> dots = new IdentityHashMap<>();
    private final List<Flash> flashes = new ArrayList<>();
    public List<Flash> flashes() { return java.util.Collections.unmodifiableList(flashes); }
    public boolean burning(CombatTarget target) { Dot d = dots.get(target); return d != null && d.fireTime > 0; }
    public boolean cursed(CombatTarget target) { Dot d = dots.get(target); return d != null && d.curseTime > 0; }
    public int trackedTargets() { return dots.size(); }
    public void update(float delta) {
        if (!Float.isFinite(delta) || delta < 0) throw new IllegalArgumentException("Invalid delta");
        var iterator = dots.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next(); CombatTarget target = entry.getKey(); Dot dot = entry.getValue();
            if (target.dead()) { iterator.remove(); continue; }
            float fire=dot.fireDps*Math.min(delta,dot.fireTime), curse=dot.curseDps*Math.min(delta,dot.curseTime);
            dot.fireTime = Math.max(0, dot.fireTime - delta); dot.curseTime = Math.max(0, dot.curseTime - delta);
            if(fire>0 && target.targetable()) damageSink.deal(target,fire,DamageSource.BURN);
            if(curse>0 && target.targetable()) damageSink.deal(target,curse,DamageSource.CURSE);
            if (target.dead() || dot.fireTime == 0 && dot.curseTime == 0) iterator.remove();
        }
        flashes.removeIf(f -> (f.life -= delta) <= 0);
    }
    public void forget(CombatTarget target) { dots.remove(target); }
    public void clear() { dots.clear(); flashes.clear(); }
    public void onHit(PlayerState player, CombatTarget target, List<? extends CombatTarget> targets,
                      float playerX, float playerY, LineOfSight line, boolean weaponHit) {
        float dps = player.dps();
        if (weaponHit && player.getWeapon() == WeaponType.CURSE) {
            if (target.targetable()) curse(player, target);
            // The impact remains the center even if its direct damage kills the first target.
            for (CombatTarget other : targets) {
                if (other != target && other.targetable() && distance(target, other) <= CURSE_RADIUS * CURSE_RADIUS
                    && line.clear(target.centerX(), target.centerY(), other.centerX(), other.centerY())) curse(player, other);
            }
            flash(Kind.CURSE, target.centerX(), target.centerY(), CURSE_RADIUS, 0);
        }
        if (weaponHit && player.getWeapon() == WeaponType.WAND && player.trySpell()) {
            float radius = player.getWeaponRarity() == WeaponRarity.RARE ? 32 : 18;
            for (CombatTarget other : targets) {
                if (other != target && other.targetable() && distance(target, other) <= radius * radius
                    && line.clear(target.centerX(), target.centerY(), other.centerX(), other.centerY())) damageSink.deal(other,dps * 0.25f,DamageSource.SPELL);
            }
            flash(Kind.SPELL, target.centerX(), target.centerY(), radius, 0);
        }
        if (player.tryBurn() && target.targetable()) {
            Dot dot = dots.computeIfAbsent(target, ignored -> new Dot());
            dot.fireDps = Math.max(dot.fireDps, dps * 0.35f * player.burnRank());
            dot.fireTime = 3;
        }
        if (player.tryShock()) {
            List<CombatTarget> visited = new ArrayList<>(); visited.add(target);
            CombatTarget from = target;
            for (int jump = 0; jump <= player.shockRank(); jump++) {
                float damage = dps * 0.35f * player.shockRank() * (burning(from) || cursed(from) ? 1.25f : 1);
                if (from.targetable()) damageSink.deal(from,damage,DamageSource.SHOCK);
                CombatTarget nearest = null; float best = 90 * 90;
                for (CombatTarget other : targets) {
                    float distance = distance(from, other);
                    if (other.targetable() && !visited.contains(other) && distance < best
                        && line.clear(from.centerX(), from.centerY(), other.centerX(), other.centerY())) {
                        nearest = other; best = distance;
                    }
                }
                if (nearest == null) { flash(Kind.SHOCK, from.centerX(), from.centerY(), from.centerX(), from.centerY()); break; }
                if (jump == player.shockRank()) break;
                flash(Kind.SHOCK, from.centerX(), from.centerY(), nearest.centerX(), nearest.centerY());
                visited.add(nearest); from = nearest;
            }
        }
        if (player.tryImpact() && target.targetable()) {
            float dx = target.centerX() - playerX, dy = target.centerY() - playerY;
            float length = Math.max(0.001f, (float) Math.sqrt(dx * dx + dy * dy));
            float force = (12 + 6 * player.impactRank()) * (player.possuiAura() ? 1.35f : 1);
            // Guardians are immovable; heavy enemies resist displacement.
            force /= Math.max(1, (float) Math.sqrt(target.strength()));
            target.push(dx / length * force, dy / length * force, line);
            flash(Kind.IMPACT, target.centerX(), target.centerY(), 12, 0);
        }
    }
    private void curse(PlayerState player, CombatTarget target) {
        Dot dot = dots.computeIfAbsent(target, ignored -> new Dot());
        dot.curseDps = Math.max(dot.curseDps, player.dps() * 0.6f);
        dot.curseTime = player.getWeaponRarity() == WeaponRarity.RARE ? 5 : 3;
    }
    private void flash(Kind kind, float x, float y, float ox, float oy) {
        if (flashes.size() < 64) flashes.add(new Flash(kind, x, y, ox, oy));
    }
    private static float distance(CombatTarget a, CombatTarget b) {
        float dx = a.centerX() - b.centerX(), dy = a.centerY() - b.centerY(); return dx * dx + dy * dy;
    }
}

