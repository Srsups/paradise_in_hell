package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UnderworldStoryTest {
    private static String source() throws IOException {
        try(var stream=UnderworldStoryTest.class.getResourceAsStream("/dialogues/underworld-ending.json")) {
            assertNotNull(stream);return new String(stream.readAllBytes(),StandardCharsets.UTF_8);
        }
    }
    private static String json(JsonValue value) { return value.toJson(JsonWriter.OutputType.json); }
    private static JsonValue copy() throws IOException { return new JsonReader().parse(source()); }
    private static void replace(JsonValue object,String key,String value) {
        object.remove(key);object.addChild(key,new JsonReader().parse(value));
    }
    private static void rejected(JsonValue value) {
        assertThrows(RuntimeException.class,()->UnderworldStory.parse(json(value)));
    }

    @Test void packagedEndingSeparatesTheSparkFromTheCurseAndNamesTheRemainingJourney() throws Exception {
        var story=UnderworldStory.parse(source());
        assertEquals("Centelha da Insurgência",UnderworldStory.title());
        assertEquals(story.title(),UnderworldStory.title());assertEquals(story.subtitle(),UnderworldStory.subtitle());
        assertEquals(story.nextObjective(),UnderworldStory.nextObjective());
        assertEquals(story.continuationNotice(),UnderworldStory.continuationNotice());
        assertEquals(story.lines(),UnderworldStory.lines());assertEquals(7,story.lines().size());
        String all=String.join(" ",story.lines().stream().map(DialogueLine::text).toList());
        assertTrue(all.contains("O Guardião caiu"));assertTrue(all.contains("A maldição faz você retornar"));
        assertTrue(all.contains("A Centelha permitirá ferir os deuses"));
        assertTrue(all.contains("Por vontade própria"));assertTrue(all.contains("refúgio"));
        assertTrue(story.nextObjective().contains("esposa"));assertTrue(story.nextObjective().contains("Zeus"));
        assertTrue(story.continuationNotice().contains("em desenvolvimento"));
        assertTrue(story.lines().stream().allMatch(line->line.text().length()<=240));
        assertThrows(UnsupportedOperationException.class,()->story.lines().clear());
    }

    @Test void readingEndingOrTheGuardianCommentDoesNotGrantProgressOrChangeConversationPriority() {
        CampaignProfile profile=new CampaignProfile();profile.createCharacter(Profession.CACADOR,GameDifficulty.MEDIUM);
        profile.finishConversation(CampaignProfile.Event.CARONTE_MET);profile.recordReturn(RunOutcome.VICTORY,0);
        String before=profile.toJson();var conversation=CaronteDialogue.select(profile);
        assertEquals(CampaignProfile.Event.GUARDIAN_RECOGNIZED,conversation.event());
        assertTrue(conversation.lines().stream().anyMatch(line->line.text().contains("licença sem prazo")));
        assertTrue(conversation.lines().stream().anyMatch(line->line.text().contains("Centelha da Insurgência")));
        DialoguePlayback playback=new DialoguePlayback(UnderworldStory.lines(),true);
        for(int i=0;i<UnderworldStory.lines().size()-1;i++)assertEquals(DialoguePlayback.Action.ADVANCED,playback.accept());
        assertEquals(DialoguePlayback.Action.FINISHED,playback.accept());
        assertEquals(DialoguePlayback.Action.FINISHED,playback.accept());
        assertEquals(before,profile.toJson(),"Presentation must not settle rewards or narrative events");
    }

    @Test void unknownMissingDuplicateAndWrongRootFieldsAreRejected() throws Exception {
        for(String key:List.of("title","subtitle","nextObjective","continuationNotice","lines")) {
            JsonValue missing=copy();missing.remove(key);rejected(missing);
        }
        JsonValue unknown=copy();unknown.addChild("reward",new JsonValue(100));rejected(unknown);
        String duplicate=source().replaceFirst("\\{","{\"title\":\"Duplicate\",");
        assertThrows(RuntimeException.class,()->UnderworldStory.parse(duplicate));
        for(String invalid:List.of("null","[]","42","\"ending\""))
            assertThrows(RuntimeException.class,()->UnderworldStory.parse(invalid));
    }

    @Test void copyFieldsRejectCoercedTypesBlankTextAndOversizedValues() throws Exception {
        for(String key:List.of("title","subtitle","nextObjective","continuationNotice"))
            for(String invalid:List.of("null","42","true","[]","{}","\" \"","\""+"x".repeat(181)+"\"")) {
                JsonValue root=copy();replace(root,key,invalid);rejected(root);
            }
    }

    @Test void endingLengthMustRemainShortAndUseful() throws Exception {
        String line=json(copy().require("lines").get(0));
        for(int count:List.of(0,1,4,5,6,7,8,32)) {
            JsonValue root=copy();replace(root,"lines","["+String.join(",",Collections.nCopies(count,line))+"]");
            if(count>=5&&count<=7)assertEquals(count,UnderworldStory.parse(json(root)).lines().size());else rejected(root);
        }
        for(String invalid:List.of("null","{}","\"dialogue\"")) {
            JsonValue root=copy();replace(root,"lines",invalid);rejected(root);
        }
    }

    @Test void linesRequireExplicitValidFieldsAndMatchingExistingPortraits() throws Exception {
        for(String key:List.of("speaker","text","portrait")) {
            JsonValue root=copy();root.require("lines").get(0).remove(key);rejected(root);
        }
        JsonValue unknown=copy();unknown.require("lines").get(0).addChild("currency",new JsonValue(5));rejected(unknown);
        for(String invalid:List.of("null","42","true","[]","{}","\"MISSING\"","\"PROTAGONIST\"","\"BLACKSMITH_NEUTRAL\"")) {
            JsonValue root=copy();replace(root.require("lines").get(0),"portrait",invalid);rejected(root);
        }
        JsonValue protagonist=copy();replace(protagonist.require("lines").get(1),"portrait","\"CHARON_NEUTRAL\"");rejected(protagonist);
        JsonValue stranger=copy();replace(stranger.require("lines").get(0),"speaker","\"Zeus\"");rejected(stranger);
        String duplicate=source().replaceFirst("\"speaker\": \"Caronte\"","\"speaker\":\"Caronte\",\"speaker\":\"Protagonista\"");
        assertThrows(RuntimeException.class,()->UnderworldStory.parse(duplicate));
    }

    @Test void invalidLineCopyCannotBypassTheDialogueContract() throws Exception {
        for(String key:List.of("speaker","text"))for(String invalid:List.of("null","42","true","[]","{}","\" \"")) {
            JsonValue root=copy();replace(root.require("lines").get(0),key,invalid);rejected(root);
        }
        JsonValue tooLong=copy();replace(tooLong.require("lines").get(0),"text","\""+"x".repeat(241)+"\"");rejected(tooLong);
        JsonValue notAnObject=copy();replace(notAnObject,"lines","[null,null,null,null,null]");rejected(notAnObject);
    }
}
