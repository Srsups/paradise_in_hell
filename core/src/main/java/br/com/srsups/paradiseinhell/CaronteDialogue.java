package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.util.List;
import java.util.Map;

/** Dialogue text lives in data; permanent events still select and settle conversations. */
public final class CaronteDialogue {
    public record Line(String speaker,String text,DialoguePortrait portrait) {
        public Line(String speaker,String text) { this(speaker,text,DialoguePortrait.defaultFor(speaker)); }
        public Line { new DialogueLine(speaker,text,portrait); }
    }
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
        for(String key:List.of("first","bribe","guardian","repeat","questAccepted","questSeed","questComplete","forged")) {
            if(!root.has(key)&&List.of("questAccepted","questSeed","questComplete","forged").contains(key))continue;
            JsonValue array=root.require(key);
            result.put(key,DialogueLine.parseLines(array,32,600).stream()
                .map(line->new Line(line.speaker(),line.text(),line.portrait())).toList());
        }
        return Map.copyOf(result);
    }
    public static Conversation select(CampaignProfile profile) {
        if(!profile.has(CampaignProfile.Event.CARONTE_MET))return new Conversation(CampaignProfile.Event.CARONTE_MET,CATALOG.get("first"));
        if(profile.deaths()>0&&!profile.has(CampaignProfile.Event.BRIBE_JOKE))return new Conversation(CampaignProfile.Event.BRIBE_JOKE,CATALOG.get("bribe"));
        if(profile.victories()>0&&!profile.has(CampaignProfile.Event.GUARDIAN_RECOGNIZED))return new Conversation(CampaignProfile.Event.GUARDIAN_RECOGNIZED,CATALOG.get("guardian"));
        String key=switch(profile.farmerQuest()) {
            case ACCEPTED->"questAccepted";case SEED_FOUND->"questSeed";case COMPLETED->"questComplete";
            case NOT_STARTED->profile.weaponRarity()==WeaponRarity.RARE?"forged":"repeat";
        };
        return new Conversation(null,CATALOG.getOrDefault(key,CATALOG.get("repeat")).stream()
            .map(line->new Line(line.speaker(),line.text().replace("{returns}",Integer.toString(profile.attempts())),line.portrait())).toList());
    }
}
