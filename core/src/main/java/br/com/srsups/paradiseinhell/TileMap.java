package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.Camera;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class TileMap {
    private Map<String, Tile> tiles;
    private Texture spritesheet;
    private TextureRegion[] grassTiles;
    private Random random;

    public TileMap(Texture spritesheet) {
        tiles = new HashMap<>();
        random = new Random();
        this.spritesheet = spritesheet;

        // Cria os 3 tipos de grama
        grassTiles = new TextureRegion[3];
        grassTiles[0] = new TextureRegion(spritesheet, 278, 209, 16, 16);
        grassTiles[1] = new TextureRegion(spritesheet, 295, 209, 16, 16);
        grassTiles[2] = new TextureRegion(spritesheet, 244, 226, 16, 16);
    }

    public boolean ehSolido(float px, float py) {
        int tx = (int)Math.floor(px / Tile.TILE_SIZE);
        int ty = (int)Math.floor(py / Tile.TILE_SIZE);
        String key = tx + "," + ty;

        Tile tile = tiles.get(key);
        if (tile == null) return false; // Se ainda não foi gerado, assume que é passável
        return tile.solido;
    }

    public void update(Camera camera) {
        int camLeft = (int)Math.floor((camera.position.x - camera.viewportWidth / 2) / Tile.TILE_SIZE) - 1;
        int camRight = (int)Math.floor((camera.position.x + camera.viewportWidth / 2) / Tile.TILE_SIZE) + 1;
        int camBottom = (int)Math.floor((camera.position.y - camera.viewportHeight / 2) / Tile.TILE_SIZE) - 1;
        int camTop = (int)Math.floor((camera.position.y + camera.viewportHeight / 2) / Tile.TILE_SIZE) + 1;

        for (int x = camLeft; x <= camRight; x++) {
            for (int y = camBottom; y <= camTop; y++) {
                String key = x + "," + y;
                if (!tiles.containsKey(key)) {
                    // Sorteia um dos 3 tipos de tile
                    TextureRegion tile = grassTiles[random.nextInt(grassTiles.length)];
                    boolean solido = false;

                    // Simula a chance de ser um obstáculo
                    if (random.nextFloat() < 0.005f) {
                        tile = new TextureRegion(spritesheet, 281, 241, 16, 16); // exemplo: tile de pedra
                        solido = true;
                    }

                    tiles.put(key, new Tile(x, y, tile, solido));

                }
            }
        }
    }

    public void draw(SpriteBatch batch) {
        for (Tile tile : tiles.values()) {
            tile.draw(batch);
        }
    }

    //Pré-gera uma área quadrada ao redor de um ponto específico para garantir que não haja obstáculos sólidos no início do jogo.
    public void gerarAreaInicialSegura(float spawnX, float spawnY, int raio) {
        // Converte a posição do mundo (em pixels) para a grade de tiles
        int tileXCentral = (int)Math.floor(spawnX / Tile.TILE_SIZE);
        int tileYCentral = (int)Math.floor(spawnY / Tile.TILE_SIZE);

        System.out.println("Gerando area segura ao redor do tile: " + tileXCentral + "," + tileYCentral);

        // Itera em um quadrado de tiles ao redor do ponto central
        for (int x = tileXCentral - raio; x <= tileXCentral + raio; x++) {
            for (int y = tileYCentral - raio; y <= tileYCentral + raio; y++) {
                String key = x + "," + y;
                // Se o tile ainda não existe no mapa, vamos criá-lo de forma segura.
                if (!tiles.containsKey(key)) {
                    // Sorteia um tile de grama normal
                    TextureRegion tileRegion = grassTiles[random.nextInt(grassTiles.length)];
                    // Cria o tile, garantindo que o parâmetro 'solido' seja 'false'
                    tiles.put(key, new Tile(x, y, tileRegion, false)); // GARANTE QUE NÃO É SÓLIDO
                }
            }
        }
    }

    public void dispose() {
    }
}
