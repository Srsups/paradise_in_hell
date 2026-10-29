package br.com.srsups.paradiseinhell;

import java.io.IOException;
import java.util.Objects;

/** Apply changes the logical size immediately; failed disk writes remain explicitly retryable. */
public final class HudSettingsService {
    private final HudSettingsStore store;
    private HudSettings value;
    private boolean writable=true,pendingSave;
    private String error="";
    public HudSettingsService(HudSettingsStore store) {
        this.store=Objects.requireNonNull(store);
        try{value=Objects.requireNonNull(store.load());}
        catch(IOException|RuntimeException failure) {
            value=HudSettings.DEFAULT;writable=false;
            error="Não foi possível ler o tamanho do HUD salvo. Preferência apenas nesta sessão; arquivo preservado.";
        }
    }
    public HudSettings value(){return value;}
    public boolean pendingSave(){return pendingSave;}
    public boolean writable(){return writable;}
    public String error(){return error;}
    public boolean apply(HudSettings next) {
        Objects.requireNonNull(next);
        if(!value.equals(next)){value=next;pendingSave=true;}
        return retrySave();
    }
    public boolean retrySave() {
        if(!writable)return false;
        if(!pendingSave)return true;
        try{store.save(value);pendingSave=false;error="";return true;}
        catch(IOException|RuntimeException failure) {
            error="Tamanho do HUD aplicado; não foi salvo. Pressione S para tentar novamente.";
            return false;
        }
    }
}
