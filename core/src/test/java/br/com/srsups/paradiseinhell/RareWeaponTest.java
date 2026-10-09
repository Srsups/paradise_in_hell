package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.math.Vector2;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RareWeaponTest {
    @Test void rarityAndDivinePercentagesRemainProportionalForEveryProfessionAndOrder() {
        for (Profession profession : Profession.values()) {
            PlayerState a = new PlayerState(profession), b = new PlayerState(profession);
            a.forgeWeapon(); Upgrade.FORCA.apply(a); Upgrade.CADENCIA.apply(a);
            Upgrade.FORCA.apply(b); Upgrade.CADENCIA.apply(b); b.forgeWeapon();
            assertEquals(profession.weapon.damage * 1.25f * 1.2f, a.getDano(), 0.0001f);
            assertEquals(a.getDano(), b.getDano()); assertEquals(a.getAttackInterval(), b.getAttackInterval());
            assertEquals(profession.weapon.interval / (1.10f * 1.12f), a.getAttackInterval(), 0.0001f);
            assertThrows(IllegalStateException.class, a::forgeWeapon);
        }
    }
    @Test void piercingAndRicochetCannotHitTheSameTargetTwiceOrExceedTwoTargets() {
        for (Profession profession : new Profession[]{Profession.CACADOR, Profession.VIGILANTE}) {
            PlayerState player = new PlayerState(profession); player.forgeWeapon();
            Projetil shot = new Projetil(0, 0, new Vector2(1, 0), null, player.getWeapon()); shot.configure(player);
            Object first = new Object(), second = new Object();
            shot.hit(first); assertFalse(shot.canHit(first)); assertFalse(shot.deveSerRemovido);
            shot.hit(first); assertFalse(shot.deveSerRemovido);
            assertEquals(profession == Profession.VIGILANTE, shot.needsRicochet());
            shot.redirect(0, 20); assertEquals(0, shot.velocidade.x, 0.001f);
            assertEquals(player.getWeapon().projectileSpeed, shot.velocidade.len(), 0.001f);
            shot.hit(second); assertTrue(shot.deveSerRemovido);
        }
        Projetil common = new Projetil(0, 0, new Vector2(1,0), null); common.hit(new Object()); assertTrue(common.deveSerRemovido);
    }
    @Test void forgedSwordExtendsReachWithoutTurningIntoAnOmnidirectionalAttack() {
        PlayerState sword = new PlayerState(Profession.FERREIRO);
        assertFalse(WeaponAttack.swordHits(0, 0, 1, 0, 45, 0, sword.swordReach()));
        sword.forgeWeapon();
        assertTrue(WeaponAttack.swordHits(0, 0, 1, 0, 45, 0, sword.swordReach()));
        assertFalse(WeaponAttack.swordHits(0, 0, 1, 0, -30, 0, sword.swordReach()));
    }
}
