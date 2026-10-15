package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;

/** Account-wide audio preferences, independent of all character saves. */
public record AudioSettings(float effects, float music, boolean muted) {
    public static final class UnsupportedVersion extends IllegalArgumentException {
        public UnsupportedVersion(){super("Unsupported audio settings version");}
    }
    public static final AudioSettings DEFAULT = new AudioSettings(.75f, .30f, false);
    public AudioSettings { volume(effects); volume(music); }
    private static void volume(float value) {
        if (!Float.isFinite(value) || value < 0 || value > 1) throw new IllegalArgumentException("Invalid volume");
    }
    public float effectsGain() { return muted ? 0 : effects; }
    public float musicGain() { return muted ? 0 : music; }
    public String toJson() {
        JsonValue root = new JsonValue(JsonValue.ValueType.object);
        root.addChild("version", new JsonValue(1L));
        root.addChild("effects", new JsonValue((double) effects));
        root.addChild("music", new JsonValue((double) music));
        root.addChild("muted", new JsonValue(muted));
        return root.prettyPrint(JsonWriter.OutputType.json, 100);
    }
    public static AudioSettings fromJson(String json) {
        JsonValue root = new JsonReader().parse(json);
        if (!root.isObject()) throw new IllegalArgumentException("Invalid audio settings");
        if (RunReport.integer(root,"version") != 1) throw new UnsupportedVersion();
        JsonValue muted = root.require("muted");
        if (!muted.isBoolean()) throw new IllegalArgumentException("Invalid mute");
        return new AudioSettings(RunReport.number(root,"effects"), RunReport.number(root,"music"), muted.asBoolean());
    }
}

