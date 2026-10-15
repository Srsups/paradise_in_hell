package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Immutable summary suitable for history and for inspection after the attempt is disposed. */
public record RunReport(String id, int saveSlot, boolean demo, Profession profession, WeaponRarity rarity,
        RunOutcome outcome, float duration, int level, int kills, int obols, int souls, String deathCause,
        Map<DamageSource,Float> dealt, float received, List<Room> rooms, Map<Upgrade,Integer> boons, int resurrections,
        String characterId, GameDifficulty difficulty) {
    public record Room(String id, String title, float duration, int kills, float entryLife, float exitLife,
                       float dealt, float received, Float unlockedAt, BossFightSummary bossFight) {
        public Room(String id,String title,float duration,int kills,float entryLife,float exitLife,
                    float dealt,float received,Float unlockedAt) {
            this(id,title,duration,kills,entryLife,exitLife,dealt,received,unlockedAt,null);
        }
    }
    /** Compatibility for summaries created before character identities were recorded. */
    public RunReport(String id, int saveSlot, boolean demo, Profession profession, WeaponRarity rarity,
            RunOutcome outcome, float duration, int level, int kills, int obols, int souls, String deathCause,
            Map<DamageSource,Float> dealt, float received, List<Room> rooms, Map<Upgrade,Integer> boons, int resurrections) {
        this(id, saveSlot, demo, profession, rarity, outcome, duration, level, kills, obols, souls, deathCause,
            dealt, received, rooms, boons, resurrections, "", GameDifficulty.MEDIUM);
    }
    /** Compatibility for summaries created before difficulty was recorded. */
    public RunReport(String id, int saveSlot, boolean demo, Profession profession, WeaponRarity rarity,
            RunOutcome outcome, float duration, int level, int kills, int obols, int souls, String deathCause,
            Map<DamageSource,Float> dealt, float received, List<Room> rooms, Map<Upgrade,Integer> boons, int resurrections,
            String characterId) {
        this(id, saveSlot, demo, profession, rarity, outcome, duration, level, kills, obols, souls, deathCause,
            dealt, received, rooms, boons, resurrections, characterId, GameDifficulty.MEDIUM);
    }
    public RunReport {
        java.util.UUID.fromString(id);
        java.util.Objects.requireNonNull(characterId);
        java.util.Objects.requireNonNull(difficulty);
        if (!characterId.isEmpty()) java.util.UUID.fromString(characterId);
        if(saveSlot<1 || saveSlot>SaveSlots.COUNT || level<1 || kills<0 || obols<0 || souls<0 || resurrections<0)
            throw new IllegalArgumentException("Invalid report");
        java.util.Objects.requireNonNull(profession); java.util.Objects.requireNonNull(rarity);
        java.util.Objects.requireNonNull(outcome); java.util.Objects.requireNonNull(deathCause);
        finite(duration); finite(received);
        dealt=Map.copyOf(dealt); rooms=List.copyOf(rooms); boons=Map.copyOf(boons);
        if(rooms.isEmpty() || rooms.size()>64) throw new IllegalArgumentException("Invalid report rooms");
        for(float amount:dealt.values()) finite(amount);
        for(var entry:boons.entrySet()) {
            // Completed runs may contain up to three Athena choices from before the XP rebalance.
            int recordedLimit = entry.getKey() == Upgrade.INTELIGENCIA ? 3 : entry.getKey().maxRank;
            if(entry.getValue()<1 || entry.getValue()>recordedLimit)
                throw new IllegalArgumentException("Invalid boon rank");
        }
        for(Room room:rooms) {
            if(room.id()==null || room.id().isBlank() || room.title()==null || room.title().isBlank() || room.kills()<0)
                throw new IllegalArgumentException("Invalid room");
            finite(room.duration()); finite(room.entryLife()); finite(room.exitLife()); finite(room.dealt()); finite(room.received());
            if(room.unlockedAt()!=null) { finite(room.unlockedAt()); if(room.unlockedAt()>room.duration()) throw new IllegalArgumentException("Invalid exit time"); }
        }
        if(demo && (souls!=0 || obols!=0)) throw new IllegalArgumentException("Demo cannot earn currency");
    }
    public float damageDealt() { return (float)dealt.values().stream().mapToDouble(Float::doubleValue).sum(); }
    public String resultLabel() { return switch(outcome) {case DEATH->"Derrota";case VICTORY->"Vitória";case ABANDONED->"Abandono";}; }
    public String toJson() { return json().prettyPrint(JsonWriter.OutputType.json,100); }
    public JsonValue json() {
        JsonValue root=new JsonValue(JsonValue.ValueType.object);
        put(root,"characterId",characterId);
        put(root,"difficulty",difficulty.name());
        put(root,"version",1); put(root,"id",id); put(root,"saveSlot",saveSlot); root.addChild("demo",new JsonValue(demo));
        put(root,"profession",profession.name()); put(root,"rarity",rarity.name()); put(root,"outcome",outcome.name());
        put(root,"duration",duration); put(root,"level",level); put(root,"kills",kills); put(root,"obols",obols); put(root,"souls",souls);
        put(root,"deathCause",deathCause); put(root,"received",received); put(root,"resurrections",resurrections);
        JsonValue damage=new JsonValue(JsonValue.ValueType.object);
        for(var entry:dealt.entrySet()) put(damage,entry.getKey().name(),entry.getValue());
        root.addChild("dealt",damage);
        JsonValue ranks=new JsonValue(JsonValue.ValueType.object);
        for(var entry:boons.entrySet()) put(ranks,entry.getKey().name(),entry.getValue());
        root.addChild("boons",ranks);
        JsonValue stages=new JsonValue(JsonValue.ValueType.array);
        for(Room room:rooms) {
            JsonValue value=new JsonValue(JsonValue.ValueType.object);
            put(value,"id",room.id()); put(value,"title",room.title()); put(value,"duration",room.duration()); put(value,"kills",room.kills());
            put(value,"entryLife",room.entryLife()); put(value,"exitLife",room.exitLife()); put(value,"dealt",room.dealt()); put(value,"received",room.received());
            if(room.unlockedAt()!=null) put(value,"unlockedAt",room.unlockedAt());
            if(room.bossFight()!=null)value.addChild("bossFight",room.bossFight().json());
            stages.addChild(value);
        }
        root.addChild("rooms",stages); return root;
    }
    public static RunReport fromJson(JsonValue root) {
        if(!root.isObject() || integer(root,"version")!=1) throw new IllegalArgumentException("Invalid report version");
        EnumMap<DamageSource,Float> damage=new EnumMap<>(DamageSource.class);
        JsonValue dealt=root.require("dealt"); if(!dealt.isObject()) throw new IllegalArgumentException("Invalid damage");
        for(JsonValue v:dealt) damage.put(DamageSource.valueOf(v.name),number(dealt,v.name));
        EnumMap<Upgrade,Integer> boons=new EnumMap<>(Upgrade.class);
        JsonValue ranks=root.require("boons"); if(!ranks.isObject()) throw new IllegalArgumentException("Invalid ranks");
        for(JsonValue v:ranks) boons.put(Upgrade.valueOf(v.name),integer(ranks,v.name));
        JsonValue stages=root.require("rooms"); if(!stages.isArray() || stages.size>64) throw new IllegalArgumentException("Invalid rooms");
        java.util.ArrayList<Room> rooms=new java.util.ArrayList<>();
        for(JsonValue r:stages) rooms.add(new Room(string(r,"id"),string(r,"title"),number(r,"duration"),integer(r,"kills"),
            number(r,"entryLife"),number(r,"exitLife"),number(r,"dealt"),number(r,"received"),r.has("unlockedAt")?number(r,"unlockedAt"):null,
            r.has("bossFight")?BossFightSummary.fromJson(r.get("bossFight")):null));
        JsonValue mode=root.require("demo"); if(!mode.isBoolean()) throw new IllegalArgumentException("Invalid mode");
        return new RunReport(string(root,"id"),integer(root,"saveSlot"),mode.asBoolean(),Profession.valueOf(string(root,"profession")),
            WeaponRarity.valueOf(string(root,"rarity")),RunOutcome.valueOf(string(root,"outcome")),number(root,"duration"),
            integer(root,"level"),integer(root,"kills"),integer(root,"obols"),integer(root,"souls"),string(root,"deathCause"),
            damage,number(root,"received"),rooms,boons,integer(root,"resurrections"),root.has("characterId")?string(root,"characterId"):"",
            root.has("difficulty")?GameDifficulty.valueOf(string(root,"difficulty")):GameDifficulty.MEDIUM);
    }
    static void put(JsonValue root,String key,String value) { root.addChild(key,new JsonValue(value)); }
    static void put(JsonValue root,String key,int value) { root.addChild(key,new JsonValue((long)value)); }
    static void put(JsonValue root,String key,float value) { root.addChild(key,new JsonValue((double)value)); }
    static int integer(JsonValue root,String key) {
        JsonValue v=root.require(key);
        if(!v.isLong() || v.asLong()<0 || v.asLong()>Integer.MAX_VALUE) throw new IllegalArgumentException("Invalid "+key);
        return v.asInt();
    }
    static float number(JsonValue root,String key) {
        JsonValue v=root.require(key); if(!v.isNumber()) throw new IllegalArgumentException("Invalid "+key);
        float value=v.asFloat(); finite(value); return value;
    }
    static String string(JsonValue root,String key) {
        JsonValue v=root.require(key); if(!v.isString()) throw new IllegalArgumentException("Invalid "+key); return v.asString();
    }
    private static void finite(float value) { if(!Float.isFinite(value) || value<0) throw new IllegalArgumentException("Invalid amount"); }
}

