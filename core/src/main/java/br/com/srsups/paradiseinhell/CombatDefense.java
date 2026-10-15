package br.com.srsups.paradiseinhell;

import java.util.List;
import java.util.random.RandomGenerator;

/** A blocked projectile cannot mask an overlapping, unblockable ground attack. */
public final class CombatDefense {
    @FunctionalInterface public interface Observer {
        void resolved(CombatHit hit,boolean blocked,float actual,boolean retaliated);
    }
    private CombatDefense() {}
    public static boolean resolve(PlayerState player, List<CombatHit> hits, float x, float y,
                                  CombatTarget source, RandomGenerator random) {
        return resolve(player,hits,x,y,source,random,DamageSink.DIRECT);
    }
    public static boolean resolve(PlayerState player,List<CombatHit> hits,float x,float y,CombatTarget source,
                                  RandomGenerator random,DamageSink damageSink) {
        return resolve(player,hits,x,y,source,random,damageSink,(hit,blocked,actual,retaliated)->{});
    }
    public static boolean resolve(PlayerState player,List<CombatHit> hits,float x,float y,CombatTarget source,
                                  RandomGenerator random,DamageSink damageSink,Observer observer) {
        boolean retaliated = false;
        var ordered = hits.stream().sorted(java.util.Comparator.comparing(CombatHit::damage).reversed()).toList();
        for (CombatHit hit : ordered) {
            float before=Math.max(0,player.getVida());
            boolean blocked=player.receive(hit,x,y),counter=false;
            if(blocked && player.tryRetaliation(random)) {
                damageSink.deal(source,player.getDano()*1.5f,DamageSource.RETALIATION);counter=retaliated=true;
            }
            observer.resolved(hit,blocked,Math.max(0,before-Math.max(0,player.getVida())),counter);
            if(!blocked)break; // Original contact invulnerability limits this frame to one unblocked hit.
        }
        return retaliated;
    }
}

