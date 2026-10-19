package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.ArrayList;

/** Short opening animations persist after the model grants/removes the chest. No delayed rewards. */
public final class ChestVisuals {
    private static final class Open {final float x,y;float time;Open(float x,float y){this.x=x;this.y=y;}}
    private final java.util.List<Open> opened=new ArrayList<>();
    public void open(RunChests.Chest chest) {if(opened.size()==6)opened.removeFirst();opened.add(new Open(chest.x(),chest.y()));}
    public void advance(float delta) {opened.removeIf(chest->{chest.time+=delta;return chest.time>=.85f;});}
    public int active() {return opened.size();}
    public void draw(SpriteBatch batch,PilotArt art,java.util.List<RunChests.Chest> closed) {
        for(var chest:closed)batch.draw(art.expanded().region("chest-normal-0"),chest.x()-12,chest.y()-10,24,20);
        for(var chest:opened) {
            float alpha=chest.time<.6f?1:(.85f-chest.time)/.25f;
            batch.setColor(1,1,1,Math.max(0,alpha));
            batch.draw(art.expanded().effect("chest-normal",Math.min(1,chest.time/.45f)),chest.x-12,chest.y-10,24,20);
        }
        batch.setColor(com.badlogic.gdx.graphics.Color.WHITE);
    }
    public void clear(){opened.clear();}
}

