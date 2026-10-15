package br.com.srsups.paradiseinhell;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

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

