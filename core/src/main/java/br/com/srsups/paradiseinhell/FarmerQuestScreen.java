package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

/** Safe dialogue and personal quest journal; neither mode advances the attempt. */
public final class FarmerQuestScreen extends ScreenAdapter {
    private final Main game;
    private final RunSession session;
    private final FitViewport viewport = new FitViewport(960,540);
    private final SpriteBatch batch = new SpriteBatch();
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont font = UiTheme.bodyFont(), titleFont = UiTheme.titleFont();
    private final Rectangle accept = new Rectangle(270,105,300,50), leave = new Rectangle(590,105,300,50);
    private final DialoguePanel dialoguePanel=new DialoguePanel(30,170,900,285,205,1.05f);
    private final SafeRunExit safeExit;
    private DialoguePlayback playback;
    private float delay = 1.5f;
    private boolean accepted, mouseReleased;

    public FarmerQuestScreen(Main game, RunSession session) {
        if(session != null && (session.finished() || !session.questEnabled() || !session.arenaComplete()
            || !session.encounters().farmerOpen())) throw new IllegalStateException("No Camponesa encounter");
        this.game=game;this.session=session;
        safeExit=new SafeRunExit(game,session,RunCheckpoint.Stage.FARMER,new Rectangle(30,105,225,50));
        accepted=session!=null&&game.profiles().profile().farmerQuest()!=FarmerQuest.NOT_STARTED;
        playback=new DialoguePlayback(session==null
            ? CamponesaDialogue.journal(game.profiles().profile().farmerQuest(),game.farmerUnlocked())
            : accepted?CamponesaDialogue.accepted():CamponesaDialogue.offer(),game.instantDialogue());
        if(session==null && !playback.complete())playback.accept();
    }
    public static FarmerQuestScreen journal(Main game) { return new FarmerQuestScreen(game,null); }
    @Override public void show() { Gdx.input.setInputProcessor(null); }
    @Override public void resize(int width,int height) { viewport.update(width,height,true); }
    @Override public void render(float delta) {
        float elapsed=Math.max(0,delta);
        delay=Math.max(0,delay-elapsed);playback.advance(elapsed);
        mouseReleased|=!Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        Vector2 pointer=viewport.unproject(new Vector2(Gdx.input.getX(),Gdx.input.getY()));
        if(safeExit.input(elapsed,pointer))return;
        boolean click=mouseReleased && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);
        boolean confirm=Gdx.input.isKeyJustPressed(Input.Keys.E) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER);
        boolean complete=playback.complete();
        if(Gdx.input.isKeyJustPressed(Input.Keys.T)) {
            game.instantDialogue(!game.instantDialogue());
            playback.instant(game.instantDialogue());
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) { leave();return; }
        if(!complete && (confirm || click)) playback.accept();
        else if(delay==0) {
            if(session==null || accepted) {
                if(confirm || click && leave.contains(pointer)) { leave();return; }
            } else if(click && dialoguePanel.contains(pointer)) {
                playback.accept();
            } else if(confirm || click && accept.contains(pointer)) {
                if(game.profiles().acceptFarmerQuest(session)) {
                    accepted=true;playback=new DialoguePlayback(CamponesaDialogue.accepted(),game.instantDialogue());
                    delay=1.5f;mouseReleased=false;
                }
            } else if(click && leave.contains(pointer)) { leave();return; }
        }
        ScreenUtils.clear(.035f,.045f,.065f,1);viewport.apply();
        shapes.setProjectionMatrix(viewport.getCamera().combined);shapes.begin(ShapeRenderer.ShapeType.Filled);
        dialoguePanel.background(shapes);
        if(session!=null && !accepted) { shapes.setColor(.29f,.25f,.12f,1);rect(accept); }
        shapes.setColor(.13f,.22f,.3f,1);rect(leave);shapes.end();
        batch.setProjectionMatrix(viewport.getCamera().combined);batch.begin();
        titleFont.getData().setScale(.95f);titleFont.setColor(UiTheme.IVORY);titleFont.draw(batch,CamponesaDialogue.title(),170,490);
        dialoguePanel.draw(batch,titleFont,font,game.visuals(),playback,
            playback.complete()?session==null?"Diário da promessa":accepted?"Promessa registrada. Continue quando quiser.":"Aceite a promessa ou continue a travessia."
                : "ENTER / E / clique: revelar fala",
            "ESC: voltar | T: texto "+(game.instantDialogue()?"instantâneo":"gradual"));
        font.getData().setScale(1);font.setColor(delay>0?Color.GRAY:Color.WHITE);
        if(session!=null && !accepted) font.draw(batch,"E / ENTER: aceitar a promessa",290,137);
        font.draw(batch,session==null?"ENTER: voltar ao refúgio":"Continuar a travessia",620,137);
        if(!game.profiles().error().isEmpty()) {
            font.setColor(Color.SALMON);font.draw(batch,game.profiles().error(),40,35,880,8,true);
        }
        batch.end();
        safeExit.draw(batch,shapes,font,pointer);
    }
    private void rect(Rectangle r) { shapes.rect(r.x,r.y,r.width,r.height); }
    private void leave() {
        if(session==null) game.setScreen(new RefugeScreen(game));
        else { session.encounters().closeFarmer();game.offerRoute(session); }
    }
    @Override public void dispose() { batch.dispose();shapes.dispose();font.dispose();titleFont.dispose(); }
}
