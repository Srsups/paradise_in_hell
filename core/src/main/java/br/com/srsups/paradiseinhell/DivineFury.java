package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.utils.JsonValue;

/** Only active fighting after a room's exit unlocks contributes pressure. Menus never tick. */
public final class DivineFury {
    public static final float GRACE = 20, WARNING = 15, LEVEL_SECONDS = 25;
    private float farming, pressure;
    private int level;
    public float farmingSeconds() { return farming; }
    public float pressure() { return pressure; }
    public int level() { return level; }
    public JsonValue checkpointState(){
        JsonValue value=new JsonValue(JsonValue.ValueType.object);SnapshotJson.put(value,"farming",farming);
        SnapshotJson.put(value,"pressure",pressure);SnapshotJson.put(value,"level",level);return value;
    }
    public void restoreCheckpointState(JsonValue value){
        SnapshotJson.object(value,"farming","pressure","level");
        farming=SnapshotJson.nonNegative(value,"farming");pressure=SnapshotJson.nonNegative(value,"pressure");
        level=SnapshotJson.integer(value,"level",0,3);
        int expected=Math.min(3,(int)(pressure/LEVEL_SECONDS)+(farming>=GRACE+WARNING?1:0));
        if(level!=expected)throw SnapshotJson.invalid("fúria");
    }
    public boolean warned() { return farming >= GRACE; }
    public void update(float delta, boolean exitUnlocked, float classRelativePower, float roomSeconds, int allyCount) {
        if (!Float.isFinite(delta) || delta < 0) throw new IllegalArgumentException("Invalid delta");
        if (!exitUnlocked) return;
        float before = farming; farming += delta;
        float expected = 1.5f + roomSeconds / 120;
        float excess = Math.max(0, classRelativePower + allyCount * 0.15f - expected);
        float multiplier = Math.min(3, 1 + excess * 0.6f);
        // The visible warning lasts at least 15 real seconds, regardless of build strength.
        float warningRemaining = Math.max(0, GRACE + WARNING - before);
        float afterWarning = Math.max(0, delta - warningRemaining);
        pressure += afterWarning * multiplier;
        level = Math.min(3, (int) (pressure / LEVEL_SECONDS) + (farming >= GRACE + WARNING ? 1 : 0));
    }
    /** Progression clears pressure for the next room. It does not punish an advancing build. */
    public void enterRoom() { farming = 0; pressure = 0; level = 0; }
    public float healthMultiplier() { return 1 + 0.20f * level; }
    public float damageMultiplier() { return 1 + 0.15f * level; }
    public float spawnMultiplier() { return 1 / (1 + 0.20f * level); }
    public float xpMultiplier() { return 1 / (1 + 0.50f * level); }
    public String notice() {
        if (!warned()) return "";
        if (level == 0) return "O DESTINO OBSERVA: avance! A fúria começa em "
            + Math.max(0, (int) Math.ceil(GRACE + WARNING - farming)) + "s.";
        return "FÚRIA DOS DEUSES " + level + "/3: hordas mais fortes; XP reduzida. E: avançar";
    }
}
