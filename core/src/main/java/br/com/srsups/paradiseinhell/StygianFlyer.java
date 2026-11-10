package br.com.srsups.paradiseinhell;

/** Slow airborne pursuit. Inner obstacles are ignored; the map boundary is still swept. */
public final class StygianFlyer {
    public static final String NAME="Morcego do Estige";
    public static final float HEALTH=8,SPEED=42,CONTACT_DAMAGE=5,SIZE=22,STRENGTH=.8f;
    public static final int XP=2,SOULS=2;
    private static final float HALF_BODY=7,STEP=1f/120;
    @FunctionalInterface public interface Bounds {boolean inside(float x,float y);}
    private Bounds bounds=(x,y)->true;
    private final float phase;
    private float time,velocityX,velocityY;
    public StygianFlyer(float x,float y) {
        if(!Float.isFinite(x)||!Float.isFinite(y))throw new IllegalArgumentException("Invalid flyer origin");
        // Mix the actual float coordinates before sampling: low double mantissa bits are usually zero here.
        long origin=((long)Float.floatToIntBits(x)<<32)^(Float.floatToIntBits(y)&0xffffffffL);
        origin=(origin^(origin>>>30))*0xbf58476d1ce4e5b9L;
        origin=(origin^(origin>>>27))*0x94d049bb133111ebL;
        origin^=origin>>>31;
        phase=(float)((origin&65535)/65536d*Math.PI*2);
    }
    public void bounds(Bounds value){bounds=java.util.Objects.requireNonNull(value);}
    public void move(EnemyNavigation.Body body,float targetX,float targetY,float delta) {
        if(!Float.isFinite(delta)||delta<0||!Float.isFinite(targetX)||!Float.isFinite(targetY))
            throw new IllegalArgumentException("Invalid flight update");
        float remaining=delta;
        while(remaining>0){float step=Math.min(STEP,remaining);advance(body,targetX,targetY,step);remaining-=step;}
    }
    private void advance(EnemyNavigation.Body body,float targetX,float targetY,float delta) {
        time+=delta;
        float x=body.x+8,y=body.y+8,dx=targetX-x,dy=targetY-y;
        float distance=(float)Math.hypot(dx,dy);
        if(distance<.025f){velocityX=velocityY=0;return;}
        float ux=dx/distance,uy=dy/distance;
        float drift=(float)Math.sin(time*2.1f+phase)*.14f*Math.min(1,distance/64);
        float desiredX=ux-uy*drift,desiredY=uy+ux*drift,length=(float)Math.hypot(desiredX,desiredY);
        float blend=1-(float)Math.exp(-10*delta);
        velocityX+=(desiredX/length*SPEED-velocityX)*blend;
        velocityY+=(desiredY/length*SPEED-velocityY)*blend;
        float moveX=velocityX*delta,moveY=velocityY*delta,travel=(float)Math.hypot(moveX,moveY);
        if(travel>distance){moveX*=distance/travel;moveY*=distance/travel;}
        if(canOccupy(x+moveX,y+moveY)){body.x+=moveX;body.y+=moveY;}
        else {
            // Slide along a boundary instead of cutting a corner or teleporting to a clamped point.
            if(canOccupy(x+moveX,y))body.x+=moveX;else velocityX=0;
            if(canOccupy(body.x+8,y+moveY))body.y+=moveY;else velocityY=0;
        }
    }
    public void push(EnemyNavigation.Body body,float dx,float dy) {
        if(!Float.isFinite(dx)||!Float.isFinite(dy))throw new IllegalArgumentException("Invalid flight push");
        int steps=Math.max(1,(int)Math.ceil(Math.hypot(dx,dy)/2));
        for(int i=0;i<steps;i++){
            float nextX=body.x+8+dx/steps,nextY=body.y+8+dy/steps;
            if(!canOccupy(nextX,nextY))break;
            body.x=nextX-8;body.y=nextY-8;
        }
    }
    public boolean canOccupy(float x,float y){return bounds.inside(x-HALF_BODY,y-HALF_BODY)
        &&bounds.inside(x+HALF_BODY,y-HALF_BODY)&&bounds.inside(x-HALF_BODY,y+HALF_BODY)&&bounds.inside(x+HALF_BODY,y+HALF_BODY);}
}
