package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.utils.*;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MilestoneFourAltarTest {
    @Test void versionOneProfilesWithoutNewPurchasesKeepTheirCharacterCoinsAndOriginalRanks(){
        CampaignProfile p=new CampaignProfile();p.createCharacter(Profession.GUERREIRO,GameDifficulty.HARD);
        p.credit(100);p.buy(PermanentAttribute.VITALITY);p.buy(PermanentAttribute.AGILITY);
        p.claimForgeMaterial(RegionDefinition.PROTOTYPE_ID);p.forgeWeapon();
        JsonValue json=new JsonReader().parse(p.toJson()),attributes=json.require("attributes");
        attributes.remove("ENDURANCE");attributes.remove("RECOVERY");attributes.remove("ARMOR");
        CampaignProfile migrated=CampaignProfile.fromJson(json.toJson(JsonWriter.OutputType.json));
        assertEquals(p.characterId(),migrated.characterId());assertEquals(p.obols(),migrated.obols());
        assertEquals(GameDifficulty.HARD,migrated.difficulty());assertEquals(Profession.GUERREIRO,migrated.profession());
        assertEquals(WeaponRarity.RARE,migrated.weaponRarity());assertFalse(migrated.canClaimForgeMaterial(RegionDefinition.PROTOTYPE_ID));
        assertEquals(1,migrated.rank(PermanentAttribute.VITALITY));assertEquals(1,migrated.rank(PermanentAttribute.AGILITY));
        assertEquals(0,migrated.bonuses().endurance());assertEquals(0,migrated.bonuses().recovery());assertEquals(0,migrated.bonuses().armor());
        assertEquals(migrated.toJson(),CampaignProfile.fromJson(migrated.toJson()).toJson());
    }
    @Test void newRanksArePersistedLimitedAndSpendOnlyTheDisplayedPrice(){
        CampaignProfile p=new CampaignProfile();p.credit(1000);
        for(PermanentAttribute attribute:PermanentAttribute.values()){
            int previous=0;
            for(int rank=0;rank<5;rank++){
                int price=attribute.price(rank),balance=p.obols();assertTrue(price>previous);previous=price;
                assertTrue(p.buy(attribute));assertEquals(balance-price,p.obols());
            }
            int balance=p.obols();assertFalse(p.buy(attribute));assertEquals(balance,p.obols());
        }
        CampaignProfile loaded=CampaignProfile.fromJson(p.toJson());
        for(PermanentAttribute attribute:PermanentAttribute.values())assertEquals(5,loaded.rank(attribute));
        assertEquals(p.obols(),loaded.obols());assertEquals(5,PermanentAttribute.VITALITY.price(0));assertEquals(21,PermanentAttribute.VITALITY.price(4));
    }
    @Test void missingRequiredLegacyFieldsAndMalformedNewRanksAreRejected(){
        JsonValue json=new JsonReader().parse(new CampaignProfile().toJson());json.require("attributes").remove("VITALITY");
        assertThrows(IllegalArgumentException.class,()->CampaignProfile.fromJson(json.toJson(JsonWriter.OutputType.json)));
        String source=new CampaignProfile().toJson();
        for(String value:new String[]{"-1","6","1.5","\"1\""})
            assertThrows(IllegalArgumentException.class,()->CampaignProfile.fromJson(source.replace("\"ENDURANCE\": 0","\"ENDURANCE\": "+value)));
        assertThrows(IllegalArgumentException.class,()->new PermanentBonuses(0,0,0,6,0,0));
        assertThrows(IllegalArgumentException.class,()->new PermanentBonuses(0,0,0,0,-1,0));
        assertThrows(IllegalArgumentException.class,()->new PermanentBonuses(0,0,0,0,0,6));
        assertThrows(IllegalArgumentException.class,()->new PermanentBonuses(0,0,0,0,0,-1));
    }
    @Test void legacyCollectionPurchasesBecomeArmorWithoutLosingCharacterOrOtherProgress(){
        for(int rank=0;rank<=PermanentAttribute.MAX_RANK;rank++){
            CampaignProfile original=new CampaignProfile();original.createCharacter(Profession.GUERREIRO,GameDifficulty.HARD);
            original.credit(500);original.buy(PermanentAttribute.VITALITY);original.buy(PermanentAttribute.STRENGTH);
            for(int i=0;i<rank;i++)assertTrue(original.buy(PermanentAttribute.ARMOR));
            original.recordReturn(RunOutcome.DEATH,23);original.finishConversation(CampaignProfile.Event.BRIBE_JOKE);
            original.claimForgeMaterial(RegionDefinition.PROTOTYPE_ID);original.forgeWeapon();
            JsonValue legacy=new JsonReader().parse(original.toJson()),attributes=legacy.require("attributes");
            attributes.remove("ARMOR");attributes.addChild("COLLECTION",new JsonValue((long)rank));
            CampaignProfile migrated=CampaignProfile.fromJson(legacy.toJson(JsonWriter.OutputType.json));
            assertEquals(original.characterId(),migrated.characterId());assertEquals(original.obols(),migrated.obols());
            assertEquals(Profession.GUERREIRO,migrated.profession());assertEquals(GameDifficulty.HARD,migrated.difficulty());
            assertEquals(original.attempts(),migrated.attempts());assertEquals(original.deaths(),migrated.deaths());
            assertTrue(migrated.has(CampaignProfile.Event.BRIBE_JOKE));assertEquals(WeaponRarity.RARE,migrated.weaponRarity());
            assertFalse(migrated.canClaimForgeMaterial(RegionDefinition.PROTOTYPE_ID));
            assertEquals(1,migrated.rank(PermanentAttribute.VITALITY));assertEquals(1,migrated.rank(PermanentAttribute.STRENGTH));
            assertEquals(rank,migrated.bonuses().armor());
            JsonValue savedAttributes=new JsonReader().parse(migrated.toJson()).require("attributes");
            assertFalse(savedAttributes.has("COLLECTION"));assertEquals(rank,savedAttributes.getInt("ARMOR"));
            CampaignProfile reloaded=CampaignProfile.fromJson(migrated.toJson());assertEquals(migrated.toJson(),reloaded.toJson());
            PlayerState player=new PlayerState(migrated.profession());reloaded.bonuses().apply(player);
            assertEquals(rank*.03f,player.damageReduction(),.0001f);assertEquals(36,player.getRaioAtracaoXp());
        }
    }
    @Test void explicitArmorWinsOverTheLegacyAliasWithoutAddingRanksOrRestoringCollection(){
        for(int rank:new int[]{0,2,5}){
            JsonValue json=new JsonReader().parse(new CampaignProfile().toJson()),attributes=json.require("attributes");
            attributes.remove("ARMOR");attributes.addChild("ARMOR",new JsonValue((long)rank));
            attributes.addChild("COLLECTION",new JsonValue(5L));
            CampaignProfile migrated=CampaignProfile.fromJson(json.toJson(JsonWriter.OutputType.json));
            assertEquals(rank,migrated.bonuses().armor());
            assertFalse(new JsonReader().parse(migrated.toJson()).require("attributes").has("COLLECTION"));
        }
    }
    @Test void malformedArmorAndLegacyCollectionAreRejectedEvenWhenTheOtherKeyIsValid(){
        for(String bad:new String[]{"-1","6","1.5","\"1\"","true","null","[]","{}","2147483648"}){
            for(String field:new String[]{"ARMOR","COLLECTION"}){
                JsonValue json=new JsonReader().parse(new CampaignProfile().toJson()),attributes=json.require("attributes");
                attributes.addChild("COLLECTION",new JsonValue(2L));attributes.remove(field);
                attributes.addChild(field,new JsonReader().parse(bad));
                assertThrows(IllegalArgumentException.class,()->CampaignProfile.fromJson(json.toJson(JsonWriter.OutputType.json)),field+"="+bad);
            }
            JsonValue legacy=new JsonReader().parse(new CampaignProfile().toJson()),attributes=legacy.require("attributes");
            attributes.remove("ARMOR");attributes.addChild("COLLECTION",new JsonReader().parse(bad));
            assertThrows(IllegalArgumentException.class,()->CampaignProfile.fromJson(legacy.toJson(JsonWriter.OutputType.json)),"legacy="+bad);
        }
    }
    @Test void armorReducesContactProjectilesAndAreaAttacksByTheSamePercentageForEveryClass(){
        for(Profession profession:Profession.values())for(CombatHit.Kind kind:CombatHit.Kind.values()){
            PlayerState p=new PlayerState(profession);new PermanentBonuses(0,0,0,0,0,5).apply(p);
            float health=p.getVida();assertFalse(p.receive(new CombatHit(40,10,0,kind),0,0));
            assertEquals(health-34,p.getVida(),.0001f,profession+" / "+kind);
        }
    }
    @Test void armorCombinesWithResistanceWithinItsCapAndAmparoMultipliesTheRemainingDamage(){
        PlayerState p=new PlayerState();new PermanentBonuses(0,0,0,0,0,5).apply(p);p.aumentarResistencia(.2f);
        assertEquals(.35f,p.damageReduction(),.0001f);p.sofrerDano(20);assertEquals(87,p.getVida(),.0001f);
        p.aumentarResistencia(10);assertEquals(.75f,p.damageReduction(),.0001f);
        Upgrade.AMPARO.apply(p);p.sofrerDano(20);assertEquals(82,p.getVida(),.0001f);
        p.sofrerDano(20);assertEquals(78.5f,p.getVida(),.0001f);
        p.advance(2);p.sofrerDano(20);assertEquals(73.5f,p.getVida(),.0001f);
    }
    @Test void armorPreservesShieldBlockingAndTemporaryInvulnerability(){
        PlayerState p=new PlayerState(Profession.GUERREIRO);new PermanentBonuses(0,0,0,0,0,5).apply(p);
        p.setGuard(true,1,0);assertTrue(p.receive(new CombatHit(40,10,0,CombatHit.Kind.PROJECTILE),0,0));
        assertTrue(p.receive(new CombatHit(40,10,0,CombatHit.Kind.CONTACT),0,0));assertEquals(150,p.getVida());
        assertFalse(p.receive(new CombatHit(40,10,0,CombatHit.Kind.GROUND),0,0));assertEquals(116,p.getVida(),.0001f);
        assertFalse(p.receive(new CombatHit(40,-10,0,CombatHit.Kind.CONTACT),0,0));assertEquals(82,p.getVida(),.0001f);
        p.ativarHabilidadeEgide();p.advance(0);p.sofrerDano(40);assertEquals(82,p.getVida(),.0001f);
        PlayerState resurrected=new PlayerState();new PermanentBonuses(0,0,0,0,0,5).apply(resurrected);
        resurrected.ganharRessurreicao();resurrected.sofrerDano(1000);assertTrue(resurrected.estaMorto());
        resurrected.usarRessurreicao();resurrected.sofrerDano(40);assertEquals(50,resurrected.getVida());
        resurrected.advance(2);resurrected.sofrerDano(40);assertEquals(16,resurrected.getVida(),.0001f);
    }
    @Test void maximumPurchasesApplyToCapacityRegenArmorAndChestClampsWithoutChangingXpCollection(){
        PlayerState p=new PlayerState(Profession.GUERREIRO);new PermanentBonuses(0,0,0,5,5,5).apply(p);
        assertEquals(125,p.getEstaminaMaxima(),.001f);assertEquals(125,p.getEstaminaAtual(),.001f);
        assertEquals(14,p.staminaRegeneration(),.001f);assertEquals(36,p.getRaioAtracaoXp(),.001f);
        assertEquals(.15f,p.damageReduction(),.0001f);
        p.setGuard(true,1,0);p.advance(1);assertEquals(117,p.getEstaminaAtual(),.001f);
        p.setGuard(false,1,0);p.advance(.5f);assertEquals(124,p.getEstaminaAtual(),.001f);
        p.recuperarEstamina(1000);assertEquals(125,p.getEstaminaAtual(),.001f);
        p.aumentarAtracaoXp();assertEquals(68,p.getRaioAtracaoXp(),.001f);
    }
    @Test void purchasesCommitBeforeApplyingAndAnExistingRunKeepsItsSnapshot()throws Exception{
        ProfileStore memory=ProfileStore.memory();CampaignProfile p=new CampaignProfile();p.credit(100);memory.save(p);
        ProfileService service=new ProfileService(memory);
        RunSession active=RunSession.campaign(CampaignProfileTest.region(),Profession.CACADOR,service.profile().bonuses());
        assertTrue(service.buy(PermanentAttribute.ENDURANCE));assertTrue(service.buy(PermanentAttribute.RECOVERY));
        assertTrue(service.buy(PermanentAttribute.ARMOR));
        assertEquals(100,active.player().getEstaminaMaxima());assertEquals(0,active.player().damageReduction());
        ProfileService loaded=new ProfileService(memory);
        RunSession next=RunSession.campaign(CampaignProfileTest.region(),Profession.CACADOR,loaded.profile().bonuses());
        assertEquals(105,next.player().getEstaminaMaxima(),.001f);assertEquals(10.8f,next.player().staminaRegeneration(),.001f);
        assertEquals(36,next.player().getRaioAtracaoXp());assertEquals(.03f,next.player().damageReduction(),.0001f);
        next.applyUpgrade(Upgrade.RASTRO);next.applyUpgrade(Upgrade.EMBOSCADA);next.player().aumentarAtracaoXp();
        next.player().aumentarResistencia(.2f);next.player().tryDash();next.end();
        RunSession restarted=next.newAttempt();assertEquals(105,restarted.player().getEstaminaAtual(),.001f);
        assertEquals(36,restarted.player().getRaioAtracaoXp());assertEquals(.03f,restarted.player().damageReduction(),.0001f);
        assertFalse(restarted.player().boons().ambushReady());assertEquals(0,restarted.rank(Upgrade.RASTRO));
        ProfileService failure=new ProfileService(new ProfileStore(){
            public CampaignProfile load(){return loaded.profile().copy();}
            public void save(CampaignProfile value)throws IOException{throw new IOException("test");}
        });
        int balance=failure.profile().obols();assertFalse(failure.buy(PermanentAttribute.ARMOR));
        assertEquals(balance,failure.profile().obols());assertEquals(1,failure.profile().bonuses().armor());
        assertEquals(1,memory.load().bonuses().armor());
    }
    @Test void previewShowsTheClassRelativeNextValueAndMaximumWithoutMutatingPlayer(){
        PlayerState p=new PlayerState(Profession.GUERREIRO);p.forgeWeapon();
        assertTrue(PermanentAttribute.ENDURANCE.preview(p,0).contains("100 para 105"));
        assertTrue(PermanentAttribute.ARMOR.preview(p,0).contains("0% para 3%"));
        assertTrue(PermanentAttribute.RECOVERY.preview(p,0).contains("10 para 10,8")
            ||PermanentAttribute.RECOVERY.preview(p,0).contains("10 para 10.8"));
        assertTrue(PermanentAttribute.STRENGTH.preview(p,5).contains("máximo"));
        assertEquals(100,p.getEstaminaMaxima());assertEquals(36,p.getRaioAtracaoXp());assertEquals(0,p.damageReduction());
        new PermanentBonuses(0,0,0,0,0,5).apply(p);
        assertTrue(PermanentAttribute.ARMOR.preview(p,5).contains("15%"));
        assertTrue(PermanentAttribute.ARMOR.preview(p,5).contains("máximo"));
        assertEquals(.15f,p.damageReduction(),.0001f);
    }
}
