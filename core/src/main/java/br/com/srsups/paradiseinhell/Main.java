package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Game;

// 1. Mude "extends ApplicationAdapter" para "extends Game"
public class Main extends Game {

    // 2. O método create agora só tem uma função: definir a primeira tela.
    @Override
    public void create() {
        // Quando o jogo abrir, ele irá diretamente para a tela de menu.
        // Passamos "this" para que a tela de menu possa controlar o jogo (como trocar de tela).
        setScreen(new MenuScreen(this));
    }
}
