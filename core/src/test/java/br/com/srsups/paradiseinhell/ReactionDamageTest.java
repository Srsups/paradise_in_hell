package br.com.srsups.paradiseinhell;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReactionDamageTest {
    @Test void realDamageRemainsObservableWhenHealingRestoresHealthInTheSameTick() {
        PlayerState player=new PlayerState();float before=player.getVida();
        long revision=player.damageRevision();player.sofrerDano(5);player.curar(5);
        assertEquals(before,player.getVida());assertEquals(revision+1,player.damageRevision());
    }
    @Test void blockedHitsAndResurrectionProtectionDoNotStartHurtReactions() {
        PlayerState player=new PlayerState(Profession.GUERREIRO);
        player.setGuard(true,1,0);
        assertTrue(player.receive(new CombatHit(10,10,0,CombatHit.Kind.CONTACT,"contato"),0,0));
        assertEquals(0,player.damageRevision());
        player.ganharRessurreicao();player.usarRessurreicao();player.sofrerDano(10);
        assertEquals(0,player.damageRevision());
    }
    @Test void zeroDamageAndHealingNeverCountAsAnImpact() {
        PlayerState player=new PlayerState();player.sofrerDano(0);player.curar(10);
        assertEquals(0,player.damageRevision());player.sofrerDano(4);
        assertEquals(1,player.damageRevision());
    }
}
