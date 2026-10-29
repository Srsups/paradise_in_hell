package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.Vector2;

/** Arena-local movement and graphics. Progression lives in the session. */
public class Jogador {
    @FunctionalInterface public interface AttackEmitter { void attack(float x, float y); }
    public float x, y;
    private final PlayerState stats;
    private final AttackEmitter emitter;
    private final GameControls controls;
    private Animation<TextureRegion> animacaoFrente, animacaoCostas, animacaoLado;
    private float stateTime;
    private final PilotArt pilot;
    private final ActorAnimation motion = new ActorAnimation();
    private final ActorAnimation look=new ActorAnimation();
    private final EquippedArt.Body body;
    private float aimAngle;
    private final PlayerAnimation visual;
    private final ActorReaction reaction=new ActorReaction();
    private final java.util.Map<WeaponType,ReactionArt.Body> reactionBodies=new java.util.EnumMap<>(WeaponType.class);
    private long seenDamage;
    private String reactionActor;
    private ActorAnimation.Direction reactionDirection=ActorAnimation.Direction.DOWN;
    public void aim(float dx,float dy) { if(dx*dx+dy*dy>.001f)aimAngle=(float)Math.toDegrees(Math.atan2(dy,dx)); }
    private final java.util.Map<TextureRegion, SpriteHitbox> hitboxes = new java.util.IdentityHashMap<>();
    public enum Direcao { FRENTE, COSTAS, ESQUERDA, DIREITA, PARADO }
    private Direcao direcaoAtual = Direcao.PARADO;

    public Jogador(float x, float y, Texture spritesheet, PlayerState stats, AttackEmitter emitter) {
        this(x,y,spritesheet,stats,emitter,null);
    }
    public Jogador(float x, float y, Texture spritesheet, PlayerState stats, AttackEmitter emitter, PilotArt art) {
        this(x,y,spritesheet,stats,emitter,art,new GameControls(new ControlSettingsService(ControlSettingsStore.memory())));
    }
    public Jogador(float x, float y, Texture spritesheet, PlayerState stats, AttackEmitter emitter, PilotArt art, GameControls controls) {
        this.controls=java.util.Objects.requireNonNull(controls);
        pilot = art;visual=new PlayerAnimation(stats.getProfession());
        seenDamage=stats.damageRevision();reactionActor=EquippedArt.actor(stats.getProfession(),stats.getWeapon());
        if(pilot!=null) {
            reactionBodies.put(stats.getWeapon(),pilot.reactions().body(reactionActor));
            if(stats.getProfession()==Profession.FAZENDEIRO)for(WeaponType weapon:new WeaponType[]{WeaponType.RAKE,WeaponType.SCYTHE})
                reactionBodies.put(weapon,pilot.reactions().body(EquippedArt.actor(stats.getProfession(),weapon)));
        }
        body = pilot == null ? null : pilot.equipped().body(EquippedArt.actor(stats.getProfession()));
        this.x = x;
        this.y = y;
        this.stats = stats;
        this.emitter = emitter;
        TextureRegion[] framesFrente = new TextureRegion[2];
        framesFrente[0] = Art.cell(spritesheet, 0, 0);
        framesFrente[1] = Art.cell(spritesheet, 1, 0);
        animacaoFrente = new Animation<>(0.25f, framesFrente);
        animacaoFrente.setPlayMode(Animation.PlayMode.LOOP);

        TextureRegion[] framesLado = new TextureRegion[2];
        framesLado[0] = Art.cell(spritesheet, 0, 1);
        framesLado[1] = Art.cell(spritesheet, 1, 1);
        animacaoLado = new Animation<>(0.25f, framesLado);
        animacaoLado.setPlayMode(Animation.PlayMode.LOOP);

        TextureRegion[] framesCostas = new TextureRegion[2];
        framesCostas[0] = Art.cell(spritesheet, 2, 0);
        framesCostas[1] = Art.cell(spritesheet, 3, 0);
        animacaoCostas = new Animation<>(0.25f, framesCostas);
        animacaoCostas.setPlayMode(Animation.PlayMode.LOOP);
        for (Animation<TextureRegion> animation : java.util.List.of(animacaoFrente, animacaoLado, animacaoCostas))
            for (TextureRegion frame : animation.getKeyFrames()) hitboxes.put(frame, Art.hitbox(frame));
    }


    public void update(float delta,TileMap tileMap){update(delta,tileMap,true);}
    public void update(float delta, TileMap tileMap,boolean combatAllowed) {
        stateTime += delta;
        visual.advance(delta);
        reaction.advance(delta);syncReaction();
        float beforeX=x,beforeY=y;
        stats.advance(delta);
        if(combatAllowed&&controls.pressed(ControlAction.ATTACK)&&stats.tryShoot()){
            visual.attack(stats.attacksPerSecond(),stats.getWeapon());emitter.attack(x+8,y+8);
        }
        Vector2 movimento = new Vector2();
        if (controls.pressed(ControlAction.MOVE_UP)) movimento.y += 1;
        if (controls.pressed(ControlAction.MOVE_DOWN)) movimento.y -= 1;
        if (controls.pressed(ControlAction.MOVE_LEFT)) movimento.x -= 1;
        if (controls.pressed(ControlAction.MOVE_RIGHT)) movimento.x += 1;

        if (movimento.len() > 0) {
            movimento.nor();

            // Dash só pode ser iniciado se houver movimento
            if (controls.justPressed(ControlAction.DASH)) {
                stats.tryDash();
            }
            float velocidadeFinal = stats.movementSpeed();
            float novaX = x + movimento.x * velocidadeFinal * delta;
            float novaY = y + movimento.y * velocidadeFinal * delta;

            // Check all four corners before moving along each axis.
            boolean colideX = tileMap.ehSolido(novaX + 1, y + 1) || tileMap.ehSolido(novaX + 15, y + 1) || tileMap.ehSolido(novaX + 1, y + 15) || tileMap.ehSolido(novaX + 15, y + 15);
            boolean colideY = tileMap.ehSolido(x + 1, novaY) || tileMap.ehSolido(x + 15, novaY) || tileMap.ehSolido(x + 1, novaY + 15) || tileMap.ehSolido(x + 15, novaY + 15);

            if (!colideX) x = novaX;
            if (!colideY) y = novaY;

            // Animação
            if (Math.abs(movimento.x) > Math.abs(movimento.y)) {
                direcaoAtual = movimento.x > 0 ? Direcao.DIREITA : Direcao.ESQUERDA;
            } else {
                direcaoAtual = movimento.y > 0 ? Direcao.COSTAS : Direcao.FRENTE;
            }
        } else {
            direcaoAtual = Direcao.PARADO;
        }
        motion.advance(delta,x-beforeX,y-beforeY);
        boolean aiming=combatAllowed&&(controls.pressed(ControlAction.ATTACK)||visual.attacking()||stats.guarding());
        look.advance(delta,aiming?(float)Math.cos(Math.toRadians(aimAngle)):x-beforeX,
            aiming?(float)Math.sin(Math.toRadians(aimAngle)):y-beforeY);
        visual.select(motion,look.direction(),combatAllowed&&stats.guarding(),stats.getWeapon());
    }


    private TextureRegion frame() {
        if(pilot!=null)return pilot.actorFrame(true,motion);
        TextureRegion currentFrame;

        switch (direcaoAtual) {
            case FRENTE:
                currentFrame = animacaoFrente.getKeyFrame(stateTime, true);
                break;
            case COSTAS:
                currentFrame = animacaoCostas.getKeyFrame(stateTime, true);
                break;
            case ESQUERDA:
                currentFrame = animacaoLado.getKeyFrame(stateTime, true);
                if (!currentFrame.isFlipX()) {
                    currentFrame.flip(true, false); // Vira a própria região
                }
                break;
            case DIREITA:
                currentFrame = animacaoLado.getKeyFrame(stateTime, true);
                if (currentFrame.isFlipX()) {
                    currentFrame.flip(true, false); // Desvira a região se ela estiver virada
                }
                break;
            default:
                currentFrame = animacaoFrente.getKeyFrame(0); // parado
        }

        return currentFrame;
    }
    /** Idle preview during refuge panels: no input, stats, shots or movement. */
    public void idle(float delta){
        visual.advance(delta);motion.advance(0,0,0);visual.select(motion,look.direction(),false,stats.getWeapon());
    }
    /** Observe actual damage after the combat tick, including damage followed by healing. */
    public void syncReaction() {
        if(seenDamage==stats.damageRevision())return;
        seenDamage=stats.damageRevision();
        if(!stats.estaMorto()&&reaction.hurt()) {
            reactionActor=visual.dominant().clip().replaceFirst("-(idle|walk|attack)$","");reactionDirection=visual.dominant().direction();
        }
    }
    public ActorReaction reaction(){return reaction;}
    public String deathActor(){return reaction.active()?reactionActor:visual.dominant().clip().replaceFirst("-(idle|walk|attack)$","");}
    public ActorAnimation.Direction deathDirection(){return reaction.active()?reactionDirection:visual.dominant().direction();}
    public float drawBodySize(){return 18;}
    private ReactionArt.Body reactionBody(){
        return reactionBodies.get(reactionActor.equals("farmer-scythe")?WeaponType.SCYTHE
            :reactionActor.equals("farmer-rake")?WeaponType.RAKE:stats.getWeapon());
    }
    public SpriteHitbox hitbox() {
        if(pilot!=null&&reaction.state()==ActorReaction.State.HURT)
            return reactionBody().hitbox(reaction,reactionDirection,x+8,y+8,drawBodySize());
        if(body!=null)return body.hitbox(visual.dominant(),x+8,y+8,18);
        TextureRegion frame = frame();
        return hitboxes.get(frame).place(x, y, 16, 16, frame.isFlipX(), frame.isFlipY());
    }
    public void draw(SpriteBatch batch) {
        if(pilot!=null&&reaction.state()==ActorReaction.State.HURT) {
            if(stats.flashing())batch.setColor(Color.SALMON);
            reactionBody().draw(batch,reaction,reactionDirection,x+8,y+8,drawBodySize());
            batch.setColor(Color.WHITE);return;
        }
        if(body!=null) {
            if(stats.flashing())batch.setColor(Color.SALMON);
            float blend=visual.blend();var tint=batch.getColor();float red=tint.r,green=tint.g,blue=tint.b,alpha=tint.a;
            if(blend<1){batch.setColor(red,green,blue,alpha*(1-blend));body.draw(batch,visual.previous(),x+8,y+8,18);}
            batch.setColor(red,green,blue,alpha*blend);body.draw(batch,visual.current(),x+8,y+8,18);
            batch.setColor(Color.WHITE);
            return;
        }
        TextureRegion currentFrame = frame();
        if (stats.flashing()) {
            batch.setColor(Color.RED);
        }

        if(pilot!=null)batch.draw(currentFrame,x-1,y-1,18,18);
        else batch.draw(currentFrame, x, y, 16, 16);

        // Reset the tint before drawing other entities.
        batch.setColor(Color.WHITE);
    }


}
