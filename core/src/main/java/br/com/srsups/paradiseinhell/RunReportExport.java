package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Detached, read-only export of the bounded history, never a command to replay or settle a run. */
public final class RunReportExport {
    public static final String JSON_FILE="history.json",ATTEMPTS_FILE="attempts.csv",ROOMS_FILE="rooms.csv";
    private final String id,json,attemptsCsv,roomsCsv;
    private final List<RunReport> reports;
    private RunReportExport(String id,String json,String attemptsCsv,String roomsCsv,List<RunReport> reports) {
        this.id=id;this.json=json;this.attemptsCsv=attemptsCsv;this.roomsCsv=roomsCsv;this.reports=List.copyOf(reports);
    }
    public static RunReportExport create(List<RunReport> history,RunReport opened) {
        return create(history,opened,Instant.now(),UUID.randomUUID().toString());
    }
    static RunReportExport create(List<RunReport> history,RunReport opened,Instant createdAt,String id) {
        java.util.Objects.requireNonNull(history);java.util.Objects.requireNonNull(opened);java.util.Objects.requireNonNull(createdAt);
        UUID.fromString(id);
        Map<String,RunReport> selected=new LinkedHashMap<>();
        for(RunReport report:history) {
            if(selected.size()>=SharedProgress.HISTORY_LIMIT)break;
            selected.putIfAbsent(report.id(),report.id().equals(opened.id())?opened:report);
        }
        boolean extra=!selected.containsKey(opened.id());selected.putIfAbsent(opened.id(),opened);
        List<RunReport> reports=List.copyOf(selected.values());
        JsonValue root=new JsonValue(JsonValue.ValueType.object);
        RunReport.put(root,"version",1);RunReport.put(root,"type","paradise-in-hell-run-history");
        RunReport.put(root,"exportedAtUtc",createdAt.toString());RunReport.put(root,"historyLimit",SharedProgress.HISTORY_LIMIT);
        RunReport.put(root,"reportCount",reports.size());root.addChild("includesOpenedReportOutsideHistory",new JsonValue(extra));
        RunReport.put(root,"scope","Available local history, up to 20 reports plus the opened report if absent. Not lifetime totals.");
        RunReport.put(root,"missingContext","Initial permanent bonuses, arena variations and balance revision were not recorded in these reports.");
        JsonValue values=new JsonValue(JsonValue.ValueType.array);for(RunReport report:reports)values.addChild(report.json());
        root.addChild("reports",values);
        return new RunReportExport(id,root.prettyPrint(JsonWriter.OutputType.json,100),attempts(reports),rooms(reports),reports);
    }
    public String id(){return id;}
    public List<RunReport> reports(){return reports;}
    public String json(){return json;}
    public String attemptsCsv(){return attemptsCsv;}
    public String roomsCsv(){return roomsCsv;}
    private static String attempts(List<RunReport> reports) {
        StringBuilder out=new StringBuilder();
        row(out,"run_id","character_id","save_slot","mode","profession","difficulty","outcome","weapon_rarity_final",
            "duration_seconds","level","kills","obols","souls","effective_damage_dealt","damage_received","effective_dps",
            "obols_per_minute","souls_per_minute","resurrections","boon_choices","route_ids","death_cause",
            "initial_permanent_bonuses","arena_variations","balance_revision");
        for(RunReport r:reports)row(out,r.id(),r.characterId(),r.saveSlot(),r.demo()?"DEMO":"CAMPAIGN",r.profession().name(),
            r.difficulty().name(),r.outcome().name(),r.rarity().name(),r.duration(),r.level(),r.kills(),r.obols(),r.souls(),r.damageDealt(),
            r.received(),rate(r.damageDealt(),r.duration(),1),rate(r.obols(),r.duration(),60),rate(r.souls(),r.duration(),60),
            r.resurrections(),r.boons().values().stream().mapToInt(Integer::intValue).sum(),
            String.join(" > ",r.rooms().stream().map(RunReport.Room::id).toList()),r.deathCause(),"","","");
        return out.toString();
    }
    private static String rooms(List<RunReport> reports) {
        StringBuilder out=new StringBuilder();List<Object> header=new ArrayList<>(List.of("run_id","character_id","save_slot","profession",
            "difficulty","outcome","room_index","room_id","room_title","duration_seconds","kills","entry_life","exit_life",
            "effective_damage_dealt","damage_received","unlocked_at_seconds","time_after_unlock_seconds","effective_dps",
            "boss_phase_1_seconds","boss_phase_2_seconds"));
        for(BossEncounter.Attack attack:BossEncounter.Attack.values())header.add("boss_"+attack.name().toLowerCase(Locale.ROOT)+"_started");
        row(out,header.toArray());
        for(RunReport r:reports)for(int i=0;i<r.rooms().size();i++) {
            RunReport.Room room=r.rooms().get(i);BossFightSummary boss=room.bossFight();
            List<Object> values=new ArrayList<>(List.of(r.id(),r.characterId(),r.saveSlot(),r.profession().name(),r.difficulty().name(),
                r.outcome().name(),i+1,room.id(),room.title(),room.duration(),room.kills(),room.entryLife(),room.exitLife(),room.dealt(),room.received(),
                room.unlockedAt()==null?"":room.unlockedAt(),room.unlockedAt()==null?"":Math.max(0,room.duration()-room.unlockedAt()),
                rate(room.dealt(),room.duration(),1),boss==null?"":boss.phaseOneSeconds(),boss==null?"":boss.phaseTwoSeconds()));
            for(BossEncounter.Attack attack:BossEncounter.Attack.values())values.add(boss==null?"":boss.attacks().getOrDefault(attack,0));
            row(out,values.toArray());
        }
        return out.toString();
    }
    private static String rate(float amount,float seconds,int factor) {
        return seconds>0?String.format(Locale.ROOT,"%.6f",(double)amount*factor/seconds):"";
    }
    /** Text remains exact in JSON; spreadsheet-directed text is escaped and cannot execute a formula. */
    private static void row(StringBuilder out,Object... values) {
        for(int index=0;index<values.length;index++) {
            if(index>0)out.append(',');Object value=values[index];String text=String.valueOf(value);
            if(!(value instanceof Number)) {
                String trimmed=text.stripLeading();
                if(!trimmed.isEmpty()&&"=+-@".indexOf(trimmed.charAt(0))>=0)text="'"+text;
            }
            out.append('"').append(text.replace("\"","\"\"")).append('"');
        }
        out.append("\r\n");
    }
}
