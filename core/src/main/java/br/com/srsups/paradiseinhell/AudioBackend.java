package br.com.srsups.paradiseinhell;

/** Small injectable output surface: gameplay and mixer tests do not require speakers or OpenAL. */
public interface AudioBackend {
    enum Track {
        REFUGE("audio/refuge_loop.wav"), COMBAT("audio/combat_loop.wav");
        public final String asset;
        Track(String asset) { this.asset = asset; }
    }
    boolean available();
    long play(SoundCue cue,float volume);
    void stop(SoundCue cue,long id);
    void volume(SoundCue cue,long id,float volume);
    void music(Track track,float volume);
    void musicVolume(float volume);
    default boolean musicPlaying(){return false;}
    default float musicPosition(){return 0;}
    void dispose();
}

