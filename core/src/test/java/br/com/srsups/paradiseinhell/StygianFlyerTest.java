package br.com.srsups.paradiseinhell;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StygianFlyerTest {
    private static double[] displacement(float x,float y,int fps) {
        EnemyNavigation.Body body=new EnemyNavigation.Body(x,y);StygianFlyer flyer=new StygianFlyer(x,y);
        for(int frame=0;frame<fps;frame++)flyer.move(body,x+308,y+8,1f/fps);
        return new double[]{body.x-x,body.y-y};
    }
    @Test void distinctOriginsVaryTheirLateralFlightWhileEachOriginRemainsReproducibleAcrossFrameRates() {
        double minimum=Double.POSITIVE_INFINITY,maximum=Double.NEGATIVE_INFINITY;
        for(float[] origin:new float[][]{{0,0},{32,0},{64,0},{0,32}}) {
            double[] sixty=displacement(origin[0],origin[1],60);
            assertArrayEquals(sixty,displacement(origin[0],origin[1],60),0,"The same origin must reproduce its path");
            assertArrayEquals(sixty,displacement(origin[0],origin[1],120),.02,"Frame rate must not select a different flight phase");
            minimum=Math.min(minimum,sixty[1]);maximum=Math.max(maximum,sixty[1]);
        }
        assertTrue(maximum-minimum>.25,"Different formation positions must not all share the same lateral drift phase");
    }
    @Test void pursuesAcrossInnerStructuresWithoutBurstingOrTeleporting() {
        EnemyNavigation.Body body=new EnemyNavigation.Body(8,8);StygianFlyer flyer=new StygianFlyer(body.x,body.y);
        flyer.bounds((x,y)->x>=0&&x<=180&&y>=0&&y<=100);
        boolean crossedWall=false;
        for(int frame=0;frame<240;frame++) {
            float beforeX=body.x,beforeY=body.y;flyer.move(body,144,16,1f/60);
            assertTrue(Math.hypot(body.x-beforeX,body.y-beforeY)<=StygianFlyer.SPEED/60+.0001);
            if(body.x+8>=48&&body.x+8<=64)crossedWall=true;
        }
        assertTrue(crossedWall,"The airborne actor must cross the inner wall band rather than follow ground navigation");
        assertEquals(144,body.x+8,.05);assertEquals(16,body.y+8,.05);
    }
    @Test void movingAndPushesRespectAllOuterBoundsEvenWhenTheTargetIsOutside() {
        EnemyNavigation.Body body=new EnemyNavigation.Body(40,40);StygianFlyer flyer=new StygianFlyer(body.x,body.y);
        flyer.bounds((x,y)->x>=0&&x<=100&&y>=0&&y<=100);
        for(int frame=0;frame<300;frame++) {
            flyer.move(body,1000,-1000,1f/60);
            assertTrue(flyer.canOccupy(body.x+8,body.y+8));
        }
        flyer.push(body,-1000,1000);assertTrue(flyer.canOccupy(body.x+8,body.y+8));
        assertTrue(body.x+8>=7&&body.x+8<=93);assertTrue(body.y+8>=7&&body.y+8<=93);
    }
    @Test void frameRateAndPausesDoNotChangeTheFlightPathOrJumpItsPosition() {
        EnemyNavigation.Body sixty=new EnemyNavigation.Body(-120,-64),oneTwenty=new EnemyNavigation.Body(-120,-64);
        StygianFlyer a=new StygianFlyer(sixty.x,sixty.y),b=new StygianFlyer(oneTwenty.x,oneTwenty.y);
        for(int frame=0;frame<180;frame++)a.move(sixty,160,80,1f/60);
        for(int frame=0;frame<360;frame++)b.move(oneTwenty,160,80,1f/120);
        assertEquals(sixty.x,oneTwenty.x,.03);assertEquals(sixty.y,oneTwenty.y,.03);
        float x=sixty.x,y=sixty.y;a.move(sixty,-1000,1000,0);assertEquals(x,sixty.x);assertEquals(y,sixty.y);
    }
    @Test void flyerIsAWeakDistinctRoleWithTwoXpAndSoulsAndMovementBelowAllBaseProfessions() {
        assertEquals(8,StygianFlyer.HEALTH);assertEquals(5,StygianFlyer.CONTACT_DAMAGE);
        assertEquals(2,StygianFlyer.XP);assertEquals(2,StygianFlyer.SOULS);assertEquals(.8f,StygianFlyer.STRENGTH);
        for(Profession profession:Profession.values())assertTrue(StygianFlyer.SPEED<profession.baseSpeed);
        assertThrows(IllegalArgumentException.class,()->new StygianFlyer(Float.NaN,0));
        StygianFlyer flyer=new StygianFlyer(0,0);EnemyNavigation.Body body=new EnemyNavigation.Body(0,0);
        assertThrows(IllegalArgumentException.class,()->flyer.move(body,0,0,-1));
        assertThrows(IllegalArgumentException.class,()->flyer.move(body,Float.POSITIVE_INFINITY,0,0));
    }
    @Test void mapBoundaryQueryIgnoresInnerTerrainAndHonorsAnExplicitArenaRectangle() {
        TileMap map=new TileMap(19);
        assertTrue(map.insideBounds(-10000,10000));
        assertFalse(map.insideBounds(Float.NaN,0));
        map.setBounds(-16,-32,100,80);
        assertTrue(map.insideBounds(0,0));assertFalse(map.insideBounds(-17,0));assertFalse(map.insideBounds(0,49));
    }
}
