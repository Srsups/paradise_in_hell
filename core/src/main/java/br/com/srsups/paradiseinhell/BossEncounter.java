package br.com.srsups.paradiseinhell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/** One bounded encounter with an introductory cycle and seedable later attack order. All attack geometry is also used by the renderer. */
public final class BossEncounter implements CombatTarget {
    public static final String NAME = "Guardião do Tártaro";
    public static final float MAX_HEALTH = 500, RADIUS = 18, PLAYER_RADIUS = 8;
    public static final float DRAW_SIZE = 46;
    public static final float LEAP_DURATION = 3, LEAP_LOCK_DURATION = 1;
    public static final float LEAP_START_RADIUS = PLAYER_RADIUS, LEAP_MAX_RADIUS = DRAW_SIZE * 0.75f;
    public static final float LEAP_TRACK_SPEED = 45, ENRAGED_LEAP_TRACK_SPEED = 130;
    public static final float LEAP_DAMAGE = 36, ENRAGED_LEAP_DAMAGE = 48;
    public static final float LEAP_WALKING_LIMIT = 24;
    private static final float LEAP_ANIMATION_SECONDS = 0.18f, EPSILON = 0.00001f;
    public static final float MIN_X = 16, MAX_X = 464, MIN_Y = 16, MAX_Y = 224;
    public enum State { INTRO, RECOVERY, TELEGRAPH, ACTIVE, DEFEATED }
    public enum Attack {
        MARKS("Marcas do Tártaro"), CHARGE("Investida"), VOLLEY("Rajada"), LEAP("Queda do Tártaro");
        public final String label;
        Attack(String label) { this.label = label; }
    }
    public record Mark(float x, float y, float radius) {}
    public static final class Shot {
        public float x, y;
        private final float velocityX, velocityY, damage;
        private float life = 4;
        private Shot(float x, float y, float velocityX, float velocityY, float damage) {
            this.x = x; this.y = y; this.velocityX = velocityX; this.velocityY = velocityY; this.damage = damage;
        }
    }
    private final List<Mark> marks = new ArrayList<>();
    private final List<Shot> shots = new ArrayList<>();
    private final List<Mark> marksView = Collections.unmodifiableList(marks);
    private final List<Shot> shotsView = Collections.unmodifiableList(shots);
    private float x = 240, y = 172, health, timer = 1.2f, elapsed, flashTimer;
    private float startX, startY, endX, endY, aimAngle, telegraphDuration, activeDuration, activeElapsed;
    private final float maxHealth, damageMultiplier;
    private float contactCooldown;
    private boolean poweredAttack, marksResolved, chargeResolved, leapResolved;
    private float leapX, leapY, leapElapsed;
    public static final float RECOVERY_SECONDS = .95f, ENRAGED_RECOVERY_SECONDS = .65f;
    private final java.util.random.RandomGenerator random;
    private final Attack[] order = Attack.values();
    private int nextAttack;
    private float phaseOneSeconds,phaseTwoSeconds;
    private final java.util.EnumMap<Attack,Integer> attackCounts=new java.util.EnumMap<>(Attack.class);
    public BossFightSummary summary() { return new BossFightSummary(phaseOneSeconds,phaseTwoSeconds,attackCounts); }
    private Attack attack = Attack.MARKS;
    private State state = State.INTRO;

    public BossEncounter() { this(EnemyDifficulty.BASE); }
    public BossEncounter(EnemyDifficulty difficulty) { this(difficulty,new java.util.Random()); }
    public BossEncounter(EnemyDifficulty difficulty, java.util.random.RandomGenerator random) {
        this.random=java.util.Objects.requireNonNull(random);
        maxHealth = MAX_HEALTH * difficulty.healthMultiplier();
        health = maxHealth; damageMultiplier = difficulty.damageMultiplier();
    }
    public float centerX() { return x; }
    public float centerY() { return y; }
    public float strength() { return Float.POSITIVE_INFINITY; }
    public boolean floorBoss() { return true; }
    private final List<CombatHit> hits = new ArrayList<>();
    public List<CombatHit> hits() { return java.util.List.copyOf(hits); }
    private void hit(float amount, float sx, float sy, CombatHit.Kind kind) {
        String cause=kind==CombatHit.Kind.PROJECTILE?"Rajada":kind==CombatHit.Kind.CONTACT
            ? state==State.ACTIVE && attack==Attack.CHARGE?"Investida":"Contato":attack.label;
        hits.add(new CombatHit(amount * damageMultiplier, sx, sy, kind,NAME+" / "+cause));
    }
    public float maxHealth() { return maxHealth; }
    public float x() { return x; }
    public float y() { return y; }
    public float health() { return health; }
    public float elapsed() { return elapsed; }
    public State state() { return state; }
    public Attack attack() { return attack; }
    public boolean enraged() { return health <= maxHealth / 2; }
    public boolean defeated() { return state == State.DEFEATED; }
    public boolean flashing() { return flashTimer > 0; }
    public boolean airborne() { return attack == Attack.LEAP && state == State.TELEGRAPH; }
    @Override public boolean targetable() { return !defeated() && !airborne(); }
    public boolean leapVisible() { return attack == Attack.LEAP && (state == State.TELEGRAPH || state == State.ACTIVE); }
    public boolean leapLocked() { return airborne() && leapElapsed >= LEAP_DURATION - LEAP_LOCK_DURATION - EPSILON; }
    public float leapX() { return leapX; }
    public float leapY() { return leapY; }
    public float leapRadius() {
        return LEAP_START_RADIUS + (LEAP_MAX_RADIUS - LEAP_START_RADIUS) * Math.min(1, leapElapsed / LEAP_DURATION);
    }
    public float leapSecondsRemaining() { return Math.max(0, LEAP_DURATION - leapElapsed); }
    public float leapTrackSpeed() { return poweredAttack ? ENRAGED_LEAP_TRACK_SPEED : LEAP_TRACK_SPEED; }
    public float walkingSpeedLimit() { return leapLocked() && poweredAttack ? LEAP_WALKING_LIMIT : Float.POSITIVE_INFINITY; }
    public boolean spriteVisible() {
        return !airborne() || leapElapsed < LEAP_ANIMATION_SECONDS || leapSecondsRemaining() < LEAP_ANIMATION_SECONDS;
    }
    public float spriteX() { return airborne() && leapElapsed > LEAP_ANIMATION_SECONDS ? leapX : x; }
    public float spriteY() {
        if (!airborne()) return y;
        float lift = leapElapsed < LEAP_ANIMATION_SECONDS ? leapElapsed / LEAP_ANIMATION_SECONDS
            : leapSecondsRemaining() / LEAP_ANIMATION_SECONDS;
        return (leapElapsed < LEAP_ANIMATION_SECONDS ? y : leapY) + Math.min(1, lift) * 360;
    }
    public List<Mark> marks() { return marksView; }
    public List<Shot> shots() { return shotsView; }
    public float startX() { return startX; }
    public float startY() { return startY; }
    public float endX() { return endX; }
    public float endY() { return endY; }
    public float aimAngle() { return aimAngle; }
    public float warningProgress() { return state == State.TELEGRAPH ? 1 - timer / telegraphDuration : 1; }
    public void hurt(float amount) {
        if (!Float.isFinite(amount) || amount < 0) throw new IllegalArgumentException("Invalid damage");
        if (!targetable() || amount == 0) return;
        health = Math.max(0, health - amount);
        flashTimer = 0.12f;
        if (health == 0) {
            state = State.DEFEATED;
            marks.clear(); shots.clear(); hits.clear();
        }
    }
    /** Returns the strongest hit this update; the screen applies player shield/resistance. */
    public float update(float delta, float playerX, float playerY) {
        return update(delta, playerX, playerY, true);
    }
    public float update(float delta, float playerX, float playerY, boolean bodyContact) {
        if (!Float.isFinite(delta) || delta < 0) throw new IllegalArgumentException("Invalid delta");
        hits.clear();
        float damage = 0;
        while (delta > 0 && !defeated()) {
            float step = Math.min(delta, 0.02f);
            damage = Math.max(damage, step(step, playerX, playerY, bodyContact));
            delta -= step;
        }
        return damage * damageMultiplier;
    }
    private float step(float delta, float playerX, float playerY, boolean bodyContact) {
        elapsed += delta;
        if(enraged())phaseTwoSeconds+=delta;else phaseOneSeconds+=delta;
        flashTimer = Math.max(0, flashTimer - delta);
        contactCooldown = Math.max(0, contactCooldown - delta);
        float damage = updateShots(delta, playerX, playerY);
        timer -= delta;
        switch (state) {
            case INTRO, RECOVERY -> {
                if (timer <= 0) beginAttack(playerX, playerY);
            }
            case TELEGRAPH -> {
                if (attack == Attack.LEAP) {
                    updateLeap(delta, playerX, playerY);
                    if (leapElapsed >= LEAP_DURATION - EPSILON) {
                        activate();
                        damage = Math.max(damage, landLeap(playerX, playerY));
                    }
                } else if (timer <= 0) activate();
            }
            case ACTIVE -> {
                if (attack == Attack.MARKS && !marksResolved) {
                    marksResolved = true;
                    for (Mark mark : marks) if (distanceSquared(playerX, playerY, mark.x(), mark.y()) <= square(mark.radius() + PLAYER_RADIUS)) {
                        damage = Math.max(damage, poweredAttack ? 28 : 22);
                        hit(poweredAttack ? 28 : 22, mark.x(), mark.y(), CombatHit.Kind.GROUND);
                    }
                } else if (attack == Attack.CHARGE) {
                    float beforeX = x, beforeY = y;
                    activeElapsed = Math.min(activeDuration, activeElapsed + delta);
                    float fraction = activeElapsed / activeDuration;
                    x = startX + (endX - startX) * fraction;
                    y = startY + (endY - startY) * fraction;
                    if (!chargeResolved && CombatGeometry.sweptCircleHits(beforeX, beforeY, x, y, playerX, playerY, RADIUS + PLAYER_RADIUS)) {
                        chargeResolved = true;
                        hit(poweredAttack ? 24 : 18, beforeX, beforeY, CombatHit.Kind.CONTACT);
                        damage = Math.max(damage, poweredAttack ? 24 : 18);
                    }
                }
                if (timer <= 0) {
                    state = State.RECOVERY;
                    timer = enraged() ? ENRAGED_RECOVERY_SECONDS : RECOVERY_SECONDS;
                    marks.clear();
                }
            }
            case DEFEATED -> { }
        }
        // Telegraphs are safe, including when the player approaches with a sword.
        if (bodyContact && (state == State.RECOVERY || state == State.ACTIVE && attack != Attack.CHARGE && attack != Attack.LEAP) && contactCooldown <= 0
                && distanceSquared(playerX, playerY, x, y) < square(RADIUS + PLAYER_RADIUS)) {
            contactCooldown = 1;
            damage = Math.max(damage, 10);
            hit(10, x, y, CombatHit.Kind.CONTACT);
        }
        return damage;
    }
    private void beginAttack(float playerX, float playerY) {
        if (nextAttack == order.length) {
            Attack last=attack;
            for(int i=order.length-1;i>0;i--) {
                int other=random.nextInt(i+1);
                Attack swap=order[i];order[i]=order[other];order[other]=swap;
            }
            if(order[0]==last) { Attack swap=order[0];order[0]=order[1];order[1]=swap; }
            nextAttack=0;
        }
        attack = order[nextAttack++];
        attackCounts.merge(attack,1,Integer::sum);
        poweredAttack = enraged();
        state = State.TELEGRAPH;
        telegraphDuration = poweredAttack ? 0.75f : 0.95f;
        timer = telegraphDuration;
        marksResolved = false; chargeResolved = false; leapResolved = false; activeElapsed = 0;
        startX = x; startY = y;
        aimAngle = (float) Math.atan2(playerY - y, playerX - x);
        marks.clear();
        if (attack == Attack.VOLLEY) shots.clear(); // Never stack two fans; leap also clears prior hazards.
        if (attack == Attack.LEAP) {
            telegraphDuration = LEAP_DURATION; timer = LEAP_DURATION;
            leapElapsed = 0;
            leapX = clamp(playerX, MIN_X + RADIUS, MAX_X - RADIUS);
            leapY = clamp(playerY, MIN_Y + RADIUS, MAX_Y - RADIUS);
            shots.clear(); // The tracking circle is the only hazard during this attack.
        } else if (attack == Attack.MARKS) {
            marks.add(new Mark(playerX, playerY, 32));
            if (poweredAttack) {
                marks.add(new Mark(clamp(playerX + (playerX < 240 ? 64 : -64), 48, 432), playerY, 30));
            }
        } else if (attack == Attack.CHARGE) {
            float directionX = (float) Math.cos(aimAngle), directionY = (float) Math.sin(aimAngle);
            float distance = (float) Math.sqrt(distanceSquared(playerX, playerY, x, y)) + 40;
            if (directionX > 0.001f) distance = Math.min(distance, (MAX_X - RADIUS - x) / directionX);
            if (directionX < -0.001f) distance = Math.min(distance, (MIN_X + RADIUS - x) / directionX);
            if (directionY > 0.001f) distance = Math.min(distance, (MAX_Y - RADIUS - y) / directionY);
            if (directionY < -0.001f) distance = Math.min(distance, (MIN_Y + RADIUS - y) / directionY);
            endX = x + directionX * distance; endY = y + directionY * distance;
            activeDuration = Math.max(0.2f, distance / (poweredAttack ? 275 : 200));
        }
    }
    private void updateLeap(float delta, float playerX, float playerY) {
        float trackingTime = leapLocked() ? 0 : Math.min(delta, Math.max(0, LEAP_DURATION - LEAP_LOCK_DURATION - leapElapsed));
        if (trackingTime > 0) {
            float targetX = clamp(playerX, MIN_X + RADIUS, MAX_X - RADIUS);
            float targetY = clamp(playerY, MIN_Y + RADIUS, MAX_Y - RADIUS);
            float dx = targetX - leapX, dy = targetY - leapY;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            if (distance > EPSILON) {
                float travel = Math.min(distance, leapTrackSpeed() * trackingTime);
                leapX += dx / distance * travel; leapY += dy / distance * travel;
            }
        }
        leapElapsed = Math.min(LEAP_DURATION, leapElapsed + delta);
    }
    private float landLeap(float playerX, float playerY) {
        if (leapResolved) return 0;
        leapResolved = true;
        x = leapX; y = leapY;
        if (distanceSquared(playerX, playerY, leapX, leapY) > square(LEAP_MAX_RADIUS + PLAYER_RADIUS)) return 0;
        float damage = poweredAttack ? ENRAGED_LEAP_DAMAGE : LEAP_DAMAGE;
        hit(damage, leapX, leapY, CombatHit.Kind.GROUND);
        return damage;
    }
    private void activate() {
        state = State.ACTIVE;
        if (attack == Attack.CHARGE) timer = activeDuration;
        else if (attack == Attack.LEAP) timer = 0.45f;
        else {
            timer = 0.3f;
            if (attack == Attack.VOLLEY) {
                int count = poweredAttack ? 9 : 5;
                float speed = poweredAttack ? 150 : 115;
                for (int index = 0; index < count; index++) {
                    float angle = aimAngle + (float) Math.toRadians((index - (count - 1) / 2f) * (poweredAttack ? 14 : 17));
                    float directionX = (float) Math.cos(angle), directionY = (float) Math.sin(angle);
                    shots.add(new Shot(x + directionX * (RADIUS + 5), y + directionY * (RADIUS + 5), directionX * speed, directionY * speed, poweredAttack ? 14 : 12));
                }
            }
        }
    }
    private float updateShots(float delta, float playerX, float playerY) {
        float damage = 0;
        Iterator<Shot> iterator = shots.iterator();
        while (iterator.hasNext()) {
            Shot shot = iterator.next();
            float beforeX = shot.x, beforeY = shot.y;
            shot.x += shot.velocityX * delta; shot.y += shot.velocityY * delta; shot.life -= delta;
            if (CombatGeometry.sweptCircleHits(beforeX, beforeY, shot.x, shot.y, playerX, playerY, PLAYER_RADIUS + 3)) {
                damage = shot.damage; hit(damage, beforeX, beforeY, CombatHit.Kind.PROJECTILE); iterator.remove();
            } else if (shot.life <= 0 || shot.x < MIN_X || shot.x > MAX_X || shot.y < MIN_Y || shot.y > MAX_Y) iterator.remove();
        }
        return damage;
    }
    private static float square(float value) { return value * value; }
    private static float distanceSquared(float x, float y, float otherX, float otherY) { return square(x - otherX) + square(y - otherY); }
    private static float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }
}
