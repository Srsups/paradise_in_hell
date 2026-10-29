package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.utils.JsonReader;
import java.util.List;
import java.util.Map;

/** One line per quest stage, preserving the encounter's existing confirm flow. */
public final class CamponesaDialogue {
    record Catalog(String name,String title,Map<String,List<DialogueLine>> messages) {}
    private static final Catalog CATALOG=load();
    private CamponesaDialogue() {}
    private static Catalog load() {
        try(var stream=CamponesaDialogue.class.getResourceAsStream("/dialogue/camponesa.json")) {
            if(stream==null)throw new IllegalStateException("Missing Camponesa dialogue");
            return parse(new String(stream.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));
        } catch(java.io.IOException error) { throw new IllegalStateException("Cannot read Camponesa dialogue",error); }
    }
    static Catalog parse(String json) {
        var root=new JsonReader().parse(json);
        if(!root.isObject())throw new IllegalArgumentException("Invalid Camponesa dialogue");
        String name=string(root,"name"),title=string(root,"title");
        if(name.isBlank()||name.length()>100||title.isBlank()||title.length()>100)
            throw new IllegalArgumentException("Invalid Camponesa title");
        var portraits=root.get("portraits");
        if(portraits!=null&&!portraits.isObject())throw new IllegalArgumentException("Invalid Camponesa portraits");
        var messages=new java.util.HashMap<String,List<DialogueLine>>();
        for(String key:List.of("offer","accepted","NOT_STARTED","ACCEPTED","SEED_FOUND","COMPLETED")) {
            DialoguePortrait portrait=DialoguePortrait.parse(portraits==null?null:exact(portraits,key),DialoguePortrait.CAMPONESA_NEUTRAL);
            messages.put(key,List.of(new DialogueLine(name,string(root,key),portrait)));
        }
        return new Catalog(name,title,Map.copyOf(messages));
    }
    /** JsonValue.get ignores case; reply "accepted" and journal stage "ACCEPTED" are distinct. */
    private static com.badlogic.gdx.utils.JsonValue exact(com.badlogic.gdx.utils.JsonValue parent,String name) {
        for(var child=parent.child;child!=null;child=child.next)if(name.equals(child.name))return child;
        return null;
    }
    private static String string(com.badlogic.gdx.utils.JsonValue parent,String name) {
        var value=exact(parent,name);
        if(value==null||!value.isString())throw new IllegalArgumentException("Invalid Camponesa field: "+name);
        return value.asString();
    }
    public static String name() { return CATALOG.name(); }
    public static String title() { return CATALOG.title(); }
    public static List<DialogueLine> offer() { return CATALOG.messages().get("offer"); }
    public static List<DialogueLine> accepted() { return CATALOG.messages().get("accepted"); }
    public static List<DialogueLine> journal(FarmerQuest state,boolean globalUnlocked) {
        java.util.Objects.requireNonNull(state);
        return CATALOG.messages().get(globalUnlocked?FarmerQuest.COMPLETED.name():state.name());
    }
}
