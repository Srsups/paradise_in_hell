package br.com.srsups.paradiseinhell;

import java.io.IOException;
import java.util.Objects;

public interface HudSettingsStore {
    HudSettings load() throws IOException;
    void save(HudSettings value) throws IOException;
    static HudSettingsStore memory() {
        return new HudSettingsStore() {
            private HudSettings value=HudSettings.DEFAULT;
            public HudSettings load(){return value;}
            public void save(HudSettings value){this.value=Objects.requireNonNull(value);}
        };
    }
}
