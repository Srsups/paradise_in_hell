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

public final class ProfessionScreen extends ScreenAdapter {
    private final Main game;
    private final RegionDefinition region;
    private String notice = "";
    private float visualTime;
    private final MenuPages pages = new MenuPages();
    private final FitViewport viewport = new FitViewport(960, 540);
    private final SpriteBatch batch = new SpriteBatch();
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont font = UiTheme.bodyFont();
    private final Rectangle[] cards = {
        new Rectangle(35, 265, 435, 150), new Rectangle(490, 265, 435, 150),
        new Rectangle(35, 105, 435, 150), new Rectangle(490, 105, 435, 150)
    };
    public ProfessionScreen(Main game, RegionDefinition region) { this.game = game; this.region = region; }
    @Override public void show() { Gdx.input.setInputProcessor(null); }
    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void render(float delta) {
        if (game.profiles().profile().professionChosen()) {
            if (region == null) game.startDemo(); else game.setScreen(new RefugeScreen(game));
            return;
        }
        visualTime = (visualTime + Math.min(Math.max(delta, 0), .05f)) % 3600;
        Vector2 pointer = viewport.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        Profession[] professions = Profession.values();
        pages.input(pointer);
        for (int index = 0; index < cards.length; index++) {
            boolean key = Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + index)
                || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1 + index);
            if (key || Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && cards[index].contains(pointer)) {
                Profession profession = professions[pages.index(index)];
                if (profession == Profession.NECROMANTE && !game.necromancerUnlocked()) {
                    notice = "Necromante bloqueado: compre o acesso no altar do refúgio."; break;
                }
                game.setScreen(new DifficultyScreen(game, profession, region));
                return;
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(region != null && game.profiles().profile().professionChosen() ? new RefugeScreen(game) : new MenuScreen(game)); return;
        }
        ScreenUtils.clear(0.045f, 0.055f, 0.085f, 1);
        viewport.apply();
        shapes.setProjectionMatrix(viewport.getCamera().combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.13f, 0.16f, 0.23f, 1);
        for (Rectangle card : cards) shapes.rect(card.x, card.y, card.width, card.height);
        shapes.end();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        font.getData().setScale(1.7f); font.setColor(Color.GOLD);
        font.draw(batch, "NARRADOR", 390, 500);
        font.getData().setScale(1.2f); font.setColor(Color.WHITE);
        font.draw(batch, "Antes de toda essa confusão, qual era sua profissão?", 205, 450);
        font.getData().setScale(1);
        for (int index = 0; index < cards.length; index++) {
            Profession profession = professions[pages.index(index)];
            boolean locked = profession == Profession.NECROMANTE && !game.necromancerUnlocked();
            Rectangle card = cards[index];
            batch.setColor(locked?Color.GRAY:Color.WHITE);
            batch.draw(game.visuals().equipped().frame(EquippedArt.actor(profession)+"-idle",
                ActorAnimation.Direction.DOWN,(int)(visualTime/PlayerAnimation.IDLE_FRAME_SECONDS)%4),card.x+card.width-54,card.y+101,44,44);
            batch.setColor(Color.WHITE);
            font.setColor(locked ? Color.SALMON : Color.GOLD);
            font.draw(batch, profession.title + (profession == Profession.NECROMANTE ? "  [LENDÁRIA]" : ""), card.x + 18, card.y + 130);
            font.setColor(Color.WHITE);
            font.draw(batch, profession.weapon.label, card.x + 18, card.y + 105);
            font.draw(batch, "Vida: " + (int) profession.baseHealth + " | Atq/s: " + CombatNumbers.formatRate(1 / profession.weapon.interval)
                + " | Dano: " + profession.weapon.damage + " | Sorte: " + CombatNumbers.format(profession.luckMultiplier) + "x", card.x + 18, card.y + 85);
            font.draw(batch, profession.description, card.x + 18, card.y + 65, card.width - 36, 8, true);
            String attack = locked ? "Lendária: desbloqueio global por "+SharedProgress.necromancerCostText()+"." : switch (profession.weapon) {
                case FISTS -> "Um alvo próximo; sorte 4x em drops e dádivas.";
                case SWORD -> "Golpe em área, na direção do mouse.";
                case PISTOL -> "Tiros rápidos, com dano menor.";
                case BOW -> "Flechas de longo alcance.";
                case WAND -> "Feitiços explodem ao acertar um inimigo.";
                case CURSE -> "Maldição em área, com dano contínuo por 3s.";
                case SOUL -> "Inimigos abatidos podem voltar como aliados.";
                case SHIELD -> "Direito: defender. Esquerdo: atacar, mesmo defendendo.";
            };
            font.draw(batch, attack, card.x + 18, card.y + 22, card.width - 36, 8, true);
        }
        font.setColor(Color.LIGHT_GRAY);
        pages.draw(batch, font);
        font.draw(batch, "Clique para escolher sua profissão. ESC: voltar", 235, 53);

        font.draw(batch, "A profissão pertence ao save. Para escolher outra, crie outro save.", 205, 25);
        if (!notice.isEmpty()) { font.setColor(Color.SALMON); font.draw(batch, notice, 35, 435); }
        batch.end();
    }
    @Override public void dispose() { batch.dispose(); shapes.dispose(); font.dispose(); }
}
