package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;

/** HUD-only preference page. Its preview never changes the native window or a live run. */
public final class HudPanel {
    public static final float INPUT_DELAY=.2f;
    private static final int[] PRESETS={80,100,125};
    private final Main game;
    private final Rectangle minus=new Rectangle(225,374,55,44),plus=new Rectangle(680,374,55,44);
    private final Rectangle track=new Rectangle(310,385,340,20);
    private final Rectangle[] presets={new Rectangle(215,325,110,36),new Rectangle(425,325,110,36),new Rectangle(635,325,110,36)};
    private final Rectangle back=new Rectangle(35,25,250,44),defaults=new Rectangle(355,25,250,44),apply=new Rectangle(675,25,250,44);
    private final PlayerState sample=new PlayerState(Profession.CACADOR);
    private final RunSession sampleRun=RunSession.demo();
    private final Matrix4 savedBatch=new Matrix4(),savedShapes=new Matrix4(),sampleTransform=new Matrix4();
    private HudSettings draft=HudSettings.DEFAULT;
    private boolean open,mouseReleased,dragging;
    private float delay;
    private String notice="";
    public HudPanel(Main game){this.game=game;}
    public boolean open(){return open;}
    public HudSettings draft(){return draft;}
    public Rectangle minusBounds(){return new Rectangle(minus);}
    public Rectangle plusBounds(){return new Rectangle(plus);}
    public Rectangle trackBounds(){return new Rectangle(track);}
    public Rectangle presetBounds(int index){return new Rectangle(presets[index]);}
    public Rectangle applyBounds(){return new Rectangle(apply);}
    public Rectangle defaultsBounds(){return new Rectangle(defaults);}
    public Rectangle backBounds(){return new Rectangle(back);}
    public void show(){draft=game.hudSettings().value();notice="";open=true;resetGuard();}
    public void close(){if(open)game.controls().blockUntilReleased();open=false;dragging=false;}
    public void suspendInput(){if(open)resetGuard();}
    private void resetGuard(){delay=INPUT_DELAY;mouseReleased=false;dragging=false;}
    private void adjust(int amount){setPercent(draft.percent()+amount);}
    private void setPercent(int percent){draft=new HudSettings(Math.max(HudSettings.MIN_PERCENT,Math.min(HudSettings.MAX_PERCENT,percent)));notice="Prévia local. Salve para usar este tamanho no combate.";}
    public boolean input(float delta,Vector2 pointer) {
        if(!open)return false;
        if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)){close();return true;}
        delay=Math.max(0,delay-(Float.isFinite(delta)?Math.max(0,delta):0));
        if(delay==0&&!Gdx.input.isButtonPressed(Input.Buttons.LEFT))mouseReleased=true;
        if(delay>0||!mouseReleased)return true;
        boolean click=Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);
        if(click&&back.contains(pointer)){close();return true;}
        if(click&&minus.contains(pointer)||Gdx.input.isKeyJustPressed(Input.Keys.LEFT))adjust(-5);
        else if(click&&plus.contains(pointer)||Gdx.input.isKeyJustPressed(Input.Keys.RIGHT))adjust(5);
        for(int index=0;index<presets.length;index++)if(click&&presets[index].contains(pointer))setPercent(PRESETS[index]);
        if(click&&track.contains(pointer))dragging=true;
        if(dragging&&Gdx.input.isButtonPressed(Input.Buttons.LEFT))
            setPercent(Math.round(HudSettings.MIN_PERCENT+(HudSettings.MAX_PERCENT-HudSettings.MIN_PERCENT)*(pointer.x-track.x)/track.width));
        if(!Gdx.input.isButtonPressed(Input.Buttons.LEFT))dragging=false;
        if(click&&defaults.contains(pointer)){draft=HudSettings.DEFAULT;notice="Padrão de 100% no rascunho. Salve para aplicar.";resetGuard();}
        else if(click&&apply.contains(pointer)||Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            boolean saved=game.hudSettings().apply(draft);game.controls().blockUntilReleased();
            notice=saved?"Tamanho do HUD salvo para todos os personagens.":"Tamanho aplicado nesta sessão. Confira a mensagem abaixo.";
            resetGuard();
        } else if(Gdx.input.isKeyJustPressed(Input.Keys.S)&&game.hudSettings().pendingSave()) {
            if(game.hudSettings().retrySave())notice="Tamanho do HUD salvo para todos os personagens.";
        }
        return true;
    }
    public void draw(SpriteBatch batch,ShapeRenderer shapes,BitmapFont font) {
        if(!open)return;
        shapes.begin(ShapeRenderer.ShapeType.Filled);shapes.setColor(UiTheme.INK);shapes.rect(0,0,960,540);
        for(Rectangle button:new Rectangle[]{minus,plus,back,defaults,apply})panel(shapes,button,button==apply);
        for(int index=0;index<presets.length;index++)panel(shapes,presets[index],draft.percent()==PRESETS[index]);
        UiTheme.panel(shapes,35,145,890,160,false);
        shapes.setColor(UiTheme.BRONZE);shapes.rect(track.x,track.y+8,track.width,4);
        float knob=track.x+track.width*(draft.percent()-HudSettings.MIN_PERCENT)/(HudSettings.MAX_PERCENT-HudSettings.MIN_PERCENT);
        shapes.setColor(Color.GOLD);shapes.rect(knob-4,track.y,8,track.height);
        shapes.end();batch.begin();batch.setColor(Color.WHITE);
        text(batch,font,"TAMANHO DO HUD",35,504,1.8f,Color.GOLD);
        text(batch,font,"Ajuste os indicadores de combate. A câmera e os personagens mantêm o tamanho.",35,466,.95f,UiTheme.IVORY);
        text(batch,font,draft.percent()+"%",440,440,1.4f,UiTheme.TEAL);
        button(batch,font,minus,"-",1.5f);button(batch,font,plus,"+",1.5f);
        for(int index=0;index<presets.length;index++)button(batch,font,presets[index],PRESETS[index]+"%",1);
        text(batch,font,"PRÉVIA",48,292,.85f,UiTheme.TEAL);
        text(batch,font,"Setas: ajustar 5%  |  Clique ou arraste a barra: ajuste de 1%",35,130,.9f,Color.LIGHT_GRAY);
        button(batch,font,back,"VOLTAR A VÍDEO [ESC]",.95f);button(batch,font,defaults,"RESTAURAR 100%",1);button(batch,font,apply,"SALVAR [ENTER]",1);
        font.getData().setScale(.85f);font.setColor(game.hudSettings().error().isEmpty()?Color.LIGHT_GRAY:Color.SALMON);
        font.draw(batch,game.hudSettings().error().isEmpty()?notice:game.hudSettings().error(),35,99,890,Align.left,true);
        font.getData().setScale(1);font.setColor(Color.WHITE);batch.end();
        drawSample(batch,shapes,font);
    }
    private void drawSample(SpriteBatch batch,ShapeRenderer shapes,BitmapFont font) {
        savedBatch.set(batch.getTransformMatrix());savedShapes.set(shapes.getTransformMatrix());
        float scale=draft.scale();
        try {
            sampleTransform.set(savedBatch).translate(65,272,0).scale(scale,scale,1).translate(-14,-526,0);batch.setTransformMatrix(sampleTransform);
            sampleTransform.set(savedShapes).translate(65,272,0).scale(scale,scale,1).translate(-14,-526,0);shapes.setTransformMatrix(sampleTransform);
            CombatHud.player(batch,shapes,font,sample);
            sampleTransform.set(savedBatch).translate(885,272,0).scale(scale,scale,1).translate(-946,-526,0);batch.setTransformMatrix(sampleTransform);
            sampleTransform.set(savedShapes).translate(885,272,0).scale(scale,scale,1).translate(-946,-526,0);shapes.setTransformMatrix(sampleTransform);
            CombatHud.goals(batch,shapes,font,sampleRun);
        } finally {batch.setTransformMatrix(savedBatch);shapes.setTransformMatrix(savedShapes);}
    }
    private static void panel(ShapeRenderer shapes,Rectangle rect,boolean selected){UiTheme.panel(shapes,rect.x,rect.y,rect.width,rect.height,selected);}
    private static void button(SpriteBatch batch,BitmapFont font,Rectangle rect,String text,float scale) {
        font.getData().setScale(scale);font.setColor(Color.WHITE);GlyphLayout layout=new GlyphLayout(font,text);
        font.draw(batch,layout,rect.x+(rect.width-layout.width)/2,rect.y+(rect.height+layout.height)/2);
    }
    private static void text(SpriteBatch batch,BitmapFont font,String value,float x,float y,float scale,Color color){font.getData().setScale(scale);font.setColor(color);font.draw(batch,value,x,y);}
}
