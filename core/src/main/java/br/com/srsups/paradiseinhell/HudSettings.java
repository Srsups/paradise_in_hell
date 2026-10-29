package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import java.util.HashSet;
import java.util.Set;

/** Account-wide combat HUD size. It never changes the world camera or native video mode. */
public record HudSettings(int percent) {
    public static final int MIN_PERCENT=80,MAX_PERCENT=125;
    public static final HudSettings DEFAULT=new HudSettings(100);
    public static final class UnsupportedVersion extends IllegalArgumentException {
        public UnsupportedVersion(){super("Versão de tamanho do HUD não suportada.");}
    }
    public HudSettings {
        if(percent<MIN_PERCENT||percent>MAX_PERCENT)
            throw new IllegalArgumentException("Escolha um tamanho de HUD entre "+MIN_PERCENT+"% e "+MAX_PERCENT+"%.");
    }
    public float scale(){return percent/100f;}
    public HudSettings withPercent(int value){return new HudSettings(value);}
    public String toJson() {
        JsonValue root=new JsonValue(JsonValue.ValueType.object);
        root.addChild("version",new JsonValue(1L));
        root.addChild("percent",new JsonValue((long)percent));
        return root.prettyPrint(JsonWriter.OutputType.json,100);
    }
    public static HudSettings fromJson(String json) {
        JsonValue root=new JsonReader().parse(json);
        if(root==null||!root.isObject())throw new IllegalArgumentException("Objeto de tamanho do HUD inválido.");
        JsonValue version=root.require("version");
        if(!version.isLong())throw new IllegalArgumentException("Versão de tamanho do HUD inválida.");
        // Inspect the version first: future files can use an entirely different schema.
        if(version.asLong()!=1)throw new UnsupportedVersion();
        Set<String> found=new HashSet<>();
        for(JsonValue child:root) {
            if(!Set.of("version","percent").contains(child.name)||!found.add(child.name))
                throw new IllegalArgumentException("Campo de tamanho do HUD desconhecido ou duplicado.");
        }
        if(!found.equals(Set.of("version","percent")))throw new IllegalArgumentException("Campo de tamanho do HUD ausente.");
        JsonValue percent=root.require("percent");
        if(!percent.isLong()||percent.asLong()<MIN_PERCENT||percent.asLong()>MAX_PERCENT)
            throw new IllegalArgumentException("Tamanho do HUD inválido.");
        return new HudSettings(percent.asInt());
    }
}
