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
import java.util.List;

/** Safe completion reward. The session owns choices and consumption, so reopening cannot reroll. */
public final class ArenaRewardScreen extends ScreenAdapter {
    private final Main game;
    private final RunSession session;
    private final boolean recovery;
    private final ChoiceInputGuard choiceGuard = new ChoiceInputGuard();
    private final BoonPanel boonPanel;
    private final List<Upgrade> choices;
    private final Rectangle[] cards;
    private final FitViewport viewport = new FitViewport(960, 540);
    private final SpriteBatch batch = new SpriteBatch();
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont font = UiTheme.bodyFont();
    private float inputDelay = 0.2f,visualTime;

    public ArenaRewardScreen(Main game, RunSession session) {
        if (!session.canClaimArenaReward()) throw new IllegalStateException("No pending arena reward");
        this.game = game; this.session = session;
        boonPanel = new BoonPanel(session);
        recovery = session.arena().reward() == ArenaReward.RECOVERY_CHEST;
        choices = recovery ? List.of() : session.arenaRewardChoices();
        int count = recovery ? 2 : Math.max(1, choices.size());
        cards = recovery ? new Rectangle[count] : BoonChoiceView.cards(count);
        if(recovery) {
            float width=(890f-20*(count-1))/count;
            for(int i=0;i<count;i++)cards[i]=new Rectangle(35+i*(width+20),155,width,125);
        }
    }
    @Override public void show() { Gdx.input.setInputProcessor(null); }
    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void render(float delta) {
        boolean panelWasOpen = boonPanel.open();
        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) boonPanel.toggle();
        else if (boonPanel.open() && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) boonPanel.close();
        if (boonPanel.open()) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) boonPanel.movePage(-1);
            if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) boonPanel.movePage(1);
        }
        boolean showingPanel = panelWasOpen || boonPanel.open();
        if (!showingPanel) {
            inputDelay -= delta;visualTime+=delta;
            if (!recovery) choiceGuard.advance(delta, Gdx.input.isButtonPressed(Input.Buttons.LEFT));
        }
        Vector2 pointer = viewport.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        if (!showingPanel && (recovery ? inputDelay <= 0 : choiceGuard.ready())) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                game.finishRun(session, RunOutcome.ABANDONED); return;
            }
            for (int i = 0; i < cards.length; i++) {
                boolean number = Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + i)
                    || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1 + i);
                boolean open = i == 0 && (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                    || recovery && Gdx.input.isKeyJustPressed(Input.Keys.F));
                if (number || open || Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && cards[i].contains(pointer)) {
                    if (recovery) session.claimRecoveryReward(i == 0);
                    else session.claimBoonReward(choices.isEmpty() ? null : choices.get(i));
                    game.offerRoute(session); return;
                }
            }
        }
        if(!recovery) {
            viewport.apply();shapes.setProjectionMatrix(viewport.getCamera().combined);batch.setProjectionMatrix(viewport.getCamera().combined);
            BoonChoiceView.draw(batch,shapes,font,game.visuals(),session,choices,cards,"RECOMPENSA DAS SOMBRAS",
                choiceGuard.ready()?"Dádiva extra Rara ou superior.":choiceGuard.notice(),"abandonar",visualTime);
            if(boonPanel.open())boonPanel.draw(batch,shapes,font,game.visuals());return;
        }
        ScreenUtils.clear(0.045f, 0.055f, 0.085f, 1); viewport.apply();
        shapes.setProjectionMatrix(viewport.getCamera().combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.13f, 0.16f, 0.23f, 1);
        for (Rectangle card : cards) shapes.rect(card.x, card.y, card.width, card.height);
        shapes.end();
        batch.setProjectionMatrix(viewport.getCamera().combined); batch.begin();
        batch.draw(game.visuals().expanded().effect("chest-special",Math.min(1,visualTime/.8f)),430,300,100,72);
        font.setColor(Color.GOLD); font.getData().setScale(1.6f);
        font.draw(batch, recovery ? "BAÚ DAS CINZAS" : "RECOMPENSA DAS SOMBRAS", 240, 485, 480, 1, false);
        font.getData().setScale(1); font.setColor(Color.WHITE);
        font.draw(batch, recovery ? "Sala concluída. Abra o baú e escolha um benefício."
            : "Sala concluída. Escolha uma dádiva extra Rara ou superior.", 160, 435, 640, 1, true);
        for (int i = 0; i < cards.length; i++) {
            Rectangle card = cards[i];
            if (recovery) {
                boolean full = session.player().getVida() >= session.player().getVidaMaxima();
                font.setColor(Color.GOLD);
                font.draw(batch, "[" + (i + 1) + "] " + (i == 0 ? "Recuperação" : "Óbolos"), card.x + 18, card.y + 100);
                font.setColor(Color.WHITE);
                font.draw(batch, i == 0 ? full ? "Vida cheia: receba " + session.previewChestObols(5) + " Óbolos ao retornar."
                    : "Recupere até 40 de vida para a próxima luta." : "Receba " + session.previewChestObols(5) + " Óbolos ao retornar ao refúgio.",
                    card.x + 18, card.y + 65, card.width - 36, 8, true);
            } else if (choices.isEmpty()) {
                font.setColor(Color.GOLD); font.draw(batch, "[1] Reserva de Óbolos", card.x + 18, card.y + 155);
                font.setColor(Color.WHITE);
                font.draw(batch, "Todas as dádivas Raras ou superiores atingiram seu limite. Receba " + session.previewChestObols(8) + " Óbolos ao retornar.",
                    card.x + 18, card.y + 110, card.width - 36, 8, true);
            } else {
                Upgrade boon = choices.get(i);
                font.setColor(boon.rarity.red, boon.rarity.green, boon.rarity.blue, 1);
                font.draw(batch, "[" + (i + 1) + "] " + boon.title, card.x + 18, card.y + 160, card.width - 36, 8, true);
                font.draw(batch, boon.rarity.label, card.x + 18, card.y + 120);
                font.setColor(Color.WHITE);
                font.draw(batch, boon.description(session.player()), card.x + 18, card.y + 95, card.width - 36, 8, true);
                font.draw(batch, "Obtida " + session.rank(boon) + " vez(es)", card.x + 18, card.y + 30);
            }
        }
        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "Vida: " + CombatNumbers.format(session.player().getVida()) + "/"
            + CombatNumbers.format(session.player().getVidaMaxima()) + " | Óbolos ao retornar: " + session.earnedObols(), 180, 110);
        font.draw(batch, recovery ? "F/ENTER: recuperação | 1, 2 ou clique: escolher | TAB: dádivas | ESC: abandonar"
            : "1, 2, 3 ou clique: escolher | ENTER: primeira opção | ESC: abandonar", 180, 65);
        if (!recovery && !choiceGuard.ready()) {
            font.setColor(Color.GOLD); font.draw(batch, choiceGuard.notice(), 180, 35);
        }
        batch.end();
        if (boonPanel.open()) boonPanel.draw(batch, shapes, font,game.visuals());
    }
    @Override public void dispose() { batch.dispose(); shapes.dispose(); font.dispose(); }
}
