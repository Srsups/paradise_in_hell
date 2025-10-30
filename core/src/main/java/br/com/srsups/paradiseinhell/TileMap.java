package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.math.Vector2;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class TileMap {
    private Map<String, Tile> tiles;
    private Texture spritesheet;
    private TextureRegion[] grassTiles;
    private Random random;
    private TextureRegion tileChaoArena;
    private TextureRegion tileParedeArena;
    public float mapPixelWidth;
    public float mapPixelHeight;
    // Coordenadas do canto inferior-esquerdo do mapa em unidades de mundo (pixels)
    public float mapPixelLeft;
    public float mapPixelBottom;

    public TileMap(Texture spritesheet) {
        tiles = new HashMap<>();
        random = new Random();
        this.spritesheet = spritesheet;

        // Cria os 3 tipos de grama
        grassTiles = new TextureRegion[3];
        grassTiles[0] = new TextureRegion(spritesheet, 278, 209, 16, 16);
        grassTiles[1] = new TextureRegion(spritesheet, 295, 209, 16, 16);
        grassTiles[2] = new TextureRegion(spritesheet, 244, 226, 16, 16);

        tileChaoArena = new TextureRegion(spritesheet, 278, 209, 16, 16);
        tileParedeArena = new TextureRegion(spritesheet, 281, 241, 16, 16);
    }

    public boolean ehSolido(float px, float py) {
        int tx = (int)Math.floor(px / Tile.TILE_SIZE);
        int ty = (int)Math.floor(py / Tile.TILE_SIZE);
        String key = tx + "," + ty;

        Tile tile = tiles.get(key);
        if (tile == null) return false; // Se ainda não foi gerado, assume que é passável
        return tile.solido;
    }

    /**
     * Busca a posição (em coordenadas de mundo) do tile não-sólido mais próximo
     * do ponto dado. Retorna o ponto central do tile encontrado como Vector2.
     * Se não encontrar nada dentro do raio máximo, retorna null.
     */
    public Vector2 findNearestFreePosition(float worldX, float worldY, int maxRadiusTiles) {
        int centerTx = (int) Math.floor(worldX / Tile.TILE_SIZE);
        int centerTy = (int) Math.floor(worldY / Tile.TILE_SIZE);

        for (int r = 0; r <= maxRadiusTiles; r++) {
            for (int dx = -r; dx <= r; dx++) {
                int dy = r - Math.abs(dx);

                // check +dy
                int tx1 = centerTx + dx;
                int ty1 = centerTy + dy;
                float px1 = tx1 * Tile.TILE_SIZE + Tile.TILE_SIZE * 0.5f;
                float py1 = ty1 * Tile.TILE_SIZE + Tile.TILE_SIZE * 0.5f;
                if (!ehSolido(px1, py1)) return new Vector2(px1, py1);

                // if dy == 0 we already checked same cell
                if (dy != 0) {
                    int tx2 = centerTx + dx;
                    int ty2 = centerTy - dy;
                    float px2 = tx2 * Tile.TILE_SIZE + Tile.TILE_SIZE * 0.5f;
                    float py2 = ty2 * Tile.TILE_SIZE + Tile.TILE_SIZE * 0.5f;
                    if (!ehSolido(px2, py2)) return new Vector2(px2, py2);
                }
            }
        }
        return null;
    }

    public void update(Camera camera) {
    // Leva em conta o zoom da câmera: a área visível é viewportWidth*zoom
        // Se a câmera for orthographic, considera o zoom, senão assume zoom = 1
        float zoom = 1f;
        if (camera instanceof OrthographicCamera) {
            zoom = ((OrthographicCamera) camera).zoom;
        }
        float visibleWidth = camera.viewportWidth * zoom;
        float visibleHeight = camera.viewportHeight * zoom;

    int camLeft = (int)Math.floor((camera.position.x - visibleWidth / 2f) / Tile.TILE_SIZE) - 1;
    // Use ceil for right/top so we include any tile partially covered at the far edges
    int camRight = (int)Math.ceil((camera.position.x + visibleWidth / 2f) / Tile.TILE_SIZE) + 1;
    int camBottom = (int)Math.floor((camera.position.y - visibleHeight / 2f) / Tile.TILE_SIZE) - 1;
    int camTop = (int)Math.ceil((camera.position.y + visibleHeight / 2f) / Tile.TILE_SIZE) + 1;

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

    /**
     * Gera uma arena retangular fechada, com chão e paredes.
     * @param centroX Posição X (em grade) do centro da arena.
     * @param centroY Posição Y (em grade) do centro da arena.
     * @param largura A largura da área interna (em número de tiles).
     * @param altura A altura da área interna (em número de tiles).
     */
    public void gerarArenaFechada(int centroX, int centroY, int largura, int altura) {
        tiles.clear(); // Limpa o mapa anterior

        int xInicial = centroX - largura / 2;
        int yInicial = centroY - altura / 2;
        
        // --- LINHAS CORRIGIDAS ---
        // Recalcula o 'final' com base no 'inicial' para garantir a largura exata.
        int xFinal = xInicial + largura - 1;
        int yFinal = yInicial + altura - 1;
        // -------------------------

        for (int x = xInicial - 1; x <= xFinal + 1; x++) {
            for (int y = yInicial - 1; y <= yFinal + 1; y++) {
                String key = x + "," + y;
                // Se estivermos na borda, cria uma parede sólida
                if (x < xInicial || x > xFinal || y < yInicial || y > yFinal) { // Lógica de borda mais robusta
                    tiles.put(key, new Tile(x, y, tileParedeArena, true)); // Parede, sólida
                } else {
                    // Senão, cria um tile de chão passável
                    tiles.put(key, new Tile(x, y, tileChaoArena, false)); // Chão, não sólido
                }
            }
        }

        // Define a área do mapa em pixels, e o canto inferior-esquerdo (offset)
        // ESTES CÁLCULOS AGORA ESTARÃO CORRETOS
        this.mapPixelWidth = (largura + 2) * Tile.TILE_SIZE;
        this.mapPixelHeight = (altura + 2) * Tile.TILE_SIZE;
        this.mapPixelLeft = (xInicial - 1) * Tile.TILE_SIZE;
        this.mapPixelBottom = (yInicial - 1) * Tile.TILE_SIZE;
    }

    public void dispose() {
    }
}
