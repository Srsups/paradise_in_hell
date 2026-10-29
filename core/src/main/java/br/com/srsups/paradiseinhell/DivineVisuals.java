package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/** Alpha sprites for boons; visible bounds follow their actual combat radius. */
public final class DivineVisuals {
    private DivineVisuals() {}
    public static void ring(SpriteBatch batch,PilotArt art,String key,float x,float y,float radius,float progress,float alpha) {
        batch.setColor(1,1,1,alpha);batch.draw(art.expanded().effect(key,progress),x-radius,y-radius,radius*2,radius*2);
        batch.setColor(Color.WHITE);
    }
    public static void player(SpriteBatch batch,PilotArt art,PlayerState player,float x,float y,float time) {
        float cycle=(time*3)%1;
        if(player.possuiAura())ring(batch,art,"water",x,y,player.getRaioAura(),cycle,.65f);
        if(player.isEscudoAtivo())ring(batch,art,"aegis",x,y,12,cycle,.8f);
        if(player.possuiImaXp())ring(batch,art,"magnet",x,y,15,cycle,.24f);
        if(player.boons().armorActive())ring(batch,art,"aegis",x,y,13,cycle,.4f);
        if(player.boons().collectionActive())ring(batch,art,"magnet",x,y,18,cycle,.55f);
        if(player.boons().ambushReady())ring(batch,art,"lightning",x,y,10,cycle,.3f);
        if(player.boons().furyActive(player.getVida(),player.getVidaMaxima()))
            ring(batch,art,"fire",x,y-3,10,cycle,.25f);
    }
    public static void effects(SpriteBatch batch,PilotArt art,CombatEffects effects,
                               java.util.List<? extends CombatTarget> targets,float time) {
        float cycle=(time*4)%1;
        for(CombatEffects.Echo echo:effects.echoes()){
            ring(batch,art,"water",echo.x,echo.y,CombatEffects.ECHO_RADIUS,echo.progress(),.22f);
            batch.setColor(.35f,.9f,1,.45f);
            batch.draw(art.expanded().effect("resurrect",echo.progress()),echo.x-10,echo.y-10,20,25);
            batch.setColor(Color.WHITE);
        }
        for(CombatTarget target:targets) {
            if(!target.targetable())continue;
            if(effects.burning(target))ring(batch,art,"fire",target.centerX(),target.centerY()+2,11,cycle,.85f);
            if(effects.cursed(target))ring(batch,art,"attack-curse",target.centerX(),target.centerY(),14,cycle,.65f);
        }
        for(CombatEffects.Flash flash:effects.flashes()) {
            float progress=flash.progress();
            if(flash.kind==CombatEffects.Kind.SHOCK) {
                float dx=flash.ox-flash.x,dy=flash.oy-flash.y,length=(float)Math.sqrt(dx*dx+dy*dy);
                batch.setColor(1,1,1,1-progress*.5f);
                WeaponVisuals.sprite(batch,art.expanded().effect("lightning",progress),(flash.x+flash.ox)/2,
                    (flash.y+flash.oy)/2,Math.max(8,length),9,(float)Math.toDegrees(Math.atan2(dy,dx)));
                batch.setColor(Color.WHITE);
            } else {
                String key=switch(flash.kind){case CURSE->"attack-curse";case SPELL->"attack-wand";
                    case FIRE,CONVERGENCE->"fire";case ECHO->"water";case AMBUSH->"lightning";default->"quake";};
                ring(batch,art,key,flash.x,flash.y,flash.ox,progress,1-progress*.5f);
            }
        }
    }
    public static void blessing(SpriteBatch batch,PilotArt art,Upgrade boon,float x,float y,float progress) {
        String key=switch(boon) {
            case POSEIDON->"water";case EGIDE,RESISTENCIA->"aegis";case CHOQUE->"lightning";
            case QUEIMADURA->"fire";case RESSURREICAO->"resurrect";case CURA,VIDA,VAMPIRISMO->"heal";
            case MAGNETISMO,INTELIGENCIA,RASTRO->"magnet";case AMPARO->"aegis";
            case FUROR,CONVERGENCIA->"fire";case FOLEGO->"heal";case EMBOSCADA->"lightning";case ECO_DO_ESTIGE->"water";default->"quake";
        };
        ring(batch,art,key,x,y,18+progress*8,progress,1-progress);
    }
    public static void slash(SpriteBatch batch,PilotArt art,PlayerState player,float x,float y,float angle,float remaining) {
        slash(batch,art,player.getWeapon(),player.meleeReach(),x,y,angle,remaining);
    }
    public static float slashDuration(WeaponType weapon) {
        return weapon == WeaponType.FISTS ? .10f : weapon == WeaponType.RAKE ? .18f : weapon == WeaponType.SCYTHE ? .22f : .14f;
    }
    /** Captured weapon/reach keep an already started swing intact when the farmer changes tools. */
    public static void slash(SpriteBatch batch,PilotArt art,WeaponType weapon,float reach,float x,float y,float angle,float remaining) {
        if(remaining<=0)return;
        float progress=Math.max(0,Math.min(1,1-remaining/slashDuration(weapon)));
        if(weapon==WeaponType.RAKE) {
            float dx=(float)Math.cos(Math.toRadians(angle)),dy=(float)Math.sin(Math.toRadians(angle));
            batch.setColor(1,1,1,.85f);
            WeaponVisuals.sprite(batch,art.expanded().effect("fx-rake",progress),x+dx*reach/2,y+dy*reach/2,reach,16,angle);
            batch.setColor(Color.WHITE);
        } else if(weapon==WeaponType.SCYTHE) {
            batch.setColor(1,1,1,.8f);
            WeaponVisuals.sprite(batch,art.expanded().effect("fx-scythe",progress),x,y,reach*2,reach*2,0);
            batch.setColor(Color.WHITE);
        } else if(weapon==WeaponType.FISTS) {
            float dx=(float)Math.cos(Math.toRadians(angle)),dy=(float)Math.sin(Math.toRadians(angle));
            ring(batch,art,"attack-fists",x+dx*reach,y+dy*reach,7,progress,.9f);
        } else {
            batch.setColor(1,1,1,.8f);
            WeaponVisuals.sprite(batch,art.expanded().effect("attack-sword",progress),x,y,reach*2,reach*2,angle);
            batch.setColor(Color.WHITE);
        }
    }
}

