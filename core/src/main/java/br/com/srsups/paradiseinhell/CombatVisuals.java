package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/** Prototype feedback shared by the two combat screens. Calls occur inside the matching batch. */
final class CombatVisuals {
    static void effects(ShapeRenderer shapes, CombatEffects effects, java.util.List<? extends CombatTarget> targets) {
        for (CombatTarget target : targets) {
            if (!target.targetable()) continue;
            if (effects.burning(target)) { shapes.setColor(Color.ORANGE); shapes.circle(target.centerX(), target.centerY(), 11, 16); }
            if (effects.cursed(target)) { shapes.setColor(Color.MAGENTA); shapes.circle(target.centerX(), target.centerY(), 14, 16); }
        }
        for (CombatEffects.Flash flash : effects.flashes()) {
            shapes.setColor(switch (flash.kind) {
                case SHOCK -> Color.YELLOW; case SPELL -> Color.SKY;
                case IMPACT -> Color.CYAN; case FIRE -> Color.ORANGE; case CURSE -> Color.MAGENTA;
            });
            if (flash.kind == CombatEffects.Kind.SHOCK) shapes.line(flash.x, flash.y, flash.ox, flash.oy);
            else shapes.circle(flash.x, flash.y, flash.ox, 20);
        }
    }
    static void guard(ShapeRenderer shapes, PlayerState player, float x, float y) {
        if (!player.guarding()) return;
        shapes.setColor(Color.SKY);
        shapes.arc(x, y, 15, player.guardAngle() - 60, 120, 24);
    }
    static void allies(SpriteBatch batch, Texture atlas, Necromancy necromancy) {
        batch.setColor(Color.LIME);
        for (Necromancy.Ally ally : necromancy.allies()) {
            float size = Math.min(24, 16 + (float) Math.sqrt(ally.strength));
            batch.draw(Art.cell(atlas, 2 + (int) (ally.remaining * 4) % 2, 1), ally.x - (size - 16) / 2, ally.y - (size - 16) / 2, size, size);
        }
        batch.setColor(Color.WHITE);
    }
}

