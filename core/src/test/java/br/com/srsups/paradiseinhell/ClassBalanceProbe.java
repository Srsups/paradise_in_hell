package br.com.srsups.paradiseinhell;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Repeatable arithmetic/shot baseline. It does not simulate navigation, aiming or a full run. */
public final class ClassBalanceProbe {
    public enum Progression { NEW, DEVELOPED_R3_RARE }
    public record Row(Profession profession,WeaponType weapon,Progression progression,GameDifficulty difficulty,
                      EnemyType enemy,int elapsedSeconds,float damage,float interval,float directDps,float health,
                      float armorPercent,float walkingSpeed,float dashSpeed,float meleeReach,float enemyHealth,
                      int idealWeaponHits,float idealDirectKillSeconds,int meleeRingTargets,float splashDamage,
                      float curseDps,float shieldWalkingSpeed,double weakResurrectionPercent){}
    private record Kill(int hits,float seconds){}
    private static final CombatEffects.LineOfSight CLEAR=(x,y,ox,oy)->true;
    private static final class Target implements CombatTarget {
        private final float x,maximum;
        private float health;
        Target(float x,float maximum){this.x=x;this.maximum=health=maximum;}
        public float centerX(){return x;}
        public float centerY(){return 0;}
        public float health(){return health;}
        public float maxHealth(){return maximum;}
        public float strength(){return 1;}
        public void hurt(float amount){health=Math.max(0,health-amount);}
    }
    private ClassBalanceProbe(){}
    public static PlayerState player(Profession profession,WeaponType weapon,Progression progression){
        PlayerState result=new PlayerState(profession);
        if(weapon!=profession.weapon){
            if(profession!=Profession.FAZENDEIRO||weapon!=WeaponType.SCYTHE)throw new IllegalArgumentException("Invalid class tool");
            result.switchTool();
        }
        if(progression==Progression.DEVELOPED_R3_RARE){
            new PermanentBonuses(3,3,3,3,3,3).apply(result);result.forgeWeapon();
        }
        return result;
    }
    private static PlayerState copy(PlayerState source){return PlayerState.fromCheckpointState(source.checkpointState());}
    public static List<Row> rows(){
        List<Row> result=new ArrayList<>();
        for(Profession profession:Profession.values())for(WeaponType weapon:profession==Profession.FAZENDEIRO
            ?List.of(WeaponType.RAKE,WeaponType.SCYTHE):List.of(profession.weapon))for(Progression progression:Progression.values()){
            PlayerState player=player(profession,weapon,progression);
            PlayerState dash=copy(player);dash.tryDash();float dashSpeed=dash.movementSpeed();
            PlayerState guard=copy(player);guard.setGuard(true,1,0);float shieldSpeed=guard.guarding()?guard.movementSpeed():0;
            int ringTargets=0;
            if(weapon.isMelee())for(int angle=0;angle<360;angle+=45)
                if(WeaponAttack.hits(weapon,0,0,1,0,24*(float)Math.cos(Math.toRadians(angle)),
                    24*(float)Math.sin(Math.toRadians(angle)),player.meleeReach(),0,CLEAR))ringTargets++;
            float splash=measuredSplash(copy(player)),curse=measuredCurse(copy(player));
            Target corpse=new Target(0,10);corpse.hurt(10);
            double resurrection=Necromancy.chance(player,corpse)*100;
            for(GameDifficulty difficulty:GameDifficulty.values())for(int elapsed:new int[]{0,120})for(EnemyType enemy:EnemyType.values()){
                float health=enemy.health*difficulty.apply(EnemyDifficulty.at(elapsed,0)).healthMultiplier();
                Kill kill=directKill(copy(player),health);
                result.add(new Row(profession,weapon,progression,difficulty,enemy,elapsed,player.getDano(),player.getAttackInterval(),
                    player.dps(),player.getVidaMaxima(),player.damageReduction()*100,player.normalWalkingSpeed(),dashSpeed,
                    weapon.isMelee()?player.meleeReach():0,health,kill.hits(),kill.seconds(),ringTargets,splash,curse,shieldSpeed,resurrection));
            }
        }
        return List.copyOf(result);
    }
    private static Kill directKill(PlayerState player,float health){
        Target target=new Target(0,health);int hits=0;float elapsed=0;
        while(!target.dead()){
            if(!player.tryShoot())throw new IllegalStateException("Attack interval and cooldown disagree");
            target.hurt(player.getDano());hits++;
            if(!target.dead()){float interval=player.getAttackInterval();player.advance(interval);elapsed+=interval;}
        }
        return new Kill(hits,elapsed);
    }
    private static float measuredSplash(PlayerState player){
        Target hit=new Target(0,1000),near=new Target(10,1000);
        CombatEffects effects=new CombatEffects();effects.onHit(player,hit,List.of(hit,near),-20,0,CLEAR,true);
        return player.getWeapon()==WeaponType.WAND?1000-near.health():0;
    }
    private static float measuredCurse(PlayerState player){
        if(player.getWeapon()!=WeaponType.CURSE)return 0;
        Target hit=new Target(0,1000);CombatEffects effects=new CombatEffects();
        effects.onHit(player,hit,List.of(hit),-20,0,CLEAR,true);effects.update(1);
        return 1000-hit.health();
    }
    public static String csv(){
        StringBuilder csv=new StringBuilder("profession,weapon,progression,difficulty,enemy,elapsed_seconds,hit_damage,attack_interval,direct_dps,player_health,armor_percent,walking_speed,dash_speed,melee_reach,enemy_health,ideal_weapon_hits,ideal_direct_kill_seconds,melee_ring_targets,splash_damage_per_hit,curse_dps,shield_walking_speed,weak_resurrection_percent\n");
        for(Row row:rows())csv.append(String.format(Locale.ROOT,
            "%s,%s,%s,%s,%s,%d,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%d,%.6f,%d,%.6f,%.6f,%.6f,%.6f%n",
            row.profession(),row.weapon(),row.progression(),row.difficulty(),row.enemy(),row.elapsedSeconds(),row.damage(),row.interval(),
            row.directDps(),row.health(),row.armorPercent(),row.walkingSpeed(),row.dashSpeed(),row.meleeReach(),row.enemyHealth(),
            row.idealWeaponHits(),row.idealDirectKillSeconds(),row.meleeRingTargets(),row.splashDamage(),row.curseDps(),
            row.shieldWalkingSpeed(),row.weakResurrectionPercent()));
        return csv.toString().replace("\r\n","\n");
    }
    public static String notes(){
        return """
            Classes: 360 combinações de nove profissões/dez armas, dois perfis, três dificuldades, três inimigos e tempos 0/120s.
            NEW: sem altar, sem forja e sem dádivas. DEVELOPED_R3_RARE: nível 3 nos seis atributos do altar e forja rara, sem dádivas.
            A forja rara é cenário de comparação; sua obtenção não é garantida. O perfil evoluído não representa um novo save.
            ideal_direct_kill_seconds: somente arma, primeiro acerto em t=0, acerto garantido e espera exata pelo intervalo real de PlayerState.
            Não inclui tempo de voo, mira, deslocamento, obstáculos, animação, procs, DoT, aura, aliados, revide, ameaça recebida ou XP.
            enemy_health usa GameDifficulty.apply(EnemyDifficulty.at(tempo,0)): sala inicial, sem Fúria, vida definida no nascimento.
            As colunas especiais são separadas: splash_damage_per_hit mede um impacto da varinha em um vizinho a 10 unidades;
            curse_dps mede 1s de maldição sem renovação; weak_resurrection_percent usa cadáver de força 1, sem chefe.
            melee_ring_targets mede somente a geometria real de oito pontos a 24 unidades; o soco continua limitado a um alvo na tela.
            melee_reach=0 identifica arma de projétil; shield_walking_speed=0 identifica profissão sem defesa de escudo.
            O Necromante tem dano direto baixo e aliados independentes: dano/recarga dos aliados não escala com Força/Cadência.
            Retorno das dádivas comuns, sorte, cura, defesa e risco de aproximação precisam de análise separada; DPS isolado não classifica profissões.
            Nenhuma linha equivale a uma partida concluída, taxa de vitória ou comprovação de diversão/balanceamento.
            """;
    }
}
