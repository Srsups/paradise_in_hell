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
    private final Rectangle legendaryButton = new Rectangle(35, 375, 890, 28);
    private final Rectangle talkButton = new Rectangle(25, 42, 215, 45), shopButton = new Rectangle(255, 42, 215, 45),
        savesButton = new Rectangle(485, 42, 215, 45), startButton = new Rectangle(715, 42, 220, 45);
    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private BitmapFont font, titleFont;
    private DialoguePlayback playback;
    private Texture atlas;
    private TileMap map;
    private Jogador player;
    private PlayerState stats;
    private State state = State.WALKING;
    private CaronteDialogue.Conversation conversation;
    private int line;
    private String notice = "";
    private float noticeTimer, visualTime;
    private boolean disposed;

    public RefugeScreen(Main game) { this.game = game; }
    @Override public void show() {
        Gdx.input.setInputProcessor(null);
        batch = new SpriteBatch(); shapes = new ShapeRenderer(); font = UiTheme.bodyFont(); titleFont = UiTheme.titleFont();
        atlas = new Texture(Art.ATLAS);
        atlas.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        map = new TileMap(atlas); map.gerarAreaInicialSegura(240, 135, 64);
        map.setBounds(24, 58, 432, 160);
        refreshPlayer();
        player = new Jogador(232, 92, atlas, stats, (x, y) -> {}, game.visuals());
        world.getCamera().position.set(240, 135, 0); world.getCamera().update(); map.update(world.getCamera());
    }
    private void refreshPlayer() {
        stats = new PlayerState(game.profiles().profile().profession());
        game.profiles().profile().bonuses().apply(stats);
        if (game.profiles().profile().weaponRarity() == WeaponRarity.RARE) stats.forgeWeapon();
        if (player != null) player = new Jogador(player.x, player.y, atlas, stats, (x, y) -> {}, game.visuals());
    }
    @Override public void resize(int width, int height) {
        world.update(width, height); hud.update(width, height, true);
        world.getCamera().position.set(240, 135, 0); world.getCamera().update();
        if (map != null) map.update(world.getCamera());
    }
    @Override public void render(float delta) {
        if (disposed) return;
        if(state==State.WALKING && Gdx.input.isKeyJustPressed(Input.Keys.O)) {
            game.setScreen(new AudioSettingsScreen(game,()->new RefugeScreen(game)));return;
        }
        float visualDelta = Math.min(Math.max(delta, 0), .05f);
        visualTime = (visualTime + visualDelta) % 3600;
        if (state != State.WALKING) player.idle(visualDelta);
        noticeTimer = Math.max(0, noticeTimer - delta);
        if (noticeTimer == 0) notice = "";
        Vector2 pointer = hud.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        boolean clicked = Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);
        if(Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            if(!game.profiles().error().isEmpty()) game.profiles().retrySave();
            if(!game.shared().error().isEmpty()) game.shared().retrySave();
        }
        switch (state) {
            case DIALOGUE -> {
                playback.advance(delta);
                if(Gdx.input.isKeyJustPressed(Input.Keys.T)) {
                    game.instantDialogue(!game.instantDialogue()); playback.instant(game.instantDialogue());
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) state = State.WALKING;
                else if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.E) || clicked) {
                    if(playback.accept()==DialoguePlayback.Action.FINISHED
                        && game.profiles().finishConversation(conversation.event())) {
                        notify(conversation.event() == CampaignProfile.Event.CARONTE_MET ? "Caronte entregou 5 óbolos." : "Conversa registrada.");
                        state = State.WALKING;
                    }
                    line=playback.line();
                }
            }
            case SHOP -> {
                if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.L)) state = State.WALKING;
                else if (Gdx.input.isKeyJustPressed(Input.Keys.N) || clicked && legendaryButton.contains(pointer)) unlockLegendary();
                else for (int index = 0; index < shopCards.length; index++) {
                    if (numberPressed(index) || clicked && shopCards[index].contains(pointer)) buy(PermanentAttribute.values()[index]);
                }
            }
            case WALKING -> {
                if(Gdx.input.isKeyJustPressed(Input.Keys.R)) {
                    if(game.latestReport()==null) notify("Nenhuma tentativa recente deste personagem.");
                    else { game.openLastReport(); return; }
                }
                player.update(visualDelta, map, false);
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
    private void talk() { notice = ""; noticeTimer = 0; conversation = CaronteDialogue.select(game.profiles().profile()); line = 0;
        playback = new DialoguePlayback(conversation,game.instantDialogue()); state = State.DIALOGUE; }
    private void unlockLegendary() {
        if (game.necromancerUnlocked()) { notify("Necromante já disponível para novos personagens."); return; }
        if (!game.shared().progress().canUnlockNecromancer()) { notify("Você precisa de "+SharedProgress.necromancerCostText()+" compartilhadas."); return; }
        if (game.unlockNecromancer()) notify("Necromante desbloqueado! Crie um novo save para escolhê-lo.");
        else notify("Desbloqueio não concluído. Suas Almas foram preservadas.");
    }
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
        shapes.end();
        batch.begin();
        batch.setColor(Color.WHITE);
        EquippedArt refugeArt = game.visuals().equipped();
        batch.draw(refugeArt.prop("refuge-crossing", visualTime), 212, 160, 56, 56);
        if (player.y > 127) player.draw(batch);
        batch.draw(refugeArt.prop("refuge-charon", visualTime), 103, 127, 34, 34);
        batch.draw(refugeArt.prop("refuge-altar", visualTime), 326, 120, 48, 48);
        if (player.y <= 127) player.draw(batch);
        font.getData().setScale(0.65f); font.setColor(Color.WHITE);
        font.draw(batch, "Caronte", 101, 177); font.draw(batch, "Altar", 338, 177); font.draw(batch, "Travessia", 220, 156);
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
        text(profile.profession().title + "   |   Vida inicial: " + CombatNumbers.format(stats.getVidaMaxima()) + "   |   Dano: " + CombatNumbers.format(stats.getDano()) + "   |   Ataques/s: " + CombatNumbers.formatRate(stats.attacksPerSecond()), 25, 484, 1, Color.WHITE);
        text("Arma " + profile.weaponRarity().label + ": permanente. Dádivas reiniciam.", 25, 459, 1, Color.LIGHT_GRAY);
        text("Almas compartilhadas: " + game.shared().progress().souls() + " | Fragmentos: " + profile.forgeMaterials(), 540, 459, 1, Color.GOLD);
        text("C: conversar", 62, 71, 1, Color.WHITE); text("L: altar", 321, 71, 1, Color.WHITE);
        text("P: saves", 536, 71, 1, Color.WHITE); text("ENTER: partir", 755, 71, 1, Color.GOLD);
        if(state!=State.DIALOGUE)text("WASD/setas: mover | E: interagir | R: tentativa | O: áudio | ESC: menu", 25, 25, 1, Color.LIGHT_GRAY);
        batch.end();
        if (state != State.WALKING) drawOverlay();
        batch.begin();
        String message = !game.shared().error().isEmpty()?game.shared().error():game.profiles().error().isEmpty()?notice:game.profiles().error();
        if (!message.isEmpty()) text(message, 30, state == State.DIALOGUE ? 255 : state == State.WALKING ? 119 : 109, 0.95f, game.profiles().error().isEmpty() && game.shared().error().isEmpty() ? Color.GOLD : Color.SALMON);
        batch.end();
    }
    private void drawOverlay() {
        if(state==State.DIALOGUE){drawDialogue();return;}
        Gdx.gl.glEnable(GL20.GL_BLEND); Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled); shapes.setColor(0, 0, 0, 0.9f); shapes.rect(0, 92, 960, 348);
        if (state == State.SHOP) { shapes.setColor(0.13f, 0.16f, 0.23f, 1); for (Rectangle card : shopCards) rect(card); }
        shapes.end(); Gdx.gl.glDisable(GL20.GL_BLEND);
        batch.begin();
            text("ALTAR: ATRIBUTOS PERMANENTES", 210, 425, 1.4f, Color.GOLD);
            text(game.necromancerUnlocked() ? "Necromante lendário disponível para novos personagens."
                : "[N / clique] Desbloquear Necromante: "+SharedProgress.necromancerCostText()+" (saldo compartilhado)", 65, 395, 1, Color.MAGENTA);
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
        batch.end();
    }
    private void drawDialogue() {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        UiTheme.panel(shapes,20,18,920,214,true);
        UiTheme.panel(shapes,34,32,180,184,false);
        shapes.end();
        batch.begin();
        boolean charon=playback.current().speaker().equals("Caronte");
        batch.setColor(Color.WHITE);
        batch.draw(charon?game.visuals().region("portrait-charon"):game.visuals().expanded().region("portrait-protagonist"),43,34,162,178);
        batch.setColor(Color.WHITE);
        titleFont.getData().setScale(.65f);titleFont.setColor(UiTheme.IVORY);
        titleFont.draw(batch,playback.current().speaker(),236,207);
        text((line+1)+" / "+conversation.lines().size(),870,206,.85f,Color.LIGHT_GRAY);
        font.getData().setScale(1.25f);font.setColor(UiTheme.IVORY);
        font.draw(batch,playback.visibleText(),236,166,670,com.badlogic.gdx.utils.Align.left,true);
        text(playback.complete()?"ENTER / E / clique: continuar":"ENTER / E / clique: revelar fala",236,70,.9f,UiTheme.TEAL);
        text("T: texto "+(game.instantDialogue()?"instantâneo":"gradual")+" | ESC: fechar",236,45,.85f,Color.LIGHT_GRAY);
        if(playback.complete())text(">",899,55,1.2f,Color.GOLD);
        batch.end();
    }
    private void rect(Rectangle r) { shapes.rect(r.x, r.y, r.width, r.height); }
    private void text(String value, float x, float y, float scale, Color color) {
        font.getData().setScale(scale); font.setColor(color); font.draw(batch, value, x, y);
    }
    @Override public void dispose() {
        if (disposed) return;
        disposed = true; batch.dispose(); shapes.dispose(); font.dispose(); titleFont.dispose(); atlas.dispose(); map.dispose();
    }
}
