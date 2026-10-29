package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class TileMap {
    public static final int MAX_CACHED_TILES=8192;
    private final Map<Long,Tile> tiles=new LinkedHashMap<>(1024,.75f,true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Long,Tile> eldest){return size()>MAX_CACHED_TILES;}
    };
    private final TextureRegion[] floors;
    private final TextureRegion obstacle;
    private final SceneryArt scenery;
    private final SceneryTheme theme;
    private final long seed;
    private int safeX, safeY, safeRadius;
    private int left, right, bottom, top;
    private Rectangle bounds;

    public TileMap(Texture atlas) {
        this(atlas,null,SceneryTheme.STYX,new Random().nextLong());
    }
    public TileMap(Texture atlas,SceneryArt scenery,SceneryTheme theme) {
        this(atlas,scenery,theme,new Random().nextLong());
    }
    public TileMap(Texture atlas,SceneryArt scenery,SceneryTheme theme,long seed) {
        this.scenery=scenery;this.theme=java.util.Objects.requireNonNull(theme);this.seed=seed;
        floors=atlas==null?null:new TextureRegion[]{Art.cell(atlas,0,2),Art.cell(atlas,1,2),Art.cell(atlas,2,2)};
        obstacle=atlas==null?null:Art.cell(atlas,3,2);
    }
    /** Logical map used by headless navigation and cache checks; drawing requires borrowed art. */
    TileMap(long seed){this(null,null,SceneryTheme.STYX,seed);}
    public SceneryTheme theme(){return theme;}
    public int cachedTileCount(){return tiles.size();}
    private static long key(int x,int y){return ((long)x<<32)^(y&0xffffffffL);}
    private boolean solid(int x,int y) {
        boolean safe=Math.abs((long)x-safeX)<=safeRadius && Math.abs((long)y-safeY)<=safeRadius;
        return !safe && TileLayout.solid(seed,x,y);
    }
    private TextureRegion floor(int x,int y) {
        if(scenery!=null)return scenery.floor(theme,theme.floorVariant(seed,x,y));
        if(floors==null)return null;
        // Keep the legacy constructor's three floor choices, independently of the themed palette.
        return floors[new Random(seed^(x*73856093L)^(y*19349663L)).nextInt(3)];
    }
    private Tile tileAt(int x, int y) {
        return tiles.computeIfAbsent(key(x,y),ignored->{
            boolean solid=solid(x,y);
            TextureRegion region;
            if(solid)region=scenery==null?obstacle:scenery.obstacle(theme,theme.obstacleVariant(seed,x,y));
            else region=floor(x,y);
            return new Tile(x,y,region,solid);
        });
    }
    public boolean ehSolido(float x, float y) {
        if (bounds != null && !bounds.contains(x, y)) return true;
        return solid((int)Math.floor(x/Tile.TILE_SIZE),(int)Math.floor(y/Tile.TILE_SIZE));
    }
    public void setBounds(float x, float y, float width, float height) {
        bounds = new Rectangle(x, y, width, height);
    }
    public void update(Camera camera) {
        float zoom=camera instanceof OrthographicCamera orthographic?orthographic.zoom:1;
        float halfWidth=camera.viewportWidth*zoom/2,halfHeight=camera.viewportHeight*zoom/2;
        left=(int)Math.floor((camera.position.x-halfWidth)/Tile.TILE_SIZE)-2;
        right=(int)Math.floor((camera.position.x+halfWidth)/Tile.TILE_SIZE)+2;
        bottom=(int)Math.floor((camera.position.y-halfHeight)/Tile.TILE_SIZE)-2;
        top=(int)Math.floor((camera.position.y+halfHeight)/Tile.TILE_SIZE)+2;
        tiles.values().removeIf(tile->tile.gridX<left||tile.gridX>right||tile.gridY<bottom||tile.gridY>top);
        for (int x = left; x <= right; x++) for (int y = bottom; y <= top; y++) tileAt(x, y);
    }
    public void draw(SpriteBatch batch) {
        if(scenery==null && floors==null)throw new IllegalStateException("A logical map has no scenery to draw");
        for (int x = left; x <= right; x++) for (int y = bottom; y <= top; y++) {
            // Obstacles sit over a floor tile so their transparent corners are filled.
            Tile tile = tileAt(x, y);
            if(tile.solido)batch.draw(scenery==null?floors[0]:floor(x,y),x*Tile.TILE_SIZE,y*Tile.TILE_SIZE,Tile.TILE_SIZE,Tile.TILE_SIZE);
            tile.draw(batch);
        }
    }
    public void gerarAreaInicialSegura(float x, float y, int radius) {
        safeX = (int) Math.floor(x / Tile.TILE_SIZE);
        safeY = (int) Math.floor(y / Tile.TILE_SIZE);
        safeRadius = radius;
        tiles.clear();
    }
    public void dispose() { tiles.clear(); }
}
