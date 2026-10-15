package br.com.srsups.paradiseinhell;

import java.util.ArrayList;
import java.util.List;

final class TestAudioBackend implements AudioBackend {
    record Played(SoundCue cue,float volume,long id){}
    final List<Played> played=new ArrayList<>();
    final List<Long> stopped=new ArrayList<>();
    boolean ready=true, disposed, fail;
    int disposals;
    float musicVolume,effectVolume;
    Track track;
    public boolean available(){return ready && !disposed;}
    public long play(SoundCue cue,float volume){
        if(fail)throw new IllegalStateException("device disappeared");
        long id=played.size()+1;played.add(new Played(cue,volume,id));return id;
    }
    public void stop(SoundCue cue,long id){stopped.add(id);}
    public void volume(SoundCue cue,long id,float volume){effectVolume=volume;}
    public void music(Track track,float volume){this.track=track;this.musicVolume=volume;}
    public void musicVolume(float volume){this.musicVolume=volume;}
    public void dispose(){disposed=true;disposals++;}
}

