package br.com.srsups.paradiseinhell;

import java.io.IOException;

/** Unlocks save balance and access together; failed reward saves remain retryable in memory. */
public final class SharedProgressService {
    private final SharedProgressStore store;
    private SharedProgress progress;
    private boolean writable=true;
    private String error="";
    public SharedProgressService(SharedProgressStore store) {
        this.store=store;
        try { progress=store.load(); }
        catch(IOException | RuntimeException failure) {
            progress=new SharedProgress(); writable=false;
            error="Não foi possível ler Almas e histórico. Arquivo preservado; progresso apenas nesta sessão.";
        }
    }
    public SharedProgress progress() { return progress; }
    public String error() { return error; }
    public boolean retrySave() {
        if(!writable) return false;
        try { store.save(progress); error=""; return true; }
        catch(IOException failure) { error="Não foi possível salvar Almas/histórico. Pressione S no refúgio para tentar novamente."; return false; }
    }
    public boolean unlockNecromancer() {
        if(!writable || !progress.canUnlockNecromancer()) return false;
        SharedProgress candidate=progress.copy(); candidate.unlockNecromancer();
        try { store.save(candidate); progress=candidate; error=""; return true; }
        catch(IOException failure) { error="Não foi possível salvar o desbloqueio. Suas Almas foram preservadas."; return false; }
    }
    public void preserveLegacyUnlock() {
        if(progress.necromancerUnlocked()) return;
        progress.preserveLegacyUnlock(); retrySave();
    }
    public RunReport settle(RunSession run,RunOutcome outcome,int slot) {
        RunReport report=run.report(outcome,slot);
        if(!run.sharedRecorded()) {
            progress.record(report); run.markSharedRecorded(); retrySave();
        }
        return report;
    }
}

