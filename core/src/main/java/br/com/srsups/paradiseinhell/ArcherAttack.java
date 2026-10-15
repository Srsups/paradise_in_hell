package br.com.srsups.paradiseinhell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Bounded ranged attack; fixed aim, swept arrows and shared obstacle navigation. */
public final class ArcherAttack {
    public static final float HEALTH = 14, SPEED = 28, CONTACT_DAMAGE = 6, ARROW_DAMAGE = 10;
    public static final int XP = 3, SOULS = 3;
    public static final float AIM_SECONDS = 1, RECOVERY_SECONDS = 2, ARROW_SPEED = 180;
    public static final float RANGE = 160, PREFERRED_DISTANCE = 72, ARROW_RADIUS = 1.5f;
    public static final String NAME = "Arqueiro do Estige";
    public enum Phase { SEEKING, AIMING, RECOVERING }
    @FunctionalInterface public interface Target {
        boolean hits(float fromX, float fromY, float toX, float toY, float radius);
    }
    public static final class Arrow {
        private float x, y, lifetime;
        private final float velocityX, velocityY, damage;
        private Arrow(float x, float y, float dx, float dy, float damage) {
            this.x = x; this.y = y; this.damage = damage;
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            velocityX = dx / length * ARROW_SPEED; velocityY = dy / length * ARROW_SPEED;
        }
        public float x() { return x; } public float y() { return y; }
        public float directionX() { return velocityX / ARROW_SPEED; }
        public float directionY() { return velocityY / ARROW_SPEED; }
    }
    private final List<Arrow> arrows = new ArrayList<>(2);
    private final List<Arrow> arrowView = Collections.unmodifiableList(arrows);
    private Phase phase = Phase.SEEKING;
    private float remaining, aimX, aimY;
    public Phase phase() { return phase; }
    public float aimX() { return aimX; } public float aimY() { return aimY; }
    public float aimProgress() { return phase == Phase.AIMING ? 1 - remaining / AIM_SECONDS : 0; }
    public List<Arrow> arrows() { return arrowView; }

    public void update(float delta, EnemyNavigation.Body body, float playerX, float playerY,
                       boolean visible, EnemyNavigation navigation, float damage) {
        if (delta <= 0) return;
        float x = body.x + 8, y = body.y + 8;
        float dx = playerX - x, dy = playerY - y, distanceSquared = dx * dx + dy * dy;
        if (phase == Phase.AIMING) {
            // A wall or leaving the viewport cancels the warning rather than producing an unseen shot.
            if (!visible || !navigation.clearProjectileLine(x, y, aimX, aimY, ARROW_RADIUS)) {
                phase = Phase.RECOVERING; remaining = RECOVERY_SECONDS; return;
            }
            remaining = Math.max(0, remaining - delta);
            if (remaining == 0) {
                if (arrows.size() < 2 && (aimX - x) * (aimX - x) + (aimY - y) * (aimY - y) > .01f)
                    arrows.add(new Arrow(x, y, aimX - x, aimY - y, damage));
                phase = Phase.RECOVERING; remaining = RECOVERY_SECONDS;
            }
            return;
        }
        if (phase == Phase.RECOVERING) {
            remaining = Math.max(0, remaining - delta);
            if (remaining == 0) phase = Phase.SEEKING;
        }
        boolean clear = navigation.clearProjectileLine(x, y, playerX, playerY, ARROW_RADIUS);
        if (phase == Phase.SEEKING && visible && distanceSquared <= RANGE * RANGE && clear) {
            aimX = playerX; aimY = playerY; remaining = AIM_SECONDS; phase = Phase.AIMING;
            return;
        }
        if (!clear || distanceSquared > RANGE * RANGE || !visible) {
            navigation.move(body, SPEED, delta);
        } else if (distanceSquared < PREFERRED_DISTANCE * PREFERRED_DISTANCE && distanceSquared > .01f) {
            // Retreat/slide only along a swept, walkable segment. Melee professions can outrun this.
            float length = (float) Math.sqrt(distanceSquared);
            if (!navigation.moveDirection(body, -dx / length, -dy / length, SPEED, delta)
                && !navigation.moveDirection(body, -dy / length, dx / length, SPEED, delta))
                navigation.moveDirection(body, dy / length, -dx / length, SPEED, delta);
        }
    }

    public List<CombatHit> advanceArrows(float delta, EnemyNavigation navigation, Target target) {
        if (delta <= 0 || arrows.isEmpty()) return List.of();
        List<CombatHit> hits = new ArrayList<>();
        var iterator = arrows.iterator();
        while (iterator.hasNext()) {
            Arrow arrow = iterator.next();
            float travelTime = Math.min(delta, 3 - arrow.lifetime);
            int steps = Math.max(1, (int) Math.ceil(ARROW_SPEED * travelTime / ARROW_RADIUS));
            float step = travelTime / steps;
            boolean remove = false;
            for (int index = 0; index < steps; index++) {
                float fromX = arrow.x, fromY = arrow.y;
                float toX = fromX + arrow.velocityX * step, toY = fromY + arrow.velocityY * step;
                if (!navigation.clearProjectileLine(fromX, fromY, toX, toY, ARROW_RADIUS)) {
                    remove = true; break;
                }
                arrow.x = toX; arrow.y = toY;
                if (target.hits(fromX, fromY, toX, toY, ARROW_RADIUS)) {
                    hits.add(new CombatHit(arrow.damage, fromX, fromY, CombatHit.Kind.PROJECTILE, NAME + " / Flecha"));
                    remove = true; break;
                }
            }
            arrow.lifetime += travelTime;
            if (remove || arrow.lifetime >= 3) iterator.remove();
        }
        return hits;
    }
    public void clear() { arrows.clear(); phase = Phase.SEEKING; remaining = 0; }
}

