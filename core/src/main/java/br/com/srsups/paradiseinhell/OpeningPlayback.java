package br.com.srsups.paradiseinhell;

import java.util.ArrayDeque;
import java.util.Objects;

/** UI-clock timeline only: cues are collected once, never from drawing or campaign state. */
public final class OpeningPlayback {
    private final OpeningStory.Script story;
    private final ArrayDeque<SoundCue> sounds=new ArrayDeque<>();
    private final boolean[][] emitted;
    private int shot;
    private float local;
    private boolean paused,finished,skipped;
    public OpeningPlayback(){this(OpeningStory.script());}
    public OpeningPlayback(OpeningStory.Script story) {
        this.story=Objects.requireNonNull(story);emitted=new boolean[story.shots().size()][];
        for(int index=0;index<emitted.length;index++)emitted[index]=new boolean[story.shots().get(index).cues().size()];
        collect();
    }
    public OpeningStory.Shot current(){return story.shots().get(shot);}
    public int shotIndex(){return shot;}
    public float elapsedInShot(){return local;}
    public float elapsedTotal(){float elapsed=local;for(int index=0;index<shot;index++)elapsed+=story.shots().get(index).duration();return elapsed;}
    public float progress(){return finished?1:elapsedTotal()/story.duration();}
    public float shotProgress(){return local/current().duration();}
    public boolean paused(){return paused;}
    public void pause(boolean value){paused=value;}
    public void paused(boolean value){pause(value);}
    public void togglePause(){pause(!paused);}
    public boolean finished(){return finished;}
    public boolean skipped(){return skipped;}
    public OpeningStory.Subtitle subtitle() {
        OpeningStory.Subtitle result=null;
        for(var subtitle:current().subtitles())if(subtitle.at()<=local)result=subtitle;else break;
        return result;
    }
    public String speaker(){var subtitle=subtitle();return subtitle==null?"":subtitle.speaker();}
    public String text(){var subtitle=subtitle();return subtitle==null?"":subtitle.text();}
    public SoundCue pollSound(){return paused?null:sounds.poll();}
    public void advance(float delta) {
        if(!Float.isFinite(delta)||delta<0)throw new IllegalArgumentException("Invalid opening time");
        if(paused||finished)return;
        float remaining=delta;
        do {
            float step=Math.min(remaining,current().duration()-local);local+=step;remaining-=step;collect();
            if(local>=current().duration()) {
                if(shot+1==story.shots().size()){finished=true;return;}
                shot++;local=0;sounds.clear();collect();
            } else return;
        } while(remaining>0);
    }
    public boolean nextShot() {
        if(paused||finished||local<current().minReadSeconds())return false;
        sounds.clear();
        if(shot+1==story.shots().size()){local=current().duration();finished=true;return true;}
        shot++;local=0;collect();return true;
    }
    public boolean next(){return nextShot();}
    public void skip(){if(finished)return;sounds.clear();skipped=true;finished=true;}
    private void collect() {
        for(int index=0;index<current().cues().size();index++) {
            var cue=current().cues().get(index);
            if(!emitted[shot][index]&&cue.at()<=local){emitted[shot][index]=true;sounds.add(cue.sound());}
        }
    }
}
