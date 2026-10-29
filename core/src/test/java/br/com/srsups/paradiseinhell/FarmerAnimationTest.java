package br.com.srsups.paradiseinhell;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FarmerAnimationTest {
    @Test void aToolChangeFinishesTheOriginalAttackPoseWithoutResettingCombatCooldown() {
        PlayerState player=new PlayerState(Profession.FAZENDEIRO);
        PlayerAnimation animation=new PlayerAnimation(player.getProfession());
        ActorAnimation motion=new ActorAnimation();
        assertTrue(player.tryShoot());
        animation.attack(player.attacksPerSecond(),player.getWeapon());
        player.switchTool();
        animation.select(motion,ActorAnimation.Direction.DOWN_LEFT,false,player.getWeapon());
        assertEquals("farmer-rake-attack",animation.current().clip());
        assertFalse(player.tryShoot());
        float duration=animation.attackDuration()+.01f;
        animation.advance(duration);player.advance(duration);
        animation.select(motion,ActorAnimation.Direction.DOWN_LEFT,false,player.getWeapon());
        assertEquals("farmer-scythe-idle",animation.current().clip());
        assertFalse(player.tryShoot(),"Finishing a cosmetic pose cannot grant a free attack");
        player.advance(.71f-duration);
        assertTrue(player.tryShoot());
        animation.attack(player.attacksPerSecond(),player.getWeapon());
        player.switchTool();animation.select(motion,ActorAnimation.Direction.UP_LEFT,false,player.getWeapon());
        assertEquals("farmer-scythe-attack",animation.current().clip());
    }
}
