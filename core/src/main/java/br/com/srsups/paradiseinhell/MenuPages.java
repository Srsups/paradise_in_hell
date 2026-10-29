package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/** Four cards per page, with explicit local number shortcuts. */
final class MenuPages {
    private int page;
    private final int count;
    MenuPages() { this(SaveSlots.COUNT); }
    MenuPages(int entries) {
        if(entries<1) throw new IllegalArgumentException("Empty menu");
        count=(entries+3)/4;
    }
    private final Rectangle previous = new Rectangle(35, 70, 110, 28), next = new Rectangle(815, 70, 110, 28);
    int index(int local) { return page * 4 + local; }
    void input(Vector2 pointer) {
        boolean click = Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.PAGE_UP)
            || click && previous.contains(pointer)) page = Math.max(0,page-1);
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) || Gdx.input.isKeyJustPressed(Input.Keys.PAGE_DOWN)
            || click && next.contains(pointer)) page = Math.min(count-1,page+1);
    }
    void draw(SpriteBatch batch, BitmapFont font) {
        font.getData().setScale(1); font.setColor(Color.CYAN);
        font.draw(batch, "< Anterior", 35, 90);
        font.draw(batch, "Próxima >", 815, 90);
        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "Página " + (page + 1) + "/"+count+"  |  Setas: trocar", 325, 90);
    }
}

