package br.com.srsups.paradiseinhell;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudLayoutTest {
    private static final float EPSILON=.0001f;
    private static void inside(HudLayout.Bounds bounds) {
        assertTrue(bounds.x()>=0&&bounds.y()>=0);
        assertTrue(bounds.right()<=HudLayout.WIDTH&&bounds.top()<=HudLayout.HEIGHT);
    }
    @Test void defaultScalePreservesTheExistingPanelsAndTheirAnchors() {
        assertEquals(HudLayout.PLAYER,HudLayout.player(HudSettings.DEFAULT));
        assertEquals(HudLayout.GOALS,HudLayout.goals(HudSettings.DEFAULT,false));
        assertEquals(HudLayout.TIMER,HudLayout.goals(HudSettings.DEFAULT,true));
        assertEquals(HudLayout.BOSS,HudLayout.boss(HudSettings.DEFAULT));
        assertEquals(HudLayout.WARNING,HudLayout.warning(HudSettings.DEFAULT));
    }
    @Test void everySupportedScaleFitsAndKeepsTheCombatPanelsSeparated() {
        for(int percent=HudSettings.MIN_PERCENT;percent<=HudSettings.MAX_PERCENT;percent++) {
            HudSettings settings=new HudSettings(percent);
            HudLayout.Bounds player=HudLayout.player(settings),boss=HudLayout.boss(settings),warning=HudLayout.warning(settings);
            inside(player);inside(boss);inside(warning);inside(HudLayout.goals(settings,false));inside(HudLayout.goals(settings,true));
            assertFalse(player.overlaps(boss),"Boss/player overlap at "+percent);
            assertFalse(player.overlaps(warning),"Boss warning/player overlap at "+percent);
            assertFalse(player.overlaps(HudLayout.goals(settings,false)),"Goals/player overlap at "+percent);
            assertFalse(boss.overlaps(warning),"Boss warning/bar overlap at "+percent);
            assertEquals(HudLayout.LEFT,player.x(),EPSILON);
            assertEquals(HudLayout.TOP,player.top(),EPSILON);
            assertEquals(HudLayout.RIGHT,boss.right(),EPSILON);
            assertEquals(HudLayout.TOP,boss.top(),EPSILON);
        }
    }
    @Test void largestScaleLeavesAUsefulGapAndKeepsTheWarningBelowItsBar() {
        HudSettings settings=new HudSettings(HudSettings.MAX_PERCENT);
        HudLayout.Bounds player=HudLayout.player(settings),boss=HudLayout.boss(settings),warning=HudLayout.warning(settings);
        assertEquals(312.5f,player.width(),EPSILON);
        assertEquals(527.5f,boss.width(),EPSILON);
        assertEquals(92f,boss.x()-player.right(),EPSILON);
        assertEquals(12.5f,boss.y()-warning.top(),EPSILON);
        assertEquals(player.y(),warning.y(),EPSILON);
    }
}
