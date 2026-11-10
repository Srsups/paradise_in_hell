package br.com.srsups.paradiseinhell;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Sustained attacks must not lose innate splash when a cadence rank is purchased. */
class WandCadenceTest {
    private static final class Target implements CombatTarget {
        private final float x;
        Target(float x){this.x=x;}
        public float centerX(){return x;}
        public float centerY(){return 0;}
        public float health(){return 1000;}
        public float maxHealth(){return 1000;}
        public float strength(){return 1;}
        public void hurt(float amount){}
    }
    private static double sustainedSplash(PlayerState player,int fps) {
        Target hit=new Target(0),near=new Target(10);
        double[] dealt={0};
        CombatEffects effects=new CombatEffects((target,damage,source)->{
            if(source==DamageSource.SPELL&&target==near)dealt[0]+=damage;
        });
        var targets=List.of(hit,near);
        for(int frame=0;frame<60*fps;frame++){
            if(frame>0){player.advance(1f/fps);effects.update(1f/fps);}
            if(player.tryShoot())effects.onHit(player,hit,targets,-20,0,(x,y,ox,oy)->true,true);
        }
        return dealt[0]/60;
    }
    private static PlayerState wand(WeaponRarity rarity,int cadence){
        PlayerState player=new PlayerState(Profession.FEITICEIRA);
        if(rarity==WeaponRarity.RARE)player.forgeWeapon();
        for(int rank=0;rank<cadence;rank++)Upgrade.CADENCIA.apply(player);
        return player;
    }
    @Test void everyCadenceRankKeepsOrImprovesSustainedSplashAtSixtyAndOneHundredTwentyHz(){
        for(int fps:new int[]{60,120})for(WeaponRarity rarity:WeaponRarity.values()){
            double previous=0;
            for(int rank=0;rank<=Upgrade.CADENCIA.maxRank;rank++){
                double current=sustainedSplash(wand(rarity,rank),fps);
                assertTrue(current>=previous,rarity+" rank "+rank+" at "+fps+"Hz: "+previous+" -> "+current);
                previous=current;
            }
        }
    }
    @Test void thePreviouslyRegressingForgedSixthCadenceRankIsNowAnImprovement(){
        for(int fps:new int[]{60,120})
            assertTrue(sustainedSplash(wand(WeaponRarity.RARE,6),fps)>sustainedSplash(wand(WeaponRarity.RARE,5),fps));
    }
    @Test void commonBaseSplashKeepsItsPreviousSustainedBudgetAndDamageScalesLinearly(){
        for(int fps:new int[]{60,120}){
            double base=sustainedSplash(wand(WeaponRarity.COMMON,0),fps);
            assertEquals(4d/3,base,.04);
            PlayerState stronger=wand(WeaponRarity.COMMON,0);Upgrade.FORCA.apply(stronger);
            assertEquals(base*1.2,sustainedSplash(stronger,fps),.00001);
        }
    }
    @Test void splashCannotBeRepeatedByDuplicateImpactsAndCooldownStillRoundTrips(){
        PlayerState player=wand(WeaponRarity.RARE,6);
        assertTrue(player.trySpell());assertFalse(player.trySpell());
        PlayerState restored=PlayerState.fromCheckpointState(player.checkpointState());
        assertFalse(restored.trySpell());
        player.advance(player.getAttackInterval());restored.advance(restored.getAttackInterval());
        assertTrue(player.trySpell());assertTrue(restored.trySpell());
        assertFalse(restored.trySpell());
    }
    @Test void otherProfessionsDoNotAcquireAnInnateSpellExplosion(){
        for(Profession profession:Profession.values()){
            if(profession==Profession.FEITICEIRA)continue;
            Target hit=new Target(0),near=new Target(10);float[] spell={0};
            CombatEffects effects=new CombatEffects((target,damage,source)->{if(source==DamageSource.SPELL)spell[0]+=damage;});
            effects.onHit(new PlayerState(profession),hit,List.of(hit,near),-20,0,(x,y,ox,oy)->true,true);
            assertEquals(0,spell[0],profession.name());
        }
    }
}
