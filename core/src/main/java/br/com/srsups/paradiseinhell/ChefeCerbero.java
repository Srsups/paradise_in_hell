package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import java.util.Random;

public class ChefeCerbero extends Inimigo {

    // Estados da IA
    private enum EstadoIA { PERSEGUINDO, CARREGANDO_ATAQUE, ATACANDO_CARGA, ESPERANDO }
    private EstadoIA estadoAtual = EstadoIA.ESPERANDO;
    private int phase = 1;
    private float aiTimer = 3f; // Timer para decidir a próxima ação
    private float attackTimer = 0f; // Timer para a duração de um ataque
    private Vector2 direcaoDoAtaque; // Para guardar a direção da carga
    private Random random = new Random(); // Para sortear os ataques
    private float danoAtaqueCarga = 25f;
    private Rectangle hitbox = new Rectangle();
    private float hitCooldown = 0f;

    // NOVA VARIÁVEL: Quão "no futuro" o chefe deve mirar? (em segundos)
    // Um valor maior torna a previsão mais longa, mas pode errar mais.
    // 0.4f é um bom ponto de partida.
    private float tempoDePrevisao = 0.4f;

    public ChefeCerbero(float spawnX, float spawnY, Texture spritesheet) {
        super(spawnX, spawnY, spritesheet);
        this.vida = 500f; // Vida alta de chefe
        this.velocidade = 40f;
    }

    @Override
    public void update(float delta, Jogador jogador, TileMap tileMap) {
        hitbox.set(this.x, this.y, this.width, this.height);
        if (hitCooldown > 0) hitCooldown -= delta;

        // Lógica de Transição de Fase
        if (this.vida < 250f && phase == 1) {
            entrarFaseDois();
        }

        aiTimer -= delta;

        // Máquina de Estados da IA
        switch (estadoAtual) {
            case ESPERANDO:
                // Se o timer acabar, escolhe uma nova ação
                if (aiTimer <= 0) {
                    escolherProximaAcao(jogador);
                }
                break;

            case PERSEGUINDO:
                // Persegue o jogador normalmente
                super.update(delta, jogador, tileMap);
                if (aiTimer <= 0) {
                    estadoAtual = EstadoIA.ESPERANDO;
                    aiTimer = 1f; // Pausa por 1 segundo antes da próxima ação
                }
                break;

            case CARREGANDO_ATAQUE:
                attackTimer -= delta;
                if (attackTimer <= 0) {
                    // --- PREVISÃO DE ÚLTIMO SEGUNDO ---
                    // 1. Pega a velocidade ATUAL do jogador.
                    Vector2 velocidadeJogador = jogador.getVelocity();

                    // 2. Calcula o deslocamento previsto.
                    Vector2 deslocamentoPrevisto = velocidadeJogador.cpy().scl(jogador.getVelocidadeAtual() * tempoDePrevisao);

                    // 3. Calcula a posição futura.
                    Vector2 posicaoPrevista = new Vector2(jogador.x, jogador.y).add(deslocamentoPrevisto);

                    // 4. "Trava a mira" na POSIÇÃO FUTURA.
                    direcaoDoAtaque = new Vector2(posicaoPrevista.x - this.x, posicaoPrevista.y - this.y).nor();
                    // ------------------------------------

                    // AGORA executa a carga
                    estadoAtual = EstadoIA.ATACANDO_CARGA;
                    attackTimer = 0.8f;
                    this.velocidade = 180f;
                }
                break;

            case ATACANDO_CARGA:
                // Move-se rapidamente na direção que foi travada
                float novaX = this.x + direcaoDoAtaque.x * velocidade * delta;
                float novaY = this.y + direcaoDoAtaque.y * velocidade * delta;

                Rectangle jogadorRect = new Rectangle(jogador.getX(), jogador.getY(), jogador.getWidth(), jogador.getHeight());
                if (Intersector.overlaps(jogadorRect, hitbox)) {
                    if (hitCooldown <= 0f) {
                        jogador.sofrerDano(danoAtaqueCarga);
                        hitCooldown = 0.6f;
                    }
                }

                attackTimer -= delta;
                if (attackTimer <= 0) {
                    // A carga terminou, volta ao estado de espera
                    this.velocidade = (phase == 1) ? 40f : 65f; // Reseta para a velocidade normal da fase
                    estadoAtual = EstadoIA.ESPERANDO;
                    aiTimer = (phase == 1) ? 2f : 1f; // Cooldown pós-ataque
                }
                break;
        }
    }
    public float getDanoAtaqueCarga() {
        return this.danoAtaqueCarga;
    }

    private void escolherProximaAcao(Jogador jogador) {
        float chance = random.nextFloat();
        float chanceDeAtacar = (phase == 1) ? 0.4f : 0.6f;

        if (chance > chanceDeAtacar) {
            estadoAtual = EstadoIA.PERSEGUINDO;
            aiTimer = 3f;
            System.out.println("Chefe decidiu: PERSEGUIR");
        } else {
            // INICIA O CARREGAMENTO, MAS NÃO CALCULA A DIREÇÃO AINDA
            estadoAtual = EstadoIA.CARREGANDO_ATAQUE;
            attackTimer = 1.2f; // Tempo que ele fica parado carregando
            System.out.println("Chefe decidiu: CARREGAR ATAQUE");
            // A direção será calculada no final do carregamento.
        }
    }

    private void entrarFaseDois() {
        System.out.println("CÉRBERO ENTROU EM FÚRIA!");
        this.phase = 2;
        this.velocidade = 65f; // Fica mais rápido
        // Os timers entre ataques serão menores (ver na lógica da IA)
    }

    public boolean estaAtacando() {
        return estadoAtual == EstadoIA.ATACANDO_CARGA;
    }

    @Override
    public void draw(SpriteBatch batch) {
        // Muda a cor na fase 2 para indicar fúria
        if (phase == 2) {
            batch.setColor(Color.ORANGE);
        }
        if (estadoAtual == EstadoIA.CARREGANDO_ATAQUE) {
            batch.setColor(Color.YELLOW);
        }

        super.draw(batch); // Chama o draw original do Inimigo
    }
}
