package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class EnemySpawner {
    private static final Random RANDOM = new Random();
    private EnemySpawner() {}
    public record SpawnPoint(float x,float y) {}
    /** A fixed offscreen approach lane shared by every batch of one horde. */
    public static final class GroupFormation {
        private final int side;
        private final float anchorX,anchorY;
        private int nextIndex;
        private GroupFormation(OrthographicCamera camera,long seed) {
            Random random=new Random(seed);side=random.nextInt(4);
            float halfWidth=camera.viewportWidth*camera.zoom/2,halfHeight=camera.viewportHeight*camera.zoom/2;
            float left=camera.position.x-halfWidth,right=camera.position.x+halfWidth;
            float bottom=camera.position.y-halfHeight,top=camera.position.y+halfHeight;
            float along=.2f+random.nextFloat()*.6f;
            anchorX=switch(side){case 0->left-48;case 1->right+32;default->left+along*(right-left);};
            anchorY=switch(side){case 2->bottom-48;case 3->top+32;default->bottom+along*(top-bottom);};
        }
        /** Consume each position once, even when the camera has moved over the original approach lane. */
        public List<SpawnPoint> nextPoints(OrthographicCamera camera,int count) {
            if(count<0||count>256)throw new IllegalArgumentException("Invalid formation batch");
            List<SpawnPoint> points=new ArrayList<>(count);
            for(int unit=0;unit<count;unit++) {
                int index=nextIndex++,column=index%6,row=index/6;
                float along=(column-2.5f)*18,depth=row*16;
                float x=anchorX+(side<2?(side==0?-depth:depth):along);
                float y=anchorY+(side<2?along:(side==2?-depth:depth));
                if(outside(camera,x+8,y+8,StygianFlyer.SIZE/2+5))points.add(new SpawnPoint(x,y));
            }
            return List.copyOf(points);
        }
        public List<Inimigo> spawnBatch(OrthographicCamera camera,Texture atlas,TileMap map,EnemyType type,
                                       EnemyNavigation navigation,PilotArt pilot,int count,boolean flyer) {
            if(flyer&&type!=EnemyType.COMMON)throw new IllegalArgumentException("Flyers use the common goal group");
            List<Inimigo> result=new ArrayList<>();
            for(SpawnPoint point:nextPoints(camera,count)) {
                // Keep the compact lane. An unavailable position is skipped, never scattered onto another edge.
                if(!valid(map,navigation,point.x,point.y,flyer))continue;
                Inimigo enemy=new Inimigo(point.x,point.y,atlas,pilot);
                if(flyer){enemy.configureFlyer();enemy.flightBounds(map::insideBounds);}else enemy.configurarTipo(type);
                result.add(enemy);
            }
            return result;
        }
    }
    public static GroupFormation formation(OrthographicCamera camera,long seed){return new GroupFormation(camera,seed);}
    public static Inimigo spawnFlyerForaDaTela(OrthographicCamera camera,Texture atlas,TileMap map,
                                             EnemyNavigation navigation,PilotArt pilot) {
        for(int attempt=0;attempt<8;attempt++){
            List<Inimigo> found=formation(camera,RANDOM.nextLong()).spawnBatch(camera,atlas,map,EnemyType.COMMON,navigation,pilot,1,true);
            if(!found.isEmpty())return found.getFirst();
        }
        return null;
    }
    private static boolean outside(OrthographicCamera camera,float centerX,float centerY,float radius) {
        float halfWidth=camera.viewportWidth*camera.zoom/2,halfHeight=camera.viewportHeight*camera.zoom/2;
        return centerX+radius<camera.position.x-halfWidth||centerX-radius>camera.position.x+halfWidth
            ||centerY+radius<camera.position.y-halfHeight||centerY-radius>camera.position.y+halfHeight;
    }
    private static boolean valid(TileMap map,EnemyNavigation navigation,float x,float y,boolean flyer) {
        if(flyer)return map.insideBounds(x+1,y+1)&&map.insideBounds(x+15,y+1)
            &&map.insideBounds(x+1,y+15)&&map.insideBounds(x+15,y+15);
        return !map.ehSolido(x+1,y+1)&&!map.ehSolido(x+15,y+1)&&!map.ehSolido(x+1,y+15)&&!map.ehSolido(x+15,y+15)
            &&navigation.reachable(x+8,y+8);
    }
    public static Inimigo spawnInimigoForaDaTela(OrthographicCamera camera, Texture atlas, TileMap map,
                                               EnemyType type, EnemyNavigation navigation) {
        return spawnInimigoForaDaTela(camera,atlas,map,type,navigation,null);
    }
    public static Inimigo spawnInimigoForaDaTela(OrthographicCamera camera,Texture atlas,TileMap map,
                                               EnemyType type,EnemyNavigation navigation,PilotArt pilot) {
        float width=camera.viewportWidth*camera.zoom,height=camera.viewportHeight*camera.zoom;
        float left = camera.position.x - width / 2;
        float bottom = camera.position.y - height / 2;
        for (int attempt = 0; attempt < 24; attempt++) {
            float x = left + RANDOM.nextFloat() * width;
            float y = bottom + RANDOM.nextFloat() * height;
            switch (RANDOM.nextInt(4)) {
                case 0 -> y = bottom + height + 24;
                case 1 -> y = bottom - 24;
                case 2 -> x = left - 24;
                case 3 -> x = left + width + 24;
            }
            if (!valid(map,navigation,x,y,false)) continue;
            Inimigo enemy = new Inimigo(x, y, atlas,pilot);
            enemy.configurarTipo(type);
            return enemy;
        }
        return null;
    }
}
