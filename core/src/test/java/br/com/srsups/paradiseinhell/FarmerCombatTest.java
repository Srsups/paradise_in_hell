package br.com.srsups.paradiseinhell;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FarmerCombatTest {
    @Test void farmerStartsWithItsOwnStatsAndRake() {
        PlayerState player = new PlayerState(Profession.FAZENDEIRO);
        assertEquals(WeaponType.RAKE, player.getWeapon());
        assertEquals(120, player.getVidaMaxima());
        assertEquals(57, player.normalWalkingSpeed());
        assertEquals(1, player.getLuckMultiplier());
        assertEquals(9, player.getDano());
        assertEquals(.70f, player.getAttackInterval());
        assertEquals(52, player.meleeReach());
        assertEquals(9 / .70f, player.baseDps());
        assertEquals(1, player.powerRatio(), .0001f);
        player.tryDash();
        assertEquals(195, player.movementSpeed());
    }

    @Test void toolChangeKeepsTheCharacterAndUsesEachToolsOwnDamageCadenceAndReach() {
        PlayerState player = new PlayerState(Profession.FAZENDEIRO);
        player.sofrerDano(15);
        assertTrue(player.switchTool());
        assertEquals(Profession.FAZENDEIRO, player.getProfession());
        assertEquals(WeaponType.SCYTHE, player.getWeapon());
        assertEquals(105, player.getVida());
        assertEquals(120, player.getVidaMaxima());
        assertEquals(6, player.getDano());
        assertEquals(.85f, player.getAttackInterval());
        assertEquals(34, player.meleeReach());
        assertEquals(6 / .85f, player.baseDps());
        assertEquals(1, player.powerRatio(), .0001f);
        assertTrue(player.switchTool());
        assertEquals(WeaponType.RAKE, player.getWeapon());
        assertEquals(9, player.getDano());
        assertEquals(.70f, player.getAttackInterval());
    }

    @Test void otherProfessionsCannotChangeTheirWeapon() {
        for (Profession profession : Profession.values()) {
            if (profession == Profession.FAZENDEIRO) continue;
            PlayerState player = new PlayerState(profession);
            assertFalse(player.switchTool(), profession.name());
            assertEquals(profession.weapon, player.getWeapon());
        }
    }

    @Test void changingOrSpammingToolsNeverResetsTheSharedAttackCooldown() {
        PlayerState player = new PlayerState(Profession.FAZENDEIRO);
        assertTrue(player.tryShoot());
        player.advance(.35f);
        for (int i = 0; i < 21; i++) {
            assertTrue(player.switchTool());
            assertFalse(player.tryShoot());
        }
        assertEquals(WeaponType.SCYTHE, player.getWeapon());
        player.advance(.351f);
        assertTrue(player.tryShoot());
        player.switchTool();
        player.advance(.70f);
        assertFalse(player.tryShoot(), "The previous scythe swing still owns its 0.85s cooldown");
        player.advance(.151f);
        assertTrue(player.tryShoot());
    }

    @Test void boonsPermanentBonusesAndTheSingleForgeUpgradeApplyToBothTools() {
        PlayerState player = new PlayerState(Profession.FAZENDEIRO);
        Upgrade.FORCA.apply(player);
        Upgrade.CADENCIA.apply(player);
        new PermanentBonuses(1, 1, 1).apply(player);
        player.forgeWeapon();
        float health = player.getVida(), speed = player.normalWalkingSpeed();
        assertEquals(9 * 1.25f * 1.25f, player.getDano(), .0001f);
        assertEquals(.70f / (1.12f * 1.10f), player.getAttackInterval(), .0001f);
        assertEquals(64, player.meleeReach());
        float power = player.powerRatio();
        player.switchTool();
        assertEquals(WeaponRarity.RARE, player.getWeaponRarity());
        assertFalse(player.canForgeWeapon());
        assertEquals(6 * 1.25f * 1.25f, player.getDano(), .0001f);
        assertEquals(.85f / (1.12f * 1.10f), player.getAttackInterval(), .0001f);
        assertEquals(44, player.meleeReach());
        assertEquals(power, player.powerRatio(), .0001f);
        assertEquals(health, player.getVida());
        assertEquals(speed, player.normalWalkingSpeed());
        assertThrows(IllegalStateException.class, player::forgeWeapon);
    }

    @Test void attackRateFloorBelongsToTheActiveToolAndPreviewDoesNotChangeIt() {
        PlayerState player = new PlayerState(Profession.FAZENDEIRO);
        for (int rank = 0; rank < 100; rank++) player.aumentarCadencia();
        assertEquals(.18f, player.getAttackInterval());
        assertEquals(1 / .18f, player.attacksPerSecondAfterCadence());
        player.switchTool();
        assertEquals(.22f, player.getAttackInterval());
        assertEquals(1 / .22f, player.attacksPerSecondAfterCadence());
        assertEquals(1 / .22f, player.attacksPerSecond());
        player.switchTool();
        assertEquals(.18f, player.getAttackInterval());
    }

    @Test void strengthPreviewUsesTheChosenToolInsteadOfTheProfessionsStartingRake() {
        PlayerState player = new PlayerState(Profession.FAZENDEIRO);
        assertTrue(PermanentAttribute.STRENGTH.preview(player, 0).contains(CombatNumbers.format(9 * 1.05f)));
        player.switchTool();
        assertTrue(PermanentAttribute.STRENGTH.preview(player, 0).contains("6.3"));
        assertEquals(6, player.getDano());
    }

    @Test void rakeThrustIsNarrowForwardAndBoundedByItsAdvertisedReach() {
        assertTrue(WeaponAttack.hits(WeaponType.RAKE, 0, 0, 1, 0, 52, 0, 52, 0));
        assertFalse(WeaponAttack.hits(WeaponType.RAKE, 0, 0, 1, 0, 52.1f, 0, 52, 0));
        assertTrue(WeaponAttack.hits(WeaponType.RAKE, 0, 0, 1, 0, 30, 3.9f, 52, 0));
        assertFalse(WeaponAttack.hits(WeaponType.RAKE, 0, 0, 1, 0, 30, 4.1f, 52, 0));
        assertFalse(WeaponAttack.hits(WeaponType.RAKE, 0, 0, 1, 0, -1, 0, 52, 0));
        assertFalse(WeaponAttack.hits(WeaponType.RAKE, 0, 0, 0, 0, 20, 0, 52, 0));
        assertTrue(WeaponAttack.hits(WeaponType.RAKE, 0, 0, 50, 0, 30, 0, 52, 0));
        assertTrue(WeaponAttack.hits(WeaponType.RAKE, 0, 0, 1, 1, 30, 30, 52, 0));
        assertFalse(WeaponAttack.hits(WeaponType.RAKE, 0, 0, 1, 1, -30, 30, 52, 0));
    }

    @Test void largeEnemiesCanBeHitAtTheirNearEdgeWithoutTurningTheRakeIntoACone() {
        assertTrue(WeaponAttack.hits(WeaponType.RAKE, 0, 0, 1, 0, 60, 0, 52, 10));
        assertFalse(WeaponAttack.hits(WeaponType.RAKE, 0, 0, 1, 0, 63, 0, 52, 10));
        assertTrue(WeaponAttack.hits(WeaponType.RAKE, 0, 0, 1, 0, 35, 13, 52, 10));
        assertFalse(WeaponAttack.hits(WeaponType.RAKE, 0, 0, 1, 0, 35, 15, 52, 10));
    }

    @Test void scytheHitsTheWholeCircleAndDoesNotDependOnAimDirection() {
        for (int direction = 0; direction < 8; direction++) {
            double angle = direction * Math.PI / 4;
            assertTrue(WeaponAttack.hits(WeaponType.SCYTHE, 10, 20, 0, 0,
                10 + (float)Math.cos(angle) * 33.9f, 20 + (float)Math.sin(angle) * 33.9f, 34, 0));
            assertFalse(WeaponAttack.hits(WeaponType.SCYTHE, 10, 20, 1, 0,
                10 + (float)Math.cos(angle) * 34.1f, 20 + (float)Math.sin(angle) * 34.1f, 34, 0));
        }
        assertTrue(WeaponAttack.hits(WeaponType.SCYTHE, 0, 0, 1, 0, -40, 0, 34, 6));
        assertFalse(WeaponAttack.hits(WeaponType.PISTOL, 0, 0, 1, 0, 10, 0, 34, 0));
    }

    @Test void bothToolsRespectArenaObstaclesIncludingTargetsInsideTheScytheCircle() {
        EnemyNavigation navigation = new EnemyNavigation((x, y) -> x >= 16 && x < 32);
        assertTrue(WeaponAttack.hits(WeaponType.RAKE, 8, 8, 1, 0, 48, 8, 52, 0));
        assertFalse(WeaponAttack.hits(WeaponType.RAKE, 8, 8, 1, 0, 48, 8, 52, 0, navigation::clearLine));
        assertTrue(WeaponAttack.hits(WeaponType.SCYTHE, 8, 8, 1, 0, 40, 8, 34, 0));
        assertFalse(WeaponAttack.hits(WeaponType.SCYTHE, 8, 8, 1, 0, 40, 8, 34, 0, navigation::clearLine));
        assertTrue(WeaponAttack.hits(WeaponType.RAKE, 8, 8, 0, 1, 8, 48, 52, 0, navigation::clearLine));
        assertTrue(WeaponAttack.hits(WeaponType.SCYTHE, 8, 8, 1, 0, -16, 8, 34, 0, navigation::clearLine));
    }

    @Test void sharedDamageEffectsScaleFromTheCurrentlyEquippedTool() {
        for (boolean scythe : List.of(false, true)) {
            PlayerState player = new PlayerState(Profession.FAZENDEIRO);
            if (scythe) player.switchTool();
            Upgrade.QUEIMADURA.apply(player);
            DummyTarget enemy = new DummyTarget();
            CombatEffects effects = new CombatEffects();
            effects.onHit(player, enemy, List.of(enemy), 0, 0, (x, y, ox, oy) -> true, true);
            effects.update(1);
            assertEquals(100 - player.dps() * .35f, enemy.health(), .0001f);
        }
    }

    private static final class DummyTarget implements CombatTarget {
        private float life = 100;
        @Override public float centerX() { return 10; }
        @Override public float centerY() { return 0; }
        @Override public float health() { return life; }
        @Override public float maxHealth() { return 100; }
        @Override public void hurt(float damage) { life -= damage; }
        @Override public boolean dead() { return life <= 0; }
        @Override public float strength() { return 1; }
        @Override public void push(float dx, float dy, CombatEffects.LineOfSight line) {}
    }
}
