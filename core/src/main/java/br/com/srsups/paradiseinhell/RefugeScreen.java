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
    private final TutorialGuide tutorial = new TutorialGuide(false);
    private final TutorialPanel tutorialPanel;
    private final VideoPanel videoPanel;
    private final ControlsPanel controlsPanel;
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
    private final DialoguePanel dialoguePanel=new DialoguePanel(20,18,920,214,180,1.25f);
    private Texture atlas;
    private TileMap map;
    private Jogador player;
    private PlayerState stats;
    private State state = State.WALKING;
    private CaronteDialogue.Conversation conversation;
    private int line;
    private String notice = "";
    private float noticeTimer, visualTime;
    private int shopPage;
    private static final int SHOP_PER_PAGE=3;
    private final Rectangle previousShopPage=new Rectangle(35,113,125,28),nextShopPage=new Rectangle(800,113,125,28);
    public int shopPage(){return shopPage;}
    public int shopPageCount(){return (PermanentAttribute.values().length+SHOP_PER_PAGE-1)/SHOP_PER_PAGE;}
    private boolean disposed;

    public RefugeScreen(Main game) {
        this.game = game;
        videoPanel = new VideoPanel(game);
        controlsPanel = new ControlsPanel(game);
        tutorialPanel = new TutorialPanel(game, tutorial, game.profiles().profile().characterId(), false);
    }
    @Override public void show() {
        Gdx.input.setInputProcessor(null);
        batch = new SpriteBatch(); shapes = new ShapeRenderer(); font = UiTheme.bodyFont(); titleFont = UiTheme.titleFont();
        atlas = new Texture(Art.ATLAS);
        atlas.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        map = new TileMap(atlas,game.visuals().scenery(),SceneryTheme.REFUGE); map.gerarAreaInicialSegura(240, 135, 64);
        map.setBounds(24, 58, 432, 160);
        refreshPlayer();
        player = new Jogador(232, 92, atlas, stats, (x, y) -> {}, game.visuals(), game.controls());
        world.getCamera().position.set(240, 135, 0); world.getCamera().update(); map.update(world.getCamera());
    }
    private void refreshPlayer() {
        stats = new PlayerState(game.profiles().profile().profession());
        game.profiles().profile().bonuses().apply(stats);
        if (game.profiles().profile().weaponRarity() == WeaponRarity.RARE) stats.forgeWeapon();
        if (player != null) player = new Jogador(player.x, player.y, atlas, stats, (x, y) -> {}, game.visuals(), game.controls());
    }
    @Override public void resize(int width, int height) {
        world.update(width, height); hud.update(width, height, true);
        world.getCamera().position.set(240, 135, 0); world.getCamera().update();
        if (map != null) map.update(world.getCamera());
    }
    @Override public void render(float delta) {
        if (disposed) return;
        if (controlsPanel.input(delta,hud.unproject(new Vector2(Gdx.input.getX(),Gdx.input.getY())))) { videoPanel.suspendInput();draw(); return; }
        if (videoPanel.input(delta,hud.unproject(new Vector2(Gdx.input.getX(),Gdx.input.getY())))) { draw(); return; }
        if (tutorialPanel.input(hud.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY())))) { draw(); return; }
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
            game.recoverPendingSettlement();
        }
        switch (state) {
            case DIALOGUE -> {
                playback.advance(delta);
                if(Gdx.input.isKeyJustPressed(Input.Keys.T)) {
                    game.instantDialogue(!game.instantDialogue()); playback.instant(game.instantDialogue());
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) state = State.WALKING;
                else if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || game.controls().justPressed(ControlAction.INTERACT) || clicked) {
                    if(playback.accept()==DialoguePlayback.Action.FINISHED
                        && game.profiles().finishConversation(conversation.event())) {
                        notify(conversation.event() == CampaignProfile.Event.CARONTE_MET ? "Caronte entregou 5 óbolos." : "Conversa registrada.");
                        tutorialPanel.observe(TutorialTopic.REFUGE);
                        state = State.WALKING;
                    }
                    line=playback.line();
                }
            }
            case SHOP -> {
                if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.L)) state = State.WALKING;
                else if(Gdx.input.isKeyJustPressed(Input.Keys.LEFT)||clicked&&previousShopPage.contains(pointer))shopPage=Math.max(0,shopPage-1);
                else if(Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)||clicked&&nextShopPage.contains(pointer))shopPage=Math.min(shopPageCount()-1,shopPage+1);
                else if (Gdx.input.isKeyJustPressed(Input.Keys.N) || clicked && legendaryButton.contains(pointer)) unlockLegendary();
                else for (int index = 0; index < shopCards.length; index++) {
                    int attribute=shopPage*SHOP_PER_PAGE+index;
                    if(attribute<PermanentAttribute.values().length&&(numberPressed(index)||clicked&&shopCards[index].contains(pointer)))
                        buy(PermanentAttribute.values()[attribute]);
                }
            }
            case WALKING -> {
                if(Gdx.input.isKeyJustPressed(Input.Keys.I)) {game.openOpening(()->new RefugeScreen(game),false);return;}
                if(Gdx.input.isKeyJustPressed(Input.Keys.V) && game.hasUnderworldEpilogue()) {
                    game.openUnderworldEpilogue(()->new RefugeScreen(game));return;
                }
                if(Gdx.input.isKeyJustPressed(Input.Keys.J)) { game.setScreen(FarmerQuestScreen.journal(game));return; }
                if(Gdx.input.isKeyJustPressed(Input.Keys.R)) {
                    if(game.latestReport()==null) notify("Nenhuma tentativa recente deste personagem.");
                    else { game.openLastReport(); return; }
                }
                float beforeX = player.x, beforeY = player.y; boolean wasDashing = stats.isDashing();
                player.update(visualDelta, map, false);
                if(Vector2.dst2(beforeX,beforeY,player.x,player.y)>.0001f)tutorialPanel.observe(TutorialTopic.MOVE);
                if(!wasDashing && stats.isDashing())tutorialPanel.observe(TutorialTopic.DASH);
                // ENTER remains the refuge's global departure shortcut, even near an NPC.
                if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || clicked && startButton.contains(pointer)) {
                    tutorialPanel.observe(TutorialTopic.REFUGE);game.startCampaign();return;
                }
                boolean interact = game.controls().justPressed(ControlAction.INTERACT);
                if (Gdx.input.isKeyJustPressed(Input.Keys.C) || clicked && talkButton.contains(pointer)
                    || interact && near(120, 145)) talk();
                else if (Gdx.input.isKeyJustPressed(Input.Keys.L) || clicked && shopButton.contains(pointer)
                    || interact && near(350, 145)) {shopPage=0;state=State.SHOP;}
                else if (Gdx.input.isKeyJustPressed(Input.Keys.P) || clicked && savesButton.contains(pointer)) {
                    game.setScreen(new SaveSelectionScreen(game, false, true)); return;
                } else if (interact && near(240, 190)) { tutorialPanel.observe(TutorialTopic.REFUGE);game.startCampaign(); return; }
                else if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) { game.setScreen(new MenuScreen(game)); return; }
            }
        }
        if (game.getScreen() == this) {tutorialPanel.advance(visualDelta,tutorialContext());draw();}
    }
    private TutorialGuide.Context tutorialContext() {
        return new TutorialGuide.Context(TutorialGuide.Place.REFUGE, state == State.WALKING && !controlsPanel.open() && !videoPanel.open() && !tutorialPanel.open(),
            noticeTimer > 0 || !game.profiles().error().isEmpty() || !game.shared().error().isEmpty(),
            false, false, game.profiles().profile().attempts() > 0);
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
        else if (game.profiles().buy(attribute)) { tutorialPanel.observe(TutorialTopic.RETURN);refreshPlayer(); notify(attribute.title + " melhorada permanentemente."); }
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
        SceneryVisuals.boundary(batch,game.visuals().scenery(),SceneryTheme.REFUGE,24,58,432,160);
        batch.setColor(Color.WHITE);
        EquippedArt refugeArt = game.visuals().equipped();
        batch.draw(refugeArt.prop("refuge-crossing", visualTime), 212, 160, 56, 56);
        if (player.y > 127) player.draw(batch);
        batch.draw(refugeArt.prop("refuge-charon", visualTime), 103, 127, 34, 34);
        batch.draw(refugeArt.prop("refuge-altar", visualTime), 326, 120, 48, 48);
        if (player.y <= 127) player.draw(batch);
        font.getData().setScale(0.65f);
        UiTheme.caption(batch,font,"Caronte",101,177,UiTheme.IVORY);
        UiTheme.caption(batch,font,"Altar",338,177,UiTheme.IVORY);
        UiTheme.caption(batch,font,"Travessia",220,156,UiTheme.IVORY);
        String nearby = near(120, 145) ? interactionControl("C")+": conversar com Caronte" : near(350, 145) ? interactionControl("L")+": comprar atributos"
            : near(240, 190) ? interactionControl("ENTER")+": iniciar a travessia" : "Um intervalo entre desastres divinos.";
        UiTheme.caption(batch,font,nearby,145,76,UiTheme.IVORY);
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
        tutorialPanel.footer(batch,font,tutorialContext());
        if(state==State.WALKING)text(game.hasUnderworldEpilogue()?"I: abertura | V: Centelha":"I: rever abertura",700,112,.85f,Color.GOLD);
        batch.end();
        if (state != State.WALKING) drawOverlay();
        batch.begin();
        String message = !game.shared().error().isEmpty()?game.shared().error():game.profiles().error().isEmpty()?notice:game.profiles().error();
        if (!message.isEmpty()) text(message, 30, state == State.DIALOGUE ? 255 : state == State.WALKING ? 119 : 109, 0.95f, game.profiles().error().isEmpty() && game.shared().error().isEmpty() ? Color.GOLD : Color.SALMON);
        batch.end();
        if(tutorialPanel.open())tutorialPanel.draw(batch,shapes,font);
        if(videoPanel.open())videoPanel.draw(batch,shapes,font);
        if(controlsPanel.open())controlsPanel.draw(batch,shapes,font);
    }
    private void drawOverlay() {
        if(state==State.DIALOGUE){drawDialogue();return;}
        Gdx.gl.glEnable(GL20.GL_BLEND); Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled); shapes.setColor(0, 0, 0, 0.9f); shapes.rect(0, 92, 960, 348);
        if (state == State.SHOP) {
            shapes.setColor(0.13f, 0.16f, 0.23f, 1); for (Rectangle card : shopCards) rect(card);
            rect(previousShopPage);rect(nextShopPage);
        }
        shapes.end(); Gdx.gl.glDisable(GL20.GL_BLEND);
        batch.begin();
            text("ALTAR: ATRIBUTOS PERMANENTES", 210, 425, 1.4f, Color.GOLD);
            text(game.necromancerUnlocked() ? "Necromante lendário disponível para novos personagens."
                : "[N / clique] Desbloquear Necromante: "+SharedProgress.necromancerCostText()+" (saldo compartilhado)", 65, 395, 1, Color.MAGENTA);
            CampaignProfile profile = game.profiles().profile();
            for (int index = 0; index < shopCards.length; index++) {
                int attributeIndex=shopPage*SHOP_PER_PAGE+index;
                if(attributeIndex>=PermanentAttribute.values().length)continue;
                PermanentAttribute attribute = PermanentAttribute.values()[attributeIndex];
                Rectangle card = shopCards[index]; int rank = profile.rank(attribute);
                font.getData().setScale(1);font.setColor(Color.GOLD);
                font.draw(batch,"["+(index+1)+"] "+attribute.title,card.x+18,card.y+195,card.width-36,8,true);
                text("Nível " + rank + " / " + PermanentAttribute.MAX_RANK, card.x + 18, card.y + 164, 1, Color.WHITE);
                font.getData().setScale(.92f);
                font.draw(batch, attribute.description, card.x + 18, card.y + 137, card.width - 36, 8, true);
                font.setColor(Color.LIGHT_GRAY);
                font.draw(batch,attribute.preview(stats,rank),card.x+18,card.y+82,card.width-36,8,true);
                String price = rank == PermanentAttribute.MAX_RANK ? "NÍVEL MÁXIMO" : attribute.price(rank) + " óbolos";
                text(price, card.x + 18, card.y + 40, 1.2f, profile.canBuy(attribute) ? Color.GREEN : Color.LIGHT_GRAY);
            }
            text("Anterior",55,133,.9f,shopPage>0?Color.WHITE:Color.GRAY);
            text("Próxima",829,133,.9f,shopPage+1<shopPageCount()?Color.WHITE:Color.GRAY);
            text("1, 2, 3 / clique: comprar | Setas: página "+(shopPage+1)+"/"+shopPageCount()+" | ESC / L: fechar",205,133,.85f,Color.WHITE);
        batch.end();
    }
    private void drawDialogue() {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        dialoguePanel.background(shapes);
        shapes.end();
        batch.begin();
        String readControl=game.controlSettings().value().bindings(ControlAction.INTERACT).equals(ControlSettings.DEFAULT.bindings(ControlAction.INTERACT))
            ?"ENTER / E / clique":game.controls().label(ControlAction.INTERACT)+" / ENTER / clique";
        dialoguePanel.draw(batch,titleFont,font,game.visuals(),playback,
            readControl+(playback.complete()?": continuar":": revelar fala"),
            "T: texto "+(game.instantDialogue()?"instantâneo":"gradual")+" | ESC: fechar");
        batch.end();
    }
    private void rect(Rectangle r) { shapes.rect(r.x, r.y, r.width, r.height); }
    private String interactionControl(String fallback) {
        for (InputBinding binding:game.controlSettings().value().bindings(ControlAction.INTERACT))
            if (!binding.equals(InputBinding.key(Input.Keys.ENTER))) return binding.label();
        return fallback;
    }
    private void text(String value, float x, float y, float scale, Color color) {
        font.getData().setScale(scale); font.setColor(color); font.draw(batch, value, x, y);
    }
    @Override public void pause() { videoPanel.suspendInput(); }
    @Override public void dispose() {
        if (disposed) return;
        disposed = true; controlsPanel.close();videoPanel.close();batch.dispose(); shapes.dispose(); font.dispose(); titleFont.dispose(); atlas.dispose(); map.dispose();tutorialPanel.close();
    }
}
