package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.graphics.g2d.*;

/** Shared rotation for effects/projectiles. Equipment icons are used only by the forge. */
public final class WeaponVisuals {
    private WeaponVisuals(){}
    public static String key(WeaponType weapon){return weapon.name().toLowerCase(java.util.Locale.ROOT);}
    public static void sprite(SpriteBatch batch,TextureRegion image,float x,float y,float w,float h,float angle){
        batch.draw(image,x-w/2,y-h/2,w/2,h/2,w,h,1,1,angle);
    }
    public static void arrows(SpriteBatch batch,PilotArt art,Inimigo enemy){
        if(!enemy.isArcher())return;
        for(ArcherAttack.Arrow arrow:enemy.archer().arrows())
            sprite(batch,art.region("arrow-0"),arrow.x(),arrow.y(),4,14,
                (float)Math.toDegrees(Math.atan2(arrow.directionY(),arrow.directionX()))-90);
    }
}
