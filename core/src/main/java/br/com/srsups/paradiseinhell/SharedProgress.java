package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import java.util.ArrayList;
import java.util.List;

/** Account-wide currency, legendary access and the last twenty run summaries. */
public final class SharedProgress {
    public static final int NECROMANCER_PRICE=1_000, HISTORY_LIMIT=20;
    public static final class UnsupportedVersion extends IllegalArgumentException {
        public UnsupportedVersion() { super("Unsupported shared progress version"); }
    }
    public static String necromancerCostText() { return String.format(java.util.Locale.forLanguageTag("pt-BR"),"%,d",NECROMANCER_PRICE)+" Almas"; }
    private int souls;
    private boolean necromancerUnlocked;
    private final List<RunReport> history=new ArrayList<>();
    public int souls() { return souls; }
    public boolean necromancerUnlocked() { return necromancerUnlocked; }
    public boolean canUnlockNecromancer() { return !necromancerUnlocked && souls>=NECROMANCER_PRICE; }
    public List<RunReport> history() { return List.copyOf(history); }
    public RunReport latest(int slot) { return history.stream().filter(r->r.saveSlot()==slot+1).findFirst().orElse(null); }
    public List<RunReport> history(int slot, String characterId) {
        return history.stream().filter(r -> r.saveSlot() == slot + 1 && r.characterId().equals(characterId)).toList();
    }
    public RunReport latest(int slot, String characterId) {
        return history(slot, characterId).stream().findFirst().orElse(null);
    }
    void credit(int amount) {
        if(amount<0) throw new IllegalArgumentException("Negative souls");
        souls=Math.addExact(souls,amount);
    }
    void unlockNecromancer() {
        if(!canUnlockNecromancer()) throw new IllegalStateException("Legendary profession unavailable");
        souls-=NECROMANCER_PRICE; necromancerUnlocked=true;
    }
    void preserveLegacyUnlock() { necromancerUnlocked=true; }
    boolean record(RunReport report) {
        if(history.stream().anyMatch(r->r.id().equals(report.id()))) return false;
        credit(report.souls()); history.addFirst(report);
        while(history.size()>HISTORY_LIMIT) history.removeLast();
        return true;
    }
    public SharedProgress copy() { return fromJson(toJson()); }
    public String toJson() {
        JsonValue root=new JsonValue(JsonValue.ValueType.object);
        RunReport.put(root,"version",1); RunReport.put(root,"souls",souls);
        root.addChild("necromancerUnlocked",new JsonValue(necromancerUnlocked));
        JsonValue reports=new JsonValue(JsonValue.ValueType.array);
        for(RunReport report:history) reports.addChild(report.json());
        root.addChild("history",reports);
        return root.prettyPrint(JsonWriter.OutputType.json,100);
    }
    public static SharedProgress fromJson(String json) {
        JsonValue root=new JsonReader().parse(json);
        if(!root.isObject()) throw new IllegalArgumentException("Invalid shared progress");
        if(RunReport.integer(root,"version")!=1) throw new UnsupportedVersion();
        SharedProgress progress=new SharedProgress(); progress.souls=RunReport.integer(root,"souls");
        JsonValue flag=root.require("necromancerUnlocked"); if(!flag.isBoolean()) throw new IllegalArgumentException("Invalid unlock");
        progress.necromancerUnlocked=flag.asBoolean();
        JsonValue reports=root.require("history");
        if(!reports.isArray() || reports.size>HISTORY_LIMIT) throw new IllegalArgumentException("Invalid history");
        java.util.Set<String> ids=new java.util.HashSet<>();
        for(JsonValue value:reports) {
            RunReport report=RunReport.fromJson(value);
            if(!ids.add(report.id())) throw new IllegalArgumentException("Duplicate report");
            progress.history.add(report);
        }
        return progress;
    }
}

