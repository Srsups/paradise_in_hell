package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NpcDialogueTest {
    private static String resource(String path) throws IOException {
        try(var stream=NpcDialogueTest.class.getResourceAsStream(path)) {
            assertNotNull(stream,path);return new String(stream.readAllBytes(),StandardCharsets.UTF_8);
        }
    }
    private static String json(JsonValue value) { return value.toJson(JsonWriter.OutputType.json); }
    private static CampaignProfile profile(Profession profession) {
        CampaignProfile p=new CampaignProfile();p.createCharacter(profession,GameDifficulty.HARD);return p;
    }
    private static boolean mentions(List<DialogueLine> lines,String value) {
        return lines.stream().anyMatch(line->line.text().contains(value));
    }
    private static void validSmithLines(List<DialogueLine> lines,WeaponType weapon) {
        assertTrue(lines.size()>0&&lines.size()<=2);assertTrue(mentions(lines,weapon.label));
        for(DialogueLine line:lines) {
            assertFalse(line.text().isBlank());assertTrue(line.text().length()<=200);assertFalse(line.text().contains("{weapon}"));
        }
    }
    @Test void portraitIdentifiersMapToThirteenDistinctNamedRegionsWithoutLoadingGraphics() {
        String[] expected={"portrait-charon-neutral","portrait-charon-amused","portrait-charon-concerned","portrait-charon-pleased",
            "portrait-blacksmith-neutral","portrait-blacksmith-amused","portrait-blacksmith-concerned","portrait-blacksmith-pleased",
            "portrait-camponesa-neutral","portrait-camponesa-amused","portrait-camponesa-concerned","portrait-camponesa-pleased","portrait-protagonist"};
        String[] actual=Arrays.stream(DialoguePortrait.values()).map(DialoguePortrait::regionName).toArray(String[]::new);
        assertArrayEquals(expected,actual);assertEquals(13,Arrays.stream(actual).distinct().count());
        assertEquals(DialoguePortrait.CHARON_NEUTRAL,new DialogueLine("Caronte","Olá.").portrait());
        assertEquals(DialoguePortrait.BLACKSMITH_NEUTRAL,new DialogueLine("Ferreiro Errante","Olá.").portrait());
        assertEquals(DialoguePortrait.CAMPONESA_NEUTRAL,new DialogueLine("Camponesa das Cinzas","Olá.").portrait());
        assertEquals(DialoguePortrait.PROTAGONIST,new DialogueLine("Protagonista","Olá.").portrait());
    }
    @Test void genericPlaybackRevealsUnicodeWithoutAdvancingAndKeepsTheChosenExpression() {
        DialoguePlayback playback=new DialoguePlayback(List.of(
            new DialogueLine("Ferreiro Errante","Já 🏹 é?",DialoguePortrait.BLACKSMITH_AMUSED),
            new DialogueLine("Protagonista","Sim.")),false);
        playback.advance(4f/42);assertEquals("Já 🏹",playback.visibleText());
        assertEquals(DialoguePortrait.BLACKSMITH_AMUSED,playback.current().portrait());
        assertEquals(DialoguePlayback.Action.REVEALED,playback.accept());assertEquals(0,playback.line());
        assertEquals("Já 🏹 é?",playback.visibleText());assertEquals(DialoguePlayback.Action.ADVANCED,playback.accept());
        assertEquals(DialoguePortrait.PROTAGONIST,playback.current().portrait());assertEquals("",playback.visibleText());
        playback.advance(100);assertEquals(1,playback.line());assertEquals(DialoguePlayback.Action.FINISHED,playback.accept());
        assertThrows(IllegalArgumentException.class,()->playback.advance(-1));
        assertThrows(IllegalArgumentException.class,()->playback.advance(Float.POSITIVE_INFINITY));
    }
    @Test void charonRetainsStoryPriorityBeforeOptionalQuestCommentsAndNeverMutatesProgress() {
        CampaignProfile p=profile(Profession.CACADOR);p.acceptFarmerQuest();
        assertEquals(CampaignProfile.Event.CARONTE_MET,CaronteDialogue.select(p).event());
        p.finishConversation(CampaignProfile.Event.CARONTE_MET);p.recordReturn(RunOutcome.DEATH,0);
        assertEquals(CampaignProfile.Event.BRIBE_JOKE,CaronteDialogue.select(p).event());
        p.finishConversation(CampaignProfile.Event.BRIBE_JOKE);p.recordReturn(RunOutcome.VICTORY,0);
        assertEquals(CampaignProfile.Event.GUARDIAN_RECOGNIZED,CaronteDialogue.select(p).event());
        p.finishConversation(CampaignProfile.Event.GUARDIAN_RECOGNIZED);
        String before=p.toJson();var accepted=CaronteDialogue.select(p);
        assertNull(accepted.event());assertTrue(accepted.lines().getFirst().text().contains("Carrasco"));
        assertEquals(DialoguePortrait.CHARON_NEUTRAL,accepted.lines().getFirst().portrait());assertEquals(before,p.toJson());
        p.advanceFarmerQuest(FarmerQuest.SEED_FOUND);before=p.toJson();var seed=CaronteDialogue.select(p);
        assertNull(seed.event());assertEquals(DialoguePortrait.CHARON_CONCERNED,seed.lines().getFirst().portrait());
        assertTrue(seed.lines().getFirst().text().contains("semente"));assertEquals(before,p.toJson());
        p.advanceFarmerQuest(FarmerQuest.COMPLETED);before=p.toJson();var completed=CaronteDialogue.select(p);
        assertNull(completed.event());assertEquals(DialoguePortrait.CHARON_PLEASED,completed.lines().getFirst().portrait());
        assertTrue(completed.lines().getFirst().text().contains("Promessa"));assertEquals(before,p.toJson());
    }
    @Test void charonCommentsOnForgingAndPreservesTheExpressionWhenReplacingTheReturnCount() throws Exception {
        CampaignProfile p=profile(Profession.FERREIRO);p.finishConversation(CampaignProfile.Event.CARONTE_MET);
        p.recordReturn(RunOutcome.ABANDONED,0);p.recordReturn(RunOutcome.ABANDONED,0);String before=p.toJson();
        var repeat=CaronteDialogue.select(p);var template=CaronteDialogue.parse(resource("/dialogues/charon.json")).get("repeat").getFirst();
        assertNull(repeat.event());assertTrue(repeat.lines().getFirst().text().contains("2 retornos"));
        assertEquals(template.portrait(),repeat.lines().getFirst().portrait());assertEquals(before,p.toJson());
        p.claimForgeMaterial(RegionDefinition.PROTOTYPE_ID);p.forgeWeapon();before=p.toJson();
        var forged=CaronteDialogue.select(p);assertNull(forged.event());
        assertTrue(forged.lines().getFirst().text().contains("rara"));assertEquals(DialoguePortrait.CHARON_AMUSED,forged.lines().getFirst().portrait());
        DialoguePlayback playback=new DialoguePlayback(forged,true);
        for(int i=0;i<forged.lines().size()+2;i++)playback.accept();assertEquals(before,p.toJson());
    }
    @Test void legacyCharonCopyWithoutPortraitsUsesDefaultsAndOptionalNewBranchesRemainOptional() throws Exception {
        JsonValue legacy=new JsonReader().parse(resource("/dialogues/charon.json"));
        for(String key:List.of("questAccepted","questSeed","questComplete","forged"))legacy.remove(key);
        for(JsonValue array:legacy)for(JsonValue line:array)line.remove("portrait");
        var loaded=CaronteDialogue.parse(json(legacy));assertEquals(4,loaded.size());
        for(var lines:loaded.values())for(var line:lines)
            assertEquals(DialoguePortrait.defaultFor(line.speaker()),line.portrait());
        var oldLine=new CaronteDialogue.Line("Caronte","Texto antigo.");
        var compatible=new DialoguePlayback(new CaronteDialogue.Conversation(null,List.of(oldLine)),false);
        assertEquals(DialoguePortrait.CHARON_NEUTRAL,compatible.current().portrait());assertEquals(1,compatible.lineCount());
    }
    @Test void blacksmithSelectsNoMaterialReadyFellowSmithAndRareWithoutChangingAnyFacts() {
        CampaignProfile p=profile(Profession.CACADOR);String before=p.toJson();
        assertEquals(DialoguePortrait.BLACKSMITH_CONCERNED,BlacksmithDialogue.select(p,WeaponType.BOW).getFirst().portrait());
        assertEquals(before,p.toJson());p.claimForgeMaterial(RegionDefinition.PROTOTYPE_ID);before=p.toJson();
        assertEquals(DialoguePortrait.BLACKSMITH_NEUTRAL,BlacksmithDialogue.select(p,WeaponType.BOW).getFirst().portrait());
        assertEquals(before,p.toJson());p.forgeWeapon();before=p.toJson();
        assertEquals(DialoguePortrait.BLACKSMITH_PLEASED,BlacksmithDialogue.select(p,WeaponType.BOW).getFirst().portrait());assertEquals(before,p.toJson());
        p.claimForgeMaterial("other-region");before=p.toJson();
        assertEquals(DialoguePortrait.BLACKSMITH_PLEASED,BlacksmithDialogue.select(p,WeaponType.BOW).getFirst().portrait());assertEquals(before,p.toJson());
        CampaignProfile colleague=profile(Profession.FERREIRO);colleague.claimForgeMaterial(RegionDefinition.PROTOTYPE_ID);before=colleague.toJson();
        assertTrue(BlacksmithDialogue.select(colleague,WeaponType.SWORD).getFirst().text().contains("colega"));
        assertEquals(DialoguePortrait.BLACKSMITH_AMUSED,BlacksmithDialogue.select(colleague,WeaponType.SWORD).getFirst().portrait());
        assertEquals(before,colleague.toJson());
    }
    @Test void blacksmithResponsesNameEveryWeaponAndStayWithinTwoShortLines() {
        CampaignProfile p=profile(Profession.FAZENDEIRO);p.claimForgeMaterial(RegionDefinition.PROTOTYPE_ID);String before=p.toJson();
        for(WeaponType weapon:WeaponType.values()) {
            validSmithLines(BlacksmithDialogue.select(p,weapon),weapon);validSmithLines(BlacksmithDialogue.success(weapon),weapon);
            validSmithLines(BlacksmithDialogue.failure(weapon),weapon);validSmithLines(BlacksmithDialogue.missingMaterial(weapon),weapon);
            assertEquals(DialoguePortrait.BLACKSMITH_PLEASED,BlacksmithDialogue.success(weapon).getFirst().portrait());
            assertEquals(DialoguePortrait.BLACKSMITH_CONCERNED,BlacksmithDialogue.failure(weapon).getFirst().portrait());
        }
        assertEquals(before,p.toJson());assertEquals(1,p.forgeMaterials());assertEquals(WeaponRarity.COMMON,p.weaponRarity());
    }
    @Test void camponesaKeepsLegacyStringsAndOneLinePerStageWithTheGlobalUnlockTakingPriority() throws Exception {
        JsonValue copy=new JsonReader().parse(resource("/dialogue/camponesa.json"));
        assertEquals(copy.getString("name"),CamponesaDialogue.name());assertEquals(copy.getString("title"),CamponesaDialogue.title());
        assertEquals(1,CamponesaDialogue.offer().size());assertEquals(copy.getString("offer"),CamponesaDialogue.offer().getFirst().text());
        assertEquals(DialoguePortrait.CAMPONESA_AMUSED,CamponesaDialogue.offer().getFirst().portrait());
        assertEquals(1,CamponesaDialogue.accepted().size());assertEquals(copy.getString("accepted"),CamponesaDialogue.accepted().getFirst().text());
        for(FarmerQuest state:FarmerQuest.values()) {
            var journal=CamponesaDialogue.journal(state,false);assertEquals(1,journal.size());
            if(state==FarmerQuest.ACCEPTED) {
                assertTrue(journal.getFirst().text().startsWith("Promessa aceita."));
                assertTrue(journal.getFirst().text().contains("A semente é garantida"));
                assertNotEquals(CamponesaDialogue.accepted().getFirst().text(),journal.getFirst().text(),
                    "The ACCEPTED journal objective differs from the accepted dialogue reply");
            } else assertEquals(copy.getString(state.name()),journal.getFirst().text());
            assertEquals(CamponesaDialogue.name(),journal.getFirst().speaker());
            assertEquals(CamponesaDialogue.journal(FarmerQuest.COMPLETED,false),CamponesaDialogue.journal(state,true));
        }
        assertEquals(DialoguePortrait.CAMPONESA_CONCERNED,CamponesaDialogue.journal(FarmerQuest.ACCEPTED,false).getFirst().portrait());
        assertEquals(DialoguePortrait.CAMPONESA_PLEASED,CamponesaDialogue.journal(FarmerQuest.COMPLETED,false).getFirst().portrait());
    }
    @Test void camponesaReadingAndGlobalJournalNeverAcceptTheQuestOrChangeEitherProgressFile() {
        CampaignProfile p=profile(Profession.CACADOR);SharedProgress shared=new SharedProgress();
        String personal=p.toJson(),global=shared.toJson();
        var playback=new DialoguePlayback(CamponesaDialogue.offer(),false);playback.advance(100);
        assertEquals(DialoguePlayback.Action.FINISHED,playback.accept());
        new DialoguePlayback(CamponesaDialogue.accepted(),true).accept();
        for(FarmerQuest stage:FarmerQuest.values())CamponesaDialogue.journal(stage,true);
        assertEquals(personal,p.toJson());assertEquals(global,shared.toJson());
    }
    @Test void camponesaAndBlacksmithLegacyPortraitDefaultsRemainReadable() throws Exception {
        JsonValue farmer=new JsonReader().parse(resource("/dialogue/camponesa.json"));farmer.remove("portraits");
        for(var lines:CamponesaDialogue.parse(json(farmer)).messages().values())
            assertEquals(DialoguePortrait.CAMPONESA_NEUTRAL,lines.getFirst().portrait());
        JsonValue smith=new JsonReader().parse(resource("/dialogues/blacksmith.json"));
        for(JsonValue array:smith)for(JsonValue line:array)line.remove("portrait");
        for(var lines:BlacksmithDialogue.parse(json(smith)).values())for(DialogueLine line:lines)
            assertEquals(DialoguePortrait.defaultFor(line.speaker()),line.portrait());
    }
    @Test void invalidPortraitsBlankCopyAndOversizedBlacksmithLinesAreRejected() throws Exception {
        for(String value:List.of("\"MISSING\"","1","true","null","[]","{}")) {
            JsonValue charon=new JsonReader().parse(resource("/dialogues/charon.json")),line=charon.require("first").get(0);
            line.remove("portrait");line.addChild("portrait",new JsonReader().parse(value));
            assertThrows(IllegalArgumentException.class,()->CaronteDialogue.parse(json(charon)),value);
            JsonValue farmer=new JsonReader().parse(resource("/dialogue/camponesa.json")),portraits=farmer.require("portraits");
            portraits.remove("offer");portraits.addChild("offer",new JsonReader().parse(value));
            assertThrows(IllegalArgumentException.class,()->CamponesaDialogue.parse(json(farmer)),value);
        }
        for(String key:List.of("ready","readyBlacksmith","rare","missingMaterial","success","failure")) {
            JsonValue smith=new JsonReader().parse(resource("/dialogues/blacksmith.json")),line=smith.require(key).get(0);
            line.remove("text");line.addChild("text",new JsonValue("x".repeat(201)));
            assertThrows(IllegalArgumentException.class,()->BlacksmithDialogue.parse(json(smith)),key);
        }
        assertThrows(IllegalArgumentException.class,()->new DialogueLine(" ","Texto."));
        assertThrows(IllegalArgumentException.class,()->new DialogueLine("Caronte"," "));
        assertThrows(IllegalArgumentException.class,()->new DialogueLine("Caronte","Texto.",null));
        assertThrows(RuntimeException.class,()->BlacksmithDialogue.parse("{}"));
        assertThrows(RuntimeException.class,()->CamponesaDialogue.parse("{}"));
    }
}
