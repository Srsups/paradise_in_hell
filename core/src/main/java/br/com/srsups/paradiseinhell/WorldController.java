package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.graphics.OrthographicCamera;

public interface WorldController {
    void criarProjetil(Jogador atirador, float x, float y, OrthographicCamera camera);
}
