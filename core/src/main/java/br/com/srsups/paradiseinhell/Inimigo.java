package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

public class Inimigo {
    private float stateTime;
    public float x, y;
    private Texture spritesheet;
    private float velocidade = 30f;
    public int vida = 10;
    private int dano;
    private Animation<TextureRegion> animacaoFrente, animacaoCostas, animacaoLado;
    private Jogador.Direcao direcaoAtual = Jogador.Direcao.PARADO;
    private float tempoFlash = 0.15f;
    private float timerFlash = 0f;
    private float cooldownAtaque = 1f; // Inimigo pode atacar a cada 1 segundo
    private float timerAtaque = 0f;

    public Inimigo(float spawnX, float spawnY, Texture spritesheet) {
        this.x = spawnX; // Correto
        this.y = spawnY; // Correto
        this.stateTime = 0f;
        this.spritesheet = spritesheet;

        TextureRegion[] framesFrente = new TextureRegion[2];
        framesFrente[0] = new TextureRegion(spritesheet, 0, 29, 16, 16);
        framesFrente[1] = new TextureRegion(spritesheet, 16, 29, 16, 16);
        animacaoFrente = new Animation<>(0.25f, framesFrente);
        animacaoFrente.setPlayMode(Animation.PlayMode.LOOP);

        TextureRegion[] framesLado = new TextureRegion[2];
        framesLado[0] = new TextureRegion(spritesheet, 51, 29, 16, 16);
        framesLado[1] = new TextureRegion(spritesheet, 35, 29, 16, 16);
        animacaoLado = new Animation<>(0.25f, framesLado);
        animacaoLado.setPlayMode(Animation.PlayMode.LOOP);

        TextureRegion[] framesCostas = new TextureRegion[2];
        framesCostas[0] = new TextureRegion(spritesheet, 69, 29, 16, 16);
        framesCostas[1] = new TextureRegion(spritesheet, 86, 29, 16, 16);
        animacaoCostas = new Animation<>(0.25f, framesCostas);
        animacaoCostas.setPlayMode(Animation.PlayMode.LOOP);
    }

    // Em Inimigo.java
    public void update(float delta, Jogador jogador, TileMap tileMap) {
        stateTime += delta;
        Vector2 direcao = new Vector2(jogador.x - this.x, jogador.y - this.y);

        if (direcao.len2() > 1) { // Usar len2() é um pouco mais otimizado que len()
            direcao.nor();

            float novaX = this.x + direcao.x * velocidade * delta;
            float novaY = this.y + direcao.y * velocidade * delta;

            // Checa colisão ANTES de mover
            boolean colideX = tileMap.ehSolido(novaX + 8, this.y + 8); // Checa o centro do inimigo
            boolean colideY = tileMap.ehSolido(this.x + 8, novaY + 8);

            // Move apenas se não houver colisão no eixo
            if (!colideX) this.x = novaX;
            if (!colideY) this.y = novaY;

            // Define a direção da animação
            if (Math.abs(direcao.x) > Math.abs(direcao.y)) {
                direcaoAtual = direcao.x > 0 ? Jogador.Direcao.DIREITA : Jogador.Direcao.ESQUERDA;
            } else {
                direcaoAtual = direcao.y > 0 ? Jogador.Direcao.COSTAS : Jogador.Direcao.FRENTE;
            }

        } else {
            direcaoAtual = Jogador.Direcao.PARADO;
        }

        if (timerAtaque > 0) {
            timerAtaque -= delta;
        }

        if (timerFlash > 0) {
            timerFlash -= delta;
        }
    }

    public boolean podeAtacar() {
        return timerAtaque <= 0;
    }

    public void resetarCooldownAtaque() {
        this.timerAtaque = this.cooldownAtaque;
    }

    public void draw(SpriteBatch batch) {
        TextureRegion currentFrame;

        switch (direcaoAtual) {
            case FRENTE:
                currentFrame = animacaoFrente.getKeyFrame(stateTime, true);
                break;
            case COSTAS:
                currentFrame = animacaoCostas.getKeyFrame(stateTime, true);
                break;
            case ESQUERDA:
                currentFrame = animacaoLado.getKeyFrame(stateTime, true);
                if (!currentFrame.isFlipX()) {
                    currentFrame.flip(true, false); // Vira a própria região
                }
                break;
            case DIREITA:
                currentFrame = animacaoLado.getKeyFrame(stateTime, true);
                if (currentFrame.isFlipX()) {
                    currentFrame.flip(true, false); // Desvira a região se ela estiver virada
                }
                break;
            default:
                currentFrame = animacaoFrente.getKeyFrame(0); // parado
        }

        if (timerFlash > 0) {
            batch.setColor(Color.RED);
        }

        batch.draw(currentFrame, x, y);

        // CRUCIAL: Reseta a cor do batch para branco para não afetar outros sprites
        batch.setColor(Color.WHITE);
    }

    public void sofrerDano(int quantidade) {
        this.vida -= quantidade;
        this.timerFlash = this.tempoFlash; // Ativa o flash
    }

    public boolean estaMorto() {
        return this.vida <= 0;
    }

    public void dispose() {
    }
}
