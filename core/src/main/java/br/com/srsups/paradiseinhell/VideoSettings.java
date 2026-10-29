package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import java.util.List;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Width/height retain the preferred decorated window size in every display mode. */
public record VideoSettings(int width,int height,Mode mode,boolean vsync) {
    public enum Mode {
        WINDOWED("Janela"),BORDERLESS("Janela sem bordas"),FULLSCREEN("Tela cheia");
        private final String label;
        Mode(String label){this.label=label;}
        public String label(){return label;}
    }
    public static final class UnsupportedVersion extends IllegalArgumentException {
        public UnsupportedVersion(){super("Versão de vídeo não suportada.");}
    }
    public record Size(int width,int height) {
        public Size { validateSize(width,height); }
        public String label(){return width+" × "+height;}
    }
    public static final VideoSettings DEFAULT=new VideoSettings(960,540,Mode.WINDOWED,true);
    public static final List<Size> PRESETS=List.of(new Size(800,450),new Size(960,540),
        new Size(1280,720),new Size(1600,900),new Size(1920,1080));
    public VideoSettings { validateSize(width,height);Objects.requireNonNull(mode,"Modo de exibição ausente."); }
    /** Compatibility for callers and version-1 preferences that had only two modes. */
    public VideoSettings(int width,int height,boolean fullscreen,boolean vsync) {
        this(width,height,fullscreen?Mode.FULLSCREEN:Mode.WINDOWED,vsync);
    }
    private static void validateSize(int width,int height) {
        if(width<640 || width>7680 || height<360 || height>4320)throw new IllegalArgumentException("Invalid window dimensions");
    }
    public boolean fullscreen(){return mode==Mode.FULLSCREEN;}
    public VideoSettings withSize(Size size){Objects.requireNonNull(size);return new VideoSettings(size.width(),size.height(),mode,vsync);}
    public VideoSettings withMode(Mode value){return new VideoSettings(width,height,value,vsync);}
    public VideoSettings withFullscreen(boolean value){return withMode(value?Mode.FULLSCREEN:Mode.WINDOWED);}
    public VideoSettings withVsync(boolean value){return new VideoSettings(width,height,mode,value);}
    public String toJson() {
        JsonValue root=new JsonValue(JsonValue.ValueType.object);
        root.addChild("version",new JsonValue(2L));root.addChild("width",new JsonValue((long)width));
        root.addChild("height",new JsonValue((long)height));root.addChild("mode",new JsonValue(mode.name()));
        root.addChild("vsync",new JsonValue(vsync));return root.prettyPrint(JsonWriter.OutputType.json,100);
    }
    public static VideoSettings fromJson(String json) {
        JsonValue root=new JsonReader().parse(json);
        if(root==null||!root.isObject())throw new IllegalArgumentException("Preferências de vídeo inválidas.");
        JsonValue versionValue=root.require("version");
        if(!versionValue.isLong())throw new IllegalArgumentException("Versão de vídeo inválida.");
        long version=versionValue.asLong();if(version!=1&&version!=2)throw new UnsupportedVersion();
        fields(root,version==1?Set.of("version","width","height","fullscreen","vsync")
            :Set.of("version","width","height","mode","vsync"));
        Mode mode;
        if(version==1)mode=flag(root,"fullscreen")?Mode.FULLSCREEN:Mode.WINDOWED;
        else {
            JsonValue field=root.require("mode");
            if(!field.isString())throw new IllegalArgumentException("Modo de exibição inválido.");
            try{mode=Mode.valueOf(field.asString());}
            catch(IllegalArgumentException invalid){throw new IllegalArgumentException("Modo de exibição desconhecido.",invalid);}
        }
        return new VideoSettings(integer(root,"width"),integer(root,"height"),mode,flag(root,"vsync"));
    }
    private static void fields(JsonValue root,Set<String> expected) {
        Set<String> found=new HashSet<>();
        for(JsonValue child:root)
            if(!expected.contains(child.name)||!found.add(child.name))throw new IllegalArgumentException("Campo de vídeo desconhecido ou duplicado.");
        if(!found.equals(expected))throw new IllegalArgumentException("Campo de vídeo ausente.");
    }
    private static int integer(JsonValue root,String key) {
        JsonValue value=root.require(key);
        if(!value.isLong() || value.asLong()<0 || value.asLong()>Integer.MAX_VALUE)throw new IllegalArgumentException("Invalid "+key);
        return value.asInt();
    }
    private static boolean flag(JsonValue root,String key) {
        JsonValue value=root.require(key);if(!value.isBoolean())throw new IllegalArgumentException("Invalid "+key);return value.asBoolean();
    }
}
