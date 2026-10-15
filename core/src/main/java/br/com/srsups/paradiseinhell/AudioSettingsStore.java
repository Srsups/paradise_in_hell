package br.com.srsups.paradiseinhell;

import java.io.IOException;

public interface AudioSettingsStore {
    AudioSettings load() throws IOException;
    void save(AudioSettings value) throws IOException;
    static AudioSettingsStore memory() {
        return new AudioSettingsStore() {
            private AudioSettings value = AudioSettings.DEFAULT;
            public AudioSettings load() { return value; }
            public void save(AudioSettings value) { this.value = value; }
        };
    }
}

