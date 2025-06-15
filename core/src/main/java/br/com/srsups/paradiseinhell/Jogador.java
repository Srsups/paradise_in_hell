package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
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

    public Jogador(float x, float y) {
        this.x = x;
        this.y = y;
        this.stateTime = 0f;

        spritesheet = new Texture("spritesheet.png");

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


    public void update(float delta, TileMap tileMap) {
        stateTime += delta;

        Vector2 movimento = new Vector2();
        if (Gdx.input.isKeyPressed(Input.Keys.W)) movimento.y += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) movimento.y -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) movimento.x -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) movimento.x += 1;

        if (movimento.len() > 0) {
            movimento.nor();

            // Dash
            if (Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_LEFT) && dashCooldownTimer <= 0) {
                dashing = true;
                dashTimer = dashDuration;
                dashCooldownTimer = dashCooldown;
            }

            float velocidadeFinal = dashing ? dashVelocidade : velocidade;
            float novaX = x + movimento.x * velocidadeFinal * delta;
            float novaY = y + movimento.y * velocidadeFinal * delta;

            // Verifica colisão nos cantos
            boolean colideX = tileMap.ehSolido(novaX, y) ||             // Canto superior esquerdo
                tileMap.ehSolido(novaX + 15, y) ||         // Canto superior direito
                tileMap.ehSolido(novaX, y + 15) ||         // Canto inferior esquerdo
                tileMap.ehSolido(novaX + 15, y + 15);      // Canto inferior direito

            // Pontos de verificação para o eixo Y
            boolean colideY = tileMap.ehSolido(x, novaY) ||             // Canto superior esquerdo
                tileMap.ehSolido(x + 15, novaY) ||         // Canto superior direito
                tileMap.ehSolido(x, novaY + 15) ||         // Canto inferior esquerdo
                tileMap.ehSolido(x + 15, novaY + 15);      // Canto inferior direito

            if (!colideX) x = novaX;
            if (!colideY) y = novaY;

            // Define direção atual
            if (Math.abs(movimento.x) > Math.abs(movimento.y)) {
                direcaoAtual = movimento.x > 0 ? Direcao.DIREITA : Direcao.ESQUERDA;
            } else {
                direcaoAtual = movimento.y > 0 ? Direcao.COSTAS : Direcao.FRENTE;
            }
        } else {
            direcaoAtual = Direcao.PARADO;
        }

        // Atualiza timers
        if (dashing) {
            dashTimer -= delta;
            if (dashTimer <= 0) dashing = false;
        }
        if (dashCooldownTimer > 0) {
            dashCooldownTimer -= delta;
        }
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

        batch.draw(currentFrame, x, y);
    }


    public void dispose() {
        spritesheet.dispose();
    }
}
