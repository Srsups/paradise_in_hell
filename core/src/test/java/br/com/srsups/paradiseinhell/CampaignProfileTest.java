package br.com.srsups.paradiseinhell;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CampaignProfileTest {
    static RegionDefinition region() {
        return new RegionDefinition("r", "R", "fight", List.of(
            new ArenaDefinition("fight", "Fight", "", ArenaDefinition.Kind.COMBAT, 0, Map.of(EnemyType.COMMON, 1), List.of("boss")),
            new ArenaDefinition("boss", "Boss", "", ArenaDefinition.Kind.BOSS_ROOM, 0, Map.of(), List.of())));
    }
    @Test void paidBonusesSurviveDeathWhileDivinePowersXpAndRanksReset() {
        ProfileStore store = ProfileStore.memory();
        ProfileService service = new ProfileService(store);
        service.chooseProfession(Profession.FERREIRO);
        service.finishConversation(CampaignProfile.Event.CARONTE_MET);
        assertTrue(service.buy(PermanentAttribute.VITALITY)); assertEquals(0, service.profile().obols());
        RunSession run = RunSession.campaign(region(), service.profile().profession(), service.profile().bonuses());
        assertEquals(143, run.player().getVidaMaxima());
        run.applyUpgrade(Upgrade.VIDA); run.applyUpgrade(Upgrade.POSEIDON); run.applyUpgrade(Upgrade.RESSURREICAO);
        run.player().ganharXP(25); run.player().subirDeNivel(); run.end();
        service.settle(run, RunOutcome.DEATH);
        CampaignProfile loaded = new ProfileService(store).profile();
        RunSession fresh = RunSession.campaign(region(), loaded.profession(), loaded.bonuses());
        assertEquals(143, fresh.player().getVida()); assertEquals(143, fresh.player().getVidaMaxima());
        assertEquals(1, fresh.player().getNivel()); assertEquals(0, fresh.player().getXpAtual());
        assertFalse(fresh.player().possuiAura()); assertFalse(fresh.player().possuiRessurreicao());
        assertFalse(fresh.player().isEscudoAtivo()); assertEquals(0, fresh.rank(Upgrade.VIDA));
        assertEquals(Profession.FERREIRO, fresh.player().getProfession());
        assertEquals(143, run.newAttempt().player().getVidaMaxima());
    }
    @Test void pricesIncreaseSpendingIsExactAndMaximumRankAndInsufficientFundsDoNotSpend() {
        CampaignProfile profile = new CampaignProfile();
        assertFalse(profile.buy(PermanentAttribute.STRENGTH));
        profile.credit(5);
        assertTrue(profile.buy(PermanentAttribute.STRENGTH));
        assertEquals(0, profile.obols()); assertEquals(1, profile.rank(PermanentAttribute.STRENGTH));
        profile.credit(8); assertFalse(profile.buy(PermanentAttribute.STRENGTH)); assertEquals(8, profile.obols());
        profile.credit(100);
        for (int rank = 1; rank < 5; rank++) assertTrue(profile.buy(PermanentAttribute.STRENGTH));
        int remaining = profile.obols();
        assertFalse(profile.buy(PermanentAttribute.STRENGTH)); assertEquals(remaining, profile.obols());
        assertEquals(5, profile.bonuses().strength());
        PlayerState player = new PlayerState(Profession.VIGILANTE);
        profile.bonuses().apply(player); assertEquals(2.5f, player.getDano(), 0.0001f);
    }
    @Test void returnsBankRewardsOnceAndDoNotRewardAnEmptyAttemptOrTreatAbandonmentAsDeath() {
        ProfileService service = new ProfileService(ProfileStore.memory());
        RunSession run = RunSession.campaign(region());
        assertEquals(0, service.settle(run, RunOutcome.DEATH)); assertEquals(0, service.profile().attempts());
        for (int count = 0; count < 250; count++) run.recordKill(EnemyType.COMMON);
        run.addChestObols(3); run.end();
        assertEquals(13, service.settle(run, RunOutcome.ABANDONED));
        assertEquals(0, service.settle(run, RunOutcome.ABANDONED));
        assertEquals(1, service.profile().attempts()); assertEquals(0, service.profile().deaths());
        RunSession empty = run.newAttempt(); empty.end();
        assertEquals(0, service.settle(empty, RunOutcome.DEATH));
        assertEquals(13, service.profile().obols()); assertEquals(1, service.profile().deaths());
    }
    @Test void bossRewardRequiresDefeatAndCannotBeCollectedTwice() {
        ProfileService service = new ProfileService(ProfileStore.memory());
        RunSession run = RunSession.campaign(region());
        for (int count = 0; count < 10; count++) run.recordKill(EnemyType.COMMON);
        run.enterArena("boss"); run.recordBossDefeat(); run.end();
        assertThrows(IllegalStateException.class, () -> service.settle(run, RunOutcome.DEATH));
        assertEquals(11, service.settle(run, RunOutcome.VICTORY));
        assertEquals(0, service.settle(run, RunOutcome.VICTORY));
        assertEquals(1, service.profile().victories());
    }
    @Test void conversationsRememberDeathAndVictoryAndInitialGiftIsGrantedOnce() {
        ProfileService service = new ProfileService(ProfileStore.memory());
        assertEquals(CampaignProfile.Event.CARONTE_MET, CaronteDialogue.select(service.profile()).event());
        service.finishConversation(CampaignProfile.Event.CARONTE_MET);
        service.finishConversation(CampaignProfile.Event.CARONTE_MET);
        assertEquals(5, service.profile().obols());
        RunSession dead = RunSession.campaign(region()); dead.end(); service.settle(dead, RunOutcome.DEATH);
        assertEquals(CampaignProfile.Event.BRIBE_JOKE, CaronteDialogue.select(service.profile()).event());
        service.finishConversation(CampaignProfile.Event.BRIBE_JOKE);
        RunSession win = RunSession.campaign(region());
        win.recordKill(EnemyType.COMMON); win.enterArena("boss"); win.recordBossDefeat(); win.end();
        service.settle(win, RunOutcome.VICTORY);
        assertEquals(CampaignProfile.Event.GUARDIAN_RECOGNIZED, CaronteDialogue.select(service.profile()).event());
        CampaignProfile loaded = new CampaignProfile();
        loaded = CampaignProfile.fromJson(service.profile().toJson());
        assertTrue(loaded.has(CampaignProfile.Event.BRIBE_JOKE)); assertEquals(1, loaded.deaths());
        assertEquals(1, loaded.victories());
    }
    @Test void malformedProfileDoesNotCreateNegativeMoneyOrUnboundedAttributes() {
        String json = new CampaignProfile().toJson();
        assertThrows(IllegalArgumentException.class, () -> CampaignProfile.fromJson(json.replace("\"version\": 2", "\"version\": 3")));
        assertThrows(IllegalArgumentException.class, () -> CampaignProfile.fromJson(json.replace("\"obols\": 0", "\"obols\": -1")));
        assertThrows(IllegalArgumentException.class, () -> CampaignProfile.fromJson(json.replace("\"VITALITY\": 0", "\"VITALITY\": 999")));
        assertThrows(IllegalArgumentException.class, () -> new PermanentBonuses(0, 0, 6));
    }
}
