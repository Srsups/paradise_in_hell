package br.com.srsups.paradiseinhell;

public class Raio {
    public float x, y;
    public float dano = 40f; // Dano alto!
    public float raioDoDano = 24f; // O raio de alcance do dano em área
    private float duracao = 0.2f; // O raio fica visível por 0.2 segundos
    public boolean danoJaAplicado = false;
    public boolean deveSerRemovido = false;

    public Raio(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void update(float delta) {
        duracao -= delta;
        if (duracao <= 0) {
            deveSerRemovido = true;
        }
    }
}
