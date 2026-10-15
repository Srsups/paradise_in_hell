package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/** Provisional bow and arrow silhouettes; no external art dependencies. */
public final class ArcherVisuals {
    private ArcherVisuals() { }
    public static void draw(ShapeRenderer shapes, Inimigo enemy) {
        if (!enemy.isArcher() || enemy.estaMorto()) return;
        ArcherAttack attack = enemy.archer();
        float x = enemy.centerX(), y = enemy.centerY();
        float angle = (float) Math.toDegrees(Math.atan2(attack.aimY() - y, attack.aimX() - x));
        shapes.setColor(.85f, .65f, 1, 1);
        shapes.arc(x, y, 10, angle - 60, 120, 12);
        float first = (float) Math.toRadians(angle - 60), last = (float) Math.toRadians(angle + 60);
        shapes.line(x + (float) Math.cos(first) * 10, y + (float) Math.sin(first) * 10,
            x + (float) Math.cos(last) * 10, y + (float) Math.sin(last) * 10);
        if (attack.phase() == ArcherAttack.Phase.AIMING) {
            float progress = attack.aimProgress();
            if (progress >= .75f) shapes.setColor(1, .25f, .25f, 1);
            else shapes.setColor(1, .7f, .25f, 1);
            shapes.line(x, y, attack.aimX(), attack.aimY());
            shapes.circle(attack.aimX(), attack.aimY(), 8, 20);
            shapes.arc(x, y, 13, -90, Math.max(1, progress * 360), 24);
        }
        shapes.setColor(1, .35f, .3f, 1);
        for (ArcherAttack.Arrow arrow : attack.arrows()) {
            float dx = arrow.directionX(), dy = arrow.directionY(), ax = arrow.x(), ay = arrow.y();
            shapes.line(ax - dx * 7, ay - dy * 7, ax, ay);
            shapes.line(ax, ay, ax - dx * 3 - dy * 2, ay - dy * 3 + dx * 2);
            shapes.line(ax, ay, ax - dx * 3 + dy * 2, ay - dy * 3 - dx * 2);
        }
    }
}

