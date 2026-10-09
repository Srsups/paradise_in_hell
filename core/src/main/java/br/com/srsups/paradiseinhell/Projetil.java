package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

public class Projetil {
    public float x, y;
    public final Vector2 velocidade;
    public float dano = 5;
    private final TextureRegion textura;
    private float angulo;
    private final float width, height;
    private Object firstHit, secondHit;
    private int hitCount, maxHits = 1;
    private boolean ricochet;
    private float remainingLife = 3;
    public boolean deveSerRemovido;
    public Projetil(float x, float y, Vector2 direction, TextureRegion texture) {
        this(x, y, direction, texture, WeaponType.BOW);
    }
    public Projetil(float x, float y, Vector2 direction, TextureRegion texture, WeaponType weapon) {
        this.x = x; this.y = y; textura = texture;
        velocidade = new Vector2(direction).scl(weapon.projectileSpeed);
        angulo = direction.angleDeg() - 90;
        width = weapon.projectileWidth; height = weapon.projectileHeight;
    }
    public void configure(PlayerState player) {
        dano = player.getDano();
        if (player.getWeaponRarity() == WeaponRarity.RARE) { maxHits = 2; ricochet = player.getWeapon() == WeaponType.PISTOL; }
    }
    public boolean canHit(Object target) { return target != firstHit && target != secondHit; }
    public void hit(Object target) {
        if (!canHit(target) || deveSerRemovido) return;
        if (hitCount++ == 0) firstHit = target; else secondHit = target;
        if (hitCount >= maxHits) deveSerRemovido = true;
    }
    public boolean needsRicochet() { return ricochet && hitCount == 1 && !deveSerRemovido; }
    public void redirect(float targetX, float targetY) {
        float speed = velocidade.len();
        velocidade.set(targetX - x, targetY - y).nor().scl(speed);
        angulo = velocidade.angleDeg() - 90;
    }
    public void update(float delta, TileMap map) {
        remainingLife -= delta;
        if (remainingLife <= 0) { deveSerRemovido = true; return; }
        // Sample the full movement so faster bullets cannot jump across a wall.
        int steps = Math.max(1, (int) Math.ceil(velocidade.len() * delta / 4));
        for (int step = 0; step < steps; step++) {
            x += velocidade.x * delta / steps;
            y += velocidade.y * delta / steps;
            if (map.ehSolido(x, y)) { deveSerRemovido = true; return; }
        }
    }
    public void draw(SpriteBatch batch) {
        batch.draw(textura, x - width / 2, y - height / 2, width / 2, height / 2,
            width, height, 1, 1, angulo);
    }
}
