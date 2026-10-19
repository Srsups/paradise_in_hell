package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.util.List;
import java.util.Map;

/** Dialogue text lives in data; permanent events still select and settle conversations. */
public final class CaronteDialogue {
    public record Line(String speaker, String text) {}
    public record Conversation(CampaignProfile.Event event, List<Line> lines) {
        public Conversation { lines=List.copyOf(lines); if(lines.isEmpty())throw new IllegalArgumentException("Empty dialogue"); }
    }
    private static final Map<String,List<Line>> CATALOG = load();
    private CaronteDialogue() {}
    private static Map<String,List<Line>> load() {
        try(var stream=CaronteDialogue.class.getResourceAsStream("/dialogues/charon.json")) {
            if(stream==null)throw new IllegalStateException("Missing Charon dialogue");
            return parse(new String(stream.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));
        } catch(java.io.IOException error) {throw new IllegalStateException("Cannot read Charon dialogue",error);}
    }
    static Map<String,List<Line>> parse(String json) {
        JsonValue root=new JsonReader().parse(json); var result=new java.util.HashMap<String,List<Line>>();
        for(String key:List.of("first","bribe","guardian","repeat")) {
            JsonValue array=root.require(key);
            if(!array.isArray() || array.size<1 || array.size>32)throw new IllegalArgumentException("Invalid dialogue");
            var lines=new java.util.ArrayList<Line>();
            for(JsonValue value:array) {
                String speaker=RunReport.string(value,"speaker"),text=RunReport.string(value,"text");
                if(speaker.isBlank()||text.isBlank()||text.length()>600)throw new IllegalArgumentException("Invalid line");
                lines.add(new Line(speaker,text));
            }
            result.put(key,List.copyOf(lines));
        }
        return Map.copyOf(result);
    }
    public static Conversation select(CampaignProfile profile) {
        if(!profile.has(CampaignProfile.Event.CARONTE_MET))return new Conversation(CampaignProfile.Event.CARONTE_MET,CATALOG.get("first"));
        if(profile.deaths()>0&&!profile.has(CampaignProfile.Event.BRIBE_JOKE))return new Conversation(CampaignProfile.Event.BRIBE_JOKE,CATALOG.get("bribe"));
        if(profile.victories()>0&&!profile.has(CampaignProfile.Event.GUARDIAN_RECOGNIZED))return new Conversation(CampaignProfile.Event.GUARDIAN_RECOGNIZED,CATALOG.get("guardian"));
        return new Conversation(null,CATALOG.get("repeat").stream()
            .map(line->new Line(line.speaker(),line.text().replace("{returns}",Integer.toString(profile.attempts())))).toList());
    }
}
