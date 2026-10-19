package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/** Short silhouettes and a damage border; telegraphs and actor positions stay unobstructed. */
public final class FeedbackVisuals {
    private FeedbackVisuals(){}
    public static Color color(WeaponType weapon){return switch(weapon){
        case BOW->Color.GOLD;case SWORD->Color.ORANGE;case PISTOL->Color.WHITE;
        case FISTS->Color.CORAL;case WAND->Color.SKY;case CURSE->Color.MAGENTA;
        case SOUL->Color.LIME;case SHIELD->Color.CYAN;
    };}
    public static void sprites(SpriteBatch batch,PilotArt art,CombatFeedback feedback) {
        for(CombatFeedback.Pulse pulse:feedback.pulses()) {
            if(pulse.kind==CombatFeedback.Kind.TRAIL)continue;
            float progress=pulse.progress();
            if(pulse.kind==CombatFeedback.Kind.XP || pulse.kind==CombatFeedback.Kind.DASH) {
                String effect=pulse.kind==CombatFeedback.Kind.XP?"xp":"dash";float size=effect.equals("xp")?12:20;
                batch.setColor(1,1,1,1-progress);
                WeaponVisuals.sprite(batch,art.effect(effect,progress),pulse.x,pulse.y,size,size,
                    effect.equals("dash")?(float)Math.toDegrees(Math.atan2(pulse.oy-pulse.y,pulse.ox-pulse.x)):0);
                batch.setColor(Color.WHITE);continue;
            }
            String key=switch(pulse.kind) {
                case BLOCK->"attack-shield";case PROTECTED->"aegis";case RESURRECTION->"resurrect";
                case RETALIATION->"lightning";default->"attack-"+WeaponVisuals.key(pulse.weapon);
            };
            float size=pulse.kind==CombatFeedback.Kind.RESURRECTION?48: pulse.kind==CombatFeedback.Kind.PROTECTED?28:16;
            if(pulse.kind==CombatFeedback.Kind.RETALIATION) {
                float dx=pulse.ox-pulse.x,dy=pulse.oy-pulse.y,length=(float)Math.sqrt(dx*dx+dy*dy);
                WeaponVisuals.sprite(batch,art.expanded().effect(key,progress),(pulse.x+pulse.ox)/2,(pulse.y+pulse.oy)/2,
                    Math.max(8,length),9,(float)Math.toDegrees(Math.atan2(dy,dx)));
            } else {
                batch.setColor(1,1,1,1-progress*.7f);
                WeaponVisuals.sprite(batch,art.expanded().effect(key,progress),pulse.x,pulse.y,size,size,
                    pulse.kind==CombatFeedback.Kind.ATTACK?(float)Math.toDegrees(Math.atan2(pulse.oy,pulse.ox)):0);
            }
            batch.setColor(Color.WHITE);
        }
    }
    public static void labels(SpriteBatch batch,BitmapFont font,CombatFeedback feedback) {
        font.getData().setScale(.65f);
        java.util.EnumSet<CombatFeedback.Kind> drawn=java.util.EnumSet.noneOf(CombatFeedback.Kind.class);
        for(int index=feedback.pulses().size()-1;index>=0;index--) {
            CombatFeedback.Pulse p=feedback.pulses().get(index);
            if(!drawn.add(p.kind))continue;
            String text=switch(p.kind){case BLOCK->"DEFESA";case PROTECTED->"PROTEGIDO";case RETALIATION->"REVIDE";default->null;};
            if(text!=null){font.setColor(Color.SKY);font.draw(batch,text,p.x-15,p.y+23+p.progress()*9);}
        }
        font.setColor(Color.WHITE);
    }
    public static void damageBorder(ShapeRenderer shapes,CombatFeedback feedback) {
        if(feedback.hurtFlash()==0)return;
        Gdx.gl.glEnable(GL20.GL_BLEND);Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA,GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);shapes.setColor(1,.1f,.1f,.4f*feedback.hurtFlash()/.18f);
        shapes.rect(0,0,960,7);shapes.rect(0,533,960,7);shapes.rect(0,7,7,526);shapes.rect(953,7,7,526);
        shapes.end();Gdx.gl.glDisable(GL20.GL_BLEND);
    }
}

