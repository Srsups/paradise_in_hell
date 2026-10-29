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
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.ScreenUtils;
import java.util.Objects;
import java.util.function.Supplier;

/** A layered story presentation. It owns its atlas and never creates a run or settles rewards. */
public final class OpeningScreen extends ScreenAdapter {
    public static final float SUBTITLE_SCALE = 1.15f, INPUT_DELAY = .2f;
    private final Main game;
    private final String characterId;
    private final boolean recordCompletion;
    private final Supplier<Screen> destination;
    private final OpeningPlayback playback = new OpeningPlayback();
    private final VideoPanel videoPanel;
    private final FitViewport viewport = new FitViewport(960, 540);
    private final Matrix4 artTransform = new Matrix4(), identity = new Matrix4();
    private final Rectangle layerRect = new Rectangle();
    private static final java.util.Set<String> ACTORS = java.util.Set.of("hero-idle", "hero-reach", "wife-idle", "wife-resist",
        "zeus-idle", "zeus-cast", "charon");
    private final Rectangle pauseButton = new Rectangle(535, 487, 190, 36), skipButton = new Rectangle(745, 487, 180, 36),
        nextButton = new Rectangle(730, 8, 195, 32), keepWatching = new Rectangle(150, 188, 300, 45),
        confirmSkip = new Rectangle(510, 188, 300, 45);
    private OpeningArt art;
    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private BitmapFont font, titleFont;
    private boolean askingSkip, mouseReleased, delivered, disposed;
    private float inputDelay = INPUT_DELAY;

    public OpeningScreen(Main game, String characterId, boolean recordCompletion, Supplier<Screen> destination) {
        this.game = Objects.requireNonNull(game); this.characterId = Objects.requireNonNull(characterId);
        this.recordCompletion = recordCompletion; this.destination = Objects.requireNonNull(destination);
        videoPanel = new VideoPanel(game);
    }
    public OpeningPlayback playback() { return playback; }
    public boolean confirmingSkip() { return askingSkip; }
    public Rectangle subtitleBounds() { return new Rectangle(55, 42, 850, 54); }
    public Rectangle pauseBounds() { return new Rectangle(pauseButton); }
    public Rectangle skipBounds() { return new Rectangle(skipButton); }
    public Rectangle nextBounds() { return new Rectangle(nextButton); }
    public Rectangle keepWatchingBounds() { return new Rectangle(keepWatching); }
    public Rectangle confirmSkipBounds() { return new Rectangle(confirmSkip); }
    /** Actual unzoomed placement, including the authored canvas ratio and anchored breathing. */
    public Rectangle layerBounds(int index) {
        var layer = playback.current().layers().get(index);
        return new Rectangle(placeLayer(layer, layerRegion(layer), Interpolation.smooth.apply(playback.shotProgress()), index));
    }
    @Override public void show() {
        Gdx.input.setInputProcessor(null);
        art = new OpeningArt(); batch = new SpriteBatch(); shapes = new ShapeRenderer();
        font = UiTheme.bodyFont(); titleFont = UiTheme.titleFont();
    }
    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void render(float delta) {
        if (disposed || delivered) return;
        float elapsed = Float.isFinite(delta) ? Math.max(0, delta) : 0;
        Vector2 pointer = viewport.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        if (videoPanel.input(elapsed, pointer)) { game.audio().stopEmitter(this); draw(); return; }
        inputDelay = Math.max(0, inputDelay - elapsed);
        if (inputDelay == 0 && !Gdx.input.isButtonPressed(Input.Buttons.LEFT)) mouseReleased = true;
        boolean click = inputDelay == 0 && mouseReleased && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);
        if (askingSkip) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || click && keepWatching.contains(pointer)) {
                askingSkip = false; resetGuard();
            } else if (inputDelay == 0 && mouseReleased && (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                    || click && confirmSkip.contains(pointer))) { deliver(true); return; }
            draw(); return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || click && skipButton.contains(pointer)) {
            askingSkip = true; resetGuard(); game.audio().stopEmitter(this); draw(); return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || click && pauseButton.contains(pointer)) {
            playback.togglePause();
            if (playback.paused()) game.audio().stopEmitter(this);
            draw(); return;
        }
        if (inputDelay == 0 && mouseReleased && (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || click && nextButton.contains(pointer))) {
            if (playback.next()) {
                resetGuard(); game.audio().stopEmitter(this);
                if (playback.finished()) { deliver(false); return; }
                if (!playback.paused()) playSounds();
                draw(); return;
            }
        }
        if (!playback.paused()) {
            // Do not consume an unseen scene after a window drag or debugger stop.
            playback.advance(Math.min(elapsed, .1f));
            if (playback.finished()) { deliver(false); return; }
            playSounds();
        }
        draw();
    }
    private void resetGuard() { inputDelay = INPUT_DELAY; mouseReleased = false; }
    private void playSounds() {
        SoundCue cue;
        while ((cue = playback.pollSound()) != null) game.audio().play(cue, this);
    }
    private TextureRegion layerRegion(OpeningStory.Layer layer) {
        return layer.asset().equals("portal") ? art.portal((playback.elapsedTotal() * 1.8f) % 1) : art.region(layer.asset());
    }
    private Rectangle placeLayer(OpeningStory.Layer layer, TextureRegion region, float progress, int index) {
        float x = layer.x() + (layer.endX() - layer.x()) * progress, y = layer.y() + (layer.endY() - layer.y()) * progress;
        float width = layer.width();
        if (ACTORS.contains(layer.asset())) {
            width = layer.height() * region.getRegionWidth() / region.getRegionHeight();
            x += (layer.width() - width) / 2;
        }
        float breath = (float)Math.sin(playback.elapsedTotal() * 2.1f + index * .8f) * layer.breath();
        return layerRect.set(x, y, width, Math.max(1, layer.height() + breath));
    }
    private void deliver(boolean skipped) {
        if (delivered || disposed) return;
        delivered = true;
        if (skipped) playback.skip();
        game.audio().stopEmitter(this);
        if (recordCompletion) game.profiles().markOpening(characterId, skipped);
        game.setScreen(Objects.requireNonNull(destination.get(), "Opening destination"));
    }
    private void draw() {
        ScreenUtils.clear(UiTheme.INK); viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined); shapes.setProjectionMatrix(viewport.getCamera().combined);
        var shot = playback.current(); float progress = Interpolation.smooth.apply(playback.shotProgress());
        float zoom = shot.startZoom() + (shot.endZoom() - shot.startZoom()) * progress;
        artTransform.idt().translate(480, 270, 0).scale(zoom, zoom, 1).translate(-480, -270, 0);
        batch.setTransformMatrix(artTransform); batch.begin();
        int index = 0;
        for (var layer : shot.layers()) {
            TextureRegion region = layerRegion(layer);
            Rectangle bounds = placeLayer(layer, region, progress, index++);
            float alpha = layer.alphaAt(playback.shotProgress());
            batch.setColor(1, 1, 1, alpha);
            batch.draw(region, bounds.x, bounds.y, bounds.width, bounds.height);
        }
        batch.setColor(Color.WHITE); batch.end(); batch.setTransformMatrix(identity);
        float fade = 1 - Math.min(1, Math.min(playback.elapsedInShot(), shot.duration() - playback.elapsedInShot()) / .4f);
        Gdx.gl.glEnable(GL20.GL_BLEND); Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        if (fade > 0) { shapes.setColor(0, 0, 0, fade); shapes.rect(0, 0, 960, 540); }
        shapes.setColor(.02f, .03f, .045f, .88f); shapes.rect(0, 0, 960, 120);
        shapes.setColor(.02f, .03f, .045f, .65f); shapes.rect(0, 479, 960, 61);
        UiTheme.panel(shapes, pauseButton.x, pauseButton.y, pauseButton.width, pauseButton.height, playback.paused());
        UiTheme.panel(shapes, skipButton.x, skipButton.y, skipButton.width, skipButton.height, false);
        UiTheme.panel(shapes, nextButton.x, nextButton.y, nextButton.width, nextButton.height, false);
        shapes.end(); Gdx.gl.glDisable(GL20.GL_BLEND);
        batch.begin(); batch.setColor(Color.WHITE);
        titleFont.getData().setScale(.64f); titleFont.setColor(UiTheme.IVORY);
        titleFont.draw(batch, OpeningStory.title(), 35, 515, 470, 8, true);
        titleFont.getData().setScale(.65f); titleFont.setColor(Color.GOLD);
        titleFont.draw(batch, playback.speaker(), 55, 118);
        font.getData().setScale(SUBTITLE_SCALE); font.setColor(UiTheme.IVORY);
        font.draw(batch, playback.text(), 55, 96, 850, 8, true);
        font.getData().setScale(.9f); font.setColor(Color.WHITE);
        font.draw(batch, playback.paused() ? "CONTINUAR [ESPAÇO]" : "PAUSAR [ESPAÇO]", pauseButton.x + 26, 510);
        font.draw(batch, "PULAR [ESC]", skipButton.x + 49, 510);
        boolean canAdvance = !playback.paused() && playback.elapsedInShot() >= shot.minReadSeconds();
        font.setColor(canAdvance ? Color.GOLD : Color.GRAY); font.draw(batch, "PRÓXIMO [ENTER]", nextButton.x + 36, 30);
        font.getData().setScale(.85f); font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "ESPAÇO: pausa | F2: vídeo | ESC: pular com confirmação", 55, 29);
        if (playback.paused()) UiTheme.caption(batch, font, "PAUSA", 454, 455, Color.GOLD);
        font.getData().setScale(1); font.setColor(Color.WHITE); batch.end();
        if (askingSkip) drawSkipConfirmation();
        if (videoPanel.open()) videoPanel.draw(batch, shapes, font);
    }
    private void drawSkipConfirmation() {
        Gdx.gl.glEnable(GL20.GL_BLEND); Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled); shapes.setColor(0, 0, 0, .84f); shapes.rect(0, 0, 960, 540);
        UiTheme.panel(shapes, 95, 145, 770, 265, true);
        UiTheme.panel(shapes, keepWatching.x, keepWatching.y, keepWatching.width, keepWatching.height, false);
        UiTheme.panel(shapes, confirmSkip.x, confirmSkip.y, confirmSkip.width, confirmSkip.height, true);
        shapes.end(); Gdx.gl.glDisable(GL20.GL_BLEND);
        batch.begin(); batch.setColor(Color.WHITE);
        titleFont.getData().setScale(.85f); titleFont.setColor(Color.GOLD); titleFont.draw(batch, "PULAR A INTRODUÇÃO?", 292, 366);
        font.getData().setScale(1); font.setColor(UiTheme.IVORY);
        font.draw(batch, "Você pode rever esta história pelo menu ou pelo refúgio.", 300, 314);
        font.setColor(Color.WHITE); font.draw(batch, "CONTINUAR VENDO [ESC]", 220, 217);
        font.setColor(mouseReleased && inputDelay == 0 ? Color.GOLD : Color.GRAY); font.draw(batch, "PULAR ABERTURA [ENTER]", 576, 217);
        font.getData().setScale(.88f); font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, inputDelay > 0 || !mouseReleased ? "Solte o botão do mouse para confirmar." : "Escolha uma opção para continuar.", 325, 168);
        font.getData().setScale(1); font.setColor(Color.WHITE); batch.end();
    }
    @Override public void pause() {
        videoPanel.suspendInput();
        if (!playback.paused()) playback.togglePause();
        game.audio().stopEmitter(this);
    }
    @Override public void hide() { game.audio().stopEmitter(this); }
    @Override public void dispose() {
        if (disposed) return; disposed = true;
        videoPanel.close(); game.audio().stopEmitter(this);
        if (art != null) art.dispose();
        if (batch != null) batch.dispose(); if (shapes != null) shapes.dispose();
        if (font != null) font.dispose(); if (titleFont != null) titleFont.dispose();
    }
}
