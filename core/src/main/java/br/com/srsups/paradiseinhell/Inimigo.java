package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Inimigo extends EnemyNavigation.Body {
    public float vida;
    private EnemyType type = EnemyType.COMMON;
    private float stateTime, attackTimer, flashTimer;
    private float speed = 30, size = 16;
    private float damage = 8;
    private final Animation<TextureRegion> animation;
    private final Color tint = new Color(Color.WHITE);

    public Inimigo(float x, float y, Texture atlas) {
        super(x, y);
        configurarTipo(EnemyType.COMMON);
        animation = new Animation<>(0.25f, Art.cell(atlas, 2, 1), Art.cell(atlas, 3, 1));
        animation.setPlayMode(Animation.PlayMode.LOOP);
    }
    public void configurarTipo(int index) { configurarTipo(EnemyType.values()[index]); }
    public void configurarTipo(EnemyType type) {
        this.type = type;
        speed = type.speed; vida = type.health; damage = type.damage; size = type.size;
        tint.set(type == EnemyType.FAST ? Color.CYAN : type == EnemyType.TANK ? Color.GOLD : Color.WHITE);
    }
    public void applyDifficulty(EnemyDifficulty difficulty) {
        vida *= difficulty.healthMultiplier();
        damage *= difficulty.damageMultiplier();
    }
    public EnemyType getType() { return type; }
    public int getXpDrop() { return type.xp; }
    public void update(float delta, EnemyNavigation navigation) {
        stateTime += delta;
        attackTimer -= delta;
        flashTimer -= delta;
        navigation.move(this, speed, delta);
    }
    public void draw(SpriteBatch batch) {
        batch.setColor(flashTimer > 0 ? Color.SALMON : tint);
        batch.draw(animation.getKeyFrame(stateTime), x - (size - 16) / 2, y - (size - 16) / 2, size, size);
        batch.setColor(Color.WHITE);
    }
    public boolean podeAtacar() { return attackTimer <= 0; }
    public void resetarCooldownAtaque() { attackTimer = 1; }
    public float getDano() { return damage; }
    public void sofrerDano(float amount) { vida -= amount; flashTimer = 0.15f; }
    public boolean estaMorto() { return vida <= 0; }
    public void dispose() {}
}
