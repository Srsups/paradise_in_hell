package br.com.srsups.paradiseinhell;

import java.io.IOException;

public interface SharedProgressStore {
    SharedProgress load() throws IOException;
    void save(SharedProgress progress) throws IOException;
    /** Receipt balance and its durable deduplication ledger are one mirrored transaction. */
    default void saveSettlement(SharedProgress progress)throws IOException {save(progress);}
    static SharedProgressStore memory() {
        return new SharedProgressStore() {
            private SharedProgress saved=new SharedProgress();
            public SharedProgress load() { return saved.copy(); }
            public void save(SharedProgress progress) { saved=progress.copy(); }
        };
    }
}

