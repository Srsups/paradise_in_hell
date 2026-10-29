package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Immutable cinematic data; loading and parsing never touch graphics or campaign progress. */
public final class OpeningStory {
    public static final String ID="opening-v1";
    public static final float MIN_READ_SECONDS=1.5f;
    public enum ShotId { HOME, ARRIVAL, ABDUCTION, CURSE, STYX, CHARON }
    private static final Set<String> ASSETS=Set.of("background-home","background-styx","foreground-home","foreground-styx",
        "hero-idle","hero-reach","wife-idle","wife-resist","zeus-idle","zeus-cast","charon","portal");
    public record Layer(String asset,float x,float y,float width,float height,float endX,float endY,
                        float startAlpha,float endAlpha,float breath,float fadeStart,float fadeEnd) {
        public Layer(String asset,float x,float y,float width,float height,float endX,float endY,
                     float startAlpha,float endAlpha,float breath) {
            this(asset,x,y,width,height,endX,endY,startAlpha,endAlpha,breath,0,1);
        }
        public Layer {
            if(!ASSETS.contains(asset))throw new IllegalArgumentException("Unknown opening layer asset");
            finite(x);finite(y);finite(endX);finite(endY);finite(width);finite(height);finite(startAlpha);finite(endAlpha);finite(breath);
            finite(fadeStart);finite(fadeEnd);
            if(Math.abs(x)>1920||Math.abs(endX)>1920||Math.abs(y)>1080||Math.abs(endY)>1080
                ||width<=0||width>2048||height<=0||height>1152||startAlpha<0||startAlpha>1||endAlpha<0||endAlpha>1||breath<0||breath>12
                ||fadeStart<0||fadeStart>=fadeEnd||fadeEnd>1)
                throw new IllegalArgumentException("Invalid opening layer geometry");
        }
        /** Opacity has its own normalized time window; position and camera keep the whole shot. */
        public float alphaAt(float progress) {
            finite(progress);float t=Math.max(0,Math.min(1,(progress-fadeStart)/(fadeEnd-fadeStart)));
            if(t<=0)return startAlpha;if(t>=1)return endAlpha;
            float smooth=t*t*(3-2*t);return startAlpha+(endAlpha-startAlpha)*smooth;
        }
    }
    public record Subtitle(float at,String speaker,String text) {
        public Subtitle { finite(at);if(at<0)throw new IllegalArgumentException("Invalid subtitle time");copy(speaker,40);copy(text,220); }
    }
    public record Cue(float at,SoundCue sound) {
        public Cue { finite(at);if(at<0||sound==null)throw new IllegalArgumentException("Invalid opening sound cue"); }
    }
    public record Shot(ShotId id,float duration,float minReadSeconds,float startZoom,float endZoom,
                       List<Layer> layers,List<Subtitle> subtitles,List<Cue> cues) {
        public Shot {
            java.util.Objects.requireNonNull(id);finite(duration);finite(minReadSeconds);finite(startZoom);finite(endZoom);
            if(duration<=0||duration>30||minReadSeconds!=MIN_READ_SECONDS||minReadSeconds>duration
                ||startZoom<.8f||startZoom>1.2f||endZoom<.8f||endZoom>1.2f)throw new IllegalArgumentException("Invalid opening shot timing/camera");
            layers=List.copyOf(layers);subtitles=List.copyOf(subtitles);cues=List.copyOf(cues);
            if(layers.isEmpty()||layers.size()>12||subtitles.isEmpty()||subtitles.size()>4||cues.size()>6)
                throw new IllegalArgumentException("Invalid opening shot content");
            float previous=-1;
            for(Subtitle subtitle:subtitles) {
                if(subtitle.at()>=duration||subtitle.at()<=previous)throw new IllegalArgumentException("Unordered opening subtitles");
                previous=subtitle.at();
            }
            previous=-1;var soundEvents=new HashSet<Cue>();
            for(Cue cue:cues) {
                if(cue.at()>=duration||cue.at()<previous||!soundEvents.add(cue))throw new IllegalArgumentException("Unordered or duplicate opening sound cue");
                previous=cue.at();
            }
        }
    }
    public record Script(String id,String title,List<Shot> shots) {
        public Script {
            if(!ID.equals(id))throw new IllegalArgumentException("Unknown opening id");copy(title,80);shots=List.copyOf(shots);
            if(shots.size()!=ShotId.values().length)throw new IllegalArgumentException("Opening needs six shots");
            float[] expected={6,7,7,9,7,9};
            for(int index=0;index<shots.size();index++)
                if(shots.get(index).id()!=ShotId.values()[index]||shots.get(index).duration()!=expected[index])
                    throw new IllegalArgumentException("Invalid opening shot order/duration");
        }
        public float duration(){float result=0;for(Shot shot:shots)result+=shot.duration();return result;}
    }
    private static final Script SCRIPT=load();
    private OpeningStory() {}
    public static String id(){return ID;}
    public static String title(){return SCRIPT.title();}
    public static List<Shot> shots(){return SCRIPT.shots();}
    public static float duration(){return SCRIPT.duration();}
    public static Script script(){return SCRIPT;}
    private static Script load() {
        try(var stream=OpeningStory.class.getResourceAsStream("/cutscenes/opening.json")) {
            if(stream==null)throw new IllegalStateException("Missing opening cinematic");
            return parse(new String(stream.readAllBytes(),StandardCharsets.UTF_8));
        } catch(IOException error){throw new IllegalStateException("Cannot read opening cinematic",error);}
    }
    static Script parse(String json) {
        JsonValue root=new JsonReader().parse(json);fields(root,Set.of("id","title","shots"));
        var shots=new ArrayList<Shot>();
        for(JsonValue scene:array(root,"shots",6)) {
            fields(scene,Set.of("id","duration","minReadSeconds","startZoom","endZoom","layers","subtitles","cues"));
            var layers=new ArrayList<Layer>();
            for(JsonValue layer:array(scene,"layers",12)) {
                fields(layer,Set.of("asset","x","y","width","height","endX","endY","startAlpha","endAlpha","breath"),Set.of("fadeStart","fadeEnd"));
                layers.add(new Layer(text(layer,"asset"),number(layer,"x"),number(layer,"y"),number(layer,"width"),number(layer,"height"),
                    number(layer,"endX"),number(layer,"endY"),number(layer,"startAlpha"),number(layer,"endAlpha"),number(layer,"breath"),
                    layer.has("fadeStart")?number(layer,"fadeStart"):0,layer.has("fadeEnd")?number(layer,"fadeEnd"):1));
            }
            var subtitles=new ArrayList<Subtitle>();
            for(JsonValue subtitle:array(scene,"subtitles",4)) {
                fields(subtitle,Set.of("at","speaker","text"));subtitles.add(new Subtitle(number(subtitle,"at"),text(subtitle,"speaker"),text(subtitle,"text")));
            }
            var cues=new ArrayList<Cue>();
            for(JsonValue cue:array(scene,"cues",6)) {
                fields(cue,Set.of("at","sound"));cues.add(new Cue(number(cue,"at"),SoundCue.valueOf(text(cue,"sound"))));
            }
            shots.add(new Shot(ShotId.valueOf(text(scene,"id")),number(scene,"duration"),number(scene,"minReadSeconds"),
                number(scene,"startZoom"),number(scene,"endZoom"),layers,subtitles,cues));
        }
        return new Script(text(root,"id"),text(root,"title"),shots);
    }
    private static JsonValue array(JsonValue object,String key,int maximum) {
        JsonValue value=object.get(key);if(value==null||!value.isArray()||value.size>maximum)throw new IllegalArgumentException("Invalid opening "+key);return value;
    }
    private static String text(JsonValue object,String key) {
        JsonValue value=object.get(key);if(value==null||!value.isString())throw new IllegalArgumentException("Invalid opening "+key);return value.asString();
    }
    private static float number(JsonValue object,String key) {
        JsonValue value=object.get(key);if(value==null||!value.isLong()&&!value.isDouble())throw new IllegalArgumentException("Invalid opening "+key);
        float result=value.asFloat();finite(result);return result;
    }
    private static void copy(String text,int maximum) {if(text==null||text.isBlank()||text.length()>maximum)throw new IllegalArgumentException("Invalid opening copy");}
    private static void finite(float value){if(!Float.isFinite(value))throw new IllegalArgumentException("Non-finite opening value");}
    private static void fields(JsonValue object,Set<String> expected) {
        fields(object,expected,Set.of());
    }
    private static void fields(JsonValue object,Set<String> expected,Set<String> optional) {
        if(object==null||!object.isObject())throw new IllegalArgumentException("Expected opening object");
        var found=new HashSet<String>();
        for(JsonValue field:object)if(!expected.contains(field.name)&&!optional.contains(field.name)||!found.add(field.name))throw new IllegalArgumentException("Unexpected/duplicate opening field");
        if(!found.containsAll(expected))throw new IllegalArgumentException("Missing opening field");
    }
}
