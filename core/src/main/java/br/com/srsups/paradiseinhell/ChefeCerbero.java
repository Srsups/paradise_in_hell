package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class ChefeCerbero extends Inimigo {

    // Estados da IA
    private enum EstadoIA { PERSEGUINDO, CARREGANDO_ATAQUE, ATACANDO, ESPERANDO }
    private EstadoIA estadoAtual = EstadoIA.ESPERANDO;

    private int phase = 1;
    private float aiTimer = 3f; // Timer para decidir a próxima ação
    private float attackTimer = 0f; // Timer para a duração de um ataque

    public ChefeCerbero(float spawnX, float spawnY, Texture spritesheet) {
        super(spawnX, spawnY, spritesheet);
        this.vida = 500f; // Vida alta de chefe
        this.velocidade = 40f;
    }

    @Override
    public void update(float delta, Jogador jogador, TileMap tileMap) {
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
                // Fica parado, talvez com um efeito visual (cor diferente)
                attackTimer -= delta;
                if (attackTimer <= 0) {
                    // IMPLEMENTAR A LÓGICA DO ATAQUE (ex: Carga, Projéteis, etc)
                    System.out.println("CHEFE ATACA!");
                    estadoAtual = EstadoIA.ESPERANDO;
                    aiTimer = (phase == 1) ? 3f : 1.5f; // Na fase 2, espera menos
                }
                break;

            case ATACANDO:
                // Lógica de um ataque em andamento (ex: durante a carga)
                break;
        }
    }

    private void escolherProximaAcao(Jogador jogador) {
        // Lógica simples: persegue por um tempo
        estadoAtual = EstadoIA.PERSEGUINDO;
        aiTimer = 3f;
        System.out.println("Chefe está perseguindo...");

        // Futuramente, aqui você sortearia entre os 3 ataques (Carga, Projéteis, Invocação)
    }

    private void entrarFaseDois() {
        System.out.println("CÉRBERO ENTROU EM FÚRIA!");
        this.phase = 2;
        this.velocidade = 65f; // Fica mais rápido
        // Os timers entre ataques serão menores (ver na lógica da IA)
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
