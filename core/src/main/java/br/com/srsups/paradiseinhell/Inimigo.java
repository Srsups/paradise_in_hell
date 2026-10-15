package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Inimigo extends EnemyNavigation.Body implements CombatTarget {
    public float vida;
    private ArcherAttack archer;
    private EnemyType type = EnemyType.COMMON;
    private float stateTime, attackTimer, flashTimer;
    private final float pursuitOffset;
    private float speed = 30, size = 16;
    private float damage = 8, maxHealth = 10;
    private final Animation<TextureRegion> animation;
    private final SpriteHitbox[] hitboxes;
    private final Color tint = new Color(Color.WHITE);

    public Inimigo(float x, float y, Texture atlas) {
        super(x, y);
        pursuitOffset = FastPursuit.offset(x, y);
        configurarTipo(EnemyType.COMMON);
        animation = new Animation<>(0.25f, Art.cell(atlas, 2, 1), Art.cell(atlas, 3, 1));
        animation.setPlayMode(Animation.PlayMode.LOOP);
        hitboxes = new SpriteHitbox[]{Art.hitbox(animation.getKeyFrames()[0]), Art.hitbox(animation.getKeyFrames()[1])};
    }
    public void configurarTipo(int index) { configurarTipo(EnemyType.values()[index]); }
    public void configurarTipo(EnemyType type) {
        this.type = type;
        if (archer != null) archer.clear();
        archer = null;
        speed = type.speed; vida = maxHealth = type.health; damage = type.damage; size = type.size;
        tint.set(type == EnemyType.FAST ? Color.CYAN : type == EnemyType.TANK ? Color.GOLD : Color.WHITE);
    }
    public void configureArcher() {
        configurarTipo(EnemyType.COMMON);
        archer = new ArcherAttack();
        speed = ArcherAttack.SPEED; vida = maxHealth = ArcherAttack.HEALTH;
        damage = ArcherAttack.CONTACT_DAMAGE; tint.set(.75f, .55f, 1, 1);
    }
    public boolean isArcher() { return archer != null; }
    public ArcherAttack archer() { return archer; }
    public String displayName() { return isArcher() ? ArcherAttack.NAME : type.label; }
    public int getSoulDrop() { return isArcher() ? ArcherAttack.SOULS : SoulRewards.enemy(type); }
    public void applyDifficulty(EnemyDifficulty difficulty) {
        vida *= difficulty.healthMultiplier(); maxHealth = vida;
        damage *= difficulty.damageMultiplier();
    }
    public float centerX() { return x + 8; }
    public float centerY() { return y + 8; }
    public float health() { return vida; }
    public float maxHealth() { return maxHealth; }
    public float strength() {
        return (isArcher() ? 1.8f : type == EnemyType.FAST ? 1.4f : type == EnemyType.TANK ? 4 : 1)
            * (float) Math.sqrt(maxHealth / (isArcher() ? ArcherAttack.HEALTH : type.health));
    }
    public void hurt(float amount) { sofrerDano(amount); }
    public void push(float dx, float dy, CombatEffects.LineOfSight line) {
        // Split the displacement so structures always stop the push.
        for (int i = 0; i < 8; i++) {
            if (!line.clear(centerX(), centerY(), centerX() + dx / 8, centerY() + dy / 8)) break;
            x += dx / 8; y += dy / 8;
        }
    }
    public EnemyType getType() { return type; }
    public int getXpDrop() { return isArcher() ? ArcherAttack.XP : type.xp; }
    public void update(float delta, EnemyNavigation navigation) { update(delta, navigation, true); }
    public void update(float delta, EnemyNavigation navigation, boolean visible) {
        stateTime += delta; attackTimer -= delta; flashTimer -= delta;
        if (estaMorto()) { if (archer != null) archer.clear(); return; }
        if (archer == null) navigation.move(this, type == EnemyType.FAST ? FastPursuit.speed(stateTime, pursuitOffset) : speed, delta);
        else archer.update(delta, this, navigation.targetX(), navigation.targetY(), visible, navigation, arrowDamage());
    }
    public float arrowDamage() { return isArcher() ? damage / ArcherAttack.CONTACT_DAMAGE * ArcherAttack.ARROW_DAMAGE : 0; }
    public SpriteHitbox hitbox() {
        return hitboxes[animation.getKeyFrameIndex(stateTime)].place(x - (size - 16) / 2, y - (size - 16) / 2, size, size, false, false);
    }
    public void draw(SpriteBatch batch) {
        batch.setColor(flashTimer > 0 ? Color.SALMON
            : type == EnemyType.FAST && FastPursuit.speed(stateTime,pursuitOffset) == FastPursuit.RECOVERY_SPEED ? Color.TEAL : tint);
        batch.draw(animation.getKeyFrame(stateTime), x - (size - 16) / 2, y - (size - 16) / 2, size, size);
        batch.setColor(Color.WHITE);
    }
    public boolean podeAtacar() { return attackTimer <= 0; }
    public void resetarCooldownAtaque() { attackTimer = 1; }
    public float getDano() { return damage; }
    public void sofrerDano(float amount) { vida -= amount; flashTimer = 0.15f; }
    public boolean estaMorto() { return vida <= 0; }
    public void dispose() { if (archer != null) archer.clear(); }
}
