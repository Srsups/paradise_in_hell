package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.Animation;


public class Jogador {

    // Posição do jogador no mundo
    public float x, y;

    // A folha de sprites completa
    private Texture spritesheet;

    // As animações para cada direção. Animation<TextureRegion> significa
    // que é uma animação composta por TextureRegions.
    private Animation<TextureRegion> animacaoFrente;
    private Animation<TextureRegion> animacaoCostas;
    private Animation<TextureRegion> animacaoLado;

    // Variável para contar o tempo de animação. (É essencial)
    private float stateTime;

    // O construtor é chamado quando cria um novo jogador.
    public Jogador(float x, float y) {
        this.x = x;
        this.y = y;
        this.stateTime = 0f; // Inicializa o contador de tempo

        // 1. Carrega a imagem principal do spritesheet
        // Imagem na pasta core/assets
        spritesheet = new Texture("spritesheet.png");

        // 2. Cria os frames da animação "Frente"
        TextureRegion[] framesFrente = new TextureRegion[2];
        framesFrente[0] = new TextureRegion(spritesheet, 0, 11, 16, 16); // Equivalente a getSprite(0, 11, 16, 16)
        framesFrente[1] = new TextureRegion(spritesheet, 16, 11, 16, 16); // Equivalente a getSprite(16, 11, 16, 16)

        // 3. Cria o objeto de Animação para "Frente"
        // 0.25f é a duração de cada frame em segundos.
        // Animation.PlayMode.LOOP significa que a animação irá repetir.
        animacaoFrente = new Animation<>(0.25f, framesFrente);
        animacaoFrente.setPlayMode(Animation.PlayMode.LOOP);

        // Exemplo para "Lado"
        TextureRegion[] framesLado = new TextureRegion[2];
        framesLado[0] = new TextureRegion(spritesheet, 51, 11, 16, 16);
        framesLado[1] = new TextureRegion(spritesheet, 35, 11, 16, 16);
        animacaoLado = new Animation<>(0.25f, framesLado);
        animacaoLado.setPlayMode(Animation.PlayMode.LOOP);

        // Exemplo para "Costas/Cima"
        TextureRegion[] framesCostas = new TextureRegion[2];
        framesCostas[0] = new TextureRegion(spritesheet, 69, 11, 16, 16);
        framesCostas[1] = new TextureRegion(spritesheet, 86, 11, 16, 16);
        animacaoCostas = new Animation<>(0.25f, framesCostas);
        animacaoCostas.setPlayMode(Animation.PlayMode.LOOP);
    }

    // Método responsável por desenhar o jogador
    // O Gdx.graphics.getDeltaTime() retorna o tempo passado desde o último frame.
    // Isso garante que a animação rode na mesma velocidade em qualquer computador.
    public void update(float delta) {
        stateTime += delta; // Acumula o tempo
    }

    public void setEstado(Estado novoEstado) {
        this.estadoAtual = novoEstado;
    }


    public enum Estado {
        FRENTE, COSTAS, LADO_ESQUERDO, LADO_DIREITO, PARADO
    }

    private Estado estadoAtual = Estado.PARADO;

    private TextureRegion getFrameAtual() {
        switch (estadoAtual) {
            case FRENTE:
                return animacaoFrente.getKeyFrame(stateTime, true);
            case COSTAS:
                return animacaoCostas.getKeyFrame(stateTime, true);
            case LADO_ESQUERDO:
            case LADO_DIREITO:
                return animacaoLado.getKeyFrame(stateTime, true);
            case PARADO:
            default:
                return animacaoFrente.getKeyFrame(0); // Primeiro frame da frente como "parado"
        }
    }

    public void draw(SpriteBatch batch) {
        batch.draw(getFrameAtual(), x, y);
    }



    // Método para liberar a textura da memória quando o jogo fechar
    public void dispose() {
        spritesheet.dispose();
    }
}
