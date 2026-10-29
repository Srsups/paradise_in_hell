package br.com.srsups.paradiseinhell;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Bounded status effects shared by arenas and guardians, independent of rendering. */
public final class CombatEffects {
    @FunctionalInterface public interface LineOfSight { boolean clear(float x, float y, float ox, float oy); }
    public enum Kind { FIRE, CURSE, SHOCK, SPELL, IMPACT, AMBUSH, CONVERGENCE, ECHO }
    public static final float ECHO_DELAY=.35f,ECHO_RADIUS=44,CONVERGENCE_RADIUS=28;
    public static final class Echo {
        public final float x,y,damage;
        private float remaining=ECHO_DELAY;
        Echo(float x,float y,float damage){this.x=x;this.y=y;this.damage=damage;}
        public float progress(){return 1-remaining/ECHO_DELAY;}
    }
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
    private final List<Echo> echoes=new ArrayList<>();
    public List<Echo> echoes(){return java.util.Collections.unmodifiableList(echoes);}
    public void dash(PlayerState player,float originX,float originY){
        float damage=player.boons().takeEcho();
        if(damage>0&&echoes.size()<4)echoes.add(new Echo(originX,originY,damage));
    }
    public void weaponBonus(CombatTarget target,WeaponBonus bonus){
        float damage=bonus.take();
        if(damage<=0)return;
        if(target.targetable())damageSink.deal(target,damage,DamageSource.AMBUSH);
        flash(Kind.AMBUSH,target.centerX(),target.centerY(),12,0);
    }
    public void update(float delta,List<? extends CombatTarget> targets,LineOfSight line){
        update(delta);
        var iterator=echoes.iterator();
        while(iterator.hasNext()){
            Echo echo=iterator.next();echo.remaining-=delta;
            if(echo.remaining>0)continue;
            area(echo.x,echo.y,ECHO_RADIUS,echo.damage,targets,line,DamageSource.STYGIAN_ECHO);
            flash(Kind.ECHO,echo.x,echo.y,ECHO_RADIUS,0);iterator.remove();
        }
    }
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
    public void clear() { dots.clear(); flashes.clear(); echoes.clear(); }
    public void onHit(PlayerState player, CombatTarget target, List<? extends CombatTarget> targets,
                      float playerX, float playerY, LineOfSight line, boolean weaponHit) {
        float dps = player.dps();
        boolean existingStatus=burning(target)||cursed(target);
        if(weaponHit&&existingStatus&&player.boons().tryConvergence()){
            area(target.centerX(),target.centerY(),CONVERGENCE_RADIUS,
                dps*.30f*player.boons().rank(Upgrade.CONVERGENCIA),targets,line,DamageSource.CONVERGENCE);
            flash(Kind.CONVERGENCE,target.centerX(),target.centerY(),CONVERGENCE_RADIUS,0);
        }
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
    private void area(float x,float y,float radius,float damage,List<? extends CombatTarget> targets,
                      LineOfSight line,DamageSource source){
        for(CombatTarget target:targets){
            float dx=target.centerX()-x,dy=target.centerY()-y;
            if(target.targetable()&&dx*dx+dy*dy<=radius*radius&&line.clear(x,y,target.centerX(),target.centerY()))
                damageSink.deal(target,damage,source);
        }
    }
    private static float distance(CombatTarget a, CombatTarget b) {
        float dx = a.centerX() - b.centerX(), dy = a.centerY() - b.centerY(); return dx * dx + dy * dy;
    }
}

