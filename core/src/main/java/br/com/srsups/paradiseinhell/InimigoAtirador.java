package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

public class InimigoAtirador extends Inimigo {

    // --- Atributos específicos do Atirador ---
    private float distanciaDeAtaque = 150f; // Distância (em pixels) que ele para de seguir e começa a atirar
    private float cooldownTiro = 2.5f;      // Atira a cada 2.5 segundos
    private float timerTiro = 0f;           // Timer para o cooldown do tiro

    public InimigoAtirador(float spawnX, float spawnY, Texture spritesheet) {
        // 1. Chama o construtor da classe pai (Inimigo) para configurar o básico
        super(spawnX, spawnY, spritesheet);

        // 2. Modifica os atributos para este tipo de inimigo
        this.velocidade = 25f;  // Um pouco mais lento que o inimigo base
        this.vida = 15f;        // Menos vida, pois ataca de longe
    }

    // 3. Sobrescrevemos (@Override) o método update para criar a nova IA
    @Override
    public void update(float delta, Jogador jogador, TileMap tileMap) {
        // Atualiza o tempo de animação e timers, como na classe pai
        stateTime += delta;
        if (timerAtaque > 0) timerAtaque -= delta; // Cooldown de ataque de contato (herdado)
        if (timerFlash > 0) timerFlash -= delta;
        if (timerTiro > 0) timerTiro -= delta;     // Cooldown do tiro

        // Calcula a direção e a distância até o jogador
        Vector2 direcaoParaJogador = new Vector2(jogador.x - this.x, jogador.y - this.y);
        // Usamos len2() para comparar distâncias, é mais rápido que len() pois não usa raiz quadrada
        float distancia2 = direcaoParaJogador.len2();

        // --- LÓGICA DE DECISÃO DA IA ---

        // SE o jogador está MAIS LONGE que a distância de ataque...
        if (distancia2 > distanciaDeAtaque * distanciaDeAtaque) {
            // ...o inimigo se COMPORTA COMO O INIMIGO NORMAL: ele persegue o jogador.
            // (Copiamos a lógica de movimento da classe Inimigo)
            direcaoParaJogador.nor(); // Normaliza o vetor para obter apenas a direção

            float novaX = this.x + direcaoParaJogador.x * velocidade * delta;
            float novaY = this.y + direcaoParaJogador.y * velocidade * delta;

            boolean colideX = tileMap.ehSolido(novaX + 8, this.y + 8);
            boolean colideY = tileMap.ehSolido(this.x + 8, novaY + 8);

            if (!colideX) this.x = novaX;
            if (!colideY) this.y = novaY;

            // Define a direção da animação baseada no movimento
            atualizarDirecaoAnimacao(direcaoParaJogador);

        } else {
            // SE o jogador está DENTRO do alcance...
            // 1. O inimigo PARA de se mover
            this.direcaoAtual = Jogador.Direcao.PARADO;

            // 2. O inimigo tenta ATIRAR
            if (timerTiro <= 0) {
                // Notifica a GameScreen para criar um projétil para nós
                GameScreen.instance.criarProjetilInimigo(this.x + 8, this.y + 8, jogador.x + 8, jogador.y + 8);
                timerTiro = cooldownTiro; // Reseta o cooldown do tiro
            }
        }
    }

    // 4. (Opcional) Podemos sobrescrever o método draw para dar uma cor diferente
    @Override
    public void draw(SpriteBatch batch) {
        // Pinta o inimigo de uma cor verde para diferenciá-lo
        batch.setColor(Color.GREEN);
        super.draw(batch); // Chama o método de desenho original da classe Inimigo
        // A cor do batch já é resetada para branco no final do super.draw()
    }
}
