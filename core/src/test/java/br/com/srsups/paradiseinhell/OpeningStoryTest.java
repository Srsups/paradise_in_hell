package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OpeningStoryTest {
    private static String source() throws Exception {
        try(var stream=OpeningStoryTest.class.getResourceAsStream("/cutscenes/opening.json")) {
            assertNotNull(stream);return new String(stream.readAllBytes(),StandardCharsets.UTF_8);
        }
    }
    private static JsonValue copy() throws Exception{return new JsonReader().parse(source());}
    private static String json(JsonValue value){return value.toJson(JsonWriter.OutputType.json);}
    private static void replace(JsonValue root,String key,String value){root.remove(key);root.addChild(key,new JsonReader().parse(value));}
    private static void rejected(JsonValue root){assertThrows(RuntimeException.class,()->OpeningStory.parse(json(root)));}

    @Test void packagedOpeningIsSixImmutableShotsAndFortyFiveSecondsWithoutGrantingTheSpark() throws Exception {
        var story=OpeningStory.parse(source());
        assertEquals(OpeningStory.ID,story.id());assertEquals("opening-v1",OpeningStory.id());
        assertEquals(45,story.duration());assertEquals(story.duration(),OpeningStory.duration());
        assertEquals(story.shots(),OpeningStory.shots());assertEquals(story.title(),OpeningStory.title());
        assertArrayEquals(OpeningStory.ShotId.values(),story.shots().stream().map(OpeningStory.Shot::id).toArray(OpeningStory.ShotId[]::new));
        assertArrayEquals(new float[]{6,7,7,9,7,9},toDurations(story));
        assertTrue(story.shots().stream().allMatch(shot->shot.minReadSeconds()==1.5f));
        String subtitles=String.join(" ",story.shots().stream().flatMap(shot->shot.subtitles().stream()).map(OpeningStory.Subtitle::text).toList());
        assertEquals(11,story.shots().stream().mapToInt(shot->shot.subtitles().size()).sum());
        assertTrue(subtitles.contains("Ela vem comigo"));
        assertTrue(subtitles.contains("Não vou com você"));assertTrue(subtitles.contains("De novo e de novo"));
        assertFalse(subtitles.contains("Centelha"));
        assertThrows(UnsupportedOperationException.class,()->story.shots().clear());
        for(var shot:story.shots()) {
            assertThrows(UnsupportedOperationException.class,()->shot.layers().clear());
            assertThrows(UnsupportedOperationException.class,()->shot.subtitles().clear());
            assertThrows(UnsupportedOperationException.class,()->shot.cues().clear());
        }
    }
    private static float[] toDurations(OpeningStory.Script story){float[] values=new float[6];for(int i=0;i<6;i++)values[i]=story.shots().get(i).duration();return values;}

    @Test void rootAndShotFieldsRejectUnknownMissingDuplicatesAndWrongTypes() throws Exception {
        for(String key:List.of("id","title","shots")){JsonValue root=copy();root.remove(key);rejected(root);}
        JsonValue unknown=copy();unknown.addChild("reward",new JsonValue(100));rejected(unknown);
        assertThrows(RuntimeException.class,()->OpeningStory.parse(source().replaceFirst("\\{","{\"id\":\"opening-v1\",")));
        for(String invalid:List.of("null","[]","42","\"opening\""))assertThrows(RuntimeException.class,()->OpeningStory.parse(invalid));
        for(String key:List.of("id","duration","minReadSeconds","startZoom","endZoom","layers","subtitles","cues")) {
            JsonValue root=copy();root.require("shots").get(0).remove(key);rejected(root);
        }
        JsonValue nullLayer=copy();replace(nullLayer.require("shots").get(0),"layers","[null]");rejected(nullLayer);
    }

    @Test void durationsOrderVersionAndReadGuardCannotBeSilentlyChanged() throws Exception {
        for(String value:List.of("null","\"opening-v2\"","42")){JsonValue root=copy();replace(root,"id",value);rejected(root);}
        for(String value:List.of("0","-1","6.1","\"6\"","true","null")) {
            JsonValue root=copy();replace(root.require("shots").get(0),"duration",value);rejected(root);
        }
        JsonValue wrongOrder=copy();replace(wrongOrder.require("shots").get(0),"id","\"CHARON\"");rejected(wrongOrder);
        JsonValue guard=copy();replace(guard.require("shots").get(0),"minReadSeconds","0");rejected(guard);
        JsonValue camera=copy();replace(camera.require("shots").get(0),"endZoom","2");rejected(camera);
        JsonValue missing=copy();replace(missing,"shots","[]");rejected(missing);
    }

    @Test void layerAssetsAndGeometryAreValidatedWithoutLoadingTextures() throws Exception {
        for(String key:List.of("x","y","width","height","endX","endY","startAlpha","endAlpha","breath")) {
            JsonValue root=copy();replace(root.require("shots").get(0).require("layers").get(0),key,"\"NaN\"");rejected(root);
        }
        for(String[] mutation:new String[][]{{"asset","\"weapon-bow\""},{"width","0"},{"height","-2"},
            {"endX","100000"},{"startAlpha","-0.1"},{"endAlpha","1.1"},{"breath","13"}}) {
            JsonValue root=copy();replace(root.require("shots").get(0).require("layers").get(0),mutation[0],mutation[1]);rejected(root);
        }
        assertTrue(OpeningStory.shots().stream().flatMap(shot->shot.layers().stream()).anyMatch(layer->layer.asset().equals("portal")));
        assertThrows(IllegalArgumentException.class,()->new OpeningStory.Layer("hero-idle",Float.NaN,0,1,1,0,0,1,1,0));
    }

    @Test void subtitlesNeedBoundedCopyAndOrderedInShotCheckpoints() throws Exception {
        for(String[] mutation:new String[][]{{"at","-1"},{"at","6"},{"speaker","\" \""},{"speaker","42"},
            {"text","\" \""},{"text","\""+"x".repeat(221)+"\""}}) {
            JsonValue root=copy();replace(root.require("shots").get(0).require("subtitles").get(0),mutation[0],mutation[1]);rejected(root);
        }
        JsonValue duplicate=copy();replace(duplicate.require("shots").get(5).require("subtitles").get(1),"at","0.15");rejected(duplicate);
        JsonValue empty=copy();replace(empty.require("shots").get(0),"subtitles","[]");rejected(empty);
    }

    @Test void soundCuesMustBeKnownUniqueAndWithinTheirShot() throws Exception {
        for(String[] mutation:new String[][]{{"sound","\"MISSING\""},{"sound","1"},{"at","7"},{"at","-1"}}) {
            JsonValue root=copy();replace(root.require("shots").get(1).require("cues").get(0),mutation[0],mutation[1]);rejected(root);
        }
        JsonValue root=copy(),shot=root.require("shots").get(1);String cue=json(shot.require("cues").get(0));
        replace(shot,"cues","["+cue+","+cue+"]");rejected(root);
        assertThrows(IllegalArgumentException.class,()->new OpeningStory.Cue(Float.POSITIVE_INFINITY,SoundCue.PASSAGE));
    }

    @Test void opacityWindowsMaterializeEarlyHoldTheAbductionAndKeepLegacyLayerDefaults() throws Exception {
        var oldConstructor=new OpeningStory.Layer("hero-idle",0,0,100,150,0,0,.2f,.8f,0);
        assertEquals(0,oldConstructor.fadeStart());assertEquals(1,oldConstructor.fadeEnd());
        assertEquals(.2f,oldConstructor.alphaAt(-1),.0001f);assertEquals(.5f,oldConstructor.alphaAt(.5f),.0001f);
        assertEquals(.8f,oldConstructor.alphaAt(2),.0001f);
        var window=new OpeningStory.Layer("hero-idle",0,0,100,150,0,0,0,1,0,.2f,.6f);
        assertEquals(0,window.alphaAt(.2f));assertEquals(.5f,window.alphaAt(.4f),.0001f);assertEquals(1,window.alphaAt(.6f));
        for(float[] invalid:new float[][]{{-.1f,1},{0,1.1f},{.3f,.3f},{.8f,.2f},{Float.NaN,1},{0,Float.POSITIVE_INFINITY}})
            assertThrows(IllegalArgumentException.class,()->new OpeningStory.Layer("hero-idle",0,0,100,150,0,0,0,1,0,invalid[0],invalid[1]));
        assertThrows(IllegalArgumentException.class,()->window.alphaAt(Float.NaN));
        var shots=OpeningStory.shots();
        var zeus=shots.get(1).layers().stream().filter(layer->layer.asset().equals("zeus-idle")).findFirst().orElseThrow();
        assertEquals(1,zeus.alphaAt(2f/7));
        var portal=shots.get(1).layers().stream().filter(layer->layer.asset().equals("portal")).findFirst().orElseThrow();
        assertEquals(portal.endAlpha(),portal.alphaAt(2f/7));
        var wife=shots.get(2).layers().stream().filter(layer->layer.asset().equals("wife-resist")).findFirst().orElseThrow();
        assertEquals(1,wife.alphaAt(.35f));assertEquals(0,wife.alphaAt(.95f));
        var cursed=shots.get(3).layers().stream().filter(layer->layer.asset().equals("hero-reach")).findFirst().orElseThrow();
        assertEquals(1,cursed.alphaAt(.5f));assertEquals(cursed.endAlpha(),cursed.alphaAt(1));
        var awakened=shots.get(4).layers().stream().filter(layer->layer.asset().equals("hero-idle")).findFirst().orElseThrow();
        assertEquals(1,awakened.alphaAt(.25f));
        JsonValue legacy=copy();for(JsonValue shot:legacy.require("shots"))for(JsonValue layer:shot.require("layers")) {
            layer.remove("fadeStart");layer.remove("fadeEnd");
        }
        for(var shot:OpeningStory.parse(json(legacy)).shots())for(var layer:shot.layers()) {
            assertEquals(0,layer.fadeStart());assertEquals(1,layer.fadeEnd());
        }
        for(String invalid:List.of("null","\"0.2\"","true","[]","{}","-1")) {
            JsonValue root=copy();replace(root.require("shots").get(0).require("layers").get(0),"fadeStart",invalid);rejected(root);
        }
        JsonValue unexpected=copy();unexpected.require("shots").get(0).require("layers").get(0).addChild("opacity",new JsonValue(1));rejected(unexpected);
    }
}
