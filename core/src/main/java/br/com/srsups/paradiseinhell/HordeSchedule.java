package br.com.srsups.paradiseinhell;

import java.util.List;
import java.util.Random;

/** Room-local spawn pacing. Absolute active time freezes naturally with gameplay and restarts with room checkpoints. */
public final class HordeSchedule {
    public static final float FIRST_HORDE_SECONDS=35,PERIOD_SECONDS=40;
    public static final float WARNING_SECONDS=3,HORDE_SECONDS=5,RECOVERY_SECONDS=4;
    private static final double EPSILON=.00001;
    public enum Phase { NORMAL,WARNING,HORDE,RECOVERY }
    /** Flyers share COMMON goal identity without becoming archers or changing the three goals. */
    public enum Kind {
        COMMON("Comuns",EnemyType.COMMON,18),FAST("Rápidos",EnemyType.FAST,6),
        TANK("Tanks",EnemyType.TANK,8),FLYER("Voadores",EnemyType.COMMON,14);
        private final String label;
        private final EnemyType enemyType;
        private final int size;
        Kind(String label,EnemyType enemyType,int size){this.label=label;this.enemyType=enemyType;this.size=size;}
        public String label(){return label;}
        public EnemyType enemyType(){return enemyType;}
        public int size(){return size;}
    }
    /** All batches of a horde have one role and one formation seed, so the screen can retain one nearby spawn origin. */
    public record Batch(Kind kind,int count,long formationSeed) {
        public Batch {java.util.Objects.requireNonNull(kind);if(count<=0||count>kind.size())throw new IllegalArgumentException("Invalid horde batch");}
    }
    private final long seed;
    private float elapsed,remaining=FIRST_HORDE_SECONDS-WARNING_SECONDS;
    private Phase phase=Phase.NORMAL;
    private Kind kind=Kind.COMMON;
    private long selectedHorde=-1;
    private long consumedHorde=-1;
    private int consumedSlots;
    public HordeSchedule(long seed){this.seed=seed;}
    /** Stable across processes without depending on String/UUID identity or on encounter/boon RNG. */
    public static long seedFor(String runId,String arenaId){
        java.util.Objects.requireNonNull(runId);java.util.Objects.requireNonNull(arenaId);
        long value=0xcbf29ce484222325L;
        for(char c:(runId+"/"+arenaId).toCharArray()){value^=c;value*=0x100000001b3L;}
        return value;
    }
    public Phase phase(){return phase;}
    public Kind kind(){return kind;}
    public float secondsRemaining(){return remaining;}
    public boolean allowsRegularSpawns(){return phase==Phase.NORMAL;}
    public float elapsed(){return elapsed;}

    /**
     * Call with active room time, not wall time. NORMAL is the only regular-spawn phase:
     * callers discard/reset their regular timer during the other phases, avoiding delayed spawn bursts.
     * Large time jumps skip expired hordes; they never queue their missed enemies for a later frame.
     */
    public List<Batch> update(float roomElapsed,float totalCombatSeconds,boolean campaign,ArenaVariation variation,int activeFast){
        if(!Float.isFinite(roomElapsed)||!Float.isFinite(totalCombatSeconds)||roomElapsed<0||roomElapsed+EPSILON<elapsed
            ||totalCombatSeconds+EPSILON<roomElapsed||activeFast<0)throw new IllegalArgumentException("Invalid horde time or active enemies");
        java.util.Objects.requireNonNull(variation);elapsed=Math.max(elapsed,roomElapsed);roomElapsed=elapsed;
        double firstWarning=FIRST_HORDE_SECONDS-WARNING_SECONDS;
        if(roomElapsed+EPSILON<firstWarning){phase=Phase.NORMAL;remaining=(float)(firstWarning-roomElapsed);return List.of();}
        double rawIndex=Math.floor((roomElapsed-firstWarning+EPSILON)/PERIOD_SECONDS);
        if(rawIndex>Long.MAX_VALUE)throw new IllegalArgumentException("Horde time is too large");
        long index=(long)rawIndex;
        double start=FIRST_HORDE_SECONDS+index*(double)PERIOD_SECONDS;
        double local=roomElapsed-start;
        float combatAtStart=(float)Math.max(0,totalCombatSeconds-roomElapsed+start);
        if(selectedHorde!=index){kind=choose(index,campaign,combatAtStart);selectedHorde=index;}
        if(local<-EPSILON){phase=Phase.WARNING;remaining=(float)-local;return List.of();}
        if(local+EPSILON<HORDE_SECONDS){
            phase=Phase.HORDE;remaining=(float)Math.max(0,HORDE_SECONDS-local);
            if(consumedHorde!=index){consumedHorde=index;consumedSlots=0;}
            int due=Math.min(kind.size(),1+(int)Math.floor((Math.max(0,local)+EPSILON)*kind.size()/HORDE_SECONDS));
            int available=due-consumedSlots;
            consumedSlots=due; // A blocked fast slot expires instead of becoming a backlog after the cap clears.
            if(kind==Kind.FAST)available=Math.min(available,Math.max(0,EnemyWaves.MAX_FAST-activeFast));
            return available>0?List.of(new Batch(kind,available,formationSeed(index))):List.of();
        }
        if(local+EPSILON<HORDE_SECONDS+RECOVERY_SECONDS){
            phase=Phase.RECOVERY;remaining=(float)(HORDE_SECONDS+RECOVERY_SECONDS-local);return List.of();
        }
        phase=Phase.NORMAL;remaining=(float)(PERIOD_SECONDS-WARNING_SECONDS-local);return List.of();
    }
    private Kind choose(long index,boolean campaign,float combatAtStart){
        if(!campaign&&combatAtStart<60)return Kind.COMMON;
        if(index==0||!campaign&&index==1)return Kind.FLYER;
        long position=index-(campaign?1:2),deck=position/Kind.values().length;
        Kind[] order=deck(deck);
        Kind previous=deck==0?Kind.FLYER:deck(deck-1)[Kind.values().length-1];
        if(order[0]==previous){Kind swap=order[0];order[0]=order[1];order[1]=swap;}
        Kind selected=order[(int)(position%order.length)];
        return !campaign&&combatAtStart<120&&selected==Kind.TANK?Kind.COMMON:selected;
    }
    private Kind[] deck(long index){
        Kind[] order=Kind.values();Random random=new Random(formationSeed(index^0x524f554e4453L));
        for(int i=order.length-1;i>0;i--){int other=random.nextInt(i+1);Kind swap=order[i];order[i]=order[other];order[other]=swap;}
        return order;
    }
    private long formationSeed(long index){
        long value=seed+0x9e3779b97f4a7c15L*(index+1);
        value=(value^(value>>>30))*0xbf58476d1ce4e5b9L;
        value=(value^(value>>>27))*0x94d049bb133111ebL;
        return value^(value>>>31);
    }
}
