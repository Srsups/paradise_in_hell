package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/** Can be drawn inside an arena: opening audio never discards its entities or advances the simulation. */
public final class AudioPanel {
    private final AudioSettingsService settings;
    private final GameAudio audio;
    private final Rectangle[] sliders = {new Rectangle(310,340,370,28),new Rectangle(310,260,370,28)};
    private final Rectangle mute = new Rectangle(310,183,370,42);
    private final Rectangle test = new Rectangle(135,105,260,45), back = new Rectangle(555,105,260,45);
    private boolean open;
    private int row, dragging = -1;
    public AudioPanel(Main game) { settings=game.audioSettings(); audio=game.audio(); }
    public boolean open() { return open; }
    public void toggle() { if(open) close(); else {open=true; dragging=-1; audio.stopEffects();} }
    public void close() { settings.save(); open=false; dragging=-1; }
    private void change(float value) {
        value=Math.round(Math.max(0,Math.min(1,value))*100)/100f;
        AudioSettings old=settings.value();
        settings.change(new AudioSettings(row==0?value:old.effects(),row==1?value:old.music(),old.muted()));
        audio.refreshVolume();
    }
    public void input(Vector2 pointer) {
        if(!open)return;
        boolean click=Gdx.input.isButtonJustPressed(Input.Buttons.LEFT),held=Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.O) || click && back.contains(pointer)) {close();return;}
        if(Gdx.input.isKeyJustPressed(Input.Keys.UP))row=0;
        if(Gdx.input.isKeyJustPressed(Input.Keys.DOWN))row=1;
        boolean keyboard=Gdx.input.isKeyJustPressed(Input.Keys.LEFT)||Gdx.input.isKeyJustPressed(Input.Keys.RIGHT);
        if(keyboard) {
            float value=row==0?settings.value().effects():settings.value().music();
            change(value+(Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)?.05f:-.05f));settings.save();
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.M) || click && mute.contains(pointer)) {
            AudioSettings old=settings.value();
            settings.change(new AudioSettings(old.effects(),old.music(),!old.muted()));audio.refreshVolume();settings.save();
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.T) || click && test.contains(pointer))audio.play(SoundCue.BLOCK);
        if(Gdx.input.isKeyJustPressed(Input.Keys.S))settings.save();
        for(int i=0;i<sliders.length;i++)if(click && sliders[i].contains(pointer)){row=i;dragging=i;}
        if(dragging>=0) {
            Rectangle slider=sliders[dragging];
            change((pointer.x-slider.x)/slider.width);
            if(!held){settings.save();dragging=-1;}
        }
    }
    public void draw(SpriteBatch batch,ShapeRenderer shapes,BitmapFont font) {
        if(!open)return;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(.035f,.045f,.07f,1);shapes.rect(0,0,960,540);
        for(int i=0;i<sliders.length;i++) {
            Rectangle slider=sliders[i];float value=i==0?settings.value().effects():settings.value().music();
            shapes.setColor(.13f,.16f,.23f,1);shapes.rect(slider.x,slider.y,slider.width,slider.height);
            shapes.setColor(i==row?Color.GOLD:Color.SKY);shapes.rect(slider.x,slider.y,slider.width*value,slider.height);
        }
        shapes.setColor(.13f,.16f,.23f,1);
        for(Rectangle rect:new Rectangle[]{mute,test,back})shapes.rect(rect.x,rect.y,rect.width,rect.height);
        shapes.end();
        batch.begin();font.getData().setScale(1.8f);font.setColor(Color.GOLD);font.draw(batch,"ÁUDIO",420,483);
        font.getData().setScale(1);font.setColor(Color.WHITE);
        font.draw(batch,"Efeitos sonoros",135,361);font.draw(batch,"Música",135,281);
        font.draw(batch,Math.round(settings.value().effects()*100)+"%",710,361);
        font.draw(batch,Math.round(settings.value().music()*100)+"%",710,281);
        font.draw(batch,"Silenciar tudo: "+(settings.value().muted()?"SIM":"NÃO")+" [M]",365,210);
        font.draw(batch,"Testar efeito [T]",197,134);font.draw(batch,"Voltar [ESC]",635,134);
        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch,"Clique/arraste o volume | Acima/baixo: escolher | Esquerda/direita: ajustar",150,73);
        String notice=!settings.error().isEmpty()?settings.error():!audio.available()?"Dispositivo de áudio indisponível. O jogo continua funcionando."
            :settings.value().muted()?"Áudio silenciado; os volumes escolhidos foram mantidos."
            :settings.pending()?"Alteração ainda não salva. Solte o controle ou volte para salvar.":"Preferências compartilhadas por todos os personagens.";
        font.setColor(settings.error().isEmpty()?Color.LIGHT_GRAY:Color.SALMON);font.draw(batch,notice,45,42,870,8,true);
        batch.end();
    }
}

