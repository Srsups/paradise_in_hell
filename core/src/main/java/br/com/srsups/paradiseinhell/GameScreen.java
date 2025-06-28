package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

// Definição dos estados do jogo
enum GameState {
    JOGANDO,
    LEVEL_UP
}

public class GameScreen implements Screen {
    private Game game;
    private GameState estadoAtual = GameState.JOGANDO; // O jogo começa no estado JOGANDO
    private float tempoDePreparo = 0.05f; // Meio segundo de tempo de preparo
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private OrthographicCamera camera;
    private OrthographicCamera hudCamera;
    private Jogador jogador;
    private TileMap tileMap;
    private Texture spritesheet;
    private ArrayList<Inimigo> inimigos = new ArrayList<>();
    private float spawnTimer = 0f;
    private float spawnInterval = 3f; // Spawn de um novo inimigo a cada 3 segundos
    private TextureRegion texturaProjetil;
    private ArrayList<Projetil> projeteis = new ArrayList<>();
    public static GameScreen instance;
    private ArrayList<OrbeXP> orbes = new ArrayList<>();
    private TextureRegion texturaOrbeXP;
    private BitmapFont font;
    private ArrayList<DamageNumber> damageNumbers = new ArrayList<>();
    private ArrayList<String> todasAsMelhorias;
    private ArrayList<String> melhoriasAtuais;
    private ArrayList<Rectangle> retangulosMelhorias;

    public GameScreen(Game game) {
        this.game = game;
        todasAsMelhorias = new ArrayList<>();
        melhoriasAtuais = new ArrayList<>();
        retangulosMelhorias = new ArrayList<>();
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        spritesheet = new Texture("spritesheet.png");

        // Cria uma câmera com a mesma dimensão da tela
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        hudCamera = new OrthographicCamera();
        hudCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        texturaProjetil = new TextureRegion(spritesheet, 178, 209, 5, 16);

        texturaOrbeXP = new TextureRegion(spritesheet, 9, 200, 6, 6);

        tileMap = new TileMap(spritesheet);

        jogador = new Jogador(100, 100, spritesheet);

        instance = this;

        tileMap.gerarAreaInicialSegura(jogador.x, jogador.y, 5);

        font = new BitmapFont();

        todasAsMelhorias.add("Resistência de Ares");
        todasAsMelhorias.add("Inteligência de Atena");
        todasAsMelhorias.add("Saúde de Minotauro");
        todasAsMelhorias.add("Sandálias aladas de Hermes");
        todasAsMelhorias.add("Cura de Apolo");
        todasAsMelhorias.add("Fúria da Quimera");
        todasAsMelhorias.add("Ressurreição de Asclépio");
        todasAsMelhorias.add("Aniquilação de Tifão");
    }

    public void iniciarLevelUp() {
        estadoAtual = GameState.LEVEL_UP; // PAUSA O JOGO
        jogador.subirDeNivel(); // Atualiza o XP e nível do jogador

        // Limpa as escolhas anteriores
        melhoriasAtuais.clear();
        retangulosMelhorias.clear();

        // Sorteia 3 melhorias únicas da lista principal
        Random random = new Random();
        ArrayList<String> copiaMelhorias = new ArrayList<>(todasAsMelhorias);
        for (int i = 0; i < 3 && !copiaMelhorias.isEmpty(); i++) {
            int index = random.nextInt(copiaMelhorias.size());
            String escolha = copiaMelhorias.remove(index);
            melhoriasAtuais.add(escolha);

            // Cria um retângulo de clique para cada opção de melhoria
            retangulosMelhorias.add(new Rectangle(
                Gdx.graphics.getWidth() / 2f - 150,
                Gdx.graphics.getHeight() / 2f + 50 - (i * 60), // Posição Y de cada opção
                300, 50
            ));
        }
    }

    private void aplicarMelhoria(String escolha) {
        System.out.println("Melhoria escolhida: " + escolha);
        // Aplica o efeito da melhoria ao jogador
        switch (escolha) {
            case "Força de Hércules":
            case "Celeridade de Ártemis":
            case "Saúde de Minotauro": jogador.aumentarVidaMaxima(25); break;
            case "Sandálias Aladas de Hermes": jogador.aumentarVelocidadeMovimento(0.10f); break;
            case "Resistência de Ares": jogador.aumentarResistencia(0.15f); break;
            case "Inteligência de Atena": jogador.aumentarInteligencia(0.20f); break;
            case "Cura de Apolo": jogador.curar(40); break;
            case "Fúria da Quimera": jogador.ativarCuraPorAbate(1); break;
            case "Égide":
            case "Raio de Zeus":
            case "Ressurreição de Asclépio": jogador.ganharRessurreicao(); todasAsMelhorias.remove("Ressurreição de Asclépio"); break;
            case "TITANOMAQUIA":
            case "GIGANTOMAQUIA":
            case "Magia de Hécate":
            case "Aniquilação de Tifão": inimigos.clear(); break;
            case "Necromancia de Hades":
            case "Correnteza de Poseidon":
            case "Cronocinese de Cronos":
            case "Limiar de Aquiles":
        }
        System.out.println("Vida Máxima: " + jogador.vidaMaxima);
        System.out.println("Vida Atual: " + jogador.vida);
        estadoAtual = GameState.JOGANDO; // VOLTA AO JOGO
    }

    private void updateLevelUp() {
        // Checa por cliques na tela de level up
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            Vector3 touchPos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);

            for (int i = 0; i < retangulosMelhorias.size(); i++) {
                Rectangle r = retangulosMelhorias.get(i);
                // É preciso converter as coordenadas do clique para o sistema da UI
                if (r.contains(touchPos.x, Gdx.graphics.getHeight() - touchPos.y)) {
                    aplicarMelhoria(melhoriasAtuais.get(i));
                    return; // Sai do método para não checar outros cliques
                }
            }
        }
    }

    private void desenharUiLevelUp() {
        // Usa o ShapeRenderer para desenhar um fundo semitransparente para pausar a tela
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 0.5f); // Cor preta com 50% de transparência
        shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // Usa o SpriteBatch para desenhar o texto das opções
        batch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.begin();

        font.draw(batch, "SUBIU DE NÍVEL! ESCOLHA UMA MELHORIA:", Gdx.graphics.getWidth() / 2f - 150, Gdx.graphics.getHeight() / 2f + 150);

        for (int i = 0; i < melhoriasAtuais.size(); i++) {
            String texto = melhoriasAtuais.get(i);
            Rectangle r = retangulosMelhorias.get(i);
            font.draw(batch, texto, r.x + 10, r.y + 35);
        }

        batch.end();
    }

    private void updateJogando(float delta){
        // Se o tempo de preparo ainda não acabou...
        if (tempoDePreparo > 0) {
            tempoDePreparo -= delta; // ...apenas diminui o timer.
        } else {

            jogador.update(delta, tileMap, camera);

            for (Inimigo inimigo : inimigos) {
                inimigo.update(delta, jogador, tileMap);
            }

            for (Projetil p : projeteis) {
                p.update(delta, tileMap);
            }

            // Atualiza a posição da câmera para seguir o jogador
            camera.position.set(jogador.x + 8, jogador.y + 8, 0);
            camera.update();
            tileMap.update(camera); // atualiza o mapa baseado na câmera

            spawnTimer += delta;
            if (spawnTimer >= spawnInterval) {
                // A Main agora é responsável por adicionar o inimigo à lista
                inimigos.add(EnemySpawner.spawnInimigoForaDaTela(camera, spritesheet, tileMap));
                spawnTimer = 0f;
            }

            // Agora, fazemos a checagem de colisões e remoções com um Iterator
            Iterator<Projetil> projetilIterator = projeteis.iterator();
            while (projetilIterator.hasNext()) {
                Projetil p = projetilIterator.next();

                // Checa se o projétil já deve ser removido (por ter atingido uma parede)
                if (p.deveSerRemovido) {
                    projetilIterator.remove(); // Remove e vai para o próximo projétil
                    continue;
                }

                // Se não atingiu uma parede, checa se atingiu um inimigo
                Iterator<Inimigo> inimigoIterator = inimigos.iterator();
                while (inimigoIterator.hasNext()) {
                    Inimigo i = inimigoIterator.next();

                    // Checa colisão entre projétil e inimigo
                    if (p.x < i.x + 16 && p.x + 8 > i.x && p.y < i.y + 16 && p.y + 8 > i.y) {
                        i.sofrerDano(p.dano);
                        p.deveSerRemovido = true; // Marca o projétil para remoção

                        damageNumbers.add(new DamageNumber(String.valueOf(p.dano), i.x + 8, i.y + 16));

                        if (i.estaMorto()) {
                            // Cria um novo orbe na posição do inimigo
                            orbes.add(new OrbeXP(i.x, i.y, texturaOrbeXP));
                            jogador.curar(jogador.getCuraPorAbate());
                            inimigoIterator.remove();
                        }
                        break; // Sai do loop de inimigos, pois o projétil já atingiu seu alvo
                    }
                }

                // Após checar contra todos os inimigos, vemos se o projétil foi marcado para remoção
                if (p.deveSerRemovido) {
                    projetilIterator.remove();
                }
            }

            Iterator<Inimigo> inimigoIterator = inimigos.iterator();
            while (inimigoIterator.hasNext()) {
                Inimigo i = inimigoIterator.next();
                if (jogador.x < i.x + 16 && jogador.x + 16 > i.x && jogador.y < i.y + 16 && jogador.y + 16 > i.y) {
                    // Checa se o inimigo pode atacar
                    if (i.podeAtacar()) {
                        jogador.sofrerDano(10);
                        i.resetarCooldownAtaque(); // Inicia o cooldown do inimigo
                    }
                }
            }

            Iterator<OrbeXP> orbeIterator = orbes.iterator();
            while (orbeIterator.hasNext()) {
                OrbeXP orbe = orbeIterator.next();
                // Checa colisão simples por distância
                if (new Vector2(jogador.x, jogador.y).dst(orbe.x, orbe.y) < 16) {
                    jogador.ganharXP(orbe.valorXP);
                    orbeIterator.remove(); // Remove o orbe após a coleta
                }
            }

            Iterator<DamageNumber> dnIterator = damageNumbers.iterator();
            while (dnIterator.hasNext()) {
                DamageNumber dn = dnIterator.next();
                dn.update(delta);
                if (dn.deveSerRemovido()) {
                    dnIterator.remove();
                }
            }

            // VERIFICA SE O JOGADOR MORREU
            if (jogador.estaMorto()) {
                if (jogador.possuiRessurreicao()) {
                    jogador.usarRessurreicao(); // Método que seta a flag para false e cura o jogador pela metade
                } else {
                    game.setScreen(new GameOverScreen(game));
                    dispose();
                    return;
                }
            }
        }
    }

    private void desenharMundo(){
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        tileMap.draw(batch);    // desenha os tiles primeiro (fundo)
        jogador.draw(batch);    // desenha o jogador por cima

        for (Inimigo inimigo : inimigos) {
            inimigo.draw(batch);
        }

        for (Projetil projetil : projeteis) {
            projetil.draw(batch);
        }

        for (OrbeXP orbe : orbes) {
            orbe.draw(batch);
        }

        for (DamageNumber dn : damageNumbers) {
            dn.draw(batch, font); // Passa a fonte para o método de desenho
        }

        batch.end();
    }

    private void desenharUI(){
        // Começamos com o ShapeRenderer para desenhar as barras (formas preenchidas)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // --- Barra de Vida ---
        float barX = 10f;
        float barY = Gdx.graphics.getHeight() - 30f;
        float barWidth = 200f;
        float barHeight = 20f;
        float percentualVida = (float)jogador.getVida() / jogador.getVidaMaxima();

        // Desenha o fundo da barra de vida
        shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(barX, barY, barWidth, barHeight);
        // Desenha a parte da frente da barra de vida
        shapeRenderer.setColor(Color.RED);
        shapeRenderer.rect(barX, barY, barWidth * percentualVida, barHeight);

        // --- Barra de Estamina ---
        barY -= 30f; // Posição da barra de estamina, um pouco abaixo da de vida
        float percentualEstamina = jogador.getEstaminaAtual() / jogador.getEstaminaMaxima();

        // Desenha o fundo da barra de estamina
        shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(barX, barY, barWidth, barHeight);
        // Desenha a parte da frente da barra de estamina, com cor dinâmica
        if (jogador.isExausto()) {
            shapeRenderer.setColor(Color.FIREBRICK); // Vermelho escuro para exaustão
        } else {
            shapeRenderer.setColor(Color.GREEN);
        }
        shapeRenderer.rect(barX, barY, barWidth * percentualEstamina, barHeight);

        shapeRenderer.end(); // Termina o desenho das formas

        // Desenha o texto por cima das barras
        batch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.begin();

        // Desenha os textos
        String textoNivel = "Nível: " + jogador.getNivel();
        font.draw(batch, textoNivel, 10, Gdx.graphics.getHeight() - 80);

        String textoXP = "XP: " + jogador.getXpAtual() + " / " + jogador.getXpParaProximoNivel();
        font.draw(batch, textoXP, 10, Gdx.graphics.getHeight() - 100);

        batch.end();
    }

    @Override
    public void render(float delta) {
        // A lógica de update agora depende do estado atual
        switch (estadoAtual) {
            case JOGANDO:
                // Toda a lógica de update vai aqui
                updateJogando(delta);
                break;
            case LEVEL_UP:
                // A lógica de input para a tela de level up vai aqui
                updateLevelUp();
                break;
        }

        desenharMundo();
        desenharUI();

        // Se estivermos no estado de level up, desenha a UI de melhorias por cima
        if (estadoAtual == GameState.LEVEL_UP) {
            desenharUiLevelUp();
        }
    }

    @Override
    public void resize(int i, int i1) {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    public void criarProjetil(float x, float y, OrthographicCamera camera) {
        // ... (cálculo da direção do mouse, sem alterações) ...
        Vector3 mousePosTela = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        Vector3 mousePosMundo = camera.unproject(mousePosTela);
        Vector2 direcao = new Vector2(mousePosMundo.x - x, mousePosMundo.y - y).nor();

        // Modificação: Passa a TextureRegion que já foi carregada e recortada
        projeteis.add(new Projetil(x, y, direcao, texturaProjetil));
    }


    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        tileMap.dispose();
        jogador.dispose();
        spritesheet.dispose();
        font.dispose();
    }
}
