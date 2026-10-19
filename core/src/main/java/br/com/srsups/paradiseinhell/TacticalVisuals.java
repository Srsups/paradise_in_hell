package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/** Textured animated warnings use the same centers, radii and paths as damage. No shape rendering. */
public final class TacticalVisuals {
    private TacticalVisuals(){}
    public static void ring(SpriteBatch batch,PilotArt art,String key,float progress,float x,float y,float radius,boolean red){
        if(red)batch.setColor(1,.32f,.25f,1);else batch.setColor(Color.WHITE);
        float size=radius*2*64/62;
        batch.draw(art.equipped().effect(key,red?1:progress),x-size/2,y-size/2,size,size);
        batch.setColor(Color.WHITE);
    }
    private static void segment(SpriteBatch batch,PilotArt art,String key,float progress,float x,float y,float tx,float ty,float width){
        float dx=tx-x,dy=ty-y,length=(float)Math.sqrt(dx*dx+dy*dy);
        if(length<.01f)return;
        WeaponVisuals.sprite(batch,art.equipped().effect(key,progress),(x+tx)/2,(y+ty)/2,length,width,
            (float)Math.toDegrees(Math.atan2(dy,dx)));
    }
    private static void fan(SpriteBatch batch,PilotArt art,float progress,float x,float y,float angle,float length,float halfSpread){
        float dx=(float)Math.cos(angle),dy=(float)Math.sin(angle);
        WeaponVisuals.sprite(batch,art.equipped().effect("stone-fan",progress),x+dx*length/2,y+dy*length/2,
            length,2*length*(float)Math.tan(Math.toRadians(halfSpread)),(float)Math.toDegrees(angle));
    }
    public static void archer(SpriteBatch batch,PilotArt art,Inimigo enemy){
        if(!enemy.isArcher()||enemy.estaMorto()||enemy.archer().phase()!=ArcherAttack.Phase.AIMING)return;
        var attack=enemy.archer();float progress=attack.aimProgress();
        ring(batch,art,"archer-target",progress,attack.aimX(),attack.aimY(),8,progress>=.75f);
    }
    public static void mini(SpriteBatch batch,PilotArt art,MiniBossEncounter mini){
        boolean warning=mini.state()==MiniBossEncounter.State.TELEGRAPH,active=mini.state()==MiniBossEncounter.State.ACTIVE;
        if(!warning&&!active)return;
        if(mini.attack()==MiniBossEncounter.Attack.SPIN)
            ring(batch,art,"mini-spin-warning",mini.warningProgress(),mini.centerX(),mini.centerY(),MiniBossEncounter.SPIN_RADIUS,active);
        else if(warning)fan(batch,art,mini.warningProgress(),mini.centerX(),mini.centerY(),mini.aimAngle(),62,38);
    }
    public static void boss(SpriteBatch batch,PilotArt art,BossEncounter boss){
        boolean warning=boss.state()==BossEncounter.State.TELEGRAPH,active=boss.state()==BossEncounter.State.ACTIVE;
        for(var mark:boss.marks())ring(batch,art,"boss-mark",boss.warningProgress(),mark.x(),mark.y(),mark.radius(),!warning);
        if(boss.leapVisible()){
            ring(batch,art,"leap-target",1-boss.leapSecondsRemaining()/BossEncounter.LEAP_DURATION,boss.leapX(),boss.leapY(),boss.leapRadius(),boss.leapLocked()||!boss.airborne());
            if(!boss.airborne())ring(batch,art,"landing-impact",boss.activeProgress(),boss.leapX(),boss.leapY(),BossEncounter.LEAP_MAX_RADIUS,false);
        }
        if((warning||active)&&boss.attack()==BossEncounter.Attack.CHARGE)
            segment(batch,art,"charge-lane",boss.warningProgress(),boss.startX(),boss.startY(),boss.endX(),boss.endY(),2*(BossEncounter.RADIUS+BossEncounter.PLAYER_RADIUS));
        if(warning&&boss.attack()==BossEncounter.Attack.VOLLEY)
            fan(batch,art,boss.warningProgress(),boss.x(),boss.y(),boss.aimAngle(),82,boss.enraged()?56:34);
    }
}

