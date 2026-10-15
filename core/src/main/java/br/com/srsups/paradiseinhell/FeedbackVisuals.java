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
    public static void pulses(ShapeRenderer shapes,CombatFeedback feedback) {
        for(CombatFeedback.Pulse p:feedback.pulses()) {
            Color color=color(p.weapon);
            if(p.kind==CombatFeedback.Kind.BLOCK || p.kind==CombatFeedback.Kind.PROTECTED)color=Color.SKY;
            float fade=.4f+.6f*(1-p.progress());
            shapes.setColor(color.r*fade,color.g*fade,color.b*fade,1);
            float radius=3+p.progress()*8;
            switch(p.kind) {
                case TRAIL -> shapes.line(p.ox,p.oy,p.x,p.y);
                case DASH -> {shapes.circle(p.x,p.y,7*(1-p.progress()),12);shapes.line(p.x,p.y,p.ox,p.oy);}
                case BLOCK -> {
                    float angle=(float)Math.toDegrees(Math.atan2(p.oy-p.y,p.ox-p.x));
                    shapes.arc(p.x,p.y,16+p.progress()*4,angle-45,90,12);
                    shapes.line(p.x+(float)Math.cos(Math.toRadians(angle))*14,p.y+(float)Math.sin(Math.toRadians(angle))*14,
                        p.x+(float)Math.cos(Math.toRadians(angle))*23,p.y+(float)Math.sin(Math.toRadians(angle))*23);
                }
                case PROTECTED -> shapes.circle(p.x,p.y,14+p.progress()*4,24);
                case RETALIATION -> {shapes.line(p.x,p.y,p.ox,p.oy);shapes.circle(p.ox,p.oy,radius,16);}
                case XP -> shapes.circle(p.x,p.y,radius/2,12);
                case RESURRECTION -> shapes.circle(p.x,p.y,8+p.progress()*28,32);
                case ATTACK,HIT -> {
                    for(int i=0;i<4;i++) {
                        float angle=i*(float)Math.PI/2+.35f;
                        float dx=(float)Math.cos(angle),dy=(float)Math.sin(angle);
                        shapes.line(p.x+dx*radius*.45f,p.y+dy*radius*.45f,p.x+dx*radius,p.y+dy*radius);
                    }
                }
            }
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

