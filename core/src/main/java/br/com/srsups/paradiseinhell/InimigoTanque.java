package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class InimigoTanque extends Inimigo {

    public InimigoTanque(float spawnX, float spawnY, Texture spritesheet) {
        // 1. Chama o construtor da classe pai
        super(spawnX, spawnY, spritesheet);

        // 2. Modifica os atributos
        this.velocidade = 20f; // Bem lento
        this.vida = 50f;       // Aguenta muitos ataques
    }

    // 3. Sobrescreve o draw para dar uma cor diferente
    @Override
    public void draw(SpriteBatch batch) {
        // Pinta o inimigo de uma cor magenta
        batch.setColor(Color.MAGENTA);
        super.draw(batch);
        // A cor já é resetada no final do super.draw()
    }
}
