package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.graphics.OrthographicCamera;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SceneryMapTest {
    private static OrthographicCamera camera(float x,float y,float zoom) {
        OrthographicCamera camera=new OrthographicCamera();
        camera.viewportWidth=480;camera.viewportHeight=270;camera.zoom=zoom;camera.position.set(x,y,0);
        return camera;
    }
    private static boolean originalSolid(long seed,int x,int y) {
        Random random=new Random(seed^(x*73856093L)^(y*19349663L));
        random.nextInt(3);
        return random.nextFloat()<.012f;
    }

    @Test void everyThemeKeepsTheLegacyGeometryAndVisualQueriesCannotChangeIt() {
        for(long seed:new long[]{0,1,-1,Long.MIN_VALUE,18397462})for(SceneryTheme theme:SceneryTheme.values()) {
            TileMap map=new TileMap(null,null,theme,seed);
            assertSame(theme,map.theme());
            map.gerarAreaInicialSegura(8,8,0);
            for(int x=-45;x<=45;x++)for(int y=-35;y<=35;y++) {
                theme.floorVariant(seed,x,y);theme.obstacleVariant(seed,x,y);theme.variant(seed,x,y,4);
                boolean expected=(x!=0||y!=0)&&originalSolid(seed,x,y);
                assertEquals(expected,map.ehSolido(x*16+8,y*16+8),theme+" at "+x+","+y);
            }
            assertEquals(0,map.cachedTileCount(),"Physics and visual hashes must not allocate cached tiles");
        }
    }

    @Test void safeAreaUsesTheOriginalSquareInNegativeWorldCoordinatesAndSurvivesEviction() {
        TileMap map=new TileMap(62);
        map.gerarAreaInicialSegura(-17,-33,5);
        for(int x=-7;x<=3;x++)for(int y=-8;y<=2;y++)assertFalse(map.ehSolido(x*16+8,y*16+8));
        OrthographicCamera camera=camera(-24,-40,1);map.update(camera);
        camera.position.set(50000,-50000,0);map.update(camera);
        camera.position.set(-24,-40,0);map.update(camera);
        for(int x=-7;x<=3;x++)for(int y=-8;y<=2;y++)assertFalse(map.ehSolido(x*16+8,y*16+8));
        map.gerarAreaInicialSegura(10000,10000,0);
        assertEquals(0,map.cachedTileCount(),"Changing the safe area clears cached appearance");
        for(int x=-7;x<=3;x++)for(int y=-8;y<=2;y++)
            assertEquals(originalSolid(62,x,y),map.ehSolido(x*16+8,y*16+8),"The old safe square must no longer override geometry");
    }

    @Test void collisionQueriesStayUncachedAndBoundedArenaEdgesKeepTheirCoordinates() {
        TileMap map=new TileMap(9);
        map.gerarAreaInicialSegura(240,135,64);map.setBounds(24,58,432,160);
        for(int x=-250;x<=250;x++)for(int y=-50;y<=50;y++)map.ehSolido(x*16+8,y*16+8);
        assertEquals(0,map.cachedTileCount());
        assertFalse(map.ehSolido(24,58));assertFalse(map.ehSolido(456,218));
        assertFalse(map.ehSolido(240,135));
        assertTrue(map.ehSolido(23.99f,58));assertTrue(map.ehSolido(456.01f,218));
        assertTrue(map.ehSolido(24,57.99f));assertTrue(map.ehSolido(456,218.01f));
        map.setBounds(BossEncounter.MIN_X,BossEncounter.MIN_Y,
            BossEncounter.MAX_X-BossEncounter.MIN_X,BossEncounter.MAX_Y-BossEncounter.MIN_Y);
        assertFalse(map.ehSolido(16,16));assertFalse(map.ehSolido(464,224));assertTrue(map.ehSolido(464.01f,224));
        assertEquals(0,map.cachedTileCount());
    }

    @Test void movingAcrossTheInfiniteWorldDoesNotAccumulateVisualTiles() {
        TileMap map=new TileMap(137);OrthographicCamera camera=camera(0,0,1);
        map.update(camera);int visible=map.cachedTileCount();assertEquals(770,visible);
        for(int i=1;i<=80;i++) {
            camera.position.set(i*4096,-i*4096,0);map.update(camera);
            assertEquals(visible,map.cachedTileCount(),"Only the current camera rectangle and two-tile margin are retained");
        }
        camera.position.set(0,0,0);map.update(camera);
        assertEquals(visible,map.cachedTileCount());map.dispose();assertEquals(0,map.cachedTileCount());
    }

    @Test void zoomAndViewportResizePopulateTheirVisibleAreaWhileKeepingAHardLimit() {
        TileMap map=new TileMap(321);OrthographicCamera camera=camera(0,0,1);
        map.update(camera);int base=map.cachedTileCount();
        camera.zoom=2;map.update(camera);assertEquals(2470,map.cachedTileCount());
        camera.viewportWidth=640;camera.viewportHeight=360;map.update(camera);
        assertEquals(4250,map.cachedTileCount());
        camera.zoom=8;map.update(camera);assertEquals(TileMap.MAX_CACHED_TILES,map.cachedTileCount());
        camera.zoom=1;camera.viewportWidth=480;camera.viewportHeight=270;map.update(camera);
        assertEquals(base,map.cachedTileCount(),"Zooming back in evicts larger-view tiles immediately");
    }

    @Test void revisitingEvictedCoordinatesRecreatesTheSameCollisionForNavigation() {
        TileMap map=new TileMap(829174);OrthographicCamera camera=camera(0,0,1);
        map.gerarAreaInicialSegura(8,8,5);map.update(camera);
        boolean[] before=new boolean[81*81];int index=0;
        for(int y=-40;y<=40;y++)for(int x=-40;x<=40;x++)before[index++]=map.ehSolido(x*16+8,y*16+8);
        EnemyNavigation nav=new EnemyNavigation(map::ehSolido);nav.update(0,8,8);
        assertTrue(nav.reachable(8,8));assertTrue(nav.canOccupy(8,8));
        camera.position.set(-100000,100000,0);map.update(camera);
        camera.position.set(0,0,0);map.update(camera);index=0;
        for(int y=-40;y<=40;y++)for(int x=-40;x<=40;x++)assertEquals(before[index++],map.ehSolido(x*16+8,y*16+8));
        assertEquals(770,map.cachedTileCount(),"Navigation queries cannot expand the renderer cache");
    }
}
