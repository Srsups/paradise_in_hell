package br.com.srsups.paradiseinhell;

import java.io.IOException;

/** Unlocks save balance and access together; failed reward saves remain retryable in memory. */
public final class SharedProgressService {
    private final SharedProgressStore store;
    private SharedProgress progress;
    private boolean writable=true;
    private String error="";
    private final java.util.Map<String,RunSettlement> pendingReceipts=new java.util.LinkedHashMap<>();
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
    /** A durable high-water mark, independent of the twenty visible reports. */
    public boolean settleReceipt(RunSettlement receipt){
        java.util.Objects.requireNonNull(receipt);
        String identity=receipt.report().characterId();
        if(receipt.sequence()<=progress.highestCompletedSequence(identity)){clearPending(identity,receipt.sequence());return true;}
        if(!writable)return false;
        SharedProgress candidate=progress.copy();candidate.recordReceipt(receipt);
        try{store.saveSettlement(candidate);progress=candidate;clearPending(identity,receipt.sequence());
            error=pendingReceipts.isEmpty()?"":"Ainda há retornos pendentes de gravação. Pressione S no refúgio.";return true;}
        catch(IOException failure){pendingReceipts.put(identity,receipt);error="O retorno foi guardado, mas faltou salvar Almas e histórico. Pressione S no refúgio.";return false;}
    }
    private void clearPending(String identity,long through){
        RunSettlement pending=pendingReceipts.get(identity);
        if(pending!=null&&pending.sequence()<=through)pendingReceipts.remove(identity);
    }
    public boolean retrySave() {
        if(!writable) return false;
        if(!pendingReceipts.isEmpty()){
            for(RunSettlement receipt:java.util.List.copyOf(pendingReceipts.values()))if(!settleReceipt(receipt))return false;
            return true;
        }
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
    public void preserveFarmerUnlock() {
        if(progress.farmerUnlocked()) return;
        progress.preserveFarmerUnlock(); retrySave();
    }
    public RunReport settle(RunSession run,RunOutcome outcome,int slot) {
        RunReport report=run.report(outcome,slot);
        if(run.attemptSequence()>0){
            if(settleReceipt(new RunSettlement(report,run.attemptSequence())))run.markSharedRecorded();
            return report;
        }
        if(!run.sharedRecorded()) {
            progress.record(report); run.markSharedRecorded(); retrySave();
        }
        return report;
    }
}

