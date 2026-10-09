package br.com.srsups.paradiseinhell;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class XpAttractionTest {
    @Test void magnetReachesPreviouslyDistantCrystalsAndPullsWithoutOvershooting() {
        PlayerState player = new PlayerState();
        XpAttraction.Motion normal = XpAttraction.pull(0, 0, 60, 0, 0.1f, player.getRaioAtracaoXp(), player.getVelocidadeAtracaoXp());
        assertEquals(60, normal.x()); assertFalse(normal.collected());
        Upgrade.MAGNETISMO.apply(player);
        XpAttraction.Motion pulled = XpAttraction.pull(0, 0, 60, 0, 0.1f, player.getRaioAtracaoXp(), player.getVelocidadeAtracaoXp());
        assertEquals(44, pulled.x(), 0.0001f);
        XpAttraction.Motion collected = XpAttraction.pull(0, 0, pulled.x(), pulled.y(), 1, player.getRaioAtracaoXp(), player.getVelocidadeAtracaoXp());
        assertEquals(0, collected.x(), 0.0001f); assertTrue(collected.collected());
    }
    @Test void magnetHasThreeRanksAndResetsWithTemporaryRunPowers() {
        RunSession session = RunSession.demo();
        for (int i = 0; i < 3; i++) session.applyUpgrade(Upgrade.MAGNETISMO);
        assertEquals(132, session.player().getRaioAtracaoXp());
        assertEquals(240, session.player().getVelocidadeAtracaoXp());
        assertThrows(IllegalStateException.class, () -> session.applyUpgrade(Upgrade.MAGNETISMO));
        assertEquals(36, session.newAttempt().player().getRaioAtracaoXp());
        assertFalse(session.newAttempt().player().possuiImaXp());
    }
}
