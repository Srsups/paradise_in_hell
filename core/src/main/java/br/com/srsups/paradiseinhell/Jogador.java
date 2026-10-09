package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.Vector2;

/** Arena-local movement and graphics. Progression lives in the session. */
public class Jogador {
    @FunctionalInterface public interface AttackEmitter { void attack(float x, float y); }
    public float x, y;
    private final PlayerState stats;
    private final AttackEmitter emitter;
    private Animation<TextureRegion> animacaoFrente, animacaoCostas, animacaoLado;
    private float stateTime;
    public enum Direcao { FRENTE, COSTAS, ESQUERDA, DIREITA, PARADO }
    private Direcao direcaoAtual = Direcao.PARADO;

    public Jogador(float x, float y, Texture spritesheet, PlayerState stats, AttackEmitter emitter) {
        this.x = x;
        this.y = y;
        this.stats = stats;
        this.emitter = emitter;
        TextureRegion[] framesFrente = new TextureRegion[2];
        framesFrente[0] = Art.cell(spritesheet, 0, 0);
        framesFrente[1] = Art.cell(spritesheet, 1, 0);
        animacaoFrente = new Animation<>(0.25f, framesFrente);
        animacaoFrente.setPlayMode(Animation.PlayMode.LOOP);

        TextureRegion[] framesLado = new TextureRegion[2];
        framesLado[0] = Art.cell(spritesheet, 0, 1);
        framesLado[1] = Art.cell(spritesheet, 1, 1);
        animacaoLado = new Animation<>(0.25f, framesLado);
        animacaoLado.setPlayMode(Animation.PlayMode.LOOP);

        TextureRegion[] framesCostas = new TextureRegion[2];
        framesCostas[0] = Art.cell(spritesheet, 2, 0);
        framesCostas[1] = Art.cell(spritesheet, 3, 0);
        animacaoCostas = new Animation<>(0.25f, framesCostas);
        animacaoCostas.setPlayMode(Animation.PlayMode.LOOP);
    }


    public void update(float delta, TileMap tileMap) {
        stateTime += delta;
        stats.advance(delta);
        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) && stats.tryShoot()) emitter.attack(x + 8, y + 8);
        Vector2 movimento = new Vector2();
        if ((Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP))) movimento.y += 1;
        if ((Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN))) movimento.y -= 1;
        if ((Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT))) movimento.x -= 1;
        if ((Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT))) movimento.x += 1;

        if (movimento.len() > 0) {
            movimento.nor();

            // Dash só pode ser iniciado se houver movimento
            if (Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_RIGHT)) {
                stats.tryDash();
            }
            float velocidadeFinal = stats.movementSpeed();
            float novaX = x + movimento.x * velocidadeFinal * delta;
            float novaY = y + movimento.y * velocidadeFinal * delta;

            // Check all four corners before moving along each axis.
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

        if (stats.flashing()) {
            batch.setColor(Color.RED);
        }

        batch.draw(currentFrame, x, y, 16, 16);

        // Reset the tint before drawing other entities.
        batch.setColor(Color.WHITE);
    }


}
