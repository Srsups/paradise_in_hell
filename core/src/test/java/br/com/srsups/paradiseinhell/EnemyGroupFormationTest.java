package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.graphics.OrthographicCamera;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EnemyGroupFormationTest {
    private static OrthographicCamera camera(float x,float y,float zoom) {
        OrthographicCamera camera=new OrthographicCamera();camera.viewportWidth=320;camera.viewportHeight=180;
        camera.position.set(x,y,0);camera.zoom=zoom;return camera;
    }
    private static boolean offscreen(OrthographicCamera camera,EnemySpawner.SpawnPoint point) {
        float radius=StygianFlyer.SIZE/2+5,cx=point.x()+8,cy=point.y()+8;
        return cx+radius<camera.position.x-camera.viewportWidth*camera.zoom/2
            ||cx-radius>camera.position.x+camera.viewportWidth*camera.zoom/2
            ||cy+radius<camera.position.y-camera.viewportHeight*camera.zoom/2
            ||cy-radius>camera.position.y+camera.viewportHeight*camera.zoom/2;
    }
    @Test void fixedSeedKeepsOneCompactApproachLaneAcrossMultipleBatchesAndEightViews() {
        for(int seed=0;seed<64;seed++) {
            OrthographicCamera camera=camera(-50,80,seed%2==0?1:1.5f);
            var first=EnemySpawner.formation(camera,seed);
            var repeat=EnemySpawner.formation(camera,seed);
            var a=first.nextPoints(camera,6);
            var b=first.nextPoints(camera,12);
            assertEquals(a,repeat.nextPoints(camera,6));assertEquals(b,repeat.nextPoints(camera,12));
            var unique=new HashSet<>(a);unique.addAll(b);assertEquals(18,unique.size());
            assertEquals(6,a.size());assertEquals(12,b.size());
            for(var point:unique)assertTrue(offscreen(camera,point));
            double width=unique.stream().mapToDouble(EnemySpawner.SpawnPoint::x).max().orElseThrow()-unique.stream().mapToDouble(EnemySpawner.SpawnPoint::x).min().orElseThrow();
            double height=unique.stream().mapToDouble(EnemySpawner.SpawnPoint::y).max().orElseThrow()-unique.stream().mapToDouble(EnemySpawner.SpawnPoint::y).min().orElseThrow();
            assertTrue(width<=90.001&&height<=90.001,"The horde must not scatter among different camera edges");
        }
    }
    @Test void aCameraOverThePlannedLaneSkipsPositionsRatherThanSpawningOnScreenOrDeferringThem() {
        OrthographicCamera camera=camera(0,0,1);var formation=EnemySpawner.formation(camera,5);
        var initial=formation.nextPoints(camera,1).getFirst();
        camera.position.set(initial.x()+8,initial.y()+8,0);camera.zoom=4;
        assertTrue(formation.nextPoints(camera,12).isEmpty());
        camera.position.set(0,0,0);camera.zoom=1;
        var later=formation.nextPoints(camera,1);assertEquals(1,later.size());assertNotEquals(initial,later.getFirst());
        assertThrows(IllegalArgumentException.class,()->formation.nextPoints(camera,-1));
    }
}
