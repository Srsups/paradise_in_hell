package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.utils.JsonValue;
import java.util.EnumMap;

/** Temporary powers of one attempt. Cooldowns are global per power, never per projectile/target. */
public final class BoonState {
    private final EnumMap<Upgrade,Integer> ranks=new EnumMap<>(Upgrade.class);
    private float armor,armorCooldown,killCooldown,collection,collectionCooldown;
    private float ambush,ambushDamage,ambushCooldown,echoCooldown,pendingEcho,convergenceCooldown;
    public int rank(Upgrade boon){return ranks.getOrDefault(boon,0);}
    public void learn(Upgrade boon){ranks.put(boon,Math.min(boon.maxRank,rank(boon)+1));}
    public void advance(float delta){
        if(!Float.isFinite(delta)||delta<0)throw new IllegalArgumentException("Invalid boon time");
        armor=Math.max(0,armor-delta);armorCooldown=Math.max(0,armorCooldown-delta);
        killCooldown=Math.max(0,killCooldown-delta);
        collection=Math.max(0,collection-delta);collectionCooldown=Math.max(0,collectionCooldown-delta);
        ambush=Math.max(0,ambush-delta);ambushCooldown=Math.max(0,ambushCooldown-delta);
        echoCooldown=Math.max(0,echoCooldown-delta);convergenceCooldown=Math.max(0,convergenceCooldown-delta);
    }
    public boolean armorActive(){return armor>0;}
    public boolean collectionActive(){return collection>0;}
    public boolean ambushReady(){return ambush>0;}
    public boolean furyActive(float health,float maximum){return health>0&&health<=maximum*.35f&&rank(Upgrade.FUROR)>0;}
    public float damageBonus(float health,float maximum){return furyActive(health,maximum)?.25f*rank(Upgrade.FUROR):0;}
    public float receivedMultiplier(){return armorActive()?.7f:1;}
    public void afterDamage(){
        if(rank(Upgrade.AMPARO)>0&&armorCooldown<=0){armor=2;armorCooldown=8;}
    }
    public float staminaOnKill(){
        if(rank(Upgrade.FOLEGO)==0||killCooldown>0)return 0;
        killCooldown=2;return 6*rank(Upgrade.FOLEGO);
    }
    public void dash(float dps){
        if(rank(Upgrade.RASTRO)>0&&collectionCooldown<=0){collection=1.5f;collectionCooldown=3;}
        if(rank(Upgrade.EMBOSCADA)>0&&ambushCooldown<=0){
            ambush=2;ambushDamage=dps*.45f*rank(Upgrade.EMBOSCADA);ambushCooldown=2;
        }
        if(rank(Upgrade.ECO_DO_ESTIGE)>0&&echoCooldown<=0){
            pendingEcho=dps*.6f*rank(Upgrade.ECO_DO_ESTIGE);echoCooldown=3;
        }
    }
    public WeaponBonus beginAttack(){
        float amount=ambushReady()?ambushDamage:0;ambush=0;ambushDamage=0;
        return new WeaponBonus(amount);
    }
    public float takeEcho(){float result=pendingEcho;pendingEcho=0;return result;}
    public boolean tryConvergence(){
        if(rank(Upgrade.CONVERGENCIA)==0||convergenceCooldown>0)return false;
        convergenceCooldown=1.5f;return true;
    }
    public float powerBonus(){
        return .10f*rank(Upgrade.AMPARO)+.03f*rank(Upgrade.FOLEGO)+.03f*rank(Upgrade.RASTRO)
            +.18f*rank(Upgrade.EMBOSCADA)+.15f*rank(Upgrade.CONVERGENCIA)+.20f*rank(Upgrade.ECO_DO_ESTIGE);
    }

    /** Captures the remaining effect windows as well as learned ranks; restoring never re-triggers a boon. */
    public JsonValue checkpointState() {
        JsonValue state=new JsonValue(JsonValue.ValueType.object);
        JsonValue learned=new JsonValue(JsonValue.ValueType.array);state.addChild("ranks",learned);
        for(Upgrade boon:Upgrade.values())if(rank(boon)>0) {
            JsonValue entry=new JsonValue(JsonValue.ValueType.object);
            SnapshotJson.put(entry,"upgrade",boon.name());SnapshotJson.put(entry,"rank",rank(boon));learned.addChild(entry);
        }
        SnapshotJson.put(state,"armor",armor);SnapshotJson.put(state,"armorCooldown",armorCooldown);
        SnapshotJson.put(state,"killCooldown",killCooldown);SnapshotJson.put(state,"collection",collection);
        SnapshotJson.put(state,"collectionCooldown",collectionCooldown);SnapshotJson.put(state,"ambush",ambush);
        SnapshotJson.put(state,"ambushDamage",ambushDamage);SnapshotJson.put(state,"ambushCooldown",ambushCooldown);
        SnapshotJson.put(state,"echoCooldown",echoCooldown);SnapshotJson.put(state,"pendingEcho",pendingEcho);
        SnapshotJson.put(state,"convergenceCooldown",convergenceCooldown);
        return state;
    }
    public void restoreCheckpointState(JsonValue state) {
        SnapshotJson.object(state,"ranks","armor","armorCooldown","killCooldown","collection","collectionCooldown",
            "ambush","ambushDamage","ambushCooldown","echoCooldown","pendingEcho","convergenceCooldown");
        BoonState candidate=new BoonState();
        JsonValue learned=SnapshotJson.array(state,"ranks");
        if(learned.size>Upgrade.values().length)throw SnapshotJson.invalid("dádivas");
        for(JsonValue entry:learned) {
            SnapshotJson.object(entry,"upgrade","rank");
            Upgrade boon=SnapshotJson.enumValue(entry,"upgrade",Upgrade.class);
            if(candidate.ranks.putIfAbsent(boon,SnapshotJson.integer(entry,"rank",1,boon.maxRank))!=null)
                throw SnapshotJson.invalid("dádiva repetida");
        }
        candidate.armor=SnapshotJson.number(state,"armor",0,2);
        candidate.armorCooldown=SnapshotJson.number(state,"armorCooldown",0,8);
        candidate.killCooldown=SnapshotJson.number(state,"killCooldown",0,2);
        candidate.collection=SnapshotJson.number(state,"collection",0,1.5f);
        candidate.collectionCooldown=SnapshotJson.number(state,"collectionCooldown",0,3);
        candidate.ambush=SnapshotJson.number(state,"ambush",0,2);
        candidate.ambushDamage=SnapshotJson.nonNegative(state,"ambushDamage");
        candidate.ambushCooldown=SnapshotJson.number(state,"ambushCooldown",0,2);
        candidate.echoCooldown=SnapshotJson.number(state,"echoCooldown",0,3);
        candidate.pendingEcho=SnapshotJson.nonNegative(state,"pendingEcho");
        candidate.convergenceCooldown=SnapshotJson.number(state,"convergenceCooldown",0,1.5f);
        if(candidate.armor>candidate.armorCooldown||candidate.collection>candidate.collectionCooldown||candidate.ambush>candidate.ambushCooldown)
            throw SnapshotJson.invalid("recarga de dádiva");
        candidate.requireLearned(Upgrade.AMPARO,candidate.armor,candidate.armorCooldown);
        candidate.requireLearned(Upgrade.FOLEGO,candidate.killCooldown);
        candidate.requireLearned(Upgrade.RASTRO,candidate.collection,candidate.collectionCooldown);
        candidate.requireLearned(Upgrade.EMBOSCADA,candidate.ambush,candidate.ambushDamage,candidate.ambushCooldown);
        candidate.requireLearned(Upgrade.ECO_DO_ESTIGE,candidate.echoCooldown,candidate.pendingEcho);
        candidate.requireLearned(Upgrade.CONVERGENCIA,candidate.convergenceCooldown);
        ranks.clear();ranks.putAll(candidate.ranks);
        armor=candidate.armor;armorCooldown=candidate.armorCooldown;killCooldown=candidate.killCooldown;
        collection=candidate.collection;collectionCooldown=candidate.collectionCooldown;
        ambush=candidate.ambush;ambushDamage=candidate.ambushDamage;ambushCooldown=candidate.ambushCooldown;
        echoCooldown=candidate.echoCooldown;pendingEcho=candidate.pendingEcho;convergenceCooldown=candidate.convergenceCooldown;
    }
    private void requireLearned(Upgrade boon,float... values) {
        if(rank(boon)==0)for(float value:values)if(value!=0)throw SnapshotJson.invalid("efeito sem dádiva");
    }
}
