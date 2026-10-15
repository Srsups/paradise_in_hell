package br.com.srsups.paradiseinhell;

import java.io.IOException;

/** Edits apply immediately; failed writes remain retryable without affecting a run or currency. */
public final class AudioSettingsService {
    private final AudioSettingsStore store;
    private AudioSettings value;
    private boolean writable = true, pending;
    private String error = "";
    public AudioSettingsService(AudioSettingsStore store) {
        this.store = store;
        try { value = store.load(); }
        catch (IOException | RuntimeException failure) {
            value = AudioSettings.DEFAULT; writable = false;
            error = "Não foi possível ler o áudio salvo. Preferências apenas nesta sessão; arquivo preservado.";
        }
    }
    public AudioSettings value() { return value; }
    public String error() { return error; }
    public boolean pending() { return pending; }
    public void change(AudioSettings next) {
        if (value.equals(next)) return;
        value = next; pending = true;
    }
    public boolean save() {
        if (!writable) return false;
        if (!pending) return true;
        try { store.save(value); pending = false; error = ""; return true; }
        catch (IOException failure) { error = "Volume aplicado; não foi salvo. Pressione S para tentar novamente."; return false; }
    }
}

