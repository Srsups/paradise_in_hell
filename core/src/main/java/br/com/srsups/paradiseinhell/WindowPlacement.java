package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import java.util.Objects;

/** Window/monitor geometry in desktop coordinates, never framebuffer pixels. */
public interface WindowPlacement {
    record Area(int x,int y,int width,int height) {
        public Area {if(width<=0||height<=0)throw new IllegalArgumentException("Invalid desktop area");}
        public int right(){return x+width;}
        public int bottom(){return y+height;}
    }
    record Insets(int left,int top,int right,int bottom) {
        public Insets {if(left<0||top<0||right<0||bottom<0)throw new IllegalArgumentException("Invalid window frame");}
        public int horizontal(){return left+right;}
        public int vertical(){return top+bottom;}
    }
    record Display(String id,String name,Graphics.Monitor monitor,Area bounds,Area workArea,int refreshRate) {
        public Display {Objects.requireNonNull(id);Objects.requireNonNull(name);Objects.requireNonNull(bounds);Objects.requireNonNull(workArea);}
    }
    Display display();
    Display findDisplay(String id);
    Area window();
    Insets frameInsets();
    boolean decorated();
    boolean resizable();
    void decorated(boolean value);
    void resizable(boolean value);
    void position(int x,int y);
    /** Generic Graphics lacks native position queries. Desktop launchers supply an exact implementation. */
    default boolean exactPlacement(){return true;}

    static WindowPlacement gdx() {
        return new WindowPlacement() {
            private boolean decorated=true,resizable=true;
            private int x,y;
            public Display display(){
                Graphics.DisplayMode mode=Gdx.graphics.getDisplayMode();
                return new Display("current","Monitor atual",null,new Area(0,0,mode.width,mode.height),
                    new Area(0,0,Math.max(640,mode.width-32),Math.max(360,mode.height-80)),mode.refreshRate);
            }
            public Display findDisplay(String id){Display display=display();return display.id().equals(id)?display:null;}
            public Area window(){return new Area(x,y,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());}
            public Insets frameInsets(){return new Insets(0,0,0,0);}
            public boolean decorated(){return decorated;}
            public boolean resizable(){return resizable;}
            public void decorated(boolean value){Gdx.graphics.setUndecorated(!value);decorated=value;}
            public void resizable(boolean value){Gdx.graphics.setResizable(value);resizable=value;}
            public void position(int nextX,int nextY){x=nextX;y=nextY;}
            public boolean exactPlacement(){return false;}
        };
    }
}
