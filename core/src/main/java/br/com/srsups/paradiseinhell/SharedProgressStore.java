package br.com.srsups.paradiseinhell;

import java.io.IOException;

public interface SharedProgressStore {
    SharedProgress load() throws IOException;
    void save(SharedProgress progress) throws IOException;
    static SharedProgressStore memory() {
        return new SharedProgressStore() {
            private SharedProgress saved=new SharedProgress();
            public SharedProgress load() { return saved.copy(); }
            public void save(SharedProgress progress) { saved=progress.copy(); }
        };
    }
}

