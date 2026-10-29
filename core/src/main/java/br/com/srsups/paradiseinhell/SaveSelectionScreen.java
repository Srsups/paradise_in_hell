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

/** Select/create characters, or explicitly confirm resetting one occupied slot. */
public final class SaveSelectionScreen extends ScreenAdapter {
    private final Main game;
    private final boolean demo, fromRefuge;
    private final MenuPages pages = new MenuPages();
    private final FitViewport viewport = new FitViewport(960, 540);
    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font = UiTheme.bodyFont();
    private final ShapeRenderer shapes = new ShapeRenderer();
    private int pendingDelete = -1;
    private float confirmDelay, noticeTimer;
    private boolean deleteMode, noticeError;
    private String notice = "";
    private final Rectangle cancel = new Rectangle(205, 155, 240, 50);
    private final Rectangle confirm = new Rectangle(515, 155, 240, 50);
    private final Rectangle[] cards = {
        new Rectangle(35, 270, 435, 150), new Rectangle(490, 270, 435, 150),
        new Rectangle(35, 110, 435, 150), new Rectangle(490, 110, 435, 150)
    };
    private final Rectangle[] deleteButtons = new Rectangle[cards.length];
    public SaveSelectionScreen(Main game, boolean demo) { this(game, demo, false); }
    public SaveSelectionScreen(Main game, boolean demo, boolean fromRefuge) {
        this.game = game; this.demo = demo; this.fromRefuge = fromRefuge;
        for (int i = 0; i < cards.length; i++) {
            Rectangle card = cards[i];
            deleteButtons[i] = new Rectangle(card.x + card.width - 120, card.y + 18, 102, 30);
        }
    }
    @Override public void show() { Gdx.input.setInputProcessor(null); }
    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    private boolean occupied(int slot) {
        ProfileService profile = game.profileAt(slot);
        return profile.profile().hasProgress() || !profile.error().isEmpty();
    }
    private void requestDelete(int slot) {
        if (!occupied(slot)) { notice = "Este slot já está vazio."; noticeError = false; noticeTimer = 3; return; }
        pendingDelete = slot; confirmDelay = .2f;
    }
    @Override public void render(float delta) {
        noticeTimer = Math.max(0, noticeTimer - delta);
        Vector2 pointer = viewport.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        boolean click = Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);
        if (pendingDelete >= 0) {
            confirmDelay = Math.max(0, confirmDelay - delta);
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || click && cancel.contains(pointer)) pendingDelete = -1;
            else if (confirmDelay == 0 && (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || click && confirm.contains(pointer))) {
                int slot = pendingDelete;
                noticeError = !game.deleteSave(slot);
                if (!noticeError) {
                    notice = "Save " + (slot + 1) + " vazio. Você pode criar outro personagem.";
                } else {
                    notice = "Não foi possível excluir. O personagem foi preservado; tente novamente.";
                }
                noticeTimer = 5; pendingDelete = -1; deleteMode = false;
            }
            draw(); return; // Modal input never selects a character or changes the page.
        }
        pages.input(pointer);
        if (Gdx.input.isKeyJustPressed(Input.Keys.FORWARD_DEL) || Gdx.input.isKeyJustPressed(Input.Keys.DEL)) deleteMode = !deleteMode;
        for (int index = 0; index < cards.length; index++) {
            int slot = pages.index(index);
            if (click && occupied(slot) && deleteButtons[index].contains(pointer)) { requestDelete(slot); draw(); return; }
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + index) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1 + index)
                || click && cards[index].contains(pointer)) {
                if (deleteMode) { requestDelete(slot); draw(); return; }
                game.selectSave(slot, demo); return;
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (deleteMode) deleteMode = false;
            else { game.setScreen(fromRefuge && game.profiles().profile().professionChosen() ? new RefugeScreen(game) : new MenuScreen(game)); return; }
        }
        draw();
    }
    private void rectangle(Rectangle box) { shapes.rect(box.x, box.y, box.width, box.height); }
    private void draw() {
        ScreenUtils.clear(0.045f, 0.055f, 0.085f, 1); viewport.apply();
        shapes.setProjectionMatrix(viewport.getCamera().combined); shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.13f, 0.16f, 0.23f, 1);
        for (Rectangle card : cards) rectangle(card);
        shapes.setColor(.38f, .12f, .15f, 1);
        for (int i = 0; i < cards.length; i++) if (occupied(pages.index(i))) rectangle(deleteButtons[i]);
        shapes.end();
        batch.setProjectionMatrix(viewport.getCamera().combined); batch.begin();
        font.setColor(Color.GOLD); font.getData().setScale(1.8f); font.draw(batch, "ESCOLHA SEU SAVE", 310, 490);
        font.setColor(Color.WHITE); font.getData().setScale(1);
        for (int index = 0; index < cards.length; index++) {
            ProfileService service = game.profileAt(pages.index(index));
            CampaignProfile profile = service.profile(); Rectangle card = cards[index];
            font.setColor(Color.GOLD); font.draw(batch, "[" + (index + 1) + "] Save " + (pages.index(index) + 1), card.x + 18, card.y + 130);
            font.setColor(Color.WHITE);
            if (profile.professionChosen()) {
                font.draw(batch, profile.profession().title + " | " + profile.profession().weapon.label + " | " + profile.weaponRarity().label, card.x + 18, card.y + 105);
                font.draw(batch, "Óbolos: " + profile.obols() + " | Fragmentos: " + profile.forgeMaterials(), card.x + 18, card.y + 80);
                font.draw(batch, profile.difficulty().label + " | Retornos: " + profile.attempts() + " | Vitórias: " + profile.victories(), card.x + 18, card.y + 58);
                font.setColor(profile.checkpoint()!=null?UiTheme.TEAL:Color.LIGHT_GRAY);
                font.draw(batch, profile.checkpoint()!=null?"Tentativa para retomar":"Continuar personagem", card.x + 18, card.y + 28);
            } else {
                font.draw(batch, "Novo personagem", card.x + 18, card.y + 95);
                font.draw(batch, "Escolha a profissão uma vez.\nProgresso próprio deste personagem.", card.x + 18, card.y + 65);
            }
            if (occupied(pages.index(index))) {
                font.setColor(Color.SALMON); Rectangle button = deleteButtons[index];
                font.draw(batch, "Excluir", button.x + 26, button.y + 22);
            }
            if (!service.error().isEmpty()) {
                font.setColor(Color.SALMON); font.draw(batch, "Erro de gravação/leitura", card.x + 18, card.y + 12);
            }
        }
        font.setColor(Color.LIGHT_GRAY); pages.draw(batch, font);
        font.draw(batch, deleteMode ? "MODO EXCLUSÃO: escolha um personagem para excluir. ESC: cancelar."
            : "Clique ou 1, 2, 3, 4: selecionar | DEL: modo exclusão | ESC: voltar", 130, 40);
        if (noticeTimer > 0) { font.setColor(noticeError ? Color.SALMON : Color.GOLD); font.draw(batch, notice, 35, 450, 890, 8, true); }
        batch.end();
        if (pendingDelete >= 0) drawConfirmation();
    }
    private void drawConfirmation() {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(.03f, .04f, .07f, 1); shapes.rect(155, 125, 650, 285);
        shapes.setColor(.19f, .23f, .31f, 1); rectangle(cancel);
        shapes.setColor(.5f, .12f, .16f, 1); rectangle(confirm); shapes.end();
        batch.begin();
        font.setColor(Color.SALMON); font.getData().setScale(1.4f);
        font.draw(batch, "Excluir personagem do Save " + (pendingDelete + 1) + "?", 185, 377);
        font.getData().setScale(1); font.setColor(Color.WHITE);
        font.draw(batch, "Profissão, Óbolos, arma, atributos, fragmentos e progresso\ndeste personagem serão perdidos. Esta ação é definitiva.", 185, 330);
        font.setColor(Color.GOLD);
        font.draw(batch, "O saldo de Almas e os desbloqueios compartilhados\nserão preservados, inclusive ao recriar este slot.", 185, 273);
        font.setColor(Color.WHITE);
        font.draw(batch, "Cancelar", cancel.x + 85, cancel.y + 32);
        font.draw(batch, "Excluir personagem", confirm.x + 52, confirm.y + 32);
        font.setColor(Color.LIGHT_GRAY); font.draw(batch, "ESC: cancelar | ENTER: confirmar", 305, 143);
        batch.end();
    }
    @Override public void dispose() { batch.dispose(); font.dispose(); shapes.dispose(); }
}
