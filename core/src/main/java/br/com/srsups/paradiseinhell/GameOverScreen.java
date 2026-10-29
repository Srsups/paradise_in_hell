package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class GameOverScreen extends ScreenAdapter {
    private final Main game;
    private final boolean victory;
    private final RunSession session;
    private final float elapsed;
    private final int kills, level;
    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font = UiTheme.bodyFont();
    private final FitViewport viewport = new FitViewport(960, 540);
    private float inputDelay = 0.4f;

    public GameOverScreen(Main game, boolean victory, RunSession session) {
        if (victory && !session.isDemo() && !session.bossDefeated()) throw new IllegalStateException("Boss must be defeated");
        this.game = game;
        this.victory = victory;
        this.session = session;
        this.elapsed = session.progress().elapsed();
        this.kills = session.progress().kills();
        this.level = session.player().getNivel();
    }
    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void render(float delta) {
        inputDelay -= delta;
        if (inputDelay <= 0) {
            if(canReadEpilogue() && Gdx.input.isKeyJustPressed(Input.Keys.C)) {
                game.openUnderworldEpilogue(()->new GameOverScreen(game,victory,session));return;
            }
            if(Gdx.input.isKeyJustPressed(Input.Keys.R)) {
                game.setScreen(new RunReportScreen(game,session.report(victory?RunOutcome.VICTORY:RunOutcome.DEATH,game.activeSlot()),
                    ()->new GameOverScreen(game,victory,session))); return;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                game.setScreen(session.isDemo() ? new GameScreen(game, session.newAttempt()) : new RefugeScreen(game));
                return;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                game.setScreen(new MenuScreen(game));
                return;
            }
        }
        ScreenUtils.clear(0.045f, 0.055f, 0.085f, 1);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        line(victory ? (session.isDemo() ? "VOCÊ SOBREVIVEU!" : "GUARDIÃO DERROTADO!") : "VOCÊ MORREU", 355, 2.5f, victory ? Color.GOLD : Color.SALMON);
        line("Tempo: " + RunProgress.formatTime(elapsed) + "   |   Abates: " + kills + "   |   Nível: " + level, 280, 1.3f, Color.WHITE);
        line(victory ? (session.isDemo() ? "A travessia da demo está completa."
            : canReadEpilogue()?"Centelha da Insurgência obtida. A Ascensão continua em desenvolvimento."
                : "Você venceu o chefe do submundo. A jornada continua em desenvolvimento.")
            : "Cada tentativa é uma nova travessia.", 225, 1.2f, Color.LIGHT_GRAY);
        if (!session.isDemo()) line("Óbolos: +" + session.earnedObols() + " | Almas: +" + session.earnedSouls() + " | Almas compartilhadas: " + game.shared().progress().souls(), 185, 1.1f, Color.WHITE);
        line(session.isDemo() ? "ENTER: jogar de novo   |   ESC ou clique: menu"
            : "ENTER: retornar ao refúgio   |   ESC ou clique: menu", 145, 1.2f, Color.GOLD);
        line("R: resumo detalhado da tentativa", 105, 1, Color.CYAN);
        if(canReadEpilogue())line(game.profiles().profile().has(CampaignProfile.Event.UNDERWORLD_EPILOGUE_READ)
            ?"C: rever o desfecho da Centelha":"C: retomar o desfecho da Centelha",67,.95f,UiTheme.TEAL);
        if(!game.profiles().error().isEmpty()) {
            font.getData().setScale(.85f);font.setColor(Color.SALMON);
            font.draw(batch,game.profiles().error(),30,34,900,com.badlogic.gdx.utils.Align.center,true);
        }
        batch.end();
    }
    private boolean canReadEpilogue() {
        return !session.isDemo() && session.characterId().equals(game.profiles().profile().characterId())
            && game.hasUnderworldEpilogue();
    }
    private void line(String text, float y, float scale, Color color) {
        font.getData().setScale(scale);
        font.setColor(color);
        GlyphLayout layout = new GlyphLayout(font, text);
        font.draw(batch, layout, (960 - layout.width) / 2, y);
    }
    @Override public void dispose() { batch.dispose(); font.dispose(); }
}
