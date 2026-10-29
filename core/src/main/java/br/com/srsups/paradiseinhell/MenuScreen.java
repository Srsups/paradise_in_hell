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

public class MenuScreen extends ScreenAdapter {
    private final Main game;
    private final FitViewport viewport = new FitViewport(960, 540);
    private final SpriteBatch batch = new SpriteBatch();
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont font = UiTheme.bodyFont();
    private final BitmapFont titleFont = UiTheme.titleFont();
    private final Rectangle play = new Rectangle(280, 265, 400, 55);
    private final Rectangle campaign = new Rectangle(280, 195, 400, 55);
    private final Rectangle audio = new Rectangle(135, 78, 165, 40);
    private final Rectangle video = new Rectangle(310, 78, 165, 40);
    private final Rectangle controls = new Rectangle(485, 78, 165, 40);
    private final Rectangle introduction = new Rectangle(660, 78, 165, 40);
    private final Rectangle exit = new Rectangle(280, 130, 400, 45);

    public MenuScreen(Main game) { this.game = game; }
    @Override public void show() { Gdx.input.setInputProcessor(null); }
    @Override public void resize(int width, int height) { viewport.update(width, height, true); }

    @Override public void render(float delta) {
        boolean clicked = Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);
        Vector2 pointer = viewport.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        if(Gdx.input.isKeyJustPressed(Input.Keys.F3) || clicked && controls.contains(pointer)) {
            game.setScreen(new ControlsSettingsScreen(game,()->new MenuScreen(game)));return;
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.I) || clicked && introduction.contains(pointer)) {
            game.openOpening(()->new MenuScreen(game),false);return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F2) || clicked && video.contains(pointer)) {
            game.setScreen(new VideoSettingsScreen(game,()->new MenuScreen(game)));return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || clicked && play.contains(pointer)) {
            game.openDemo();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.C) || clicked && campaign.contains(pointer)) {
            game.openCampaign();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.O) || clicked && audio.contains(pointer)) {
            game.setScreen(new AudioSettingsScreen(game,()->new MenuScreen(game))); return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || clicked && exit.contains(pointer)) {
            Gdx.app.exit();
            return;
        }
        ScreenUtils.clear(0.045f, 0.055f, 0.085f, 1);
        viewport.apply();
        shapes.setProjectionMatrix(viewport.getCamera().combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        UiTheme.panel(shapes,play.x,play.y,play.width,play.height,true);
        UiTheme.panel(shapes,campaign.x,campaign.y,campaign.width,campaign.height,false);
        UiTheme.panel(shapes,exit.x,exit.y,exit.width,exit.height,false);
        UiTheme.panel(shapes,audio.x,audio.y,audio.width,audio.height,false);
        UiTheme.panel(shapes,video.x,video.y,video.width,video.height,false);
        UiTheme.panel(shapes,controls.x,controls.y,controls.width,controls.height,false);
        UiTheme.panel(shapes,introduction.x,introduction.y,introduction.width,introduction.height,false);
        shapes.setColor(UiTheme.BRONZE); shapes.rect(220,348,520,1);
        shapes.rect(220,475,520,1);
        shapes.end();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        titleFont.setColor(UiTheme.IVORY);
        titleFont.getData().setScale(1.3f);
        GlyphLayout title=new GlyphLayout(titleFont,"PARADISE IN HELL");
        titleFont.draw(batch,title,(960-title.width)/2,458);
        centered("Uma maldição. Muitas tentativas.", 402, 1.05f, UiTheme.TEAL);
        centered("Demo de sobrevivência e protótipo da campanha", 385, 1.2f, Color.WHITE);
        centered("DEMO: 5 MINUTOS  [ENTER]", 300, 1.2f, UiTheme.IVORY);
        centered("CAMPANHA: PROTÓTIPO  [C]", 230, 1.2f, Color.WHITE);
        centered("SAIR  [ESC]", 158, 1.2f, Color.WHITE);
        font.getData().setScale(1);font.setColor(Color.WHITE);
        GlyphLayout audioLabel=new GlyphLayout(font,"ÁUDIO [O]"),videoLabel=new GlyphLayout(font,"VÍDEO [F2]"),introLabel=new GlyphLayout(font,"ABERTURA [I]"),controlsLabel=new GlyphLayout(font,"CONTROLES [F3]");
        font.draw(batch,audioLabel,audio.x+(audio.width-audioLabel.width)/2,106);
        font.draw(batch,videoLabel,video.x+(video.width-videoLabel.width)/2,106);
        font.draw(batch,controlsLabel,controls.x+(controls.width-controlsLabel.width)/2,106);
        font.draw(batch,introLabel,introduction.x+(introduction.width-introLabel.width)/2,106);
        centered("F3: configurar controles   |   Dash: "+game.controls().label(ControlAction.DASH), 65, 1.05f, Color.LIGHT_GRAY);
        centered("Mouse: mirar   |   Atacar: "+game.controls().label(ControlAction.ATTACK), 43, 1.05f, Color.LIGHT_GRAY);
        centered("ESC: pausar   |   1, 2, 3: escolher uma dádiva", 21, 1.1f, Color.LIGHT_GRAY);
        batch.end();
    }
    private void centered(String text, float y, float scale, Color color) {
        font.getData().setScale(scale);
        font.setColor(color);
        GlyphLayout layout = new GlyphLayout(font, text);
        font.draw(batch, layout, (960 - layout.width) / 2, y);
    }
    @Override public void dispose() { batch.dispose(); shapes.dispose(); font.dispose(); titleFont.dispose(); }
}
