package br.com.srsups.paradiseinhell;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/** At most eight effect voices; important warnings replace lower-priority repetitive sounds. */
public final class GameAudio {
    public static final int MAX_VOICES = 8;
    private record Voice(SoundCue cue,long id,double until,Object emitter) { }
    private final AudioBackend backend;
    private final AudioSettingsService settings;
    private final List<Voice> voices = new ArrayList<>(MAX_VOICES);
    private final EnumMap<SoundCue,Double> last = new EnumMap<>(SoundCue.class);
    private double time;
    private boolean disposed, failed;
    public GameAudio(AudioSettingsService settings,AudioBackend backend) { this.settings=settings; this.backend=backend; }
    public boolean available() { return !disposed && !failed && backend.available(); }
    public boolean musicPlaying(){return available() && backend.musicPlaying();}
    public float musicPosition(){return available()?backend.musicPosition():0;}
    public int activeVoices() { return voices.size(); }
    public void advance(float delta) {
        if (!Float.isFinite(delta) || delta < 0) throw new IllegalArgumentException("Invalid audio time");
        if (disposed) return;
        time += delta;
        var iterator=voices.iterator();
        while(iterator.hasNext()) {
            Voice voice=iterator.next();
            if(voice.until<=time) { safeStop(voice); iterator.remove(); }
        }
    }
    public boolean play(SoundCue cue) { return play(cue,null); }
    public boolean play(SoundCue cue,Object emitter) {
        if (!available() || settings.value().effectsGain()==0 || time-last.getOrDefault(cue,-100d)<cue.interval) return false;
        if (voices.size() == MAX_VOICES) {
            Voice lowest=voices.stream().min(java.util.Comparator.comparingInt(v->v.cue.priority)).orElseThrow();
            if (lowest.cue.priority >= cue.priority) return false;
            safeStop(lowest); voices.remove(lowest);
        }
        try {
            long id=backend.play(cue,settings.value().effectsGain()*cue.gain);
            if (id < 0) return false;
            voices.add(new Voice(cue,id,time+cue.duration,emitter)); last.put(cue,time); return true;
        } catch (RuntimeException failure) { fail(); return false; }
    }
    public void music(AudioBackend.Track track) {
        if (!available()) return;
        try { backend.music(track,settings.value().musicGain()); } catch(RuntimeException failure) { fail(); }
    }
    public void refreshVolume() {
        if (!available()) return;
        try {
            if (settings.value().effectsGain()==0) stopEffects();
            else for(Voice voice:voices) backend.volume(voice.cue,voice.id,settings.value().effectsGain()*voice.cue.gain);
            backend.musicVolume(settings.value().musicGain());
        } catch(RuntimeException failure) { fail(); }
    }
    /** Stop only voices owned by this world emitter; cue cooldowns remain global and bounded. */
    public void stopEmitter(Object emitter) {
        java.util.Objects.requireNonNull(emitter);
        var iterator=voices.iterator();
        while(iterator.hasNext()) {
            Voice voice=iterator.next();
            if(voice.emitter==emitter) { safeStop(voice); iterator.remove(); }
        }
    }
    public void stopEffects() {
        for(Voice voice:voices) safeStop(voice);
        voices.clear(); last.clear();
    }
    private void safeStop(Voice voice) {
        try { backend.stop(voice.cue,voice.id); } catch(RuntimeException failure) { failed=true; }
    }
    private void fail() { failed=true; stopEffects(); try{backend.musicVolume(0);}catch(RuntimeException ignored){} }
    public void dispose() { if(!disposed) { stopEffects(); disposed=true; backend.dispose(); } }
}

