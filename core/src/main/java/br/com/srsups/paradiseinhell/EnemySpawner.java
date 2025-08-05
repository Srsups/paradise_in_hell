package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.graphics.OrthographicCamera;
import br.com.srsups.paradiseinhell.Inimigo;
import com.badlogic.gdx.graphics.Texture;

import java.util.ArrayList;
import java.util.Random;

public class EnemySpawner {
    private static Random random = new Random();

    // Esta é a nova função de spawn
    public static Inimigo spawnInimigoForaDaTela(OrthographicCamera camera, Texture spritesheet, TileMap tileMap) {
        // 1. Calcula a área do mundo que a câmera está vendo
        float viewX = camera.position.x - camera.viewportWidth / 2;
        float viewY = camera.position.y - camera.viewportHeight / 2;
        float viewWidth = camera.viewportWidth;
        float viewHeight = camera.viewportHeight;

        // 2. Define uma pequena margem para o inimigo aparecer um pouco fora da tela
        float offset = 32f;

        // 3. Sorteia de qual dos 4 lados o inimigo vai aparecer (0=cima, 1=baixo, 2=esquerda, 3=direita)
        int lado = random.nextInt(4);

        float spawnX = 0, spawnY = 0;

        // Checa se a posição gerada é sólida. Usamos o centro do inimigo (8,8) para a checagem.
        if (tileMap.ehSolido(spawnX + 8, spawnY + 8)) {
            return null; // Posição inválida, não cria o inimigo desta vez.
        }

        switch (lado) {
            case 0: // Cima
                spawnX = viewX + random.nextFloat() * viewWidth; // Posição X aleatória ao longo da visão da câmera
                spawnY = viewY + viewHeight + offset;            // Posição Y fixa, um pouco acima da tela
                break;
            case 1: // Baixo
                spawnX = viewX + random.nextFloat() * viewWidth;
                spawnY = viewY - offset;                         // Posição Y fixa, um pouco abaixo da tela
                break;
            case 2: // Esquerda
                spawnX = viewX - offset;                         // Posição X fixa, um pouco à esquerda da tela
                spawnY = viewY + random.nextFloat() * viewHeight; // Posição Y aleatória
                break;
            case 3: // Direita
                spawnX = viewX + viewWidth + offset;             // Posição X fixa, um pouco à direita da tela
                spawnY = viewY + random.nextFloat() * viewHeight;
                break;
        }

        // 4. Cria o novo inimigo e adiciona à lista
        return new Inimigo(spawnX, spawnY, spritesheet);
    }
}
