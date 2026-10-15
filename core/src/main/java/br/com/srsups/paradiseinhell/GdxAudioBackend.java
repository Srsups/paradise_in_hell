package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import java.util.EnumMap;

/** Loads shared clips once and streams only the active loop. Missing devices leave gameplay usable. */
public final class GdxAudioBackend implements AudioBackend {
    private final EnumMap<SoundCue,Sound> sounds = new EnumMap<>(SoundCue.class);
    private final EnumMap<Track,Music> loops = new EnumMap<>(Track.class);
    private Track track;
    private boolean available, disposed;
    public GdxAudioBackend() {
        try {
            for (SoundCue cue : SoundCue.values()) sounds.put(cue,Gdx.audio.newSound(Gdx.files.internal(cue.asset())));
            for (Track next : Track.values()) {
                Music loop = Gdx.audio.newMusic(Gdx.files.internal(next.asset));
                loop.setLooping(true); loops.put(next,loop);
            }
            available = true;
        } catch (RuntimeException | LinkageError failure) { dispose(); }
    }
    public boolean available() { return available && !disposed; }
    public int soundCount() { return sounds.size(); }
    public boolean musicPlaying() { return track != null && loops.get(track).isPlaying(); }
    public float musicPosition() { return track == null ? 0 : loops.get(track).getPosition(); }
    public long play(SoundCue cue,float volume) { return available() ? sounds.get(cue).play(volume) : -1; }
    public void stop(SoundCue cue,long id) { if (available()) sounds.get(cue).stop(id); }
    public void volume(SoundCue cue,long id,float volume) { if (available()) sounds.get(cue).setVolume(id,volume); }
    public void music(Track next,float volume) {
        if (!available()) return;
        if (track != next) {
            if (track != null) loops.get(track).stop();
            track = next;
            Music loop = loops.get(next); loop.setVolume(volume); loop.play();
        } else loops.get(next).setVolume(volume);
    }
    public void musicVolume(float volume) { if (available() && track != null) loops.get(track).setVolume(volume); }
    public void dispose() {
        if (disposed) return;
        disposed = true; available = false;
        for (Sound sound : sounds.values()) sound.dispose();
        for (Music loop : loops.values()) loop.dispose();
        sounds.clear(); loops.clear(); track = null;
    }
}

