package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.utils.JsonValue;
import java.util.ArrayList;
import java.util.List;

/** Immutable presentation data. Showing or revealing a line never changes progress. */
public record DialogueLine(String speaker,String text,DialoguePortrait portrait) {
    public DialogueLine(String speaker,String text) { this(speaker,text,DialoguePortrait.defaultFor(speaker)); }
    public DialogueLine {
        if(speaker==null||speaker.isBlank()||speaker.length()>100||text==null||text.isBlank()||text.length()>600||portrait==null)
            throw new IllegalArgumentException("Invalid dialogue line");
    }
    static DialogueLine parse(JsonValue value) {
        if(!value.isObject())throw new IllegalArgumentException("Invalid dialogue line");
        String speaker=RunReport.string(value,"speaker"),text=RunReport.string(value,"text");
        return new DialogueLine(speaker,text,DialoguePortrait.parse(value.get("portrait"),DialoguePortrait.defaultFor(speaker)));
    }
    static List<DialogueLine> parseLines(JsonValue array,int maximumLines,int maximumTextLength) {
        if(!array.isArray()||array.size<1||array.size>maximumLines)throw new IllegalArgumentException("Invalid dialogue");
        var lines=new ArrayList<DialogueLine>();
        for(JsonValue value:array) {
            DialogueLine line=parse(value);
            if(line.text().length()>maximumTextLength)throw new IllegalArgumentException("Dialogue line too long");
            lines.add(line);
        }
        return List.copyOf(lines);
    }
}
