package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.Vector2;

public class Jogador {
    public float x, y;
    private Texture spritesheet;
    private Animation<TextureRegion> animacaoFrente, animacaoCostas, animacaoLado;
    private float stateTime;
    private float velocidade = 60f; // velocidade normal
    private float dashVelocidade = 200f;
    private boolean dashing = false;
    private float dashDuration = 0.15f;
    private float dashCooldown = 1f;
    private float dashTimer = 0f;
    private float dashCooldownTimer = 0f;
    public int vida = 100;
    private float tempoEntreTiros = 1f; // Define o intervalo de 1 segundo
    private float cooldownTiro = 0f;  // O timer que fará a contagem regressiva
    public int nivel = 1;
    private float xpAtual = 0;
    private int xpParaProximoNivel = 10; // Começa precisando de 10 XP
    private float estaminaMaxima = 100f;
    private float estaminaAtual = estaminaMaxima;
    private float custoDash = 20f;
    private float regeneracaoEstamina = 10f; // Estamina por segundo
    private boolean exausto = false;
    private float tempoFlash = 0.15f;
    private float timerFlash = 0f;
    private float modVelocidade = 1.0f; // 1.0f = 100% da velocidade base
    private float modDanoRecebido = 1.0f; // 1.0f = 100% do dano recebido
    private float modXpGanho = 1.0f; // 1.0f = 100% do XP ganho
    public int vidaMaxima = 100;
    private boolean temRessurreicao = false;
    private int curaPorAbate = 0;

    public Jogador(float x, float y, Texture spritesheet) {
        this.x = x;
        this.y = y;
        this.stateTime = 0f;
        this.spritesheet = spritesheet;

        TextureRegion[] framesFrente = new TextureRegion[2];
        framesFrente[0] = new TextureRegion(spritesheet, 0, 11, 16, 16);
        framesFrente[1] = new TextureRegion(spritesheet, 16, 11, 16, 16);
        animacaoFrente = new Animation<>(0.25f, framesFrente);
        animacaoFrente.setPlayMode(Animation.PlayMode.LOOP);

        TextureRegion[] framesLado = new TextureRegion[2];
        framesLado[0] = new TextureRegion(spritesheet, 51, 11, 16, 16);
        framesLado[1] = new TextureRegion(spritesheet, 35, 11, 16, 16);
        animacaoLado = new Animation<>(0.25f, framesLado);
        animacaoLado.setPlayMode(Animation.PlayMode.LOOP);

        TextureRegion[] framesCostas = new TextureRegion[2];
        framesCostas[0] = new TextureRegion(spritesheet, 69, 11, 16, 16);
        framesCostas[1] = new TextureRegion(spritesheet, 86, 11, 16, 16);
        animacaoCostas = new Animation<>(0.25f, framesCostas);
        animacaoCostas.setPlayMode(Animation.PlayMode.LOOP);
    }

    public enum Direcao {
        FRENTE, COSTAS, ESQUERDA, DIREITA, PARADO
    }

    private Direcao direcaoAtual = Direcao.PARADO;


    public void update(float delta, TileMap tileMap, OrthographicCamera camera) {
        stateTime += delta;

        // ETAPA 1: LÓGICA BASEADA NO TEMPO (Acontece sempre)

        // Atualiza timers de cooldowns (tiro e dash)
        if (cooldownTiro > 0) cooldownTiro -= delta;
        if (dashCooldownTimer > 0) dashCooldownTimer -= delta;
        if (timerFlash > 0) timerFlash -= delta;

        // --- LÓGICA DE ESTAMINA E EXAUSTÃO (MOVIDA PARA FORA DO BLOCO DE MOVIMENTO) ---
        if (dashing) {
            dashTimer -= delta;
            if (dashTimer <= 0) dashing = false;
        }

        // Condição para SAIR da exaustão
        if (exausto && estaminaAtual >= 20) {
            exausto = false;
        }

        // Condição para ENTRAR na exaustão
        if (estaminaAtual <= 5 && !exausto) {
            exausto = true;
        }

        // Lógica de Regeneração da Estamina
        if (!dashing) {
            if (exausto) {
                estaminaAtual += (regeneracaoEstamina / 3f) * delta;
            } else {
                estaminaAtual += regeneracaoEstamina * delta;
            }
        }
        estaminaAtual = Math.min(estaminaAtual, estaminaMaxima);
        // --------------------------------------------------------------------------

        // Lógica de Tiro
        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            if (cooldownTiro <= 0) {
                GameScreen.instance.criarProjetil(this.x + 8, this.y + 8, camera);
                cooldownTiro = tempoEntreTiros;
            }
        }

        // ETAPA 2: LÓGICA BASEADA NO INPUT DE MOVIMENTO

        Vector2 movimento = new Vector2();
        if (Gdx.input.isKeyPressed(Input.Keys.W)) movimento.y += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) movimento.y -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) movimento.x -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) movimento.x += 1;

        if (movimento.len() > 0) {
            movimento.nor();

            // Dash só pode ser iniciado se houver movimento
            if (Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_LEFT) && dashCooldownTimer <= 0 && estaminaAtual >= custoDash && !exausto) {
                dashing = true;
                dashTimer = dashDuration;
                dashCooldownTimer = dashCooldown;
                estaminaAtual -= custoDash;
            }

            float velocidadeNormal = exausto ? (velocidade * modVelocidade) * 0.6f : (velocidade * modVelocidade);
            float velocidadeFinal = dashing ? dashVelocidade : velocidadeNormal;
            float novaX = x + movimento.x * velocidadeFinal * delta;
            float novaY = y + movimento.y * velocidadeFinal * delta;

            // Colisão (agora com a correção que sugeri anteriormente)
            boolean colideX = tileMap.ehSolido(novaX + 1, y + 1) || tileMap.ehSolido(novaX + 15, y + 1) || tileMap.ehSolido(novaX + 1, y + 15) || tileMap.ehSolido(novaX + 15, y + 15);
            boolean colideY = tileMap.ehSolido(x + 1, novaY) || tileMap.ehSolido(x + 15, novaY) || tileMap.ehSolido(x + 1, novaY + 15) || tileMap.ehSolido(x + 15, novaY + 15);

            if (!colideX) x = novaX;
            if (!colideY) y = novaY;

            // Animação
            if (Math.abs(movimento.x) > Math.abs(movimento.y)) {
                direcaoAtual = movimento.x > 0 ? Direcao.DIREITA : Direcao.ESQUERDA;
            } else {
                direcaoAtual = movimento.y > 0 ? Direcao.COSTAS : Direcao.FRENTE;
            }
        } else {
            direcaoAtual = Direcao.PARADO;
        }
    }

    public float getEstaminaAtual() {
        return estaminaAtual;
    }

    public float getEstaminaMaxima() {
        return estaminaMaxima;
    }

    public boolean isExausto() {
        return exausto;
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
        this.vida -= (int)(quantidade * modDanoRecebido);
        this.timerFlash = this.tempoFlash; // Ativa o flash
    }

    public boolean estaMorto() {
        return this.vida <= 0;
    }

    public void ganharXP(int quantidade) {
        this.xpAtual += quantidade * modXpGanho;

        if (xpAtual >= xpParaProximoNivel) {
            // Em vez de chamar subirDeNivel() diretamente, notifica a GameScreen
            GameScreen.instance.iniciarLevelUp();
        }
    }

    public void subirDeNivel() {
        this.nivel++;
        this.xpAtual -= xpParaProximoNivel;
        this.xpParaProximoNivel *= 1.5;
    }

    public void aumentarVidaMaxima(int valor) {
        this.vidaMaxima += valor;
        this.vida += valor; // Também cura o jogador no mesmo valor
    }
    public void aumentarVelocidadeMovimento(float percentual) {
        this.modVelocidade += percentual; // ex: 0.10f para +10%
    }
    public void aumentarResistencia(float percentual) {
        this.modDanoRecebido -= percentual; // ex: 0.10f para -10% de dano
    }
    public void aumentarInteligencia(float percentual) {
        this.modXpGanho += percentual;
    }
    public void curar(int valor){
        this.vida = Math.min(this.vida + valor, this.vidaMaxima);
    }
    public boolean possuiRessurreicao(){
        return temRessurreicao;
    }
    public void ganharRessurreicao(){
        temRessurreicao = true;
    }
    public void usarRessurreicao(){
        temRessurreicao = false;
        this.vida = this.vidaMaxima / 2;
    }
    public void ativarCuraPorAbate(int valor){
        curaPorAbate = valor;
    }
    public int getCuraPorAbate(){
        return curaPorAbate;
    }

    // Métodos "get" para que a Main possa ler os valores para a UI
    public int getVida() { return this.vida; }
    public int getNivel() { return this.nivel; }
    public float getXpAtual() { return this.xpAtual; }
    public int getXpParaProximoNivel() { return this.xpParaProximoNivel; }
    public int getVidaMaxima() { return this.vidaMaxima; }

    public void dispose() {
    }
}
