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
    private boolean necromancerUnlocked, farmerUnlocked;
    private final List<RunReport> history=new ArrayList<>();
    private final java.util.Map<String,Long> completedSequences=new java.util.TreeMap<>();
    public long highestCompletedSequence(String characterId){return completedSequences.getOrDefault(java.util.Objects.requireNonNull(characterId),0L);}
    public int souls() { return souls; }
    public boolean necromancerUnlocked() { return necromancerUnlocked; }
    public boolean farmerUnlocked() { return farmerUnlocked; }
    void preserveFarmerUnlock() { farmerUnlocked = true; }
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
    boolean recordReceipt(RunSettlement receipt){
        String identity=receipt.report().characterId();
        if(receipt.sequence()<=highestCompletedSequence(identity))return false;
        record(receipt.report());completedSequences.put(identity,receipt.sequence());return true;
    }
    public SharedProgress copy() { return fromJson(toJson()); }
    public String toJson() {
        JsonValue root=new JsonValue(JsonValue.ValueType.object);
        RunReport.put(root,"version",2); RunReport.put(root,"souls",souls);
        JsonValue completed=new JsonValue(JsonValue.ValueType.object);
        for(var entry:completedSequences.entrySet())completed.addChild(entry.getKey(),new JsonValue(entry.getValue()));
        root.addChild("completedSequences",completed);
        root.addChild("necromancerUnlocked",new JsonValue(necromancerUnlocked));
        root.addChild("farmerUnlocked",new JsonValue(farmerUnlocked));
        JsonValue reports=new JsonValue(JsonValue.ValueType.array);
        for(RunReport report:history) reports.addChild(report.json());
        root.addChild("history",reports);
        return root.prettyPrint(JsonWriter.OutputType.json,100);
    }
    public static SharedProgress fromJson(String json) {
        JsonValue root=new JsonReader().parse(json);
        if(!root.isObject()) throw new IllegalArgumentException("Invalid shared progress");
        int version=RunReport.integer(root,"version");
        if(version!=1&&version!=2)throw new UnsupportedVersion();
        JsonValue history=root.get("history");
        if(history!=null&&history.isArray())for(JsonValue report:history){
            JsonValue reportVersion=report.isObject()?report.get("version"):null;
            if(reportVersion!=null&&reportVersion.isLong()&&reportVersion.asLong()>1)throw new UnsupportedVersion();
        }
        java.util.Set<String> rootFields=new java.util.HashSet<>();
        for(JsonValue field:root)if(!rootFields.add(field.name))throw new IllegalArgumentException("Duplicate shared progress field");
        SharedProgress progress=new SharedProgress(); progress.souls=RunReport.integer(root,"souls");
        JsonValue flag=root.require("necromancerUnlocked"); if(!flag.isBoolean()) throw new IllegalArgumentException("Invalid unlock");
        progress.necromancerUnlocked=flag.asBoolean();
        if(root.has("farmerUnlocked")) {
            JsonValue farmer=root.require("farmerUnlocked");
            if(!farmer.isBoolean()) throw new IllegalArgumentException("Invalid epic unlock");
            progress.farmerUnlocked=farmer.asBoolean();
        }
        JsonValue reports=root.require("history");
        if(!reports.isArray() || reports.size>HISTORY_LIMIT) throw new IllegalArgumentException("Invalid history");
        java.util.Set<String> ids=new java.util.HashSet<>();
        for(JsonValue value:reports) {
            RunReport report=RunReport.fromJson(value);
            if(!ids.add(report.id())) throw new IllegalArgumentException("Duplicate report");
            progress.history.add(report);
        }
        if(version==2){
            JsonValue completed=root.require("completedSequences");
            if(!completed.isObject())throw new IllegalArgumentException("Invalid completed attempts");
            for(JsonValue entry:completed){
                java.util.UUID.fromString(entry.name);
                if(!entry.isLong()||entry.asLong()<=0||progress.completedSequences.putIfAbsent(entry.name,entry.asLong())!=null)
                    throw new IllegalArgumentException("Invalid completed attempt sequence");
            }
        }
        return progress;
    }
}

