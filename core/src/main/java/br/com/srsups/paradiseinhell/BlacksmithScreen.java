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

/** Rare, safe encounter. Material and permanent rarity commit before changing the run. */
public final class BlacksmithScreen extends ScreenAdapter {
    private final Main game;
    private final RunSession session;
    private final FitViewport viewport = new FitViewport(960, 540);
    private final SpriteBatch batch = new SpriteBatch();
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont font = UiTheme.bodyFont(), titleFont = UiTheme.titleFont();
    private final Rectangle forge = new Rectangle(120, 53, 350, 45), leave = new Rectangle(490, 53, 350, 45);
    private final DialoguePanel dialoguePanel=new DialoguePanel(30,290,900,195,174,1.05f);
    private final SafeRunExit safeExit;
    private DialoguePlayback playback;
    private String notice = "";
    private float inputDelay = 0.2f, noticeTimer;
    private boolean mouseReleased;
    public BlacksmithScreen(Main game, RunSession session) {
        if (session.isDemo() || session.finished() || !session.encounters().blacksmithOpen())
            throw new IllegalStateException("No blacksmith encounter");
        this.game = game; this.session = session;
        safeExit=new SafeRunExit(game,session,RunCheckpoint.Stage.BLACKSMITH,new Rectangle(690,493,240,35));
        play(BlacksmithDialogue.select(game.profiles().profile(),session.player().getWeapon()));
    }
    @Override public void show() { Gdx.input.setInputProcessor(null); }
    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void render(float delta) {
        float elapsed=Math.max(0,delta);
        inputDelay = Math.max(0,inputDelay-elapsed); noticeTimer = Math.max(0, noticeTimer - elapsed);
        playback.advance(elapsed);
        if (noticeTimer == 0) notice = "";
        Vector2 pointer = viewport.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        if(safeExit.input(delta,pointer))return;
        mouseReleased|=!Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        boolean click = mouseReleased&&Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);
        if(Gdx.input.isKeyJustPressed(Input.Keys.T)) {
            game.instantDialogue(!game.instantDialogue());playback.instant(game.instantDialogue());
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || click&&dialoguePanel.contains(pointer)) {
            playback.accept();
        } else if (inputDelay == 0) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1)
                || Gdx.input.isKeyJustPressed(Input.Keys.E) || click && forge.contains(pointer)) {
                buy();
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_2)
                || Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
                || click && leave.contains(pointer)) {
                session.encounters().closeBlacksmith();
                game.offerRoute(session); return;
            }
        }
        ScreenUtils.clear(0.045f, 0.055f, 0.085f, 1); viewport.apply();
        shapes.setProjectionMatrix(viewport.getCamera().combined); shapes.begin(ShapeRenderer.ShapeType.Filled);
        dialoguePanel.background(shapes);
        UiTheme.panel(shapes,30,120,900,155,false);
        shapes.setColor(session.canForgeWeapon()&&game.profiles().profile().canForgeWeapon()?UiTheme.BRONZE:UiTheme.PANEL);
        shapes.rect(forge.x, forge.y, forge.width, forge.height);
        shapes.setColor(0.12f, 0.25f, 0.33f, 1); shapes.rect(leave.x, leave.y, leave.width, leave.height);

        shapes.end();
        batch.setProjectionMatrix(viewport.getCamera().combined); batch.begin();
        titleFont.setColor(UiTheme.IVORY);titleFont.getData().setScale(.95f);titleFont.draw(batch,"FORJA DO ERRANTE",280,520);
        dialoguePanel.draw(batch,titleFont,font,game.visuals(),playback,
            !playback.complete()?"ESPAÇO / clique na fala: revelar"
                : playback.line()+1<playback.lineCount()?"ESPAÇO / clique na fala: continuar"
                : "Conversa concluída. Escolha uma opção abaixo.",
            "T: texto "+(game.instantDialogue()?"instantâneo":"gradual")+" | 1 / E: melhorar | ENTER: partir");
        PlayerState player = session.player();
        batch.setColor(player.getWeaponRarity()==WeaponRarity.RARE?Color.GOLD:Color.WHITE);
        batch.draw(game.visuals().expanded().region("weapon-"+WeaponVisuals.key(player.getWeapon())),778,155,90,90);
        batch.setColor(Color.WHITE);
        font.setColor(Color.GOLD);font.getData().setScale(1);
        font.draw(batch, player.getProfession().title + " | " + player.getWeapon().label + " | " + player.getWeaponRarity().label,48,255);
        font.setColor(Color.WHITE);font.getData().setScale(.95f);
        font.draw(batch, "Fragmentos guardados: " + game.profiles().profile().forgeMaterials()+" | Custo: 1 Fragmento de Forja",48,230);
        font.draw(batch,"Rara: +25% do dano base e +10% de cadência.",48,207);
        font.getData().setScale(.90f);font.draw(batch,WeaponRarity.power(player.getWeapon()),48,184,700,8,true);
        font.setColor(Color.LIGHT_GRAY);
        font.getData().setScale(.85f);font.draw(batch,"Melhoria permanente: continua após morte, novas tentativas e ao fechar o jogo.",48,143);
        font.getData().setScale(1);font.setColor(Color.WHITE); font.draw(batch, "[1 / E] Melhorar arma", 200, 82);
        font.draw(batch, "[2 / ENTER] Continuar", 565, 82);
        String message=game.profiles().error().isEmpty()?notice:game.profiles().error();
        if(!message.isEmpty()) {
            font.setColor(game.profiles().error().isEmpty()?Color.GOLD:Color.SALMON);
            font.getData().setScale(.9f);font.draw(batch,message,48,28,864,8,true);
        }
        batch.end();
        safeExit.draw(batch,shapes,font,pointer);
    }
    private void play(java.util.List<DialogueLine> lines) { playback=new DialoguePlayback(lines,game.instantDialogue()); }
    private void buy() {
        WeaponType weapon=session.player().getWeapon();
        if(!session.player().canForgeWeapon()) {
            notice="Sua arma já tem a melhoria permanente disponível.";
            play(BlacksmithDialogue.select(game.profiles().profile(),weapon));
        } else if(game.profiles().profile().forgeMaterials()==0) {
            notice="Falta um Fragmento de Forja. Ele vem dos mini-bosses.";
            play(BlacksmithDialogue.missingMaterial(weapon));
        } else if(game.profiles().forgeWeapon(session)) {
            notice="Arma rara salva permanentemente! Zeus vai adorar.";
            play(BlacksmithDialogue.success(weapon));
        } else {
            notice="A forja não foi concluída; seu material foi preservado.";
            play(BlacksmithDialogue.failure(weapon));
        }
        noticeTimer=3.5f;mouseReleased=false;
    }
    @Override public void dispose() { batch.dispose(); shapes.dispose(); font.dispose();titleFont.dispose(); }
}
