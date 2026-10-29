package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import java.util.HashSet;
import java.util.Set;

/** Durable receipt linking one character attempt to its personal and shared rewards. */
public record RunSettlement(RunReport report,long sequence) {
    public static final class UnsupportedVersion extends IllegalArgumentException {
        public UnsupportedVersion(){super("Unsupported settlement receipt version");}
    }
    public RunSettlement {
        java.util.Objects.requireNonNull(report);
        if(sequence<=0||report.demo()||report.characterId().isEmpty())throw new IllegalArgumentException("Invalid settlement receipt");
        java.util.UUID.fromString(report.characterId());
    }
    public JsonValue json(){
        JsonValue root=new JsonValue(JsonValue.ValueType.object);
        root.addChild("version",new JsonValue(1L));root.addChild("sequence",new JsonValue(sequence));
        root.addChild("report",report.json());return root;
    }
    public String toJson(){return json().prettyPrint(JsonWriter.OutputType.json,100);}
    public static RunSettlement fromJson(String json){return fromJson(new JsonReader().parse(json));}
    public static RunSettlement fromJson(JsonValue root){
        if(root==null||!root.isObject())throw new IllegalArgumentException("Invalid settlement receipt");
        Set<String> fields=new HashSet<>();
        for(JsonValue field:root)if(!Set.of("version","sequence","report").contains(field.name)||!fields.add(field.name))
            throw new IllegalArgumentException("Invalid settlement receipt fields");
        if(fields.size()!=3)throw new IllegalArgumentException("Invalid settlement receipt fields");
        int version=RunReport.integer(root,"version");
        if(version>1)throw new UnsupportedVersion();
        if(version!=1)throw new IllegalArgumentException("Invalid settlement receipt version");
        JsonValue sequence=root.require("sequence");
        if(!sequence.isLong()||sequence.asLong()<=0)throw new IllegalArgumentException("Invalid settlement sequence");
        return new RunSettlement(RunReport.fromJson(root.require("report")),sequence.asLong());
    }
}
