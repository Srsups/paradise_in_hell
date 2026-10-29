package br.com.srsups.paradiseinhell;

import java.io.IOException;
import java.util.function.Consumer;

/** Purchases commit to disk before spending; failed autosaves retain return rewards in memory. */
public final class ProfileService {
    private final ProfileStore store;
    private CampaignProfile profile;
    private boolean writable = true;
    private boolean settlementDurable=true;
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
    public RunSettlement pendingSettlement(){return profile.pendingSettlement();}
    public boolean settlementDurable(){return settlementDurable;}
    public boolean writable(){return writable;}
    public boolean retrySave() {
        if (!writable) return false;
        try { write(profile,profile.pendingSettlement()!=null);settlementDurable=true; error = ""; return true; }
        catch (IOException failure) { error = "Não foi possível salvar. Pressione S no refúgio para tentar novamente."; return false; }
    }
    private boolean commit(Consumer<CampaignProfile> action) {
        return commit(action,false);
    }
    private boolean commit(Consumer<CampaignProfile> action,boolean terminal) {
        if (!writable) return false;
        CampaignProfile candidate = profile.copy(); action.accept(candidate);
        try { write(candidate,terminal||candidate.pendingSettlement()!=null);profile = candidate;settlementDurable=true; error = ""; return true; }
        catch (IOException failure) { error = "Não foi possível salvar. A alteração não foi aplicada."; return false; }
    }
    private void write(CampaignProfile candidate,boolean terminal)throws IOException {
        if(terminal)store.saveTerminal(candidate);else store.save(candidate);
    }
    public boolean ensureCharacterIdentity(){
        if(!profile.professionChosen())return false;
        return !profile.characterId().isEmpty()||commit(CampaignProfile::ensureCharacterIdentity);
    }
    /** Advances a recovered/legacy identity beyond shared receipts, without crediting their rewards again. */
    public boolean syncAttemptSequence(long highWater){
        if(highWater<0)throw new IllegalArgumentException("Negative completed sequence");
        if(!writable)return false;
        if(profile.checkpoint()!=null||profile.pendingSettlement()!=null){
            error="Conclua ou descarte a tentativa pendente antes de sincronizar seu registro.";return false;
        }
        if(highWater<=profile.lastSettledSequence())return true;
        return commit(candidate->candidate.syncAttemptSequence(highWater),true);
    }
    /** A global receipt is proof that this recovered checkpoint must never be played or paid again. */
    public boolean discardCompletedCheckpoint(long highWater){
        if(highWater<0)throw new IllegalArgumentException("Negative completed sequence");
        if(!writable)return false;
        if(profile.pendingSettlement()!=null){error="Conclua o registro do retorno antes de remover a tentativa.";return false;}
        RunCheckpoint checkpoint=profile.checkpoint();
        if(checkpoint==null)return syncAttemptSequence(highWater);
        if(checkpoint.sequence()>highWater){error="A tentativa atual ainda não foi registrada como concluída.";return false;}
        return commit(candidate->{candidate.discardCheckpoint();candidate.syncAttemptSequence(highWater);},true);
    }
    /** Explicit recovery discards the incompatible attempt and preserves all permanent character facts. */
    public boolean discardIncompatibleCheckpoint(){
        if(!writable)return false;
        if(profile.pendingSettlement()!=null){error="Conclua o registro do retorno antes de remover a tentativa.";return false;}
        if(profile.checkpoint()==null)return true;
        return commit(CampaignProfile::discardCheckpoint,true);
    }
    public boolean saveCheckpoint(RunSession session,RunCheckpoint.Stage stage){
        java.util.Objects.requireNonNull(session);java.util.Objects.requireNonNull(stage);
        if(!writable)return false;
        if(session.isDemo()||session.finished()||profile.characterId().isEmpty()||!currentCharacter(session.characterId())||profile.pendingSettlement()!=null
            ||session.player().getProfession()!=profile.profession()||session.difficulty()!=profile.difficulty()) {
            error="Não foi possível salvar esta tentativa para o personagem atual.";return false;
        }
        RunCheckpoint previous=profile.checkpoint();long sequence=session.attemptSequence(),revision;
        if(sequence==0){
            if(previous!=null){error="Este personagem já tem uma tentativa para retomar.";return false;}
            sequence=Math.addExact(profile.attemptSequence(),1);revision=1;
        }else{
            if(previous==null||sequence!=profile.attemptSequence()||sequence<=profile.lastSettledSequence()
                ||!previous.runId().equals(session.id())) {error="Este checkpoint pertence a uma tentativa antiga.";return false;}
            revision=Math.addExact(previous.revision(),1);
        }
        final RunCheckpoint checkpoint;
        try{checkpoint=RunCheckpoint.capture(session,stage,sequence,revision);}
        catch(RuntimeException invalid){error="A tentativa ainda não está em um ponto seguro para salvar.";return false;}
        long committedSequence=sequence;
        if(!commit(candidate->candidate.checkpoint(checkpoint)))return false;
        session.bindAttemptSequence(committedSequence);return true;
    }
    public boolean acknowledgeSettlement(RunSettlement receipt){
        java.util.Objects.requireNonNull(receipt);
        if(!settlementDurable){error="Salve o retorno antes de concluir seus registros compartilhados.";return false;}
        if(profile.pendingSettlement()==null)return profile.characterId().equals(receipt.report().characterId())
            &&receipt.sequence()<=profile.lastSettledSequence();
        CampaignProfile candidate=profile.copy();
        if(!candidate.acknowledgeSettlement(receipt)){error="O retorno pertence a outra tentativa ou personagem.";return false;}
        if(!writable)return false;
        try{store.saveTerminal(candidate);profile=candidate;error="";return true;}
        catch(IOException failure){error="O retorno foi guardado; falta concluir o registro. Pressione S no refúgio.";return false;}
    }
    public boolean reset() {
        CampaignProfile empty = new CampaignProfile();
        try { store.reset(empty); profile = empty; writable = true;settlementDurable=true; error = ""; return true; }
        catch (IOException failure) { error = "Não foi possível excluir o personagem. Tente novamente."; return false; }
    }
    public boolean buy(PermanentAttribute attribute) {
        return profile.canBuy(attribute) && commit(candidate -> candidate.buy(attribute));
    }
    public void prepareRun(RunSession session) {
        session.bindCharacter(profile.characterId());
        if (session.isDemo()) return;
        session.prepareFarmerQuest(profile.farmerQuest());
        if (!profile.canClaimForgeMaterial(session.region().id())) session.encounters().blockRegionMaterial();
        if (profile.weaponRarity() == WeaponRarity.RARE && session.player().canForgeWeapon()) session.player().forgeWeapon();
    }
    public int collectForgeMaterials(RunSession session) {
        if (session.isDemo()) return 0;
        if(session.attemptSequence()>0&&(!currentCharacter(session.characterId())||session.attemptSequence()<=profile.lastSettledSequence()))return 0;
        int amount = session.pendingForgeMaterials();
        if (amount == 0) return 0;
        boolean claimed = profile.claimForgeMaterial(session.region().id());
        session.clearPendingForgeMaterials(); session.encounters().blockRegionMaterial();
        if (claimed) retrySave();
        return claimed ? 1 : 0;
    }
    public boolean forgeWeapon(RunSession session) {
        if (!session.canForgeWeapon() || !profile.canForgeWeapon()) return false;
        RunCheckpoint updated=updatedSafeCheckpoint(session,RunCheckpoint.Stage.BLACKSMITH,run->run.player().forgeWeapon());
        if (!commit(candidate->{candidate.forgeWeapon();if(updated!=null)candidate.checkpoint(updated);})) return false;
        session.player().forgeWeapon(); return true;
    }
    public boolean acceptFarmerQuest(RunSession session) {
        if (!session.questEnabled() || session.finished() || !session.arenaComplete() || !session.encounters().farmerOpen()
            || !session.characterId().equals(profile.characterId()) || profile.farmerQuest() != FarmerQuest.NOT_STARTED) return false;
        RunCheckpoint updated=updatedSafeCheckpoint(session,RunCheckpoint.Stage.FARMER,run->run.prepareFarmerQuest(FarmerQuest.ACCEPTED));
        if (!commit(candidate->{candidate.acceptFarmerQuest();if(updated!=null)candidate.checkpoint(updated);})) return false;
        session.prepareFarmerQuest(profile.farmerQuest());
        return true;
    }
    private RunCheckpoint updatedSafeCheckpoint(RunSession session,RunCheckpoint.Stage stage,Consumer<RunSession> change){
        RunCheckpoint old=profile.checkpoint();
        if(session.attemptSequence()==0||old==null)return null;
        if(!currentCharacter(session.characterId())||!old.runId().equals(session.id()))throw new IllegalStateException("Foreign attempt");
        long revision=Math.addExact(old.revision(),1);
        RunSession candidate=RunCheckpoint.capture(session,stage,old.sequence(),revision).restore(session.region());
        change.accept(candidate);
        return RunCheckpoint.capture(candidate,stage,old.sequence(),revision);
    }
    /** Earned objectives remain in memory on autosave failure, just like run rewards. */
    public void collectFarmerQuest(RunSession session) {
        if (!session.questEnabled() || !session.characterId().equals(profile.characterId())
            || profile.farmerQuest() == FarmerQuest.NOT_STARTED || session.farmerQuest().ordinal() <= profile.farmerQuest().ordinal()) return;
        profile.advanceFarmerQuest(session.farmerQuest());
        retrySave();
    }
    public boolean chooseProfession(Profession profession) { return chooseProfession(profession, profile.necromancerUnlocked()); }
    public boolean chooseProfession(Profession profession, boolean legendaryUnlocked) {
        return chooseProfession(profession, legendaryUnlocked, false);
    }
    public boolean chooseProfession(Profession profession, boolean legendaryUnlocked, boolean epicUnlocked) {
        if (profession == Profession.NECROMANTE && !legendaryUnlocked) return false;
        if (profession == Profession.FAZENDEIRO && !epicUnlocked) return false;
        if (profile.professionChosen()) return false;
        profile.chooseProfession(profession); retrySave(); return true;
    }
    /** Save both choices together, before exposing the new character to the game. */
    public boolean createCharacter(Profession profession, GameDifficulty difficulty, boolean legendaryUnlocked) {
        return createCharacter(profession, difficulty, legendaryUnlocked, false);
    }
    public boolean createCharacter(Profession profession, GameDifficulty difficulty, boolean legendaryUnlocked, boolean epicUnlocked) {
        java.util.Objects.requireNonNull(profession); java.util.Objects.requireNonNull(difficulty);
        if (profile.professionChosen() || profession == Profession.NECROMANTE && !legendaryUnlocked) return false;
        if (profession == Profession.FAZENDEIRO && !epicUnlocked) return false;
        return commit(candidate -> candidate.createCharacter(profession, difficulty));
    }
    public boolean finishConversation(CampaignProfile.Event event) {
        if (event == null || profile.has(event)) return true;
        profile.finishConversation(event); retrySave(); return true;
    }
    private boolean currentCharacter(String characterId) {
        return profile.professionChosen() && profile.characterId().equals(characterId);
    }
    /** Completed reading stays in memory if autosave fails; retrySave makes it durable later. */
    public boolean completeTutorial(String characterId,TutorialTopic topic) {
        if(!currentCharacter(characterId)||topic==null)return false;
        if(profile.tutorialSeen(topic))return true;
        profile.completeTutorial(topic);return retrySave();
    }
    public boolean setTutorialEnabled(String characterId,boolean enabled) {
        if(!currentCharacter(characterId))return false;
        if(profile.tutorialEnabled()==enabled)return true;
        profile.setTutorialEnabled(enabled);return retrySave();
    }
    public boolean finishUnderworldEpilogue(String characterId) {
        if(!currentCharacter(characterId)||!profile.has(CampaignProfile.Event.INSURGENCE_SPARK_OBTAINED))return false;
        if(profile.has(CampaignProfile.Event.UNDERWORLD_EPILOGUE_READ))return true;
        profile.finishConversation(CampaignProfile.Event.UNDERWORLD_EPILOGUE_READ);return retrySave();
    }
    /** Cinematic acknowledgement never settles a run or grants a narrative/combat reward. */
    public boolean markOpening(String characterId,boolean skipped) {
        if(!currentCharacter(characterId))return false;
        if(!profile.markOpening(skipped))return true;
        return retrySave();
    }
    public int settle(RunSession session, RunOutcome outcome) {
        return settle(session,outcome,0);
    }
    public int settle(RunSession session,RunOutcome outcome,int slot) {
        if(session.attemptSequence()>0)return settleCheckpointAttempt(session,outcome,slot);
        if (!session.canSettle()) return 0;
        if ((outcome == RunOutcome.VICTORY) != session.bossDefeated()) throw new IllegalStateException("Invalid campaign result");
        collectFarmerQuest(session);
        collectForgeMaterials(session);
        int earned = session.earnedObols();
        if(outcome==RunOutcome.VICTORY && !session.isDemo() && session.bossDefeated()
            && session.region().id().equals(RegionDefinition.PROTOTYPE_ID) && currentCharacter(session.characterId()))
            profile.finishConversation(CampaignProfile.Event.INSURGENCE_SPARK_OBTAINED);
        profile.recordReturn(outcome, earned); session.markSettled();
        retrySave(); return earned;
    }
    private int settleCheckpointAttempt(RunSession session,RunOutcome outcome,int slot){
        if(!session.canSettle())return 0;
        long sequence=session.attemptSequence();
        if(!currentCharacter(session.characterId())||sequence!=profile.attemptSequence()) {
            error="O retorno pertence a outro personagem ou a uma tentativa antiga.";return 0;
        }
        if(sequence<=profile.lastSettledSequence()){session.markSettled();return 0;}
        RunCheckpoint current=profile.checkpoint();
        if(current==null||!current.runId().equals(session.id())) {
            error="Não foi possível identificar a tentativa para registrar o retorno.";return 0;
        }
        if((outcome==RunOutcome.VICTORY)!=session.bossDefeated())throw new IllegalStateException("Invalid campaign result");
        RunSettlement receipt=new RunSettlement(session.report(outcome,slot),sequence);
        CampaignProfile candidate=profile.copy();
        if(session.questEnabled()&&candidate.farmerQuest()!=FarmerQuest.NOT_STARTED
            &&session.farmerQuest().ordinal()>candidate.farmerQuest().ordinal())candidate.advanceFarmerQuest(session.farmerQuest());
        if(session.pendingForgeMaterials()>0)candidate.claimForgeMaterial(session.region().id());
        if(outcome==RunOutcome.VICTORY&&session.region().id().equals(RegionDefinition.PROTOTYPE_ID))
            candidate.finishConversation(CampaignProfile.Event.INSURGENCE_SPARK_OBTAINED);
        candidate.recordSettlement(receipt);profile=candidate;settlementDurable=false;
        if(session.pendingForgeMaterials()>0){session.clearPendingForgeMaterials();session.encounters().blockRegionMaterial();}
        session.markSettled();retrySave();return receipt.report().obols();
    }
}
