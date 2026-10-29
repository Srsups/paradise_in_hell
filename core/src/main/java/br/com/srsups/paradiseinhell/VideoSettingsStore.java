package br.com.srsups.paradiseinhell;

import java.io.IOException;

public interface VideoSettingsStore {
    VideoSettings load() throws IOException;
    void save(VideoSettings value) throws IOException;
    static VideoSettingsStore memory() {
        return new VideoSettingsStore() {
            private VideoSettings value=VideoSettings.DEFAULT;
            public VideoSettings load(){return value;}
            public void save(VideoSettings next){value=java.util.Objects.requireNonNull(next);}
        };
    }
}
