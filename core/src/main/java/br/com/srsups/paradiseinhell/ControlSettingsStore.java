package br.com.srsups.paradiseinhell;

import java.io.IOException;
import java.util.Objects;

public interface ControlSettingsStore {
    ControlSettings load() throws IOException;
    void save(ControlSettings value) throws IOException;
    static ControlSettingsStore memory() {
        return new ControlSettingsStore() {
            private ControlSettings value = ControlSettings.DEFAULT;
            public ControlSettings load() { return value; }
            public void save(ControlSettings value) { this.value = Objects.requireNonNull(value); }
        };
    }
}
