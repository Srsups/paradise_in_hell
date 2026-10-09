package br.com.srsups.paradiseinhell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/** One bounded, deterministic encounter. All attack geometry is also used by the renderer. */
public final class BossEncounter {
    public static final String NAME = "Guardião do Tártaro";
    public static final float MAX_HEALTH = 420, RADIUS = 18, PLAYER_RADIUS = 8;
    public static final float MIN_X = 16, MAX_X = 464, MIN_Y = 16, MAX_Y = 224;
    public enum State { INTRO, RECOVERY, TELEGRAPH, ACTIVE, DEFEATED }
    public enum Attack {
        MARKS("Marcas do Tártaro"), CHARGE("Investida"), VOLLEY("Rajada");
        public final String label;
        Attack(String label) { this.label = label; }
    }
    public record Mark(float x, float y, float radius) {}
    public static final class Shot {
        public float x, y;
        private final float velocityX, velocityY;
        private float life = 4;
        private Shot(float x, float y, float velocityX, float velocityY) {
            this.x = x; this.y = y; this.velocityX = velocityX; this.velocityY = velocityY;
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
    private boolean poweredAttack, marksResolved, chargeResolved;
    private int nextAttack;
    private Attack attack = Attack.MARKS;
    private State state = State.INTRO;

    public BossEncounter() { this(EnemyDifficulty.BASE); }
    public BossEncounter(EnemyDifficulty difficulty) {
        maxHealth = MAX_HEALTH * difficulty.healthMultiplier();
        health = maxHealth; damageMultiplier = difficulty.damageMultiplier();
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
        if (defeated() || amount == 0) return;
        health = Math.max(0, health - amount);
        flashTimer = 0.12f;
        if (health == 0) {
            state = State.DEFEATED;
            marks.clear(); shots.clear();
        }
    }
    /** Returns the strongest hit this update; the screen applies player shield/resistance. */
    public float update(float delta, float playerX, float playerY) {
        if (!Float.isFinite(delta) || delta < 0) throw new IllegalArgumentException("Invalid delta");
        float damage = 0;
        while (delta > 0 && !defeated()) {
            float step = Math.min(delta, 0.02f);
            damage = Math.max(damage, step(step, playerX, playerY));
            delta -= step;
        }
        return damage * damageMultiplier;
    }
    private float step(float delta, float playerX, float playerY) {
        elapsed += delta;
        flashTimer = Math.max(0, flashTimer - delta);
        contactCooldown = Math.max(0, contactCooldown - delta);
        float damage = updateShots(delta, playerX, playerY);
        timer -= delta;
        switch (state) {
            case INTRO, RECOVERY -> {
                if (timer <= 0) beginAttack(playerX, playerY);
            }
            case TELEGRAPH -> {
                if (timer <= 0) activate();
            }
            case ACTIVE -> {
                if (attack == Attack.MARKS && !marksResolved) {
                    marksResolved = true;
                    for (Mark mark : marks) if (distanceSquared(playerX, playerY, mark.x(), mark.y()) <= square(mark.radius() + PLAYER_RADIUS))
                        damage = Math.max(damage, poweredAttack ? 28 : 22);
                } else if (attack == Attack.CHARGE) {
                    float beforeX = x, beforeY = y;
                    activeElapsed = Math.min(activeDuration, activeElapsed + delta);
                    float fraction = activeElapsed / activeDuration;
                    x = startX + (endX - startX) * fraction;
                    y = startY + (endY - startY) * fraction;
                    if (!chargeResolved && CombatGeometry.sweptCircleHits(beforeX, beforeY, x, y, playerX, playerY, RADIUS + PLAYER_RADIUS)) {
                        chargeResolved = true;
                        damage = Math.max(damage, poweredAttack ? 24 : 18);
                    }
                }
                if (timer <= 0) {
                    state = State.RECOVERY;
                    timer = enraged() ? 0.9f : 1.2f;
                    marks.clear();
                }
            }
            case DEFEATED -> { }
        }
        // Telegraphs are safe, including when the player approaches with a sword.
        if ((state == State.RECOVERY || state == State.ACTIVE && attack != Attack.CHARGE) && contactCooldown <= 0
                && distanceSquared(playerX, playerY, x, y) < square(RADIUS + PLAYER_RADIUS)) {
            contactCooldown = 1;
            damage = Math.max(damage, 10);
        }
        return damage;
    }
    private void beginAttack(float playerX, float playerY) {
        attack = Attack.values()[nextAttack++ % Attack.values().length];
        poweredAttack = enraged();
        state = State.TELEGRAPH;
        telegraphDuration = poweredAttack ? 0.75f : 0.95f;
        timer = telegraphDuration;
        marksResolved = false; chargeResolved = false; activeElapsed = 0;
        startX = x; startY = y;
        aimAngle = (float) Math.atan2(playerY - y, playerX - x);
        marks.clear();
        if (attack == Attack.MARKS) {
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
            activeDuration = Math.max(0.2f, distance / (poweredAttack ? 250 : 190));
        }
    }
    private void activate() {
        state = State.ACTIVE;
        if (attack == Attack.CHARGE) timer = activeDuration;
        else {
            timer = 0.3f;
            if (attack == Attack.VOLLEY) {
                int count = poweredAttack ? 7 : 5;
                float speed = poweredAttack ? 135 : 110;
                for (int index = 0; index < count; index++) {
                    float angle = aimAngle + (float) Math.toRadians((index - (count - 1) / 2f) * 17);
                    float directionX = (float) Math.cos(angle), directionY = (float) Math.sin(angle);
                    shots.add(new Shot(x + directionX * (RADIUS + 5), y + directionY * (RADIUS + 5), directionX * speed, directionY * speed));
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
                damage = 12; iterator.remove();
            } else if (shot.life <= 0 || shot.x < MIN_X || shot.x > MAX_X || shot.y < MIN_Y || shot.y > MAX_Y) iterator.remove();
        }
        return damage;
    }
    private static float square(float value) { return value * value; }
    private static float distanceSquared(float x, float y, float otherX, float otherY) { return square(x - otherX) + square(y - otherY); }
    private static float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }
}
