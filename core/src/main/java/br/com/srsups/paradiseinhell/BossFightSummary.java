package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.utils.JsonValue;
import java.util.EnumMap;
import java.util.Map;

/** Bounded encounter measurements for comparing full playtests, not per-frame events. */
public record BossFightSummary(float phaseOneSeconds,float phaseTwoSeconds,Map<BossEncounter.Attack,Integer> attacks) {
    public BossFightSummary {
        if(!Float.isFinite(phaseOneSeconds)||!Float.isFinite(phaseTwoSeconds)||phaseOneSeconds<0||phaseTwoSeconds<0)
            throw new IllegalArgumentException("Invalid boss time");
        attacks=Map.copyOf(attacks);
        for(int value:attacks.values())if(value<1)throw new IllegalArgumentException("Invalid boss attack count");
    }
    public JsonValue json() {
        JsonValue root=new JsonValue(JsonValue.ValueType.object);
        RunReport.put(root,"phaseOneSeconds",phaseOneSeconds);RunReport.put(root,"phaseTwoSeconds",phaseTwoSeconds);
        JsonValue counts=new JsonValue(JsonValue.ValueType.object);
        for(var entry:attacks.entrySet())RunReport.put(counts,entry.getKey().name(),entry.getValue());
        root.addChild("attacks",counts);return root;
    }
    public static BossFightSummary fromJson(JsonValue root) {
        if(!root.isObject())throw new IllegalArgumentException("Invalid boss summary");
        JsonValue counts=root.require("attacks");if(!counts.isObject())throw new IllegalArgumentException("Invalid attack counts");
        EnumMap<BossEncounter.Attack,Integer> map=new EnumMap<>(BossEncounter.Attack.class);
        for(JsonValue value:counts)map.put(BossEncounter.Attack.valueOf(value.name),RunReport.integer(counts,value.name));
        return new BossFightSummary(RunReport.number(root,"phaseOneSeconds"),RunReport.number(root,"phaseTwoSeconds"),map);
    }
}

