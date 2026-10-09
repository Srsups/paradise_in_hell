package br.com.srsups.paradiseinhell;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProfessionBalanceTest {
    @Test void everyClassGetsTheSameRelativeDamageAndAttackRateFromBoonAndPermanentRanks() {
        for (Profession profession : Profession.values()) {
            PlayerState player = new PlayerState(profession);
            float baseDamage = player.getDano(), baseDps = baseDamage / player.getAttackInterval(), baseSpeed = player.movementSpeed();
            Upgrade.FORCA.apply(player); Upgrade.CADENCIA.apply(player); Upgrade.VELOCIDADE.apply(player);
            assertEquals(1.2f, player.getDano() / baseDamage, 0.0001f);
            assertEquals(1.344f, player.getDano() / player.getAttackInterval() / baseDps, 0.0001f);
            assertEquals(baseSpeed * 1.1f, player.movementSpeed(), 0.0001f);
            new PermanentBonuses(1, 1, 1).apply(player);
            assertEquals(1.25f, player.getDano() / baseDamage, 0.0001f);
            assertEquals(baseSpeed * 1.13f, player.movementSpeed(), 0.0001f);
            assertEquals(profession.baseHealth * 1.1f, player.getVidaMaxima(), 0.0001f);
        }
    }
    @Test void lowDamageWeaponsKeepFractionalBonusesAndRepeatedRanksUseTheOriginalBase() {
        PlayerState pistol = new PlayerState(Profession.VIGILANTE);
        Upgrade.FORCA.apply(pistol); assertEquals(2.4f, pistol.getDano(), 0.0001f);
        Upgrade.FORCA.apply(pistol); assertEquals(2.8f, pistol.getDano(), 0.0001f);
        Upgrade.VIDA.apply(pistol); assertEquals(102, pistol.getVidaMaxima());
        Upgrade.VIDA.apply(pistol); assertEquals(119, pistol.getVidaMaxima());
        assertEquals(130, new PlayerState(Profession.FERREIRO).getVidaMaxima());
        assertTrue(new PlayerState(Profession.VIGILANTE).movementSpeed() > new PlayerState(Profession.FERREIRO).movementSpeed());
    }
}
