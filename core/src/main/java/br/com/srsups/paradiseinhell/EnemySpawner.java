package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import java.util.Random;

public final class EnemySpawner {
    private static final Random RANDOM = new Random();
    private EnemySpawner() {}
    public static Inimigo spawnInimigoForaDaTela(OrthographicCamera camera, Texture atlas, TileMap map,
                                               EnemyType type, EnemyNavigation navigation) {
        float left = camera.position.x - camera.viewportWidth / 2;
        float bottom = camera.position.y - camera.viewportHeight / 2;
        for (int attempt = 0; attempt < 24; attempt++) {
            float x = left + RANDOM.nextFloat() * camera.viewportWidth;
            float y = bottom + RANDOM.nextFloat() * camera.viewportHeight;
            switch (RANDOM.nextInt(4)) {
                case 0 -> y = bottom + camera.viewportHeight + 24;
                case 1 -> y = bottom - 24;
                case 2 -> x = left - 24;
                case 3 -> x = left + camera.viewportWidth + 24;
            }
            if (map.ehSolido(x + 1, y + 1) || map.ehSolido(x + 15, y + 1)
                || map.ehSolido(x + 1, y + 15) || map.ehSolido(x + 15, y + 15)
                || !navigation.reachable(x + 8, y + 8)) continue;
            Inimigo enemy = new Inimigo(x, y, atlas);
            enemy.configurarTipo(type);
            return enemy;
        }
        return null;
    }
}
