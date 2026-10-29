package br.com.srsups.paradiseinhell;

import java.io.IOException;
import java.util.Objects;

/** Explicit Apply changes the session immediately; failed writes remain retryable. */
public final class ControlSettingsService {
    private final ControlSettingsStore store;
    private ControlSettings value;
    private boolean writable = true, pendingSave;
    private String error = "";

    public ControlSettingsService(ControlSettingsStore store) {
        this.store = Objects.requireNonNull(store);
        try { value = Objects.requireNonNull(store.load()); }
        catch (IOException | RuntimeException failure) {
            value = ControlSettings.DEFAULT;
            writable = false;
            error = "Não foi possível ler os controles salvos. Preferências apenas nesta sessão; arquivo preservado.";
        }
    }
    public ControlSettings value() { return value; }
    public boolean pendingSave() { return pendingSave; }
    public boolean writable() { return writable; }
    public String error() { return error; }
    public boolean apply(ControlSettings next) {
        Objects.requireNonNull(next);
        if (!value.equals(next)) { value = next; pendingSave = true; }
        return retrySave();
    }
    public boolean retrySave() {
        if (!writable) return false;
        if (!pendingSave) return true;
        try { store.save(value); pendingSave = false; error = ""; return true; }
        catch (IOException | RuntimeException failure) {
            error = "Controles aplicados; não foram salvos. Pressione S para tentar novamente.";
            return false;
        }
    }
}
