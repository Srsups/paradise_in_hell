package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion; // MUDOU de Texture para TextureRegion
import com.badlogic.gdx.math.Vector2;

public class Projetil {
    public float x, y;
    public Vector2 velocidade;
    public int dano = 5;
    private TextureRegion textura;
    private float angulo;
    public boolean deveSerRemovido = false;

    // 2. O construtor agora recebe uma TextureRegion já pronta
    public Projetil(float x, float y, Vector2 direcao, TextureRegion texturaProjetil) {
        this.x = x;
        this.y = y;
        this.textura = texturaProjetil; // Apenas guarda a referência
        // Define a velocidade baseada na direção e em uma magnitude
        this.velocidade = new Vector2(direcao).scl(300f); // Usamos new Vector2 para não modificar o original
        // Subtraímos 90 graus porque a sprite original aponta para CIMA (e não para a DIREITA).
        this.angulo = direcao.angleDeg() - 90f;
    }

    public void update(float delta, TileMap tileMap) {
        x += velocidade.x * delta;
        y += velocidade.y * delta;

        // Checa se a posição ATUAL do projétil está dentro de um tile sólido.
        // Usamos o centro do projétil para a checagem (supondo que o projétil tenha 8x8 de tamanho visual)
        if (tileMap.ehSolido(this.x + 4, this.y + 4)) {
            this.deveSerRemovido = true; // "Levanta a bandeira" para ser removido
        }
    }

    public void draw(SpriteBatch batch) {
        // Definimos as dimensões da nossa sprite
        float largura = textura.getRegionWidth();   // 5 pixels
        float altura = textura.getRegionHeight();  // 16 pixels

        // O ponto de origem da rotação (o "pivô").
        // Queremos que a flecha gire em torno de seu próprio centro.
        float origemX = largura / 2f;
        float origemY = altura / 2f;

        // Usamos o método draw avançado:
        batch.draw(
            textura,      // A TextureRegion a ser desenhada
            x,            // Posição X no mundo
            y,            // Posição Y no mundo
            origemX,      // O ponto X do pivô de rotação, relativo ao sprite
            origemY,      // O ponto Y do pivô de rotação, relativo ao sprite
            largura,      // Largura do sprite
            altura,       // Altura do sprite
            1f,           // Escala no eixo X (1f = 100%)
            1f,           // Escala no eixo Y (1f = 100%)
            this.angulo   // O ângulo de rotação calculado
        );
    }
}
