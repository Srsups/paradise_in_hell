package br.com.srsups.paradiseinhell;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import com.badlogic.gdx.utils.JsonValue;

/** Bounded room aggregates, without retaining enemies, screens or per-frame events. */
public final class RunStatistics implements DamageSink {
    private static final class Room {
        String id, title;
        float duration, entryLife, exitLife, dealt, received;
        int kills;
        Float unlockedAt;
        BossEncounter boss;
        BossFightSummary bossFight;
        void close(float life) {
            exitLife=life;
            if(boss!=null) {bossFight=boss.summary();boss=null;}
        }
        RunReport.Room snapshot() {
            return new RunReport.Room(id,title,duration,kills,entryLife,exitLife,dealt,received,unlockedAt,bossFight);
        }
    }
    private final List<Room> rooms = new ArrayList<>();
    private final EnumMap<DamageSource,Float> dealt = new EnumMap<>(DamageSource.class);
    private float received;
    private String lastCause = "Dano não identificado";
    private int resurrections;
    private boolean ended;
    public void enterRoom(String id, String title, float life) {
        if (ended) throw new IllegalStateException("Report already ended");
        if (!rooms.isEmpty()) rooms.getLast().close(life);
        Room room = new Room(); room.id=id; room.title=title; room.entryLife=room.exitLife=life; rooms.add(room);
    }
    public void observeBoss(BossEncounter boss) {
        if(ended)throw new IllegalStateException("Report already ended");
        rooms.getLast().boss=java.util.Objects.requireNonNull(boss);
    }
    public void advance(float delta) { if (!ended) rooms.getLast().duration += delta; }
    public void kill() { if (!ended) rooms.getLast().kills++; }
    public void unlockExit() { if (!ended && rooms.getLast().unlockedAt==null) rooms.getLast().unlockedAt=rooms.getLast().duration; }
    @Override public void deal(CombatTarget target, float amount, DamageSource source) {
        if (!Float.isFinite(amount) || amount<0) throw new IllegalArgumentException("Invalid damage");
        float before=Math.max(0,target.health());
        target.hurt(amount);
        float actual=Math.max(0,before-Math.max(0,target.health()));
        if (!ended && actual>0) {
            dealt.merge(source,actual,Float::sum); rooms.getLast().dealt+=actual;
        }
    }
    public void receive(float actual, String cause) {
        if (!ended && actual>0) { received+=actual; rooms.getLast().received+=actual; lastCause=cause; }
    }
    public void resurrect() { if (!ended) resurrections++; }
    public void end(float life) { if (!ended) { rooms.getLast().close(Math.max(0,life)); ended=true; } }
    public JsonValue checkpointState(){
        JsonValue root=new JsonValue(JsonValue.ValueType.object);
        SnapshotJson.put(root,"received",received);SnapshotJson.put(root,"lastCause",lastCause);
        SnapshotJson.put(root,"resurrections",resurrections);SnapshotJson.put(root,"ended",ended);
        JsonValue damage=new JsonValue(JsonValue.ValueType.object);
        for(var entry:dealt.entrySet())SnapshotJson.put(damage,entry.getKey().name(),entry.getValue());root.addChild("dealt",damage);
        JsonValue values=new JsonValue(JsonValue.ValueType.array);
        for(Room room:rooms){
            JsonValue value=new JsonValue(JsonValue.ValueType.object);
            SnapshotJson.put(value,"id",room.id);SnapshotJson.put(value,"title",room.title);
            SnapshotJson.put(value,"duration",room.duration);SnapshotJson.put(value,"kills",room.kills);
            SnapshotJson.put(value,"entryLife",room.entryLife);SnapshotJson.put(value,"exitLife",room.exitLife);
            SnapshotJson.put(value,"dealt",room.dealt);SnapshotJson.put(value,"received",room.received);
            if(room.unlockedAt==null)value.addChild("unlockedAt",new JsonValue(JsonValue.ValueType.nullValue));else SnapshotJson.put(value,"unlockedAt",room.unlockedAt);
            BossFightSummary boss=room.boss==null?room.bossFight:room.boss.summary();
            value.addChild("bossFight",boss==null?new JsonValue(JsonValue.ValueType.nullValue):boss.json());values.addChild(value);
        }
        root.addChild("rooms",values);return root;
    }
    public void restoreCheckpointState(JsonValue root){
        SnapshotJson.object(root,"received","lastCause","resurrections","ended","dealt","rooms");
        received=SnapshotJson.nonNegative(root,"received");lastCause=SnapshotJson.text(root,"lastCause");
        if(lastCause.length()>2000)throw SnapshotJson.invalid("causa do dano");
        resurrections=SnapshotJson.integer(root,"resurrections",0,Integer.MAX_VALUE);ended=SnapshotJson.flag(root,"ended");
        JsonValue damage=SnapshotJson.required(root,"dealt");
        if(!damage.isObject()||damage.size>DamageSource.values().length)throw SnapshotJson.invalid("dano causado");
        dealt.clear();java.util.Set<String> seen=new java.util.HashSet<>();
        for(JsonValue entry:damage){
            if(!seen.add(entry.name))throw SnapshotJson.invalid("dano repetido");
            DamageSource source;
            try{source=DamageSource.valueOf(entry.name);}catch(IllegalArgumentException invalid){throw SnapshotJson.invalid("origem de dano");}
            dealt.put(source,SnapshotJson.nonNegative(damage,entry.name));
        }
        JsonValue values=SnapshotJson.array(root,"rooms");
        if(values.size<1||values.size>64)throw SnapshotJson.invalid("salas das estatísticas");
        rooms.clear();
        for(JsonValue value:values){
            SnapshotJson.object(value,"id","title","duration","kills","entryLife","exitLife","dealt","received","unlockedAt","bossFight");
            Room room=new Room();room.id=SnapshotJson.text(value,"id");room.title=SnapshotJson.text(value,"title");
            if(room.id.isBlank()||room.title.isBlank()||room.id.length()>200||room.title.length()>2000)throw SnapshotJson.invalid("sala das estatísticas");
            room.duration=SnapshotJson.nonNegative(value,"duration");room.kills=SnapshotJson.integer(value,"kills",0,Integer.MAX_VALUE);
            room.entryLife=SnapshotJson.nonNegative(value,"entryLife");room.exitLife=SnapshotJson.nonNegative(value,"exitLife");
            room.dealt=SnapshotJson.nonNegative(value,"dealt");room.received=SnapshotJson.nonNegative(value,"received");
            JsonValue unlocked=SnapshotJson.required(value,"unlockedAt");
            room.unlockedAt=unlocked.isNull()?null:SnapshotJson.number(value,"unlockedAt",0,room.duration);
            JsonValue boss=SnapshotJson.required(value,"bossFight");room.bossFight=boss.isNull()?null:BossFightSummary.fromJson(boss);
            rooms.add(room);
        }
    }
    String checkpointRoomId(){return rooms.getLast().id;}
    float checkpointElapsed(){double total=0;for(Room room:rooms)total+=room.duration;return (float)total;}
    int checkpointKills(){int total=0;for(Room room:rooms)total=Math.addExact(total,room.kills);return total;}
    boolean checkpointEnded(){return ended;}
    public RunReport snapshot(RunSession run, RunOutcome outcome, int slot) {
        if (!ended) throw new IllegalStateException("End the attempt first");
        EnumMap<Upgrade,Integer> boons=new EnumMap<>(Upgrade.class);
        for(Upgrade boon:Upgrade.values()) if(run.rank(boon)>0) boons.put(boon,run.rank(boon));
        return new RunReport(run.id(),slot+1,run.isDemo(),run.player().getProfession(),run.player().getWeaponRarity(),
            outcome,run.progress().elapsed(),run.player().getNivel(),run.progress().kills(),run.earnedObols(),
            run.earnedSouls(),outcome==RunOutcome.DEATH?lastCause:"",dealt,received,
            rooms.stream().map(Room::snapshot).toList(),boons,resurrections,run.characterId(),run.difficulty());
    }
}

