package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.utils.JsonValue;

/** Explicit 48-bit random state for resumable decisions; no reflective access to java.util.Random. */
public final class RunRandom extends java.util.Random {
    private static final long MULTIPLIER=0x5DEECE66DL,ADDEND=0xBL,MASK=(1L<<48)-1;
    private long state;
    public RunRandom(){this(java.util.concurrent.ThreadLocalRandom.current().nextLong());}
    public RunRandom(long seed){super(0);setSeed(seed);}
    @Override public synchronized void setSeed(long seed){super.setSeed(seed);state=(seed^MULTIPLIER)&MASK;}
    @Override protected synchronized int next(int bits){state=(state*MULTIPLIER+ADDEND)&MASK;return (int)(state>>>(48-bits));}
    @Override public double nextGaussian(){
        double first,second,radius;
        do{first=2*nextDouble()-1;second=2*nextDouble()-1;radius=first*first+second*second;}while(radius>=1||radius==0);
        return first*Math.sqrt(-2*Math.log(radius)/radius);
    }
    public synchronized long state(){return state;}
    public synchronized void restoreState(long value){if(value<0||value>MASK)throw SnapshotJson.invalid("random");state=value;}
    public JsonValue checkpointState(){JsonValue value=new JsonValue(JsonValue.ValueType.object);SnapshotJson.put(value,"state",state());return value;}
    public void restoreCheckpointState(JsonValue value){SnapshotJson.object(value,"state");restoreState(SnapshotJson.longInteger(value,"state",0,MASK));}
    public static RunRandom fromCheckpointState(JsonValue value){RunRandom result=new RunRandom(0);result.restoreCheckpointState(value);return result;}
    static JsonValue checkpoint(java.util.random.RandomGenerator random){return random instanceof RunRandom resumable?resumable.checkpointState():new RunRandom(0).checkpointState();}
}
