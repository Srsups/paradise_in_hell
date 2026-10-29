package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import java.util.Random;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlayerCheckpointTest {
    private static String json(JsonValue value){return value.prettyPrint(JsonWriter.OutputType.json,1000);}
    private static PlayerState reload(PlayerState player){return PlayerState.fromCheckpointState(new JsonReader().parse(json(player.checkpointState())));}
    private static void same(PlayerState a,PlayerState b){assertEquals(json(a.checkpointState()),json(b.checkpointState()));}
    private static void invalid(PlayerState player,Consumer<JsonValue> change) {
        JsonValue state=new JsonReader().parse(json(player.checkpointState()));change.accept(state);
        assertThrows(IllegalArgumentException.class,()->PlayerState.fromCheckpointState(state));
    }

    @Test void allProfessionsRoundTripWithoutSharingTheMutablePlayerOrItsBoonState() {
        for(Profession profession:Profession.values()) {
            PlayerState player=new PlayerState(profession),restored=reload(player);
            same(player,restored);assertEquals(profession,restored.getProfession());
            assertEquals(profession.weapon,restored.getWeapon());assertEquals(profession.baseHealth,restored.getVida());
            assertEquals(profession.baseSpeed,restored.normalWalkingSpeed());
            assertTrue(restored.checkpointState().get("walkingSpeedLimit").isNull());
            restored.sofrerDano(5);restored.boons().learn(Upgrade.FUROR);
            assertEquals(profession.baseHealth,player.getVida());assertEquals(0,player.boons().rank(Upgrade.FUROR));
        }
    }

    @Test void progressionAndClassPercentageBonusesAreRestoredWithoutHealingOrApplyingUpgradesAgain() {
        for(Profession profession:Profession.values()) {
            PlayerState player=new PlayerState(profession);new PermanentBonuses(2,3,4,2,3,1).apply(player);
            for(Upgrade boon:new Upgrade[]{Upgrade.FORCA,Upgrade.CADENCIA,Upgrade.VIDA,Upgrade.VELOCIDADE,Upgrade.RESISTENCIA,
                Upgrade.INTELIGENCIA,Upgrade.MAGNETISMO,Upgrade.VAMPIRISMO,Upgrade.POSEIDON,Upgrade.CHOQUE,Upgrade.QUEIMADURA,Upgrade.IMPACTO})boon.apply(player);
            player.forgeWeapon();player.ganharXP(31);player.subirDeNivel();player.subirDeNivel();
            player.sofrerDano(37);player.tryShoot();player.tryShock();player.tryBurn();player.tryImpact();player.trySpell();
            player.setWalkingSpeedLimit(18);player.advance(.11f);
            PlayerState restored=reload(player);same(player,restored);
            assertEquals(player.getDano(),restored.getDano());assertEquals(player.getVida(),restored.getVida());
            assertEquals(player.getXpAtual(),restored.getXpAtual());assertEquals(player.getEstaminaMaxima(),restored.getEstaminaMaxima());
            assertEquals(player.damageRevision(),restored.damageRevision());assertEquals(18,restored.movementSpeed());
            assertEquals(player.tryShoot(),restored.tryShoot());assertEquals(player.tryShock(),restored.tryShock());
            player.advance(.7f);restored.advance(.7f);same(player,restored);
        }
    }

    @Test void anAlreadyConsumedResurrectionDoesNotReturnAndItsProtectionContinuesForOnlyTheRemainingTime() {
        PlayerState player=new PlayerState();player.ganharRessurreicao();player.sofrerDano(1000);player.usarRessurreicao();player.advance(.5f);
        PlayerState restored=reload(player);assertFalse(restored.possuiRessurreicao());assertEquals(50,restored.getVida());same(player,restored);
        restored.sofrerDano(20);assertEquals(50,restored.getVida());restored.advance(1.5f);restored.sofrerDano(20);
        assertEquals(30,restored.getVida());assertThrows(IllegalStateException.class,restored::usarRessurreicao);
    }

    @Test void farmerKeepsTheActiveToolAndTheLongerSharedCooldownWhenSwitchingAndForging() {
        PlayerState player=new PlayerState(Profession.FAZENDEIRO);assertTrue(player.switchTool());
        assertEquals(WeaponType.SCYTHE,reload(player).getWeapon());assertTrue(player.tryShoot());
        player.switchTool();player.forgeWeapon();PlayerState restored=reload(player);
        assertEquals(WeaponType.RAKE,restored.getWeapon());assertEquals(WeaponRarity.RARE,restored.getWeaponRarity());
        assertFalse(restored.tryShoot());player.advance(.84f);restored.advance(.84f);same(player,restored);assertFalse(restored.tryShoot());
        player.advance(.02f);restored.advance(.02f);assertTrue(player.tryShoot());assertTrue(restored.tryShoot());same(player,restored);
    }

    @Test void dashAndTemporaryBoonWindowsKeepTheirRemainingDurationAndOneShotBenefits() {
        PlayerState player=new PlayerState();
        for(Upgrade boon:new Upgrade[]{Upgrade.AMPARO,Upgrade.FOLEGO,Upgrade.RASTRO,Upgrade.EMBOSCADA,Upgrade.ECO_DO_ESTIGE,Upgrade.CONVERGENCIA})boon.apply(player);
        player.sofrerDano(20);player.tryDash();player.onKill();assertTrue(player.boons().tryConvergence());player.advance(.06f);
        PlayerState restored=reload(player);same(player,restored);
        assertTrue(restored.isDashing());assertTrue(restored.boons().armorActive());assertTrue(restored.boons().collectionActive());
        assertFalse(restored.canDash());assertFalse(restored.boons().tryConvergence());
        float echo=player.boons().takeEcho();assertTrue(echo>0);assertEquals(echo,restored.boons().takeEcho());assertEquals(0,restored.boons().takeEcho());
        float ambush=player.boons().beginAttack().take();assertTrue(ambush>0);assertEquals(ambush,restored.boons().beginAttack().take());
        assertEquals(0,restored.boons().beginAttack().take());same(player,restored);
        player.advance(1.6f);restored.advance(1.6f);same(player,restored);
        assertFalse(restored.boons().collectionActive());assertFalse(restored.isDashing());
    }

    @Test void shieldDirectionStaminaAndRetaliationCooldownSurviveReload() {
        PlayerState player=new PlayerState(Profession.GUERREIRO);player.forgeWeapon();player.setGuard(true,2,9);player.tryRetaliation(new Random(1));
        PlayerState restored=reload(player);same(player,restored);assertTrue(restored.guarding());
        CombatHit front=new CombatHit(20,2,9,CombatHit.Kind.PROJECTILE);
        assertTrue(player.receive(front,0,0));assertTrue(restored.receive(front,0,0));same(player,restored);
        assertFalse(restored.tryRetaliation(new Random(0)));assertFalse(player.tryRetaliation(new Random(0)));
        player.advance(.6f);restored.advance(.6f);same(player,restored);
        assertEquals(player.tryRetaliation(new Random(2)),restored.tryRetaliation(new Random(2)));
    }

    @Test void exhaustionAndPeriodicShieldPhaseDoNotResetIntoAFreeDashOrFreshShield() {
        PlayerState player=new PlayerState(Profession.GUERREIRO);player.setGuard(true,1,0);
        for(int hit=0;hit<8;hit++)assertTrue(player.receive(new CombatHit(1,1,0,CombatHit.Kind.PROJECTILE),0,0));
        player.advance(.01f);
        assertTrue(player.isExausto());assertFalse(player.canDash());PlayerState restored=reload(player);same(player,restored);
        player.advance(.3f);restored.advance(.3f);same(player,restored);assertTrue(restored.isExausto());
        player.ativarHabilidadeEgide();player.advance(0);assertTrue(player.isEscudoAtivo());player.advance(1.4f);
        restored=reload(player);same(player,restored);assertTrue(restored.isEscudoAtivo());
        player.advance(.7f);restored.advance(.7f);same(player,restored);assertFalse(restored.isEscudoAtivo());
    }

    @Test void schemaRejectsUnknownMissingDuplicateNonFiniteAndInconsistentPlayerFields() {
        PlayerState player=new PlayerState();
        assertThrows(IllegalArgumentException.class,()->PlayerState.fromCheckpointState(null));
        invalid(player,state->state.remove("weapon"));
        invalid(player,state->state.addChild("unknown",new JsonValue(true)));
        invalid(player,state->state.addChild("profession",new JsonValue("CACADOR")));
        invalid(player,state->state.get("weapon").set("SWORD"));
        invalid(player,state->state.get("profession").set("UNKNOWN"));
        invalid(player,state->state.get("vida").set(101d,null));
        invalid(player,state->state.get("vidaMaxima").set(0d,null));
        invalid(player,state->state.get("estaminaAtual").set(101d,null));
        invalid(player,state->state.get("nivel").set(2L,null));
        invalid(player,state->state.get("xpAtual").set(Double.NaN,null));
        invalid(player,state->state.get("modVelocidade").set(Double.POSITIVE_INFINITY,null));
        invalid(player,state->state.get("damageBonus").set((double)Float.MAX_VALUE,null));
        invalid(player,state->state.get("tempoEntreTiros").set(.1d,null));
        invalid(player,state->state.get("guarding").set(true));
        invalid(player,state->state.get("guardX").set(0d,null));
        invalid(player,state->state.get("version").set(2L,null));
        invalid(player,state->state.get("walkingSpeedLimit").set(-1d,null));
    }

    @Test void invalidBoonRestoreIsAtomicAndRejectsPowersWithoutTheirLearnedRank() {
        BoonState boons=new BoonState();boons.learn(Upgrade.AMPARO);boons.afterDamage();
        String before=json(boons.checkpointState());JsonValue bad=new JsonReader().parse(before);bad.get("armorCooldown").set(9d,null);
        assertThrows(IllegalArgumentException.class,()->boons.restoreCheckpointState(bad));assertEquals(before,json(boons.checkpointState()));
        JsonValue missingPower=new BoonState().checkpointState();missingPower.get("pendingEcho").set(10d,null);
        assertThrows(IllegalArgumentException.class,()->boons.restoreCheckpointState(missingPower));assertEquals(before,json(boons.checkpointState()));
        JsonValue duplicate=new JsonReader().parse(before);duplicate.get("ranks").addChild(new JsonReader().parse(json(duplicate.get("ranks").get(0))));
        assertThrows(IllegalArgumentException.class,()->boons.restoreCheckpointState(duplicate));assertEquals(before,json(boons.checkpointState()));
    }
}
