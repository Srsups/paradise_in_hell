package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Shared scenery textures. Maps and screens borrow regions; PilotArt owns their lifetime. */
public final class SceneryArt implements Disposable {
    public static final int TILE_PIXELS=64,FLOOR_VARIANTS=6,OBSTACLE_VARIANTS=3,PASSAGE_FRAMES=4;
    private final TextureAtlas atlas=new TextureAtlas(Gdx.files.internal("visual/m6-scenery.atlas"));
    private final Map<SceneryTheme,TextureRegion[]> floors=new EnumMap<>(SceneryTheme.class);
    private final Map<SceneryTheme,TextureRegion[]> obstacles=new EnumMap<>(SceneryTheme.class);
    private final Map<SceneryTheme,TextureRegion> previews=new EnumMap<>(SceneryTheme.class);
    private final TextureRegion[] passage;
    private boolean disposed;

    public SceneryArt() {
        for(SceneryTheme theme:SceneryTheme.values()) {
            floors.put(theme,read(theme.key()+"-floor",FLOOR_VARIANTS));
            obstacles.put(theme,read(theme.key()+"-obstacle",OBSTACLE_VARIANTS));
        }
        for(SceneryTheme theme:new SceneryTheme[]{SceneryTheme.ASHES,SceneryTheme.SHADOWS,SceneryTheme.GUARDIAN})
            previews.put(theme,required("route-"+theme.key()));
        passage=read("passage",PASSAGE_FRAMES);
    }
    private TextureRegion[] read(String key,int count) {
        TextureRegion[] result=new TextureRegion[count];
        for(int i=0;i<count;i++)result[i]=required(key+"-"+i);
        return result;
    }
    private TextureRegion required(String name) {
        TextureRegion region=atlas.findRegion(name);
        if(region==null)throw new IllegalStateException("Missing scenery sprite "+name);
        return region;
    }
    private TextureRegion frame(Map<SceneryTheme,TextureRegion[]> source,SceneryTheme theme,int index,int count) {
        ensureOpen();Objects.requireNonNull(theme);
        if(index<0||index>=count)throw new IllegalArgumentException("Invalid scenery variant");
        return source.get(theme)[index];
    }
    public TextureRegion floor(SceneryTheme theme,int index){return frame(floors,theme,index,FLOOR_VARIANTS);}
    public TextureRegion obstacle(SceneryTheme theme,int index){return frame(obstacles,theme,index,OBSTACLE_VARIANTS);}
    public TextureRegion preview(SceneryTheme theme) {
        ensureOpen();Objects.requireNonNull(theme);
        return previews.getOrDefault(theme,floors.get(theme)[0]);
    }
    public TextureRegion passage(float progress) {
        ensureOpen();
        if(!Float.isFinite(progress))throw new IllegalArgumentException("Invalid passage progress");
        return passage[Math.max(0,Math.min(PASSAGE_FRAMES-1,(int)(progress*PASSAGE_FRAMES)))];
    }
    public boolean disposed(){return disposed;}
    private void ensureOpen(){if(disposed)throw new IllegalStateException("Disposed scenery");}
    @Override public void dispose() {
        if(disposed)return;
        disposed=true;atlas.dispose();floors.clear();obstacles.clear();previews.clear();
    }
}
