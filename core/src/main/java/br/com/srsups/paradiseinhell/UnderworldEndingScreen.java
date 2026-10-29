package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.util.Objects;
import java.util.function.Supplier;

/** A settled victory's story. Deferring or rereading it never settles the attempt again. */
public final class UnderworldEndingScreen extends ScreenAdapter {
    private final Main game;
    private final String characterId;
    private final Supplier<Screen> returnTo;
    private final FitViewport viewport=new FitViewport(960,540);
    private final DialoguePanel dialoguePanel=new DialoguePanel(30,155,900,280,205,1.15f);
    private final ChoiceInputGuard inputGuard=new ChoiceInputGuard();
    private final DialoguePlayback playback;
    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private BitmapFont font,titleFont;
    private TextureRegion backdrop;
    private boolean disposed,delivered;

    public UnderworldEndingScreen(Main game,String characterId,Supplier<Screen> returnTo) {
        this.game=Objects.requireNonNull(game);this.characterId=Objects.requireNonNull(characterId);
        this.returnTo=Objects.requireNonNull(returnTo);
        if(!characterId.equals(game.profiles().profile().characterId()) || !game.hasUnderworldEpilogue())
            throw new IllegalStateException("No earned Underworld story for this character");
        playback=new DialoguePlayback(UnderworldStory.lines(),game.instantDialogue());
    }
    @Override public void show() {
        Gdx.input.setInputProcessor(null);
        batch=new SpriteBatch();shapes=new ShapeRenderer();
        font=UiTheme.bodyFont();titleFont=UiTheme.titleFont();
        backdrop=SceneryVisuals.cover(game.visuals().scenery().preview(SceneryTheme.GUARDIAN),960,540);
    }
    @Override public void resize(int width,int height){viewport.update(width,height,true);}
    @Override public void render(float delta) {
        if(disposed || delivered)return;
        float elapsed=Float.isFinite(delta)?Math.max(0,delta):0;
        inputGuard.advance(elapsed,Gdx.input.isButtonPressed(Input.Buttons.LEFT));
        playback.advance(elapsed);
        if(Gdx.input.isKeyJustPressed(Input.Keys.T)) {
            game.instantDialogue(!game.instantDialogue());playback.instant(game.instantDialogue());
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)){leave(false);return;}
        Vector2 pointer=viewport.unproject(new Vector2(Gdx.input.getX(),Gdx.input.getY()));
        boolean confirm=Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.E)
            || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
            || Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && dialoguePanel.contains(pointer);
        if(inputGuard.ready() && confirm && playback.accept()==DialoguePlayback.Action.FINISHED){leave(true);return;}
        draw();
    }
    private void leave(boolean read) {
        if(read)game.profiles().finishUnderworldEpilogue(characterId);
        delivered=true;game.setScreen(returnTo.get());
    }
    private void draw() {
        ScreenUtils.clear(UiTheme.INK);viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        shapes.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();batch.setColor(Color.WHITE);batch.draw(backdrop,0,0,960,540);batch.end();
        Gdx.gl.glEnable(GL20.GL_BLEND);Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA,GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0,0,0,.8f);shapes.rect(0,0,960,540);
        dialoguePanel.background(shapes);UiTheme.panel(shapes,30,25,900,105,false);
        shapes.end();Gdx.gl.glDisable(GL20.GL_BLEND);
        batch.begin();titleFont.getData().setScale(.88f);titleFont.setColor(UiTheme.IVORY);
        titleFont.draw(batch,UnderworldStory.title(),35,506,890,8,true);
        font.getData().setScale(.95f);font.setColor(UiTheme.TEAL);
        font.draw(batch,UnderworldStory.subtitle(),35,463,890,8,true);
        String hint=!inputGuard.ready()?inputGuard.remaining()>0
            ?"Leia o desfecho. Continue em "+CombatNumbers.format(inputGuard.remaining())+"s."
            :"Solte o botão de ataque para continuar."
            : playback.complete()?"ENTER / E / ESPAÇO / clique na fala: continuar":"ENTER / E / ESPAÇO / clique na fala: revelar";
        dialoguePanel.draw(batch,titleFont,font,game.visuals(),playback,hint,
            "ESC: adiar leitura | T: texto "+(game.instantDialogue()?"instantâneo":"gradual"));
        font.getData().setScale(.95f);font.setColor(Color.GOLD);
        font.draw(batch,UnderworldStory.nextObjective(),48,107,864,8,true);
        font.getData().setScale(.86f);font.setColor(Color.LIGHT_GRAY);
        font.draw(batch,UnderworldStory.continuationNotice(),48,64,864,8,true);
        font.getData().setScale(1);font.setColor(Color.WHITE);batch.end();
    }
    @Override public void dispose() {
        if(disposed)return;disposed=true;
        if(batch!=null)batch.dispose();if(shapes!=null)shapes.dispose();
        if(font!=null)font.dispose();if(titleFont!=null)titleFont.dispose();
    }
}
