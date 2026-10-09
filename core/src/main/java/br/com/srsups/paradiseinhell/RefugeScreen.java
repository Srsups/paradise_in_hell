package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

/** Safe refuge; it owns a fresh preview player and never retains the previous run. */
public final class RefugeScreen extends ScreenAdapter {
    private enum State { WALKING, DIALOGUE, SHOP }
    private final Main game;
    private final FitViewport world = new FitViewport(480, 270), hud = new FitViewport(960, 540);
    private final Rectangle[] shopCards = {
        new Rectangle(35, 145, 280, 225), new Rectangle(340, 145, 280, 225), new Rectangle(645, 145, 280, 225)
    };
    private final Rectangle talkButton = new Rectangle(25, 42, 215, 45), shopButton = new Rectangle(255, 42, 215, 45),
        savesButton = new Rectangle(485, 42, 215, 45), startButton = new Rectangle(715, 42, 220, 45);
    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private BitmapFont font;
    private Texture atlas;
    private TileMap map;
    private Jogador player;
    private PlayerState stats;
    private State state = State.WALKING;
    private CaronteDialogue.Conversation conversation;
    private int line;
    private String notice = "";
    private float noticeTimer;
    private boolean disposed;

    public RefugeScreen(Main game) { this.game = game; }
    @Override public void show() {
        Gdx.input.setInputProcessor(null);
        batch = new SpriteBatch(); shapes = new ShapeRenderer(); font = new BitmapFont();
        atlas = new Texture(Art.ATLAS);
        atlas.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        map = new TileMap(atlas); map.gerarAreaInicialSegura(240, 135, 64);
        map.setBounds(24, 58, 432, 160);
        refreshPlayer();
        player = new Jogador(232, 92, atlas, stats, (x, y) -> {});
        world.getCamera().position.set(240, 135, 0); world.getCamera().update(); map.update(world.getCamera());
    }
    private void refreshPlayer() {
        stats = new PlayerState(game.profiles().profile().profession());
        game.profiles().profile().bonuses().apply(stats);
        if (player != null) player = new Jogador(player.x, player.y, atlas, stats, (x, y) -> {});
    }
    @Override public void resize(int width, int height) {
        world.update(width, height); hud.update(width, height, true);
        world.getCamera().position.set(240, 135, 0); world.getCamera().update();
        if (map != null) map.update(world.getCamera());
    }
    @Override public void render(float delta) {
        if (disposed) return;
        noticeTimer = Math.max(0, noticeTimer - delta);
        if (noticeTimer == 0) notice = "";
        Vector2 pointer = hud.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        boolean clicked = Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);
        if (Gdx.input.isKeyJustPressed(Input.Keys.S) && !game.profiles().error().isEmpty()) game.profiles().retrySave();
        switch (state) {
            case DIALOGUE -> {
                if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) state = State.WALKING;
                else if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.E) || clicked) {
                    if (line + 1 < conversation.lines().size()) line++;
                    else if (game.profiles().finishConversation(conversation.event())) {
                        notify(conversation.event() == CampaignProfile.Event.CARONTE_MET ? "Caronte entregou 5 óbolos." : "Conversa registrada.");
                        state = State.WALKING;
                    }
                }
            }
            case SHOP -> {
                if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.L)) state = State.WALKING;
                else for (int index = 0; index < shopCards.length; index++) {
                    if (numberPressed(index) || clicked && shopCards[index].contains(pointer)) buy(PermanentAttribute.values()[index]);
                }
            }
            case WALKING -> {
                player.update(Math.min(delta, 0.05f), map);
                boolean interact = Gdx.input.isKeyJustPressed(Input.Keys.E);
                if (Gdx.input.isKeyJustPressed(Input.Keys.C) || clicked && talkButton.contains(pointer)
                    || interact && near(120, 145)) talk();
                else if (Gdx.input.isKeyJustPressed(Input.Keys.L) || clicked && shopButton.contains(pointer)
                    || interact && near(350, 145)) state = State.SHOP;
                else if (Gdx.input.isKeyJustPressed(Input.Keys.P) || clicked && savesButton.contains(pointer)) {
                    game.setScreen(new SaveSelectionScreen(game, false, true)); return;
                } else if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || clicked && startButton.contains(pointer)
                    || interact && near(240, 190)) { game.startCampaign(); return; }
                else if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) { game.setScreen(new MenuScreen(game)); return; }
            }
        }
        if (game.getScreen() == this) draw();
    }
    private boolean numberPressed(int index) {
        return Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + index) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1 + index);
    }
    private boolean near(float x, float y) { return Vector2.dst2(player.x + 8, player.y + 8, x, y) < 38 * 38; }
    private void notify(String text) { notice = text; noticeTimer = 3.5f; }
    private void talk() { notice = ""; noticeTimer = 0; conversation = CaronteDialogue.select(game.profiles().profile()); line = 0; state = State.DIALOGUE; }
    private void buy(PermanentAttribute attribute) {
        CampaignProfile profile = game.profiles().profile();
        if (profile.rank(attribute) >= PermanentAttribute.MAX_RANK) notify("Atributo no nível máximo.");
        else if (!profile.canBuy(attribute)) notify("Óbolos insuficientes.");
        else if (game.profiles().buy(attribute)) { refreshPlayer(); notify(attribute.title + " melhorada permanentemente."); }
        else notify("Compra não concluída. Seus óbolos foram preservados.");
    }
    private void draw() {
        ScreenUtils.clear(0.045f, 0.055f, 0.085f, 1); world.apply();
        batch.setProjectionMatrix(world.getCamera().combined);
        batch.begin(); map.draw(batch); batch.end();
        shapes.setProjectionMatrix(world.getCamera().combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.03f, 0.04f, 0.07f, 1);
        shapes.rect(0, 0, 480, 58); shapes.rect(0, 218, 480, 52);
        shapes.rect(0, 58, 24, 160); shapes.rect(456, 58, 24, 160);
        shapes.setColor(0.12f, 0.34f, 0.44f, 1); shapes.rect(95, 130, 50, 24);
        shapes.setColor(0.55f, 0.4f, 0.15f, 1); shapes.rect(335, 130, 30, 25);
        shapes.setColor(0.35f, 0.2f, 0.45f, 1); shapes.rect(215, 185, 50, 25);
        shapes.end();
        batch.begin();
        batch.setColor(Color.CYAN); batch.draw(Art.cell(atlas, 2, 1), 111, 136, 18, 25); batch.setColor(Color.WHITE);
        player.draw(batch);
        font.getData().setScale(0.65f); font.setColor(Color.WHITE);
        font.draw(batch, "Caronte", 101, 177); font.draw(batch, "Altar", 338, 177); font.draw(batch, "Travessia", 220, 216);
        String nearby = near(120, 145) ? "E: conversar com Caronte" : near(350, 145) ? "E: comprar atributos"
            : near(240, 190) ? "E: iniciar a travessia" : "Um intervalo entre desastres divinos.";
        font.draw(batch, nearby, 145, 76);
        batch.end();
        drawHud();
    }
    private void drawHud() {
        CampaignProfile profile = game.profiles().profile();
        hud.apply(); shapes.setProjectionMatrix(hud.getCamera().combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.045f, 0.055f, 0.085f, 1); shapes.rect(0, 440, 960, 100); shapes.rect(0, 0, 960, 92);
        shapes.setColor(0.13f, 0.22f, 0.3f, 1); rect(talkButton); rect(shopButton); rect(savesButton);
        shapes.setColor(0.5f, 0.36f, 0.13f, 1); rect(startButton);
        shapes.end();
        batch.setProjectionMatrix(hud.getCamera().combined); batch.begin();
        text("REFÚGIO DO ESTIGE", 25, 520, 1.6f, Color.GOLD);
        text("Óbolos: " + profile.obols() + "   |   Retornos: " + profile.attempts() + "   |   Guardião vencido: " + profile.victories(), 480, 520, 1, Color.WHITE);
        text(profile.profession().title + "   |   Vida inicial: " + CombatNumbers.format(stats.getVidaMaxima()) + "   |   Dano: " + CombatNumbers.format(stats.getDano()), 25, 484, 1, Color.WHITE);
        text("Dádivas e raridade reiniciam. Compras permanecem.", 25, 459, 1, Color.LIGHT_GRAY);
        text("Fragmentos de Forja: " + profile.forgeMaterials(), 650, 459, 1, Color.GOLD);
        text("C: conversar", 62, 71, 1, Color.WHITE); text("L: altar", 321, 71, 1, Color.WHITE);
        text("P: saves", 536, 71, 1, Color.WHITE); text("ENTER: partir", 755, 71, 1, Color.GOLD);
        text("WASD/setas: mover   |   E: interagir   |   ESC: menu", 25, 25, 1, Color.LIGHT_GRAY);
        batch.end();
        if (state != State.WALKING) drawOverlay();
        batch.begin();
        String message = game.profiles().error().isEmpty() ? notice : game.profiles().error();
        if (!message.isEmpty()) text(message, 30, state == State.WALKING ? 119 : 109, 0.95f, game.profiles().error().isEmpty() ? Color.GOLD : Color.SALMON);
        batch.end();
    }
    private void drawOverlay() {
        Gdx.gl.glEnable(GL20.GL_BLEND); Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled); shapes.setColor(0, 0, 0, 0.9f); shapes.rect(0, 92, 960, 348);
        if (state == State.SHOP) { shapes.setColor(0.13f, 0.16f, 0.23f, 1); for (Rectangle card : shopCards) rect(card); }
        shapes.end(); Gdx.gl.glDisable(GL20.GL_BLEND);
        batch.begin();
        if (state == State.DIALOGUE) {
            CaronteDialogue.Line current = conversation.lines().get(line);
            text(current.speaker(), 70, 365, 1.5f, Color.GOLD);
            font.getData().setScale(1.2f); font.setColor(Color.WHITE);
            font.draw(batch, current.text(), 70, 312, 820, 8, true);
            text("ENTER / E / clique: continuar   |   ESC: fechar", 240, 165, 1, Color.LIGHT_GRAY);
        } else {
            text("ALTAR: ATRIBUTOS PERMANENTES", 210, 415, 1.4f, Color.GOLD);
            CampaignProfile profile = game.profiles().profile();
            for (int index = 0; index < shopCards.length; index++) {
                PermanentAttribute attribute = PermanentAttribute.values()[index];
                Rectangle card = shopCards[index]; int rank = profile.rank(attribute);
                text("[" + (index + 1) + "] " + attribute.title, card.x + 18, card.y + 190, 1.2f, Color.GOLD);
                text("Nível " + rank + " / " + PermanentAttribute.MAX_RANK, card.x + 18, card.y + 150, 1, Color.WHITE);
                font.draw(batch, attribute.description, card.x + 18, card.y + 110, card.width - 36, 8, true);
                String price = rank == PermanentAttribute.MAX_RANK ? "NÍVEL MÁXIMO" : attribute.price(rank) + " óbolos";
                text(price, card.x + 18, card.y + 50, 1.2f, profile.canBuy(attribute) ? Color.GREEN : Color.LIGHT_GRAY);
            }
            text("Clique ou 1, 2, 3: comprar   |   ESC / L: fechar", 250, 133, 1, Color.WHITE);
        }
        batch.end();
    }
    private void rect(Rectangle r) { shapes.rect(r.x, r.y, r.width, r.height); }
    private void text(String value, float x, float y, float scale, Color color) {
        font.getData().setScale(scale); font.setColor(color); font.draw(batch, value, x, y);
    }
    @Override public void dispose() {
        if (disposed) return;
        disposed = true; batch.dispose(); shapes.dispose(); font.dispose(); atlas.dispose(); map.dispose();
    }
}
