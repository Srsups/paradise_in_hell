package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/** Borrows rendering resources. A local draft never touches the window before Apply. */
public final class VideoPanel {
    public static final float INPUT_DELAY = .2f;
    private final Main game;
    private final HudPanel hudPanel;
    private final Rectangle hudButton=new Rectangle(750,485,175,36);
    private boolean hudPage;
    private final Rectangle windowed = new Rectangle(255,369,210,45),borderless = new Rectangle(480,369,210,45),fullscreen = new Rectangle(705,369,210,45);
    private final Rectangle[] presets = new Rectangle[VideoSettings.PRESETS.size()];
    private final Rectangle vsyncButton = new Rectangle(255, 213, 450, 45);
    private final Rectangle apply = new Rectangle(530, 82, 300, 48), back = new Rectangle(130, 82, 300, 48);
    private final Rectangle keep = new Rectangle(150, 155, 300, 48), revert = new Rectangle(510, 155, 300, 48);
    private VideoSettings draft = VideoSettings.DEFAULT;
    private boolean open, mouseReleased, wasPreviewing;
    private float delay;
    private int row;

    public VideoPanel(Main game) {
        this.game = game;
        hudPanel=new HudPanel(game);
        for (int i = 0; i < presets.length; i++) presets[i] = new Rectangle(35 + i * 180, 287, 170, 43);
    }
    public boolean open() { return open; }
    public boolean hudPage() { return hudPage; }
    public Rectangle hudButtonBounds() { return new Rectangle(hudButton); }
    public VideoSettings draft() { return draft; }
    public Rectangle presetBounds(int index) { return new Rectangle(presets[index]); }
    public Rectangle windowedBounds() { return new Rectangle(windowed); }
    public Rectangle borderlessBounds() { return new Rectangle(borderless); }
    public Rectangle fullscreenBounds() { return new Rectangle(fullscreen); }
    public Rectangle vsyncBounds() { return new Rectangle(vsyncButton); }
    public Rectangle applyBounds() { return new Rectangle(apply); }
    public Rectangle keepBounds() { return new Rectangle(keep); }
    public Rectangle revertBounds() { return new Rectangle(revert); }
    public Rectangle backBounds() { return new Rectangle(back); }
    public void showHud() {
        if(!open)show();
        if(game.video().busy()||game.video().previewing())return;
        hudPage=true;hudPanel.show();
    }
    public void show() {
        hudPanel.close();hudPage=false;
        draft = game.video().current(); open = true; row = 0;
        wasPreviewing = game.video().previewing(); resetGuard(); game.audio().stopEffects();
    }
    public void close() {
        hudPanel.close();hudPage=false;
        if (open && game.video().previewing()) game.video().cancelPreview();
        open = false;
    }
    private void resetGuard() { delay = INPUT_DELAY; mouseReleased = false; }
    public void suspendInput() {
        if(!open)return;
        resetGuard();hudPanel.suspendInput();
    }
    private void synchronizePreview() {
        boolean previewing = game.video().previewing();
        if (wasPreviewing && !previewing) { draft = game.video().current(); resetGuard(); }
        wasPreviewing = previewing;
    }
    /** Consume opening, closing, mode changes and every frame while the panel is visible. */
    public boolean input(float delta, Vector2 pointer) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) {
            if (open) close(); else show();
            return true;
        }
        if (!open) return false;
        if (game.video().busy()) return true;
        if(hudPage) {
            hudPanel.input(delta,pointer);
            if(!hudPanel.open()){hudPage=false;resetGuard();}
            return true;
        }
        synchronizePreview();
        delay = Math.max(0, delay - (Float.isFinite(delta) ? Math.max(0, delta) : 0));
        if (delay == 0 && !Gdx.input.isButtonPressed(Input.Buttons.LEFT)) mouseReleased = true;
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (game.video().previewing()) { game.video().cancelPreview(); synchronizePreview(); }
            else close();
            return true;
        }
        if (delay > 0 || !mouseReleased) return true;
        boolean click = mouseReleased && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);
        if (game.video().previewing()) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || click && keep.contains(pointer)) {
                game.video().confirm(); synchronizePreview(); resetGuard();
            } else if (click && revert.contains(pointer)) {
                game.video().cancelPreview(); synchronizePreview();
            }
            return true;
        }
        if(click&&hudButton.contains(pointer)) {showHud();return true;}
        if (Gdx.input.isKeyJustPressed(Input.Keys.S)) { game.video().retrySave(); return true; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) row = Math.max(0, row - 1);
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) row = Math.min(2, row + 1);
        boolean left = Gdx.input.isKeyJustPressed(Input.Keys.LEFT), right = Gdx.input.isKeyJustPressed(Input.Keys.RIGHT);
        if (left || right) {
            if (row == 0) {
                int index=draft.mode().ordinal();
                setMode(VideoSettings.Mode.values()[Math.max(0,Math.min(VideoSettings.Mode.values().length-1,index+(right?1:-1)))]);
            }
            else if (row == 1 && draft.mode()==VideoSettings.Mode.WINDOWED) {
                int index = 0;
                for (int i = 0; i < presets.length; i++) if (VideoSettings.PRESETS.get(i).equals(new VideoSettings.Size(draft.width(), draft.height()))) index = i;
                draft = draft.withSize(VideoSettings.PRESETS.get(Math.max(0, Math.min(presets.length - 1, index + (right ? 1 : -1)))));
            } else if (row == 2) draft = draft.withVsync(!draft.vsync());
        }
        if (click && windowed.contains(pointer)) { row = 0; setMode(VideoSettings.Mode.WINDOWED); }
        else if (click && borderless.contains(pointer)) { row = 0; setMode(VideoSettings.Mode.BORDERLESS); }
        else if (click && fullscreen.contains(pointer)) { row = 0; setMode(VideoSettings.Mode.FULLSCREEN); }
        else if (click && vsyncButton.contains(pointer)) { row = 2; draft = draft.withVsync(!draft.vsync()); }
        else if (click && back.contains(pointer)) { close(); return true; }
        for (int i = 0; i < presets.length; i++)
            if (draft.mode()==VideoSettings.Mode.WINDOWED && (click && presets[i].contains(pointer)
                || Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + i) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1 + i))) {
                row = 1; draft = draft.withSize(VideoSettings.PRESETS.get(i));
            }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || click && apply.contains(pointer)) {
            game.video().preview(draft); wasPreviewing = game.video().previewing(); resetGuard(); return true;
        }
        return true;
    }
    private void setMode(VideoSettings.Mode value) {draft=draft.withMode(value);}
    public void draw(SpriteBatch batch, ShapeRenderer shapes, BitmapFont font) {
        if (!open) return;
        if(hudPage){hudPanel.draw(batch,shapes,font);return;}
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(UiTheme.INK); shapes.rect(0, 0, 960, 540);
        UiTheme.panel(shapes, windowed.x, windowed.y, windowed.width, windowed.height, draft.mode()==VideoSettings.Mode.WINDOWED);
        UiTheme.panel(shapes, borderless.x, borderless.y, borderless.width, borderless.height, draft.mode()==VideoSettings.Mode.BORDERLESS);
        UiTheme.panel(shapes, fullscreen.x, fullscreen.y, fullscreen.width, fullscreen.height, draft.mode()==VideoSettings.Mode.FULLSCREEN);
        for (int i = 0; i < presets.length; i++) {
            Rectangle rect = presets[i]; VideoSettings.Size size = VideoSettings.PRESETS.get(i);
            UiTheme.panel(shapes, rect.x, rect.y, rect.width, rect.height, draft.mode()==VideoSettings.Mode.WINDOWED && draft.width() == size.width() && draft.height() == size.height());
        }
        UiTheme.panel(shapes, vsyncButton.x, vsyncButton.y, vsyncButton.width, vsyncButton.height, draft.vsync());
        UiTheme.panel(shapes, apply.x, apply.y, apply.width, apply.height, true);
        UiTheme.panel(shapes, back.x, back.y, back.width, back.height, false);
        UiTheme.panel(shapes,hudButton.x,hudButton.y,hudButton.width,hudButton.height,false);
        shapes.end();
        batch.begin(); batch.setColor(Color.WHITE);
        text(batch, font, "VÍDEO", 420, 501, 1.8f, Color.GOLD);
        text(batch,font,"TAMANHO DO HUD",764,508,.9f,Color.WHITE);
        text(batch, font, "Configure a imagem e teste antes de manter.", 343, 465, .95f, UiTheme.IVORY);
        text(batch, font, "Modo de exibição", 35, 400, 1, row == 0 ? Color.GOLD : Color.WHITE);
        text(batch,font,"Janela",328,398,1,Color.WHITE);
        text(batch,font,"Janela sem bordas",513,398,1,Color.WHITE);
        text(batch,font,"Tela cheia",762,398,1,Color.WHITE);
        String sizeHint=switch(draft.mode()) {
            case WINDOWED -> "Resolução da janela — ajustada à área útil do monitor. Escolha com 1 a 5 ou clique.";
            case BORDERLESS -> "Janela sem bordas ocupa o monitor atual. O tamanho da janela fica guardado.";
            case FULLSCREEN -> "Tela cheia usa a resolução atual do monitor. O tamanho da janela fica guardado.";
        };
        text(batch,font,sizeHint,35,352,.95f,row==1?Color.GOLD:Color.WHITE);
        for (int i = 0; i < presets.length; i++) text(batch, font, VideoSettings.PRESETS.get(i).label(), presets[i].x + 31, 314, .95f,
            draft.mode()!=VideoSettings.Mode.WINDOWED ? Color.GRAY : Color.WHITE);
        text(batch, font, "Sincronização vertical", 35, 242, 1, row == 2 ? Color.GOLD : Color.WHITE);
        text(batch, font, "VSync: " + (draft.vsync() ? "LIGADO" : "DESLIGADO"), 395, 242, 1, Color.WHITE);
        text(batch, font, "VSync reduz cortes na imagem. Desligado: limite de 120 FPS.", 303, 187, .9f, Color.LIGHT_GRAY);
        VideoSettings actual = game.video().current();
        font.getData().setScale(.88f);font.setColor(Color.LIGHT_GRAY);
        font.draw(batch,game.video().monitorDescription(),35,169,890,8,true);
        text(batch, font, "Atual: "+actual.mode().label()
            +(actual.mode()==VideoSettings.Mode.WINDOWED?" "+actual.width()+" × "+actual.height():"")
            + " | VSync " + (actual.vsync() ? "ligado" : "desligado"), 35, 145, .95f, UiTheme.TEAL);
        text(batch, font, "Voltar [ESC / F2]", 204, 112, 1, Color.WHITE);
        text(batch, font, "APLICAR [ENTER]", 611, 112, 1, Color.GOLD);
        String message = !game.video().error().isEmpty() ? game.video().error()
            : game.video().pendingSave() ? "Imagem mantida. S: tentar salvar novamente." : game.video().notice();
        text(batch, font, "Setas: selecionar/ajustar | S: tentar salvar | ESC/F2: voltar", 247, 57, .9f, Color.LIGHT_GRAY);
        font.getData().setScale(.88f); font.setColor(game.video().error().isEmpty() ? Color.LIGHT_GRAY : Color.SALMON);
        font.draw(batch, message, 35, 29, 890, 8, true);
        font.getData().setScale(1); font.setColor(Color.WHITE); batch.end();
        if (game.video().previewing()) drawPreview(batch, shapes, font);
    }
    private void drawPreview(SpriteBatch batch, ShapeRenderer shapes, BitmapFont font) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(UiTheme.INK); shapes.rect(0, 0, 960, 540);
        UiTheme.panel(shapes, 95, 125, 770, 280, true);
        UiTheme.panel(shapes, keep.x, keep.y, keep.width, keep.height, true);
        UiTheme.panel(shapes, revert.x, revert.y, revert.width, revert.height, false);
        shapes.end();
        batch.begin(); batch.setColor(Color.WHITE);
        text(batch, font, "MANTER ESTA IMAGEM?", 307, 366, 1.5f, Color.GOLD);
        text(batch, font, "Confira a resolução, o modo e a leitura dos textos.", 306, 322, 1, Color.WHITE);
        text(batch, font, "Reverte automaticamente em " + (int)Math.ceil(game.video().remainingSeconds()) + " segundos.", 300, 280, 1.1f, UiTheme.TEAL);
        text(batch, font, "ENTER / clique: manter", 220, 185, 1, Color.GOLD);
        text(batch, font, "ESC / clique: reverter", 582, 185, 1, Color.WHITE);
        text(batch, font, "O jogo está pausado. Nada é salvo antes de confirmar.", 309, 98, .95f, Color.LIGHT_GRAY);
        font.getData().setScale(1); font.setColor(Color.WHITE); batch.end();
    }
    private static void text(SpriteBatch batch, BitmapFont font, String value, float x, float y, float scale, Color color) {
        font.getData().setScale(scale); font.setColor(color); font.draw(batch, value, x, y);
    }
}
