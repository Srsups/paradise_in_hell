package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class InimigoRapido extends Inimigo {

    public InimigoRapido(float spawnX, float spawnY, Texture spritesheet) {
        // 1. Chama o construtor da classe pai (Inimigo) para configurar tudo
        super(spawnX, spawnY, spritesheet);

        // 2. Modifica os atributos para este tipo de inimigo
        this.velocidade = 75f; // Muito mais rápido
        this.vida = 10f;        // Morre com dois tiros
    }

    // 3. (Opcional) Podemos sobrescrever o método draw para dar uma cor diferente
    @Override
    public void draw(SpriteBatch batch) {
        // Pinta o inimigo de uma cor ciano para diferenciá-lo
        batch.setColor(Color.CYAN);
        super.draw(batch); // Chama o método de desenho original da classe Inimigo
        // A cor do batch já é resetada para branco no final do super.draw()
    }
}
