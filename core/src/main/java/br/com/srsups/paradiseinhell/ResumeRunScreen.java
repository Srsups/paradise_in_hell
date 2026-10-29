package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

/** Restoring a checkpoint is separate from abandoning it or starting a new attempt. */
public final class ResumeRunScreen extends ScreenAdapter {
    private final Main game;
    private final RunCheckpoint checkpoint;
    private final RunSession preview;
    private final FitViewport viewport=new FitViewport(960,540);
    private final SpriteBatch batch=new SpriteBatch();
    private final ShapeRenderer shapes=new ShapeRenderer();
    private final BitmapFont font=UiTheme.bodyFont(),title=UiTheme.titleFont();
    private final Rectangle resume=new Rectangle(230,204,500,54),abandon=new Rectangle(230,138,500,46),back=new Rectangle(230,76,500,46);
    private boolean confirming,mouseReleased;
    private float delay=.3f;
    public ResumeRunScreen(Main game) {
        this.game=game;checkpoint=java.util.Objects.requireNonNull(game.profiles().profile().checkpoint());
        RunSession candidate;
        try {candidate=checkpoint.restore(game.campaignRegion());}
        catch(IllegalArgumentException | IllegalStateException invalid){candidate=null;}
        preview=candidate;
    }
    @Override public void show(){Gdx.input.setInputProcessor(null);}
    @Override public void resize(int w,int h){viewport.update(w,h,true);}
    @Override public void render(float delta) {
        delay=Math.max(0,delay-Math.max(0,delta));
        mouseReleased|=!Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        Vector2 pointer=viewport.unproject(new Vector2(Gdx.input.getX(),Gdx.input.getY()));
        boolean click=mouseReleased&&Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);
        if(delay==0) {
            if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)||click&&back.contains(pointer)) {
                if(confirming){confirming=false;delay=.3f;mouseReleased=false;}
                else {game.setScreen(new MenuScreen(game));return;}
            } else if(Gdx.input.isKeyJustPressed(Input.Keys.ENTER)||click&&resume.contains(pointer)) {
                if(confirming){game.abandonSavedAttempt();return;}
                if(preview!=null){game.resumeCampaign();return;}
            } else if(!confirming&&(Gdx.input.isKeyJustPressed(Input.Keys.FORWARD_DEL)
                ||Gdx.input.isKeyJustPressed(Input.Keys.DEL)||click&&abandon.contains(pointer))) {
                confirming=true;delay=.3f;mouseReleased=false;
            }
        }
        ScreenUtils.clear(UiTheme.INK);viewport.apply();
        shapes.setProjectionMatrix(viewport.getCamera().combined);shapes.begin(ShapeRenderer.ShapeType.Filled);
        UiTheme.panel(shapes,110,50,740,435,false);
        UiTheme.panel(shapes,resume.x,resume.y,resume.width,resume.height,resume.contains(pointer));
        if(!confirming)UiTheme.panel(shapes,abandon.x,abandon.y,abandon.width,abandon.height,abandon.contains(pointer));
        UiTheme.panel(shapes,back.x,back.y,back.width,back.height,back.contains(pointer));shapes.end();
        batch.setProjectionMatrix(viewport.getCamera().combined);batch.begin();
        title.getData().setScale(1);title.setColor(UiTheme.IVORY);
        center(title,confirming?"ABANDONAR A TENTATIVA?":"SUA JORNADA CONTINUA",442);
        font.getData().setScale(1.05f);font.setColor(UiTheme.TEAL);
        center(font,checkpoint.profession().title+"  |  "+checkpoint.difficulty().label,397);
        font.getData().setScale(1);font.setColor(UiTheme.IVORY);
        String detail=preview==null?"Este checkpoint não é compatível com o conteúdo atual.\nO progresso permanente do personagem foi preservado."
            :preview.arena().title()+"  |  Nível "+preview.player().getNivel()+"\n"+stageLabel(checkpoint.stage());
        if(confirming)detail=preview==null?"Descartar somente a tentativa incompatível?\nOs atributos permanentes, moedas e desbloqueios serão mantidos."
            :"As dádivas desta tentativa serão perdidas.\nAs recompensas obtidas até o checkpoint serão recolhidas no refúgio.";
        font.draw(batch,detail,155,352,650,1,true);
        font.getData().setScale(.9f);font.setColor(Color.LIGHT_GRAY);
        if(!confirming)center(font,"Fechar durante um combate retoma do último ponto seguro.",282);
        font.getData().setScale(1.05f);font.setColor(preview==null&&!confirming?Color.GRAY:UiTheme.IVORY);
        center(font,confirming?"Confirmar abandono [ENTER]":"Continuar tentativa [ENTER]",239);
        font.setColor(Color.SALMON);
        if(!confirming)center(font,preview==null?"Descartar tentativa [DEL]":"Abandonar tentativa [DEL]",168);
        font.setColor(Color.LIGHT_GRAY);center(font,confirming?"Cancelar [ESC]":"Voltar ao menu [ESC]",106);
        batch.end();
    }
    private void center(BitmapFont f,String text,float y){GlyphLayout layout=new GlyphLayout(f,text);f.draw(batch,layout,(960-layout.width)/2,y);}
    public static String stageLabel(RunCheckpoint.Stage stage) {
        return switch(stage) {
            case ROOM_START -> "Início da sala";
            case ARENA_REWARD -> "Recompensa da sala";
            case BLACKSMITH -> "Encontro com o ferreiro";
            case FARMER -> "Encontro com a Camponesa das Cinzas";
            case ROUTE -> "Escolha do próximo destino";
        };
    }
    @Override public void dispose(){batch.dispose();shapes.dispose();font.dispose();title.dispose();}
}
