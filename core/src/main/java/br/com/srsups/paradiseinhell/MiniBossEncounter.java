package br.com.srsups.paradiseinhell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/** Navigation is normal pursuit; attacks are stationary and telegraphed. No charge. */
public final class MiniBossEncounter extends EnemyNavigation.Body implements CombatTarget {
    public static final String NAME = "Carrasco das Sombras";
    public static final float BASE_HEALTH = 180, SPEED = 24, BODY_RADIUS = 12, SPIN_RADIUS = 42;
    public static final int XP = 20;
    public enum State { RECOVERY, TELEGRAPH, ACTIVE, DEFEATED }
    public enum Attack { SPIN, STONES }
    public static final class Stone {
        public float x, y;
        private final float vx, vy;
        private float life = 3;
        private Stone(float x, float y, float vx, float vy) { this.x = x; this.y = y; this.vx = vx; this.vy = vy; }
    }
    private final float maxHealth, damageMultiplier;
    private float health, timer = 1.5f, angle, contactCooldown, flashTimer;
    private boolean hitResolved;
    private State state = State.RECOVERY;
    private Attack attack = Attack.STONES;
    private final List<Stone> stones = new ArrayList<>();
    private final List<Stone> view = Collections.unmodifiableList(stones);
    public MiniBossEncounter(float x, float y, EnemyDifficulty difficulty) {
        super(x, y);
        health = maxHealth = BASE_HEALTH * difficulty.healthMultiplier();
        damageMultiplier = difficulty.damageMultiplier();
    }
    public float strength() { return 18 * (float) Math.sqrt(maxHealth / BASE_HEALTH); }
    private final List<CombatHit> hits = new ArrayList<>();
    public List<CombatHit> hits() { return java.util.List.copyOf(hits); }
    private void hit(float amount, float sx, float sy, CombatHit.Kind kind) {
        String cause=kind==CombatHit.Kind.PROJECTILE?"Pedras":state==State.RECOVERY?"Contato":"Giro de espada";
        hits.add(new CombatHit(amount * damageMultiplier, sx, sy, kind,NAME+" / "+cause));
    }
    public float health() { return health; }
    public float maxHealth() { return maxHealth; }
    public float centerX() { return x + 8; }
    public float centerY() { return y + 8; }
    public State state() { return state; }
    public Attack attack() { return attack; }
    public float aimAngle() { return angle; }
    public float warningProgress(){return state==State.TELEGRAPH?Math.max(0,1-timer/(attack==Attack.SPIN?1:1.1f)):1;}
    public float activeProgress(){return state==State.ACTIVE?Math.max(0,Math.min(1,1-timer/.45f)):0;}
    public boolean flashing() { return flashTimer > 0; }
    public List<Stone> stones() { return view; }
    public boolean defeated() { return state == State.DEFEATED; }
    public void hurt(float amount) {
        if (!Float.isFinite(amount) || amount < 0) throw new IllegalArgumentException("Invalid damage");
        if (defeated()) return;
        health = Math.max(0, health - amount); flashTimer = 0.12f;
        if (health == 0) { state = State.DEFEATED; stones.clear(); }
    }
    public float update(float delta, float playerX, float playerY, EnemyNavigation navigation) {
        return update(delta, playerX, playerY, navigation, true);
    }
    public float update(float delta, float playerX, float playerY, EnemyNavigation navigation, boolean bodyContact) {
        if (!Float.isFinite(delta) || delta < 0) throw new IllegalArgumentException("Invalid delta");
        hits.clear();
        float damage = 0;
        while (delta > 0 && !defeated()) {
            float step = Math.min(delta, 0.02f);
            damage = Math.max(damage, step(step, playerX, playerY, navigation, bodyContact));
            delta -= step;
        }
        return damage * damageMultiplier;
    }
    private float step(float delta, float playerX, float playerY, EnemyNavigation navigation, boolean bodyContact) {
        flashTimer = Math.max(0, flashTimer - delta);
        contactCooldown = Math.max(0, contactCooldown - delta);
        float damage = updateStones(delta, playerX, playerY, navigation);
        if (state == State.RECOVERY) navigation.move(this, SPEED, delta);
        timer -= delta;
        if (state == State.RECOVERY && timer <= 0) {
            attack = attack == Attack.SPIN ? Attack.STONES : Attack.SPIN;
            state = State.TELEGRAPH; timer = attack == Attack.SPIN ? 1 : 1.1f;
            angle = (float) Math.atan2(playerY - centerY(), playerX - centerX());
            hitResolved = false;
        } else if (state == State.TELEGRAPH && timer <= 0) {
            state = State.ACTIVE; timer = 0.45f;
            if (attack == Attack.STONES) {
                for (int i = 0; i < 5; i++) {
                    float direction = angle + (float) Math.toRadians((i - 2) * 16);
                    float dx = (float) Math.cos(direction), dy = (float) Math.sin(direction);
                    stones.add(new Stone(centerX() + dx * 15, centerY() + dy * 15, dx * 115, dy * 115));
                }
            }
        } else if (state == State.ACTIVE) {
            if (attack == Attack.SPIN && !hitResolved
                && distanceSquared(playerX, playerY, centerX(), centerY()) <= (SPIN_RADIUS + 8) * (SPIN_RADIUS + 8)
                && navigation.clearLine(centerX(), centerY(), playerX, playerY)) {
                hitResolved = true; damage = Math.max(damage, 28);
                hit(28, centerX(), centerY(), CombatHit.Kind.CONTACT);
            }
            if (timer <= 0) { state = State.RECOVERY; timer = 2.2f; }
        }
        if (bodyContact && state == State.RECOVERY && contactCooldown == 0
            && distanceSquared(playerX, playerY, centerX(), centerY()) <= (BODY_RADIUS + 8) * (BODY_RADIUS + 8)
            && navigation.clearLine(centerX(), centerY(), playerX, playerY)) {
            contactCooldown = 1; damage = Math.max(damage, 20);
            hit(20, centerX(), centerY(), CombatHit.Kind.CONTACT);
        }
        return damage;
    }
    private float updateStones(float delta, float playerX, float playerY, EnemyNavigation navigation) {
        float damage = 0;
        Iterator<Stone> iterator = stones.iterator();
        while (iterator.hasNext()) {
            Stone stone = iterator.next();
            float beforeX = stone.x, beforeY = stone.y;
            stone.x += stone.vx * delta; stone.y += stone.vy * delta; stone.life -= delta;
            // Test the wall before the player: stones cannot hurt through structures.
            if (stone.life <= 0 || !navigation.clearLine(beforeX, beforeY, stone.x, stone.y)) iterator.remove();
            else if (CombatGeometry.sweptCircleHits(beforeX, beforeY, stone.x, stone.y, playerX, playerY, 11)) {
                damage = 20; hit(20, beforeX, beforeY, CombatHit.Kind.PROJECTILE); iterator.remove();
            }
        }
        return damage;
    }
    private static float distanceSquared(float x, float y, float ox, float oy) { return (x - ox) * (x - ox) + (y - oy) * (y - oy); }
}
