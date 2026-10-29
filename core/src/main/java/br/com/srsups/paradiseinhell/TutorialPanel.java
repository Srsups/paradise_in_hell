package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import java.util.List;

/** Shared reference overlay. It borrows graphics and never marks topics merely by opening. */
public final class TutorialPanel {
    public static final float DETAIL_WIDTH = 862, DETAIL_SCALE = .88f;
    private static final List<TutorialTopic> BASICS = List.of(TutorialTopic.MOVE, TutorialTopic.ATTACK,
        TutorialTopic.DASH, TutorialTopic.EXPERIENCE, TutorialTopic.BOONS);
    private static final List<TutorialTopic> CAMPAIGN = List.of(TutorialTopic.REFUGE, TutorialTopic.GOALS,
        TutorialTopic.EXIT, TutorialTopic.GUARDIAN, TutorialTopic.RETURN);
    private final Main game;
    private final TutorialGuide tutorial;
    private final String characterId;
    private final boolean demo;
    private final Profession profession;
    private final Rectangle close = new Rectangle(35, 52, 205, 42), previous = new Rectangle(275, 52, 125, 42),
        next = new Rectangle(415, 52, 125, 42), hints = new Rectangle(605, 52, 320, 42);
    private boolean open, mouseReleased;
    private int page;

    public TutorialPanel(Main game, TutorialGuide tutorial, String characterId, boolean demo) {
        this(game,tutorial,characterId,demo,game.profiles().profile().profession());
    }
    public TutorialPanel(Main game, TutorialGuide tutorial, String characterId, boolean demo, Profession profession) {
        this.game = game; this.tutorial = tutorial; this.characterId = characterId; this.demo = demo;
        this.profession=java.util.Objects.requireNonNull(profession);
        synchronize();
    }
    public boolean open() { return open; }
    public int page() { return page; }
    public int pageCount() { return demo ? 1 : 2; }
    public void close() { open = false; }
    public List<TutorialTopic> topics() { return page == 0 ? BASICS : CAMPAIGN; }
    public String detail(TutorialTopic topic) {
        ControlSettings controls=game.controlSettings().value();
        if(topic==TutorialTopic.ATTACK && profession==Profession.GUERREIRO) {
            if(!TutorialGuide.changed(controls,ControlAction.ATTACK)&&!TutorialGuide.changed(controls,ControlAction.GUARD))
                return "Mire e segure o botão esquerdo para atacar. O botão direito defende na direção da mira; você pode atacar defendendo. Solte o escudo para dar dash.";
            return "Mire com o mouse. Ataque: "+controls.label(ControlAction.ATTACK)+". Defesa: "+controls.label(ControlAction.GUARD)+" na direção da mira. Você pode atacar defendendo; solte o escudo para dar dash.";
        }
        if(topic==TutorialTopic.ATTACK && profession==Profession.FAZENDEIRO) {
            if(!TutorialGuide.changed(controls,ControlAction.ATTACK)&&!TutorialGuide.changed(controls,ControlAction.SWITCH_TOOL))
                return "Mire e segure o botão esquerdo. Q alterna ancinho (estocada) e foice (corte circular), só no combate ativo. As duas ferramentas compartilham a recarga.";
            return "Mire com o mouse. Ataque: "+controls.label(ControlAction.ATTACK)+". "+controls.label(ControlAction.SWITCH_TOOL)+" alterna ancinho e foice no combate ativo. As ferramentas compartilham a recarga.";
        }
        return TutorialGuide.detail(topic,controls);
    }
    public void movePage(int amount) { page = Math.max(0, Math.min(pageCount() - 1, page + amount)); }
    public Rectangle bodyBounds(int row) {
        if(row<0 || row>=5)throw new IllegalArgumentException("Invalid tutorial row");
        return new Rectangle(48,389-row*68,DETAIL_WIDTH,34);
    }
    public void synchronize() {
        CampaignProfile profile = game.profiles().profile();
        tutorial.synchronize(profile.tutorialEnabled(), profile::tutorialSeen);
        flush();
    }
    /** True consumes the whole frame, including the frame in which the overlay closes. */
    public boolean input(Vector2 pointer) {
        synchronize();
        boolean wasOpen = open;
        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
            open = !open;
            if (open) { page = 0; mouseReleased = false; game.audio().stopEffects(); }
            return true;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.H)) toggleHints();
        if (!open) return wasOpen;
        mouseReleased |= !Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        boolean click = mouseReleased && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || click && close.contains(pointer)) { close(); return true; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT) || click && previous.contains(pointer)) movePage(-1);
        else if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) || click && next.contains(pointer)) movePage(1);
        else if (click && hints.contains(pointer)) toggleHints();
        return true;
    }
    private void toggleHints() {
        game.profiles().setTutorialEnabled(characterId, !tutorial.enabled()); synchronize();
    }
    public void observe(TutorialTopic topic) { tutorial.action(topic); flush(); }
    public void advance(float delta, TutorialGuide.Context context) {
        synchronize(); tutorial.advance(delta, context); flush();
    }
    private void flush() {
        TutorialTopic topic;
        while ((topic = tutorial.pollCompleted()) != null)
            if (!demo) game.profiles().completeTutorial(characterId, topic);
    }
    public void footer(SpriteBatch batch, BitmapFont font, TutorialGuide.Context context) {
        if (!context.active() || context.priorityNotice() || open) return;
        TutorialTopic topic = tutorial.current(context);
        String copy = topic == null ? "F1: guia de jogo | F3: controles | H: " + (tutorial.enabled() ? "desativar dicas" : "ativar dicas")
            : TutorialGuide.hint(topic,game.controlSettings().value()) + "  |  F1: guia";
        font.getData().setScale(.85f);
        UiTheme.caption(batch, font, copy, 25, 25, topic == null ? Color.LIGHT_GRAY : UiTheme.IVORY);
        font.getData().setScale(1); font.setColor(Color.WHITE);
    }
    public void draw(SpriteBatch batch, ShapeRenderer shapes, BitmapFont font) {
        if (!open) return;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(UiTheme.INK); shapes.rect(0, 0, 960, 540);
        for (int row = 0; row < topics().size(); row++) UiTheme.panel(shapes, 35, 388 - row * 68, 890, 65, false);
        for (Rectangle button : new Rectangle[]{close, previous, next, hints})
            UiTheme.panel(shapes, button.x, button.y, button.width, button.height, false);
        shapes.end();
        batch.begin(); batch.setColor(Color.WHITE);
        font.getData().setScale(1.7f); font.setColor(Color.GOLD); font.draw(batch, "GUIA DE JOGO", 35, 511);
        font.getData().setScale(.95f); font.setColor(UiTheme.IVORY);
        font.draw(batch, demo ? "Demo: sobreviva por 05:00. Não concede moedas ou compras da campanha."
            : "Página " + (page + 1) + "/2 — " + (page == 0 ? "Combate e controles" : "Travessia e progressão"), 35, 478);
        for (int row = 0; row < topics().size(); row++) {
            TutorialTopic topic = topics().get(row); float y = 443 - row * 68;
            font.getData().setScale(.98f); font.setColor(UiTheme.TEAL);
            font.draw(batch, TutorialGuide.title(topic), 48, y);
            font.getData().setScale(DETAIL_SCALE); font.setColor(Color.WHITE);
            font.draw(batch, detail(topic), 48, y - 20, DETAIL_WIDTH, 8, true);
        }
        font.getData().setScale(.95f); font.setColor(Color.WHITE);
        font.draw(batch, "F1 / ESC: fechar", 64, 80);
        font.setColor(page > 0 ? Color.WHITE : Color.GRAY); font.draw(batch, "Anterior", 307, 80);
        font.setColor(page + 1 < pageCount() ? Color.WHITE : Color.GRAY); font.draw(batch, "Próxima", 449, 80);
        font.setColor(Color.GOLD); font.draw(batch, "H: dicas " + (tutorial.enabled() ? "ATIVADAS" : "DESATIVADAS"), 667, 80);
        font.getData().setScale(.85f); font.setColor(Color.LIGHT_GRAY);
        String message = game.profiles().error().isEmpty() ? "Jogo pausado. Setas mudam a página; consultar não gasta recursos."
            : game.profiles().error();
        font.draw(batch, message, 35, 30, 890, 8, true);
        font.getData().setScale(1); font.setColor(Color.WHITE); batch.end();
    }
}
