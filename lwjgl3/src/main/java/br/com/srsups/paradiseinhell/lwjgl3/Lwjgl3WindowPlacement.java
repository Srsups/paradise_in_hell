package br.com.srsups.paradiseinhell.lwjgl3;

import br.com.srsups.paradiseinhell.WindowPlacement;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import java.nio.IntBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;

/** Exact GLFW work areas, origins, logical client sizes and window attributes for desktop. */
public final class Lwjgl3WindowPlacement implements WindowPlacement {
    private Lwjgl3Graphics graphics(){return (Lwjgl3Graphics)Gdx.graphics;}
    private long windowHandle(){return graphics().getWindow().getWindowHandle();}
    private long monitorHandle(Graphics.Monitor monitor){return ((Lwjgl3Graphics.Lwjgl3Monitor)monitor).getMonitorHandle();}
    private Display describe(Graphics.Monitor monitor){
        long handle=monitorHandle(monitor);Graphics.DisplayMode mode=graphics().getDisplayMode(monitor);
        try(MemoryStack stack=MemoryStack.stackPush()){
            IntBuffer x=stack.mallocInt(1),y=stack.mallocInt(1),width=stack.mallocInt(1),height=stack.mallocInt(1);
            GLFW.glfwGetMonitorWorkarea(handle,x,y,width,height);
            Area bounds=new Area(monitor.virtualX,monitor.virtualY,mode.width,mode.height);
            Area work=width.get(0)>0&&height.get(0)>0?new Area(x.get(0),y.get(0),width.get(0),height.get(0)):bounds;
            return new Display(Long.toUnsignedString(handle),monitor.name,monitor,bounds,work,mode.refreshRate);
        }
    }
    @Override public Display display(){return describe(graphics().getMonitor());}
    @Override public Display findDisplay(String id){
        for(Graphics.Monitor monitor:graphics().getMonitors())if(Long.toUnsignedString(monitorHandle(monitor)).equals(id))return describe(monitor);
        return null;
    }
    @Override public Area window(){
        try(MemoryStack stack=MemoryStack.stackPush()){
            IntBuffer x=stack.mallocInt(1),y=stack.mallocInt(1),width=stack.mallocInt(1),height=stack.mallocInt(1);
            GLFW.glfwGetWindowPos(windowHandle(),x,y);GLFW.glfwGetWindowSize(windowHandle(),width,height);
            return new Area(x.get(0),y.get(0),width.get(0),height.get(0));
        }
    }
    @Override public Insets frameInsets(){
        try(MemoryStack stack=MemoryStack.stackPush()){
            IntBuffer left=stack.mallocInt(1),top=stack.mallocInt(1),right=stack.mallocInt(1),bottom=stack.mallocInt(1);
            GLFW.glfwGetWindowFrameSize(windowHandle(),left,top,right,bottom);
            return new Insets(Math.max(0,left.get(0)),Math.max(0,top.get(0)),Math.max(0,right.get(0)),Math.max(0,bottom.get(0)));
        }
    }
    @Override public boolean decorated(){return GLFW.glfwGetWindowAttrib(windowHandle(),GLFW.GLFW_DECORATED)==GLFW.GLFW_TRUE;}
    @Override public boolean resizable(){return GLFW.glfwGetWindowAttrib(windowHandle(),GLFW.GLFW_RESIZABLE)==GLFW.GLFW_TRUE;}
    @Override public void decorated(boolean value){graphics().setUndecorated(!value);}
    @Override public void resizable(boolean value){graphics().setResizable(value);}
    @Override public void position(int x,int y){graphics().getWindow().setPosition(x,y);}
}
