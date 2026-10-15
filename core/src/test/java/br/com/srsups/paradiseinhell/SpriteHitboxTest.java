package br.com.srsups.paradiseinhell;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SpriteHitboxTest {
    private static SpriteHitbox box(float x, float size) {
        return new SpriteHitbox(SpriteHitbox.Shape.fromAlpha(64,64,(px,py) ->
            px >= 16 && px < 48 && py >= 8 && py < 56 ? 255 : 0)).place(x,0,size,size,false,false);
    }
    @Test void transparentMarginsDoNotDealDamageButVisibleBodiesDo() {
        SpriteHitbox a = box(0,16), b = box(10,16);
        assertFalse(a.overlaps(b), "The old 16x16 boxes overlap while the visible bodies are separated");
        b.place(7,0,16,16,false,false);
        assertTrue(a.overlaps(b));
    }
    @Test void scalesAndFlipsUseTheActualDrawRectangleAndFrame() {
        SpriteHitbox.Shape shape = SpriteHitbox.Shape.fromAlpha(4,4,(x,y) -> x == 0 && y == 0 ? 255 : 0);
        SpriteHitbox body = new SpriteHitbox(shape).place(100,50,32,32,false,false);
        assertEquals(100,body.left()); assertEquals(108,body.right());
        assertEquals(74,body.bottom()); assertEquals(82,body.top());
        assertTrue(body.contains(104,78));
        body.place(100,50,32,32,true,true);
        assertEquals(124,body.left()); assertEquals(132,body.right());
        assertEquals(50,body.bottom()); assertEquals(58,body.top());
        assertTrue(body.contains(128,54)); assertFalse(body.contains(104,78));
    }
    @Test void transparentHolesAndFaintShadowsRemainSafe() {
        SpriteHitbox frame = new SpriteHitbox(SpriteHitbox.Shape.fromAlpha(64,64,(x,y) ->
            x < 8 || x >= 56 || y < 8 || y >= 56 ? 255 : 50)).place(0,0,16,16,false,false);
        SpriteHitbox inside = box(5,4).place(5,5,4,4,false,false);
        assertFalse(frame.overlaps(inside), "Bounds overlap but the opaque pixels do not");
        SpriteHitbox empty = new SpriteHitbox(SpriteHitbox.Shape.fromAlpha(4,4,(x,y)->0)).place(0,0,16,16,false,false);
        assertFalse(empty.overlaps(frame));
    }
    @Test void fastProjectilesHitVisiblePixelsAndPassThroughTransparentPadding() {
        SpriteHitbox target = box(0,16);
        assertTrue(target.sweptHits(-20,8,30,8,1));
        assertFalse(target.sweptHits(-20,.5f,30,.5f,1));
        assertFalse(target.sweptHits(-20,8,2,8,1));
        target.place(100,50,21,21,false,false);
        assertTrue(target.sweptHits(90,60,140,60,1));
    }
}

