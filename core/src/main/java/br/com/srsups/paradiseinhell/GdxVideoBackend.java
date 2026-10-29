package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import java.util.Objects;

/** Native rendering mode plus desktop placement; preference files belong to the service. */
public final class GdxVideoBackend implements VideoBackend {
    private final WindowPlacement placement;
    private int windowWidth=VideoSettings.DEFAULT.width(),windowHeight=VideoSettings.DEFAULT.height();
    private boolean vsync=VideoSettings.DEFAULT.vsync();
    private WindowPlacement.Insets decoratedInsets=new WindowPlacement.Insets(8,32,8,8);
    private record NativeSnapshot(VideoSettings settings,WindowPlacement.Area window,String displayId,
                                  boolean decorated,boolean resizable) implements VideoBackend.Snapshot {}

    public GdxVideoBackend(){this(WindowPlacement.gdx());}
    public GdxVideoBackend(WindowPlacement placement){this.placement=Objects.requireNonNull(placement);}
    private Graphics graphics(){return Objects.requireNonNull(Gdx.graphics,"Graphics not initialized");}
    private VideoSettings.Mode actualMode(){return graphics().isFullscreen()?VideoSettings.Mode.FULLSCREEN:
        placement.decorated()?VideoSettings.Mode.WINDOWED:VideoSettings.Mode.BORDERLESS;}
    private void rememberWindow(){
        if(actualMode()==VideoSettings.Mode.WINDOWED){
            WindowPlacement.Area area=placement.window();windowWidth=area.width();windowHeight=area.height();
            WindowPlacement.Insets insets=placement.frameInsets();
            if(insets.horizontal()+insets.vertical()>0||!placement.exactPlacement())decoratedInsets=insets;
        }
    }
    @Override public VideoSettings current(){rememberWindow();return new VideoSettings(windowWidth,windowHeight,actualMode(),vsync);}
    @Override public boolean matches(VideoSettings expected){
        if(!current().equals(expected))return false;
        WindowPlacement.Area window=placement.window();WindowPlacement.Display monitor=placement.display();
        if(expected.mode()!=VideoSettings.Mode.WINDOWED){
            WindowPlacement.Area bounds=monitor.bounds();
            if(window.width()!=bounds.width()||window.height()!=bounds.height()
                ||placement.exactPlacement()&&(window.x()!=bounds.x()||window.y()!=bounds.y()))return false;
            return expected.mode()!=VideoSettings.Mode.BORDERLESS||!placement.decorated()&&!placement.resizable();
        }
        WindowPlacement.Area work=monitor.workArea();WindowPlacement.Insets frame=placement.frameInsets();
        return !placement.exactPlacement()||window.x()-frame.left()>=work.x()&&window.y()-frame.top()>=work.y()
            &&window.right()+frame.right()<=work.right()&&window.bottom()+frame.bottom()<=work.bottom();
    }
    @Override public boolean supportsFullscreen(){return graphics().supportsDisplayModeChange();}
    @Override public String monitorDescription(){
        WindowPlacement.Display monitor=placement.display();
        return monitor.name()+" — "+monitor.bounds().width()+" × "+monitor.bounds().height();
    }
    @Override public VideoSettings adapt(VideoSettings requested){
        Objects.requireNonNull(requested);rememberWindow();return fit(requested,placement.display());
    }
    private VideoSettings fit(VideoSettings requested,WindowPlacement.Display display){
        if(requested.mode()!=VideoSettings.Mode.WINDOWED)return requested;
        WindowPlacement.Area work=display.workArea();
        int maxWidth=work.width()-decoratedInsets.horizontal(),maxHeight=work.height()-decoratedInsets.vertical();
        if(maxWidth<640||maxHeight<360)throw new IllegalArgumentException("A área útil do monitor é pequena demais para o modo janela.");
        float factor=Math.min(1,Math.min(maxWidth/(float)requested.width(),maxHeight/(float)requested.height()));
        int width=Math.max(640,Math.min(maxWidth,(int)Math.floor(requested.width()*factor)));
        int height=Math.max(360,Math.min(maxHeight,(int)Math.floor(requested.height()*factor)));
        return new VideoSettings(width,height,requested.mode(),requested.vsync());
    }
    @Override public VideoSettings safeDefaults(){return fit(VideoSettings.DEFAULT,placement.display());}
    @Override public VideoBackend.Snapshot snapshot(){
        VideoSettings value=current();return new NativeSnapshot(value,placement.window(),placement.display().id(),placement.decorated(),placement.resizable());
    }
    @Override public boolean restore(VideoBackend.Snapshot state){
        Objects.requireNonNull(state);
        if(!(state instanceof NativeSnapshot saved))return apply(state.settings());
        WindowPlacement.Display display=placement.findDisplay(saved.displayId());
        if(display==null)display=placement.display();
        VideoSettings fitted=fit(saved.settings(),display);
        return applyOnDisplay(fitted,display,saved.window(),saved.decorated(),saved.resizable());
    }
    @Override public boolean apply(VideoSettings requested){
        Objects.requireNonNull(requested);rememberWindow();
        WindowPlacement.Display display=placement.display();
        VideoSettings target=fit(requested,display);
        WindowPlacement.Area old=placement.window();
        boolean keepPosition=actualMode()==VideoSettings.Mode.WINDOWED&&target.mode()==VideoSettings.Mode.WINDOWED
            &&old.width()==target.width()&&old.height()==target.height();
        return applyOnDisplay(target,display,keepPosition?old:null,target.mode()==VideoSettings.Mode.WINDOWED,
            target.mode()==VideoSettings.Mode.WINDOWED);
    }
    private boolean applyOnDisplay(VideoSettings target,WindowPlacement.Display display,WindowPlacement.Area savedPosition,
                                   boolean decorated,boolean resizable){
        Graphics graphics=graphics();
        if(target.mode()==VideoSettings.Mode.FULLSCREEN&&!supportsFullscreen())return false;
        WindowPlacement.Area expected;
        if(target.mode()==VideoSettings.Mode.FULLSCREEN){
            placement.decorated(decorated);placement.resizable(resizable);
            Graphics.DisplayMode nativeMode=display.monitor()==null?graphics.getDisplayMode():graphics.getDisplayMode(display.monitor());
            if(!graphics.setFullscreenMode(nativeMode)||!graphics.isFullscreen())return false;
            expected=display.bounds();
        } else {
            placement.decorated(decorated);placement.resizable(resizable);
            int width=target.mode()==VideoSettings.Mode.BORDERLESS?display.bounds().width():target.width();
            int height=target.mode()==VideoSettings.Mode.BORDERLESS?display.bounds().height():target.height();
            WindowPlacement.Area actual=placement.window();
            if(graphics.isFullscreen()||actual.width()!=width||actual.height()!=height){
                if(!graphics.setWindowedMode(width,height))return false;
            }
            if(graphics.isFullscreen())return false;
            if(target.mode()==VideoSettings.Mode.BORDERLESS)expected=display.bounds();
            else {
                WindowPlacement.Insets insets=placement.frameInsets();
                if(insets.horizontal()+insets.vertical()>0||!placement.exactPlacement())decoratedInsets=insets;
                WindowPlacement.Area work=display.workArea();
                int lowX=work.x()+decoratedInsets.left(),lowY=work.y()+decoratedInsets.top();
                int highX=work.right()-decoratedInsets.right()-width,highY=work.bottom()-decoratedInsets.bottom()-height;
                if(highX<lowX||highY<lowY)return false;
                int x=savedPosition==null?lowX+Math.max(0,(highX-lowX)/2):Math.max(lowX,Math.min(highX,savedPosition.x()));
                int y=savedPosition==null?lowY+Math.max(0,(highY-lowY)/2):Math.max(lowY,Math.min(highY,savedPosition.y()));
                expected=new WindowPlacement.Area(x,y,width,height);
            }
            placement.position(expected.x(),expected.y());
            WindowPlacement.Area actualAfter=placement.window();
            if(actualAfter.width()!=expected.width()||actualAfter.height()!=expected.height()
                ||placement.exactPlacement()&&(actualAfter.x()!=expected.x()||actualAfter.y()!=expected.y())
                ||placement.decorated()!=decorated||placement.resizable()!=resizable)return false;
        }
        if(target.mode()==VideoSettings.Mode.FULLSCREEN){
            WindowPlacement.Area actual=placement.window();
            if(actual.width()!=expected.width()||actual.height()!=expected.height()
                ||placement.exactPlacement()&&(actual.x()!=expected.x()||actual.y()!=expected.y())
                ||placement.decorated()!=decorated||placement.resizable()!=resizable)return false;
        }
        graphics.setVSync(target.vsync());
        int refresh=display.refreshRate()>0?display.refreshRate():60;
        graphics.setForegroundFPS(target.vsync()?refresh+1:120);
        windowWidth=target.width();windowHeight=target.height();vsync=target.vsync();
        return true;
    }
}
