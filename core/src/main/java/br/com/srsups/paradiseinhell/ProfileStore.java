package br.com.srsups.paradiseinhell;

import java.io.IOException;

public interface ProfileStore {
    CampaignProfile load() throws IOException;
    void save(CampaignProfile profile) throws IOException;

    /** Reset only the selected character; file stores also invalidate their old backup. */
    default void reset(CampaignProfile empty) throws IOException { save(empty); }

    /** Tests and the diagnostic launcher must never modify a player's real profile. */
    static ProfileStore memory() {
        return new ProfileStore() {
            private CampaignProfile saved = new CampaignProfile();
            public CampaignProfile load() { return saved.copy(); }
            public void save(CampaignProfile profile) { saved = profile.copy(); }
        };
    }
}
