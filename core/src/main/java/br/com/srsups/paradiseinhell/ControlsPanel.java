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
import com.badlogic.gdx.utils.Align;

/** Global gameplay bindings, edited as a local draft. Menus keep their navigation controls. */
public final class ControlsPanel {
    public static final float INPUT_DELAY=.2f;
    public static final int ROWS=6;
    private final Main game;
    private final Rectangle[][] cells=new Rectangle[ROWS][2];
    private final Rectangle previous=new Rectangle(35,112,140,32),next=new Rectangle(785,112,140,32);
    private final Rectangle back=new Rectangle(35,25,250,44),defaults=new Rectangle(355,25,250,44),apply=new Rectangle(675,25,250,44);
    private ControlSettings draft=ControlSettings.DEFAULT;
    private boolean open,mouseReleased,captureReleased;
    private float delay;
    private int page,captureSlot;
    private ControlAction capture;
    private String notice="";

    public ControlsPanel(Main game) {
        this.game=game;
        for(int row=0;row<ROWS;row++)for(int slot=0;slot<2;slot++)
            cells[row][slot]=new Rectangle(350+slot*290,390-row*46,275,40);
    }
    public boolean open(){return open;}
    public boolean capturing(){return capture!=null;}
    public int page(){return page;}
    public ControlSettings draft(){return draft;}
    public String notice(){return notice;}
    public Rectangle bindingBounds(int row,int slot){return new Rectangle(cells[row][slot]);}
    public Rectangle applyBounds(){return new Rectangle(apply);}
    public Rectangle defaultsBounds(){return new Rectangle(defaults);}
    public Rectangle backBounds(){return new Rectangle(back);}
    public Rectangle previousBounds(){return new Rectangle(previous);}
    public Rectangle nextBounds(){return new Rectangle(next);}
    public void show() {
        draft=game.controlSettings().value();page=0;capture=null;notice="";open=true;resetGuard();
        game.audio().stopEffects();
    }
    public void close() {
        if(open)game.controls().blockUntilReleased();
        open=false;capture=null;
    }
    private void resetGuard(){delay=INPUT_DELAY;mouseReleased=false;captureReleased=false;}
    private int pages(){return (ControlAction.values().length+ROWS-1)/ROWS;}
    private ControlAction action(int row){int index=page*ROWS+row;return index<ControlAction.values().length?ControlAction.values()[index]:null;}
    /** Always consumes open/close/capture frames, before the caller can advance its world. */
    public boolean input(float delta,Vector2 pointer) {
        game.controls().poll();
        if(Gdx.input.isKeyJustPressed(Input.Keys.F3)) {if(open)close();else show();return true;}
        if(!open)return false;
        delay=Math.max(0,delay-(Float.isFinite(delta)?Math.max(0,delta):0));
        if(capture!=null){captureInput();return true;}
        if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)){close();return true;}
        if(delay==0&&!Gdx.input.isButtonPressed(Input.Buttons.LEFT))mouseReleased=true;
        if(delay>0||!mouseReleased)return true;
        boolean click=Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);
        if(click&&back.contains(pointer)){close();return true;}
        if(click&&previous.contains(pointer)||Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {page=Math.max(0,page-1);return true;}
        if(click&&next.contains(pointer)||Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {page=Math.min(pages()-1,page+1);return true;}
        if(click&&defaults.contains(pointer)) {draft=ControlSettings.DEFAULT;notice="Padrão restaurado no rascunho. Salve para usar.";resetGuard();return true;}
        if(click&&apply.contains(pointer)||Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            boolean saved=game.controlSettings().apply(draft);
            game.controls().blockUntilReleased();
            notice=saved?"Controles salvos para todos os personagens.":"Controles aplicados nesta sessão. Confira a mensagem abaixo.";
            resetGuard();return true;
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.S)&&game.controlSettings().pendingSave()) {
            if(game.controlSettings().retrySave())notice="Controles salvos para todos os personagens.";
            return true;
        }
        if(click)for(int row=0;row<ROWS;row++)for(int slot=0;slot<2;slot++)
            if(action(row)!=null&&cells[row][slot].contains(pointer)) {
                capture=action(row);captureSlot=slot;notice="";resetGuard();return true;
            }
        return true;
    }
    private boolean anyHeld() {
        for(int key=0;key<=Input.Keys.MAX_KEYCODE;key++)if(Gdx.input.isKeyPressed(key))return true;
        for(int button=0;button<=Input.Buttons.FORWARD;button++)if(Gdx.input.isButtonPressed(button))return true;
        return false;
    }
    private void captureInput() {
        if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)){capture=null;resetGuard();return;}
        if(!captureReleased) {
            if(delay==0&&!anyHeld())captureReleased=true;
            return;
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)||Gdx.input.isKeyJustPressed(Input.Keys.FORWARD_DEL)) {
            try{draft=draft.withoutBinding(capture,captureSlot);capture=null;notice="Associação removida do rascunho.";resetGuard();}
            catch(IllegalArgumentException conflict){notice=conflict.getMessage();}
            return;
        }
        for(int button=0;button<=Input.Buttons.FORWARD;button++)if(Gdx.input.isButtonJustPressed(button)) {
            accept(InputBinding.mouse(button));return;
        }
        for(int key=1;key<=Input.Keys.MAX_KEYCODE;key++)if(Gdx.input.isKeyJustPressed(key)) {
            accept(InputBinding.key(key));return;
        }
    }
    private void accept(InputBinding binding) {
        try {
            draft=draft.withBinding(capture,captureSlot,binding);capture=null;
            notice="Rascunho atualizado. Salve para usar os novos controles.";resetGuard();
        } catch(IllegalArgumentException conflict){notice=conflict.getMessage();}
    }
    public void draw(SpriteBatch batch,ShapeRenderer shapes,BitmapFont font) {
        if(!open)return;
        shapes.begin(ShapeRenderer.ShapeType.Filled);shapes.setColor(UiTheme.INK);shapes.rect(0,0,960,540);
        for(int row=0;row<ROWS;row++)if(action(row)!=null)for(int slot=0;slot<2;slot++)panel(shapes,cells[row][slot],false);
        panel(shapes,previous,false);panel(shapes,next,false);panel(shapes,back,false);panel(shapes,defaults,false);panel(shapes,apply,true);
        shapes.end();batch.begin();batch.setColor(Color.WHITE);
        text(batch,font,"CONTROLES",35,508,1.8f,Color.GOLD);
        text(batch,font,"Escolha uma associação, solte os controles e pressione a nova tecla ou botão.",35,473,.95f,UiTheme.IVORY);
        text(batch,font,"COMANDO",35,446,.9f,UiTheme.TEAL);
        text(batch,font,"PRINCIPAL",350,446,.9f,UiTheme.TEAL);
        text(batch,font,"ALTERNATIVO",640,446,.9f,UiTheme.TEAL);
        for(int row=0;row<ROWS;row++) {
            ControlAction action=action(row);if(action==null)continue;
            text(batch,font,action.label(),35,cells[row][0].y+26,1.05f,Color.WHITE);
            for(int slot=0;slot<2;slot++) {
                var bindings=draft.bindings(action);
                button(batch,font,cells[row][slot],slot<bindings.size()?bindings.get(slot).label():"Adicionar",.98f);
            }
        }
        button(batch,font,previous,"Anterior",.95f);button(batch,font,next,"Próxima",.95f);
        text(batch,font,"Página "+(page+1)+" / "+pages()+"   |   Mouse: mirar   |   ESC/F3: voltar",242,134,.9f,Color.LIGHT_GRAY);
        button(batch,font,back,"VOLTAR [ESC]",1);button(batch,font,defaults,"RESTAURAR PADRÃO",1);button(batch,font,apply,"SALVAR [ENTER]",1);
        String message=game.controlSettings().error().isEmpty()?notice:game.controlSettings().error();
        font.getData().setScale(.85f);font.setColor(game.controlSettings().error().isEmpty()?Color.LIGHT_GRAY:Color.SALMON);
        font.draw(batch,message,35,99,890,Align.left,true);
        font.getData().setScale(1);font.setColor(Color.WHITE);batch.end();
        if(capture!=null)drawCapture(batch,shapes,font);
    }
    private void drawCapture(SpriteBatch batch,ShapeRenderer shapes,BitmapFont font) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);shapes.setColor(UiTheme.INK);shapes.rect(0,0,960,540);
        UiTheme.panel(shapes,100,150,760,270,true);shapes.end();batch.begin();batch.setColor(Color.WHITE);
        text(batch,font,"ESCOLHA O CONTROLE",265,382,1.5f,Color.GOLD);
        text(batch,font,capture.label()+" — "+(captureSlot==0?"principal":"alternativo"),155,333,1.1f,UiTheme.IVORY);
        text(batch,font,captureReleased?"Pressione uma tecla ou um botão do mouse.":"Solte todos os controles para começar.",155,289,1,UiTheme.TEAL);
        text(batch,font,"ESC: cancelar   |   Delete/Backspace: remover associação",155,246,.95f,Color.LIGHT_GRAY);
        font.getData().setScale(.9f);font.setColor(Color.SALMON);font.draw(batch,notice,155,208,650,Align.left,true);
        font.getData().setScale(1);font.setColor(Color.WHITE);batch.end();
    }
    private static void panel(ShapeRenderer shapes,Rectangle rect,boolean selected){UiTheme.panel(shapes,rect.x,rect.y,rect.width,rect.height,selected);}
    private static void button(SpriteBatch batch,BitmapFont font,Rectangle rect,String label,float scale) {
        font.getData().setScale(scale);font.setColor(Color.WHITE);GlyphLayout layout=new GlyphLayout(font,label);
        if(layout.width>rect.width-18){font.getData().setScale(scale*(rect.width-18)/layout.width);layout.setText(font,label);}
        font.draw(batch,layout,rect.x+(rect.width-layout.width)/2,rect.y+(rect.height+layout.height)/2);
    }
    private static void text(SpriteBatch batch,BitmapFont font,String text,float x,float y,float scale,Color color) {
        font.getData().setScale(scale);font.setColor(color);font.draw(batch,text,x,y);
    }
}
