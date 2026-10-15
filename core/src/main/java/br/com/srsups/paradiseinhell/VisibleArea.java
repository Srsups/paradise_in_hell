package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.Viewport;

/** Sprite bounds intersect the visible world camera, independent of screen pixels/letterboxing. */
public final class VisibleArea {
    private VisibleArea() {}
    public static boolean sprite(Viewport viewport,SpriteHitbox sprite) {
        if(viewport.getScreenWidth()<=0 || viewport.getScreenHeight()<=0)return false;
        OrthographicCamera camera=(OrthographicCamera)viewport.getCamera();
        float halfWidth=camera.viewportWidth*camera.zoom/2,halfHeight=camera.viewportHeight*camera.zoom/2;
        return sprite.visibleWithin(camera.position.x-halfWidth,camera.position.y-halfHeight,
            camera.position.x+halfWidth,camera.position.y+halfHeight);
    }
    public static boolean sprite(Viewport viewport,float x,float y,float width,float height) {
        OrthographicCamera camera=(OrthographicCamera)viewport.getCamera();
        if(viewport.getScreenWidth()<=0 || viewport.getScreenHeight()<=0) return false;
        float halfWidth=camera.viewportWidth*camera.zoom/2,halfHeight=camera.viewportHeight*camera.zoom/2;
        return intersects(camera.position.x,camera.position.y,halfWidth*2,halfHeight*2,x,y,width,height);
    }
    public static boolean intersects(float centerX,float centerY,float viewWidth,float viewHeight,
                                     float x,float y,float width,float height) {
        if(viewWidth<=0 || viewHeight<=0 || width<=0 || height<=0)return false;
        return x+width>centerX-viewWidth/2 && x<centerX+viewWidth/2
            && y+height>centerY-viewHeight/2 && y<centerY+viewHeight/2;
    }
}

