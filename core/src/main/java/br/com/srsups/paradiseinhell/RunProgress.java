package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.utils.JsonValue;

/** Simulation time; menus and paused frames do not advance a run. */
public final class RunProgress {
    public static final float DURATION_SECONDS = 300f;
    private float elapsed;
    private int kills;
    private final boolean timedDemo;

    public RunProgress() { this(true); }
    public RunProgress(boolean timedDemo) { this.timedDemo = timedDemo; }

    public void advance(float delta) {
        if (!Float.isFinite(delta) || delta < 0) throw new IllegalArgumentException("Invalid delta");
        elapsed = timedDemo ? Math.min(DURATION_SECONDS, elapsed + delta) : elapsed + delta;
    }
    public void recordKill() { kills++; }
    public float elapsed() { return elapsed; }
    public int kills() { return kills; }
    public JsonValue checkpointState(){
        JsonValue value=new JsonValue(JsonValue.ValueType.object);
        SnapshotJson.put(value,"elapsed",elapsed);SnapshotJson.put(value,"kills",kills);SnapshotJson.put(value,"demo",timedDemo);return value;
    }
    public void restoreCheckpointState(JsonValue value){
        SnapshotJson.object(value,"elapsed","kills","demo");
        if(SnapshotJson.flag(value,"demo")!=timedDemo)throw SnapshotJson.invalid("modo da tentativa");
        elapsed=SnapshotJson.number(value,"elapsed",0,timedDemo?DURATION_SECONDS:Float.MAX_VALUE);
        kills=SnapshotJson.integer(value,"kills",0,Integer.MAX_VALUE);
    }
    public boolean won() { return timedDemo && elapsed >= DURATION_SECONDS; }
    public float spawnInterval() { return Math.max(0.32f, 1.7f - elapsed / 210f); }
    public int waveSize() { return 1 + (int) (elapsed / 60f); }
    public int phase() { return Math.min(5, 1 + (int) (elapsed / 60f)); }
    public static String formatTime(float seconds) {
        int total = Math.max(0, (int) seconds);
        return String.format(java.util.Locale.ROOT, "%02d:%02d", total / 60, total % 60);
    }
}
