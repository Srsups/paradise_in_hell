package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import java.util.Objects;

/** Reuses matrices and borrows rendering resources. Call only between complete drawing passes. */
public final class HudScaleRenderer {
    private final Matrix4 savedBatch=new Matrix4(),savedShapes=new Matrix4();
    private final Matrix4 scaledBatch=new Matrix4(),scaledShapes=new Matrix4();
    private SpriteBatch batch;
    private ShapeRenderer shapes;

    public boolean active(){return batch!=null;}
    public void begin(SpriteBatch batch,ShapeRenderer shapes,HudSettings settings,HudLayout.Anchor anchor) {
        Objects.requireNonNull(batch);Objects.requireNonNull(shapes);Objects.requireNonNull(settings);Objects.requireNonNull(anchor);
        if(active())throw new IllegalStateException("HUD scale pass already active");
        if(batch.isDrawing()||shapes.isDrawing())throw new IllegalStateException("Finish drawing before changing HUD scale");
        savedBatch.set(batch.getTransformMatrix());savedShapes.set(shapes.getTransformMatrix());
        float x=HudLayout.anchorX(anchor),y=HudLayout.anchorY(),scale=settings.scale();
        scaledBatch.set(savedBatch).translate(x,y,0).scale(scale,scale,1).translate(-x,-y,0);
        scaledShapes.set(savedShapes).translate(x,y,0).scale(scale,scale,1).translate(-x,-y,0);
        batch.setTransformMatrix(scaledBatch);
        try {shapes.setTransformMatrix(scaledShapes);}
        catch(RuntimeException failure){batch.setTransformMatrix(savedBatch);throw failure;}
        this.batch=batch;this.shapes=shapes;
    }
    /** Restores previous transforms; projection matrices, cameras and viewports were never changed. */
    public void end() {
        if(!active())return;
        if(batch.isDrawing()||shapes.isDrawing())throw new IllegalStateException("Finish HUD drawing before restoring scale");
        try {batch.setTransformMatrix(savedBatch);}
        finally {try{shapes.setTransformMatrix(savedShapes);}finally{batch=null;shapes=null;}}
    }
}
