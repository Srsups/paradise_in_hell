package br.com.srsups.paradiseinhell;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WeaponAttackTest {
    @Test void swordHitsOnlyNearbyEnemiesInAimDirection() {
        assertTrue(WeaponAttack.swordHits(0, 0, 1, 0, 25, 10));
        assertFalse(WeaponAttack.swordHits(0, 0, 1, 0, -25, 0));
        assertFalse(WeaponAttack.swordHits(0, 0, 1, 0, 0, 25));
        assertFalse(WeaponAttack.swordHits(0, 0, 1, 0, 40, 0));
    }
    @Test void professionsUseDistinctWeaponsAndPistolFiresMoreOften() {
        PlayerState bow = new PlayerState(Profession.CACADOR), sword = new PlayerState(Profession.FERREIRO), pistol = new PlayerState(Profession.VIGILANTE);
        assertEquals(WeaponType.BOW, bow.getWeapon()); assertEquals(WeaponType.SWORD, sword.getWeapon());
        assertEquals(WeaponType.PISTOL, pistol.getWeapon());
        assertTrue(sword.getDano() > bow.getDano()); assertTrue(bow.getDano() > pistol.getDano());
        bow.tryShoot(); pistol.tryShoot(); bow.advance(0.3f); pistol.advance(0.3f);
        assertFalse(bow.tryShoot()); assertTrue(pistol.tryShoot());
    }
}
