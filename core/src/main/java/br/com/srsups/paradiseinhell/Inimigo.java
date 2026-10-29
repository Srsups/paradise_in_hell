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
    private final PilotArt pilot;
    private final ActorAnimation motion = new ActorAnimation();
    private ExpandedArt.Body body;
    private EquippedArt.Body archerBody;
    private float releaseAnimation;
    private String visualKind;
    private ReactionArt.Body reactionBody;
    private final ActorReaction reaction=new ActorReaction();
    private ActorAnimation.Direction reactionDirection=ActorAnimation.Direction.DOWN;
    public ActorReaction reaction(){return reaction;}
    public String deathActor(){return visualKind!=null?visualKind:isArcher()?"archer":type.name().toLowerCase(java.util.Locale.ROOT);}
    public ActorAnimation.Direction deathDirection(){return reaction.active()?reactionDirection:currentDirection();}
    public float drawBodySize(){return size+2;}
    private ActorAnimation.Direction currentDirection(){
        return archerActing()?ActorAnimation.facing(archer.aimX()-centerX(),archer.aimY()-centerY()):motion.dominantDirection();
    }
    public String visualKind() { return visualKind; }
    private void refreshBody() {
        if(pilot==null)return;
        String kind=isArcher()?"archer":type.name().toLowerCase(java.util.Locale.ROOT);
        if(!kind.equals(visualKind)){
            visualKind=kind;body=pilot.expanded().body(kind);
            reactionBody=pilot.reactions().body(kind);reaction.clear();
            archerBody=isArcher()?pilot.equipped().body("archer"):null;releaseAnimation=0;
        }
    }
    private boolean usesPilot() { return pilot != null; }
    private final float pursuitOffset;
    private float speed = 30, size = 16;
    private float damage = 8, maxHealth = 10;
    private final Animation<TextureRegion> animation;
    private final SpriteHitbox[] hitboxes;
    private final Color tint = new Color(Color.WHITE);

    public Inimigo(float x, float y, Texture atlas) { this(x,y,atlas,null); }
    public Inimigo(float x, float y, Texture atlas, PilotArt pilot) {
        super(x, y);
        this.pilot=pilot;
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
        refreshBody();
    }
    public void configureArcher() {
        configurarTipo(EnemyType.COMMON);
        archer = new ArcherAttack();
        speed = ArcherAttack.SPEED; vida = maxHealth = ArcherAttack.HEALTH;
        damage = ArcherAttack.CONTACT_DAMAGE; tint.set(.75f, .55f, 1, 1);refreshBody();
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
        reaction.advance(delta);
        stateTime += delta; attackTimer -= delta; flashTimer -= delta;releaseAnimation=Math.max(0,releaseAnimation-delta);
        float beforeX=x,beforeY=y;
        if (estaMorto()) { if (archer != null) archer.clear(); return; }
        if (archer == null) navigation.move(this, type == EnemyType.FAST ? FastPursuit.speed(stateTime, pursuitOffset) : speed, delta);
        else {
            int fired=archer.shotsFired();
            archer.update(delta,this,navigation.targetX(),navigation.targetY(),visible,navigation,arrowDamage());
            if(archer.shotsFired()!=fired)releaseAnimation=.22f;
        }
        if(usesPilot())motion.advance(delta,x-beforeX,y-beforeY);
    }
    public float arrowDamage() { return isArcher() ? damage / ArcherAttack.CONTACT_DAMAGE * ArcherAttack.ARROW_DAMAGE : 0; }
    private boolean archerActing(){return isArcher()&&(archer.phase()==ArcherAttack.Phase.AIMING||releaseAnimation>0);}
    private EquippedArt.Pose archerPose(){
        if(!archerActing())return archerBody.walk(motion);
        var facing=ActorAnimation.facing(archer.aimX()-centerX(),archer.aimY()-centerY());
        float progress=archer.phase()==ArcherAttack.Phase.AIMING?archer.aimProgress()*4f/6:4f/6+(1-releaseAnimation/.22f)*2f/6;
        return archerBody.pose("attack",facing,progress);
    }
    public SpriteHitbox hitbox() {
        if(usesPilot()&&reaction.state()==ActorReaction.State.HURT)
            return reactionBody.hitbox(reaction,reactionDirection,centerX(),centerY(),drawBodySize());
        if(archerBody!=null)return archerBody.hitbox(archerPose(),centerX(),centerY(),size+2);
        if(usesPilot())return body.hitbox(motion,centerX(),centerY(),size+2);
        return hitboxes[animation.getKeyFrameIndex(stateTime)].place(x - (size - 16) / 2, y - (size - 16) / 2, size, size, false, false);
    }
    public void draw(SpriteBatch batch) {
        if(usesPilot()&&reaction.active()) {
            batch.setColor(reaction.state()==ActorReaction.State.HURT&&flashTimer>0?Color.SALMON:Color.WHITE);
            reactionBody.draw(batch,reaction,reactionDirection,centerX(),centerY(),drawBodySize());
            batch.setColor(Color.WHITE);return;
        }
        batch.setColor(flashTimer > 0 ? Color.SALMON
            : type == EnemyType.FAST && FastPursuit.speed(stateTime,pursuitOffset) == FastPursuit.RECOVERY_SPEED ? Color.TEAL : tint);
        if(usesPilot()) {
            batch.setColor(flashTimer>0?Color.SALMON:Color.WHITE);
            if(archerBody!=null){
                if(archerActing())archerBody.draw(batch,archerPose(),centerX(),centerY(),size+2);
                else archerBody.draw(batch,motion,centerX(),centerY(),size+2);
            }else body.draw(batch,motion,centerX(),centerY(),size+2);
        }
        else batch.draw(animation.getKeyFrame(stateTime), x - (size - 16) / 2, y - (size - 16) / 2, size, size);
        batch.setColor(Color.WHITE);
    }
    public boolean podeAtacar() { return attackTimer <= 0; }
    public void resetarCooldownAtaque() { attackTimer = 1; }
    public float getDano() { return damage; }
    public void sofrerDano(float amount) {
        float before=vida;vida -= amount;flashTimer = 0.15f;
        if(before>0&&amount>0) {
            var direction=deathDirection();
            if(estaMorto()){if(reaction.die())reactionDirection=direction;}
            else if(reaction.hurt())reactionDirection=direction;
        }
    }
    public boolean estaMorto() { return vida <= 0; }
    public void dispose() { if (archer != null) archer.clear(); }
}
