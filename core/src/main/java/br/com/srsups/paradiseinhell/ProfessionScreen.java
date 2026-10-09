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
    private final FitViewport viewport = new FitViewport(960, 540);
    private final SpriteBatch batch = new SpriteBatch();
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont font = new BitmapFont();
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
        Vector2 pointer = viewport.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        Profession[] professions = Profession.values();
        for (int index = 0; index < professions.length; index++) {
            boolean key = Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + index)
                || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1 + index);
            if (key || Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && cards[index].contains(pointer)) {
                if (!game.profiles().chooseProfession(professions[index])) { notice = "Este save já tem uma profissão."; break; }
                if (region == null) game.startDemo();
                else game.setScreen(new RefugeScreen(game));
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
        for (int index = 0; index < professions.length; index++) {
            Profession profession = professions[index];
            Rectangle card = cards[index];
            font.setColor(Color.GOLD);
            font.draw(batch, "[" + (index + 1) + "] " + profession.title, card.x + 18, card.y + 130);
            font.setColor(Color.WHITE);
            font.draw(batch, profession.weapon.label, card.x + 18, card.y + 105);
            font.draw(batch, "Vida: " + (int) profession.baseHealth + " | Vel.: " + (int) profession.baseSpeed
                + " | Dano: " + profession.weapon.damage + " | Sorte: " + CombatNumbers.format(profession.luckMultiplier) + "x", card.x + 18, card.y + 85);
            font.draw(batch, profession.description, card.x + 18, card.y + 65, card.width - 36, 8, true);
            String attack = profession.weapon == WeaponType.FISTS ? "Socos em um alvo próximo; sorte maior em drops e dádivas."
                : profession.weapon == WeaponType.SWORD ? "Golpe em área, na direção do mouse."
                : profession.weapon == WeaponType.PISTOL ? "Tiros rápidos, com dano menor." : "Flechas de longo alcance.";
            font.draw(batch, attack, card.x + 18, card.y + 22, card.width - 36, 8, true);
        }
        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "Clique ou use 1, 2, 3, 4.   |   ESC: voltar", 285, 80);
        font.draw(batch, "Mouse: mirar   |   Segure o botão esquerdo: atacar", 255, 53);
        font.draw(batch, "A profissão pertence ao save. Para escolher outra, crie outro save.", 205, 25);
        if (!notice.isEmpty()) { font.setColor(Color.SALMON); font.draw(batch, notice, 35, 30); }
        batch.end();
    }
    @Override public void dispose() { batch.dispose(); shapes.dispose(); font.dispose(); }
}
