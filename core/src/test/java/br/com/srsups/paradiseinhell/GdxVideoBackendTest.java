package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Desktop contracts with fake monitors/window state; never changes a real monitor. */
class GdxVideoBackendTest {
    private static final class Monitor extends Graphics.Monitor {
        Monitor(int x,int y,String name){super(x,y,name);}
    }
    private static final class Mode extends Graphics.DisplayMode {
        final WindowPlacement.Display display;
        Mode(WindowPlacement.Display display){super(display.bounds().width(),display.bounds().height(),display.refreshRate(),32);this.display=display;}
    }
    private static final class Window implements WindowPlacement,AutoCloseable {
        final Graphics original=Gdx.graphics;
        final List<Display> displays=new ArrayList<>();
        int x=100,y=100,width=960,height=540,fps,calls,fullCalls;
        boolean fullscreen,vsync=true,supported=true,ignoreSize,ignorePosition,ignoreDecoration,ignoreFullscreenOrigin,decorated=true,resizable=true;
        Insets insets=new Insets(8,32,8,8);
        Display fullDisplay;
        Window(){
            displays.add(display("primary",0,0,1920,1080,0,0,1920,1040,60));
            Gdx.graphics=(Graphics)Proxy.newProxyInstance(Graphics.class.getClassLoader(),new Class<?>[]{Graphics.class},(proxy,method,args)->switch(method.getName()){
                case "getWidth"->width;case "getHeight"->height;
                case "getBackBufferWidth"->width*2;case "getBackBufferHeight"->height*2;
                case "getDisplayMode"->{
                    Display target=display();
                    if(args!=null&&args.length==1)for(Display candidate:displays)if(candidate.monitor()==args[0])target=candidate;
                    yield new Mode(target);
                }
                case "supportsDisplayModeChange"->supported;case "isFullscreen"->fullscreen;
                case "setWindowedMode"->{calls++;if(!ignoreSize){width=(int)args[0];height=(int)args[1];fullscreen=false;}yield true;}
                case "setFullscreenMode"->{calls++;fullCalls++;if(!ignoreSize){Mode mode=(Mode)args[0];fullDisplay=mode.display;fullscreen=true;if(!ignoreFullscreenOrigin){x=fullDisplay.bounds().x();y=fullDisplay.bounds().y();}width=mode.width;height=mode.height;}yield true;}
                case "setVSync"->{vsync=(boolean)args[0];yield null;}
                case "setForegroundFPS"->{fps=(int)args[0];yield null;}
                default->throw new AssertionError("Unexpected graphics call: "+method.getName());
            });
        }
        static Display display(String id,int x,int y,int width,int height,int workX,int workY,int workWidth,int workHeight,int refresh){
            return new Display(id,id,new Monitor(x,y,id),new Area(x,y,width,height),new Area(workX,workY,workWidth,workHeight),refresh);
        }
        public Display display(){
            if(fullscreen&&displays.contains(fullDisplay))return fullDisplay;
            Display selected=displays.getFirst();long maximum=-1;
            for(Display display:displays){
                Area bounds=display.bounds();
                long overlap=(long)Math.max(0,Math.min(x+width,bounds.right())-Math.max(x,bounds.x()))
                    *Math.max(0,Math.min(y+height,bounds.bottom())-Math.max(y,bounds.y()));
                if(overlap>maximum){maximum=overlap;selected=display;}
            }
            return selected;
        }
        public Display findDisplay(String id){return displays.stream().filter(value->value.id().equals(id)).findFirst().orElse(null);}
        public Area window(){return new Area(x,y,width,height);}
        public Insets frameInsets(){return decorated?insets:new Insets(0,0,0,0);}
        public boolean decorated(){return decorated;}
        public boolean resizable(){return resizable;}
        public void decorated(boolean value){if(!ignoreDecoration)decorated=value;}
        public void resizable(boolean value){resizable=value;}
        public void position(int nextX,int nextY){if(!ignorePosition){x=nextX;y=nextY;}}
        public void close(){Gdx.graphics=original;}
    }
    @Test void currentReadsLogicalManualWindowSizeInsteadOfPhysicalFramebufferSize(){
        try(Window window=new Window()){
            GdxVideoBackend backend=new GdxVideoBackend(window);window.width=1111;window.height=777;
            assertEquals(new VideoSettings(1111,777,false,true),backend.current());
            window.width=1000;window.height=600;assertEquals(1000,backend.current().width());
        }
    }
    @Test void threeModesKeepTheWindowPreferenceAndUseDifferentNativeOperations(){
        try(Window window=new Window()){
            GdxVideoBackend backend=new GdxVideoBackend(window);
            VideoSettings borderless=new VideoSettings(1280,720,VideoSettings.Mode.BORDERLESS,false);
            assertTrue(backend.apply(borderless));assertFalse(window.fullscreen);assertFalse(window.decorated);assertFalse(window.resizable);
            assertEquals(1920,window.width);assertEquals(1080,window.height);assertEquals(0,window.x);assertEquals(0,window.y);
            assertEquals(0,window.fullCalls);assertEquals(borderless,backend.current());
            VideoSettings full=borderless.withMode(VideoSettings.Mode.FULLSCREEN);
            assertTrue(backend.apply(full));assertTrue(window.fullscreen);assertEquals(1,window.fullCalls);assertEquals(full,backend.current());
            assertTrue(backend.apply(VideoSettings.DEFAULT));assertFalse(window.fullscreen);assertTrue(window.decorated);assertTrue(window.resizable);
            assertEquals(960,window.width);assertEquals(540,window.height);assertEquals(VideoSettings.DEFAULT,backend.current());
        }
    }
    @Test void windowsFitTheWorkAreaAndFrameProportionallyBeforeThePreviewIsApplied(){
        try(Window window=new Window()){
            GdxVideoBackend backend=new GdxVideoBackend(window);VideoSettings requested=new VideoSettings(1920,1080,false,true);
            VideoSettings fitted=backend.adapt(requested);
            assertEquals(1777,fitted.width());assertEquals(1000,fitted.height());assertEquals(0,window.calls);
            assertTrue(backend.apply(fitted));
            assertTrue(window.x-window.insets.left()>=0);assertTrue(window.y-window.insets.top()>=0);
            assertTrue(window.x+window.width+window.insets.right()<=1920);
            assertTrue(window.y+window.height+window.insets.bottom()<=1040);
        }
    }
    @Test void secondaryMonitorWithNegativeOriginIsUsedForWindowBorderlessAndFullscreen(){
        try(Window window=new Window()){
            window.displays.add(Window.display("left",-2560,-200,2560,1440,-2560,-200,2560,1400,144));window.x=-2300;
            GdxVideoBackend backend=new GdxVideoBackend(window);
            assertTrue(backend.apply(new VideoSettings(1280,720,VideoSettings.Mode.BORDERLESS,true)));
            assertEquals(new WindowPlacement.Area(-2560,-200,2560,1440),window.window());assertEquals(145,window.fps);
            assertTrue(backend.apply(new VideoSettings(1280,720,VideoSettings.Mode.FULLSCREEN,true)));
            assertEquals("left",window.fullDisplay.id());assertEquals(2560,window.width);
            assertTrue(backend.apply(new VideoSettings(1280,720,false,true)));assertTrue(window.x<0);
            assertTrue(window.x-window.insets.left()>=-2560);assertTrue(window.y-window.insets.top()>=-200);
        }
    }
    @Test void snapshotRestoresPositionSizeDecorationAndResizePolicyExactly(){
        try(Window window=new Window()){
            window.x=237;window.y=149;window.resizable=false;
            GdxVideoBackend backend=new GdxVideoBackend(window);VideoBackend.Snapshot snapshot=backend.snapshot();
            assertTrue(backend.apply(VideoSettings.DEFAULT.withMode(VideoSettings.Mode.BORDERLESS)));
            assertTrue(backend.restore(snapshot));assertEquals(237,window.x);assertEquals(149,window.y);
            assertEquals(960,window.width);assertEquals(540,window.height);assertTrue(window.decorated);assertFalse(window.resizable);
            assertEquals(snapshot.settings(),backend.current());
        }
    }
    @Test void fullscreenSnapshotRestoresItsNativeModeAndAttributesAfterWindowedPreview(){
        try(Window window=new Window()){
            GdxVideoBackend backend=new GdxVideoBackend(window);
            VideoSettings full=VideoSettings.DEFAULT.withMode(VideoSettings.Mode.FULLSCREEN);
            assertTrue(backend.apply(full));VideoBackend.Snapshot snapshot=backend.snapshot();
            assertTrue(backend.apply(VideoSettings.DEFAULT));assertTrue(window.decorated);assertTrue(window.resizable);
            assertTrue(backend.restore(snapshot));assertTrue(window.fullscreen);assertFalse(window.decorated);assertFalse(window.resizable);
            assertEquals(full,backend.current());assertTrue(backend.matches(full));
        }
    }
    @Test void removedMonitorDuringPreviewRestoresIntoAvailableWorkArea(){
        try(Window window=new Window()){
            window.displays.add(Window.display("left",-1920,0,1920,1080,-1920,0,1920,1040,60));window.x=-1600;
            GdxVideoBackend backend=new GdxVideoBackend(window);VideoBackend.Snapshot snapshot=backend.snapshot();
            assertTrue(backend.apply(VideoSettings.DEFAULT.withMode(VideoSettings.Mode.BORDERLESS)));
            window.displays.removeIf(display->display.id().equals("left"));
            assertTrue(backend.restore(snapshot));assertFalse(window.fullscreen);assertTrue(window.decorated);
            assertTrue(window.x>=8);assertTrue(window.y>=32);assertTrue(window.x+window.width+8<=1920);
        }
    }
    @Test void unsupportedFullscreenIsRejectedBeforeNativeMutationButBorderlessStillWorks(){
        try(Window window=new Window()){
            GdxVideoBackend backend=new GdxVideoBackend(window);window.supported=false;
            assertFalse(backend.apply(VideoSettings.DEFAULT.withFullscreen(true)));assertEquals(0,window.calls);assertTrue(window.decorated);
            assertTrue(backend.apply(VideoSettings.DEFAULT.withMode(VideoSettings.Mode.BORDERLESS)));assertFalse(window.fullscreen);
        }
    }
    @Test void vsyncUsesTheCurrentMonitorRefreshAndDisabledModeHasABoundedCap(){
        try(Window window=new Window()){
            window.displays.set(0,Window.display("primary",0,0,1920,1080,0,0,1920,1040,30));
            GdxVideoBackend backend=new GdxVideoBackend(window);
            assertTrue(backend.apply(VideoSettings.DEFAULT));assertTrue(window.vsync);assertEquals(31,window.fps);
            assertTrue(backend.apply(VideoSettings.DEFAULT.withVsync(false)));assertFalse(window.vsync);assertEquals(120,window.fps);
        }
    }
    @Test void nativeSuccessWithoutActualSizeOrDecorationChangeIsRejected(){
        try(Window window=new Window()){
            GdxVideoBackend backend=new GdxVideoBackend(window);window.ignoreSize=true;
            assertFalse(backend.apply(new VideoSettings(1280,720,false,false)));
            assertFalse(window.fullscreen);assertTrue(window.vsync);
            window.ignoreSize=false;window.ignoreDecoration=true;
            assertFalse(backend.apply(VideoSettings.DEFAULT.withMode(VideoSettings.Mode.BORDERLESS)));
        }
    }
    @Test void nativeSuccessWithoutPositionChangeCannotConfirmBorderless(){
        try(Window window=new Window()){
            window.ignorePosition=true;GdxVideoBackend backend=new GdxVideoBackend(window);
            assertFalse(backend.apply(VideoSettings.DEFAULT.withMode(VideoSettings.Mode.BORDERLESS)));
        }
    }
    @Test void borderlessMovedOrResizedOutsideTheGameCannotBeConfirmed()throws Exception{
        try(Window window=new Window()){
            GdxVideoBackend backend=new GdxVideoBackend(window);
            VideoSettings target=VideoSettings.DEFAULT.withMode(VideoSettings.Mode.BORDERLESS);
            VideoSettingsStore store=VideoSettingsStore.memory();
            VideoSettingsService service=new VideoSettingsService(store,backend);
            assertTrue(service.preview(target));assertTrue(backend.matches(target));
            window.x=10;assertFalse(backend.matches(target));
            assertFalse(service.confirm());assertEquals(VideoSettings.DEFAULT,store.load());
            assertEquals(VideoSettings.DEFAULT,backend.current());
            assertTrue(service.preview(target));window.width=960;assertFalse(backend.matches(target));
            assertFalse(service.confirm());assertEquals(VideoSettings.DEFAULT,store.load());
        }
    }
    @Test void fullscreenWithCorrectResolutionAtTheWrongMonitorOriginIsRejected(){
        try(Window window=new Window()){
            window.ignoreFullscreenOrigin=true;GdxVideoBackend backend=new GdxVideoBackend(window);
            assertFalse(backend.apply(VideoSettings.DEFAULT.withMode(VideoSettings.Mode.FULLSCREEN)));
        }
    }
    @Test void anAreaSmallerThanTheMinimumWindowIsRejectedWithoutResizing(){
        try(Window window=new Window()){
            window.displays.set(0,Window.display("tiny",0,0,640,360,0,0,640,320,60));
            GdxVideoBackend backend=new GdxVideoBackend(window);
            assertThrows(IllegalArgumentException.class,()->backend.adapt(VideoSettings.DEFAULT));assertEquals(0,window.calls);
        }
    }
}
