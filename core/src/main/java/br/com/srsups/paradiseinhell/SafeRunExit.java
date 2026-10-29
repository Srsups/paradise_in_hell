package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/** Shared safe-point action, armed only after incoming attack and shortcut inputs are released. */
final class SafeRunExit {
    private final Main game;
    private final RunSession run;
    private final RunCheckpoint.Stage stage;
    private final Rectangle button;
    private float delay=.2f;
    private boolean released;

    SafeRunExit(Main game,RunSession run,RunCheckpoint.Stage stage,Rectangle button) {
        this.game=game;this.run=run;this.stage=stage;this.button=button;
    }
    boolean visible(){return run!=null&&!run.isDemo()&&run.attemptSequence()>0;}
    private boolean shortcutAvailable() {
        InputBinding shortcut=InputBinding.key(Input.Keys.F5);
        for(ControlAction action:ControlAction.values())
            if(game.controlSettings().value().bindings(action).contains(shortcut))return false;
        return true;
    }
    boolean input(float delta,Vector2 pointer) {
        if(!visible())return false;
        delay=Math.max(0,delay-Math.max(0,delta));
        boolean shortcut=shortcutAvailable();
        released|=!Gdx.input.isButtonPressed(Input.Buttons.LEFT)&&(!shortcut||!Gdx.input.isKeyPressed(Input.Keys.F5))
            &&!game.controls().physicallyPressed(ControlAction.ATTACK);
        if(delay>0||!released)return false;
        if(shortcut&&Gdx.input.isKeyJustPressed(Input.Keys.F5)
            ||Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)&&button.contains(pointer)) {
            game.saveAndExit(run,stage);return true;
        }
        return false;
    }
    void draw(SpriteBatch batch,ShapeRenderer shapes,BitmapFont font,Vector2 pointer) {
        if(!visible())return;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        UiTheme.panel(shapes,button.x,button.y,button.width,button.height,button.contains(pointer));
        shapes.end();batch.begin();font.getData().setScale(.85f);font.setColor(UiTheme.IVORY);
        GlyphLayout label=new GlyphLayout(font,shortcutAvailable()?"Salvar e sair [F5]":"Salvar e sair");
        font.draw(batch,label,button.x+(button.width-label.width)/2,button.y+(button.height+label.height)/2);
        font.getData().setScale(1);font.setColor(Color.WHITE);batch.end();
    }
}
