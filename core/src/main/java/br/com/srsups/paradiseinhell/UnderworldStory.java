package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Presentation-only ending copy; obtaining the spark and acknowledging it belong to progress. */
public final class UnderworldStory {
    private static final Script SCRIPT=load();
    private UnderworldStory() {}

    public static String title() { return SCRIPT.title(); }
    public static String subtitle() { return SCRIPT.subtitle(); }
    public static String nextObjective() { return SCRIPT.nextObjective(); }
    public static String continuationNotice() { return SCRIPT.continuationNotice(); }
    public static List<DialogueLine> lines() { return SCRIPT.lines(); }

    record Script(String title,String subtitle,String nextObjective,String continuationNotice,List<DialogueLine> lines) {
        Script { lines=List.copyOf(lines); }
    }

    private static Script load() {
        try(var stream=UnderworldStory.class.getResourceAsStream("/dialogues/underworld-ending.json")) {
            if(stream==null)throw new IllegalStateException("Missing Underworld ending dialogue");
            return parse(new String(stream.readAllBytes(),StandardCharsets.UTF_8));
        } catch(IOException error) { throw new IllegalStateException("Cannot read Underworld ending dialogue",error); }
    }

    static Script parse(String json) {
        JsonValue root=new JsonReader().parse(json);
        fields(root,Set.of("title","subtitle","nextObjective","continuationNotice","lines"));
        String title=text(root,"title",80),subtitle=text(root,"subtitle",160),objective=text(root,"nextObjective",180),
            notice=text(root,"continuationNotice",120);
        JsonValue array=root.get("lines");
        if(!array.isArray()||array.size<5||array.size>7)throw new IllegalArgumentException("Underworld ending requires five to seven lines");
        for(JsonValue value:array)fields(value,Set.of("speaker","text","portrait"));
        List<DialogueLine> lines=DialogueLine.parseLines(array,7,240);
        for(DialogueLine line:lines) {
            boolean charon="Caronte".equals(line.speaker()),protagonist="Protagonista".equals(line.speaker());
            if(!charon&&!protagonist)throw new IllegalArgumentException("Unexpected Underworld ending speaker");
            boolean charonPortrait=switch(line.portrait()) {
                case CHARON_NEUTRAL,CHARON_AMUSED,CHARON_CONCERNED,CHARON_PLEASED->true;
                default->false;
            };
            if(charon!=charonPortrait||protagonist&&line.portrait()!=DialoguePortrait.PROTAGONIST)
                throw new IllegalArgumentException("Underworld ending portrait does not match speaker");
        }
        return new Script(title,subtitle,objective,notice,lines);
    }

    private static String text(JsonValue object,String key,int maximumLength) {
        JsonValue value=object.get(key);
        if(value==null||!value.isString()||value.asString().isBlank()||value.asString().length()>maximumLength)
            throw new IllegalArgumentException("Invalid Underworld ending "+key);
        return value.asString();
    }

    private static void fields(JsonValue object,Set<String> expected) {
        if(object==null||!object.isObject())throw new IllegalArgumentException("Expected Underworld ending object");
        var found=new HashSet<String>();
        for(JsonValue child:object)
            if(!expected.contains(child.name)||!found.add(child.name))
                throw new IllegalArgumentException("Unexpected or duplicate Underworld ending field: "+child.name);
        if(!found.equals(expected))throw new IllegalArgumentException("Missing Underworld ending fields");
    }
}
