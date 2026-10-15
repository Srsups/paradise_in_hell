package br.com.srsups.paradiseinhell;

import java.io.IOException;
import java.util.function.Consumer;

/** Purchases commit to disk before spending; failed autosaves retain return rewards in memory. */
public final class ProfileService {
    private final ProfileStore store;
    private CampaignProfile profile;
    private boolean writable = true;
    private String error = "";
    public ProfileService(ProfileStore store) {
        this.store = store;
        try { profile = store.load(); }
        catch (IOException | RuntimeException failure) {
            profile = new CampaignProfile(); writable = false;
            error = "Não foi possível ler o perfil. Progresso apenas nesta sessão; arquivo preservado.";
        }
    }
    public CampaignProfile profile() { return profile; }
    public String error() { return error; }
    public boolean retrySave() {
        if (!writable) return false;
        try { store.save(profile); error = ""; return true; }
        catch (IOException failure) { error = "Não foi possível salvar. Pressione S no refúgio para tentar novamente."; return false; }
    }
    private boolean commit(Consumer<CampaignProfile> action) {
        if (!writable) return false;
        CampaignProfile candidate = profile.copy(); action.accept(candidate);
        try { store.save(candidate); profile = candidate; error = ""; return true; }
        catch (IOException failure) { error = "Não foi possível salvar. A alteração não foi aplicada."; return false; }
    }
    public boolean reset() {
        CampaignProfile empty = new CampaignProfile();
        try { store.reset(empty); profile = empty; writable = true; error = ""; return true; }
        catch (IOException failure) { error = "Não foi possível excluir o personagem. Tente novamente."; return false; }
    }
    public boolean buy(PermanentAttribute attribute) {
        return profile.canBuy(attribute) && commit(candidate -> candidate.buy(attribute));
    }
    public void prepareRun(RunSession session) {
        session.bindCharacter(profile.characterId());
        if (session.isDemo()) return;
        if (!profile.canClaimForgeMaterial(session.region().id())) session.encounters().blockRegionMaterial();
        if (profile.weaponRarity() == WeaponRarity.RARE && session.player().canForgeWeapon()) session.player().forgeWeapon();
    }
    public int collectForgeMaterials(RunSession session) {
        if (session.isDemo()) return 0;
        int amount = session.pendingForgeMaterials();
        if (amount == 0) return 0;
        boolean claimed = profile.claimForgeMaterial(session.region().id());
        session.clearPendingForgeMaterials(); session.encounters().blockRegionMaterial();
        if (claimed) retrySave();
        return claimed ? 1 : 0;
    }
    public boolean forgeWeapon(RunSession session) {
        if (!session.canForgeWeapon() || !profile.canForgeWeapon()) return false;
        if (!commit(CampaignProfile::forgeWeapon)) return false;
        session.player().forgeWeapon(); return true;
    }
    public boolean chooseProfession(Profession profession) { return chooseProfession(profession, profile.necromancerUnlocked()); }
    public boolean chooseProfession(Profession profession, boolean legendaryUnlocked) {
        if (profession == Profession.NECROMANTE && !legendaryUnlocked) return false;
        if (profile.professionChosen()) return false;
        profile.chooseProfession(profession); retrySave(); return true;
    }
    /** Save both choices together, before exposing the new character to the game. */
    public boolean createCharacter(Profession profession, GameDifficulty difficulty, boolean legendaryUnlocked) {
        java.util.Objects.requireNonNull(profession); java.util.Objects.requireNonNull(difficulty);
        if (profile.professionChosen() || profession == Profession.NECROMANTE && !legendaryUnlocked) return false;
        return commit(candidate -> candidate.createCharacter(profession, difficulty));
    }
    public boolean finishConversation(CampaignProfile.Event event) {
        if (event == null || profile.has(event)) return true;
        profile.finishConversation(event); retrySave(); return true;
    }
    public int settle(RunSession session, RunOutcome outcome) {
        if (!session.canSettle()) return 0;
        if ((outcome == RunOutcome.VICTORY) != session.bossDefeated()) throw new IllegalStateException("Invalid campaign result");
        collectForgeMaterials(session);
        int earned = session.earnedObols();
        profile.recordReturn(outcome, earned); session.markSettled();
        retrySave(); return earned;
    }
}
