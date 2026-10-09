package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.ScreenAdapter;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import static org.junit.jupiter.api.Assertions.*;

class ScreenLifecycleTest {
    private static class Screen extends ScreenAdapter {
        int shows, hides, disposals;
        @Override public void show() { shows++; }
        @Override public void hide() { hides++; }
        @Override public void dispose() { disposals++; }
    }

    @Test void replacingAndClosingScreensReleasesEachExactlyOnce() {
        Graphics previous = Gdx.graphics;
        Gdx.graphics = (Graphics) Proxy.newProxyInstance(Graphics.class.getClassLoader(),
            new Class<?>[]{Graphics.class}, (proxy, method, args) -> 960);
        try {
            Main game = new Main();
            Screen menu = new Screen(), play = new Screen();
            game.setScreen(menu);
            game.setScreen(menu);
            assertEquals(1, menu.shows);
            assertEquals(0, menu.disposals);
            game.setScreen(play);
            assertEquals(1, menu.hides);
            assertEquals(1, menu.disposals);
            assertEquals(1, play.shows);
            game.dispose();
            game.dispose();
            assertEquals(1, play.hides);
            assertEquals(1, play.disposals);
            assertNull(game.getScreen());
        } finally { Gdx.graphics = previous; }
    }
}
