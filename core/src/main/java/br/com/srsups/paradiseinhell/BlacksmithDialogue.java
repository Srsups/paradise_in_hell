package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.utils.JsonReader;
import java.util.List;
import java.util.Map;

/** The forge's presentation depends on permanent facts; selecting copy never buys anything. */
public final class BlacksmithDialogue {
    private static final Map<String,List<DialogueLine>> CATALOG=load();
    private BlacksmithDialogue() {}
    private static Map<String,List<DialogueLine>> load() {
        try(var stream=BlacksmithDialogue.class.getResourceAsStream("/dialogues/blacksmith.json")) {
            if(stream==null)throw new IllegalStateException("Missing blacksmith dialogue");
            return parse(new String(stream.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));
        } catch(java.io.IOException error) { throw new IllegalStateException("Cannot read blacksmith dialogue",error); }
    }
    static Map<String,List<DialogueLine>> parse(String json) {
        var root=new JsonReader().parse(json);var result=new java.util.HashMap<String,List<DialogueLine>>();
        if(!root.isObject())throw new IllegalArgumentException("Invalid blacksmith dialogue");
        for(String key:List.of("ready","readyBlacksmith","rare","missingMaterial","success","failure"))
            result.put(key,DialogueLine.parseLines(root.require(key),2,200));
        return Map.copyOf(result);
    }
    public static List<DialogueLine> select(CampaignProfile profile,WeaponType weapon) {
        java.util.Objects.requireNonNull(profile);
        String key=profile.weaponRarity()==WeaponRarity.RARE?"rare":profile.forgeMaterials()==0?"missingMaterial"
            :profile.profession()==Profession.FERREIRO?"readyBlacksmith":"ready";
        return render(key,weapon);
    }
    public static List<DialogueLine> success(WeaponType weapon) { return render("success",weapon); }
    public static List<DialogueLine> failure(WeaponType weapon) { return render("failure",weapon); }
    public static List<DialogueLine> missingMaterial(WeaponType weapon) { return render("missingMaterial",weapon); }
    private static List<DialogueLine> render(String key,WeaponType weapon) {
        java.util.Objects.requireNonNull(weapon);
        return CATALOG.get(key).stream().map(line->new DialogueLine(line.speaker(),
            line.text().replace("{weapon}",weapon.label),line.portrait())).toList();
    }
}
