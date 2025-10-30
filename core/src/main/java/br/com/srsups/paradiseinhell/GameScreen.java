package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

// Definição dos estados do jogo
enum GameState {
    JOGANDO,
    LEVEL_UP
}

public class GameScreen implements Screen, WorldController {
    private Main game;
    private GameState estadoAtual = GameState.JOGANDO; // O jogo começa no estado JOGANDO
    private float tempoDePreparo = 0.05f; // Meio segundo de tempo de preparo
    private Viewport viewport;
    private OrthographicCamera camera;
    private Jogador jogador;
    private TileMap tileMap;
    private Texture spritesheet;
    private ArrayList<Inimigo> inimigos = new ArrayList<>();
    private float spawnTimer = 0f;
    private float spawnInterval = 3f; // Spawn de um novo inimigo a cada 3 segundos
    private TextureRegion texturaProjetil;
    private ArrayList<Projetil> projeteis = new ArrayList<>();
    private ArrayList<ProjetilInimigo> projeteisInimigos = new ArrayList<>();
    public static GameScreen instance;
    private ArrayList<OrbeXP> orbes = new ArrayList<>();
    private TextureRegion texturaOrbeXP;
    private ArrayList<Obolo> obolos = new ArrayList<>();
    private int obolosColetadosNaRun = 0;
    private Random randomParaDrops = new Random();
    private TextureRegion texturaObolo;
    private ArrayList<DamageNumber> damageNumbers = new ArrayList<>();
    private ArrayList<String> todasAsMelhorias;
    private ArrayList<String> melhoriasAtuais;
    private ArrayList<Rectangle> retangulosMelhorias;
    private ArrayList<Raio> raios = new ArrayList<>();
    private float gameTimer = 0f;
    private float bossPortalSpawnTime = 12f; // 2 minutos
    private Portal portal = null; // Começa como nulo
    private TextureRegion texturaPortal; // Vamos carregar a textura no show()

    public GameScreen(Main game) {
        this.game = game;
        todasAsMelhorias = new ArrayList<>();
        melhoriasAtuais = new ArrayList<>();
        retangulosMelhorias = new ArrayList<>();
    }

    @Override
    public void show() {
        spritesheet = new Texture("spritesheet.png");

        // Cria uma câmera com a mesma dimensão da tela
        camera = new OrthographicCamera();
        viewport = new ExtendViewport(1920, 1080, camera); // "virtual" world de 1920x1080
        viewport.apply();
        // Note: não podemos acessar 'jogador' aqui ainda — ele é criado mais abaixo.
        // A posição inicial da câmera será ajustada depois que o jogador for criado.

        texturaProjetil = new TextureRegion(spritesheet, 178, 209, 5, 16);

        texturaOrbeXP = new TextureRegion(spritesheet, 9, 200, 6, 6);

        texturaObolo = new TextureRegion(spritesheet, 9, 200, 6, 6);

        tileMap = new TileMap(spritesheet);

        // 1. Carrega os dados salvos
        Preferences prefs = Gdx.app.getPreferences("ParadiseInHellSave");
        int nivelUpgradeVida = prefs.getInteger("upgrade_vida_nivel", 0);
        int nivelUpgradeDano = prefs.getInteger("upgrade_dano_nivel", 0);
        int nivelUpgradeVelocidade = prefs.getInteger("upgrade_velocidade_nivel", 0);

        // 2. Calcula o bônus com base no nível do upgrade
        float bonusDeVida = nivelUpgradeVida * 20f;
        System.out.println("Iniciando jogo com bônus de vida: +" + bonusDeVida);

        float bonusDeDano = nivelUpgradeDano * 20f;
        System.out.println("Iniciando jogo com bônus de dano: +" + bonusDeDano);

        float bonusDeVelocidade = nivelUpgradeVelocidade * 20f;
        System.out.println("Iniciando jogo com bônus de velocidade: +" + bonusDeVelocidade);


        // 3. Cria o jogador, passando o bônus calculado para o novo construtor
        jogador = new Jogador(100, 100, spritesheet, bonusDeVida, bonusDeDano, bonusDeVelocidade);

        instance = this;

    // Agora que o jogador foi criado, posiciona a câmera sobre ele e atualiza.
    camera.position.set(jogador.getX() + jogador.getWidth() / 2f, jogador.getY() + jogador.getHeight() / 2f, 0);
    camera.update();

        tileMap.gerarAreaInicialSegura(jogador.x, jogador.y, 5);

        texturaPortal = new TextureRegion(spritesheet, /*x*/249, /*y*/270, /*w*/24, /*h*/24); // Exemplo: uma pedra mágica

        todasAsMelhorias.add("Saúde de Minotauro");
        todasAsMelhorias.add("Sandálias Aladas de Hermes");
        todasAsMelhorias.add("Resistência de Ares");
        todasAsMelhorias.add("Inteligência de Atena");
        todasAsMelhorias.add("Cura de Apolo");
        todasAsMelhorias.add("Fúria da Quimera");
        todasAsMelhorias.add("Égide");
        todasAsMelhorias.add("Ressurreição de Asclépio");
        todasAsMelhorias.add("Aniquilação de Tifão");
        todasAsMelhorias.add("Correnteza de Poseidon");
        todasAsMelhorias.add("Raio de Zeus");
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
            case "Sangue de Quimera": jogador.ativarCuraPorAbate(1); break;
            case "Égide": jogador.ativarHabilidadeEgide(); break;
            case "Raio de Zeus": jogador.ativarHabilidadeRaio(); break;
            case "Ressurreição de Asclépio": jogador.ganharRessurreicao(); todasAsMelhorias.remove("Ressurreição de Asclépio"); break;
            case "TITANOMAQUIA": //muitos inimigos
            case "GIGANTOMAQUIA": //ainda mais inimigos
            case "Magia de Hécate":
            case "Aniquilação de Tifão":
                // Itera sobre uma cópia da lista para evitar problemas de modificação concorrente
                for (Inimigo inimigo : new ArrayList<>(inimigos)) {
                    inimigo.matar();
                }
                break;
            case "Necromancia de Hades":
            case "Correnteza de Poseidon": jogador.ativarAuraDePoseidon(); break;
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
        // A HUD já tem uma viewport, vamos usar a câmera dela para desenhar.
        // Isso garante que a UI de Level Up escale da mesma forma que o resto da HUD.
        OrthographicCamera uiCamera = (OrthographicCamera) game.hud.getViewport().getCamera();

        // Usa o ShapeRenderer para desenhar um fundo semitransparente
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        game.shapeRenderer.setProjectionMatrix(uiCamera.combined); // <-- USA A CÂMERA DA HUD
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        game.shapeRenderer.setColor(0, 0, 0, 0.7f); // Um pouco mais escuro
        // Desenha o retângulo cobrindo a visão da viewport da HUD
        game.shapeRenderer.rect(0, 0, game.hud.getViewport().getWorldWidth(), game.hud.getViewport().getWorldHeight());
        game.shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // Usa o SpriteBatch para desenhar o texto das opções
        game.batch.setProjectionMatrix(uiCamera.combined); // <-- USA A CÂMERA DA HUD
        game.batch.begin();

        // Posiciona o texto com base na resolução virtual da HUD (1920x1080)
        float centerX = game.hud.getViewport().getWorldWidth() / 2f;
        float startY = game.hud.getViewport().getWorldHeight() * 0.8f;

        game.font.draw(game.batch, "SUBIU DE NÍVEL! ESCOLHA UMA MELHORIA:", centerX - 300, startY);

        for (int i = 0; i < melhoriasAtuais.size(); i++) {
            String texto = melhoriasAtuais.get(i);
            // Recalcula os retângulos aqui para serem baseados na viewport
            Rectangle r = new Rectangle(
                centerX - 200,
                startY - 150 - (i * 100), // Posição Y de cada opção
                400, 80
            );
            retangulosMelhorias.set(i, r); // Atualiza o retângulo de clique
            game.font.draw(game.batch, texto, r.x + 20, r.y + 55);
        }

        game.batch.end();
    }

    private void updateJogando(float delta){
        // Se o tempo de preparo ainda não acabou...
        if (tempoDePreparo > 0) {
            tempoDePreparo -= delta; // ...apenas diminui o timer.
        } else {

            jogador.update(delta, tileMap, camera, this);

            for (Inimigo inimigo : inimigos) {
                inimigo.update(delta, jogador, tileMap);
            }

            for (Projetil p : projeteis) {
                p.update(delta, tileMap);
            }

            // Atualiza a posição da câmera para seguir o jogador
            camera.position.lerp(
                new Vector3(jogador.getX() + jogador.getWidth() / 2f, jogador.getY() + jogador.getHeight() / 2f, 0),
                0.1f  // suavização (10%)
            );
            camera.update();
            tileMap.update(camera); // atualiza o mapa baseado na câmera

            if (jogador.possuiAura()) {
                for (Inimigo inimigo : inimigos) {
                    // Usamos dst2 (distância ao quadrado) porque é mais rápido que dst (que usa raiz quadrada)
                    if (new Vector2(jogador.x, jogador.y).dst2(inimigo.x, inimigo.y) < jogador.getRaioAura() * jogador.getRaioAura()) {
                        // Aplica dano contínuo baseado no delta time
                        inimigo.sofrerDano(jogador.getDanoAura() * delta);
                    }
                }
            }

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
                for (Inimigo i : inimigos) {
                    // Checa colisão entre projétil e inimigo
                    if (p.x < i.x + 16 && p.x + 8 > i.x && p.y < i.y + 16 && p.y + 8 > i.y) {
                        i.sofrerDano(p.dano);
                        p.deveSerRemovido = true; // Marca o projétil para remoção

                        damageNumbers.add(new DamageNumber(String.valueOf(p.dano), i.x + 8, i.y + 16, Color.YELLOW));

                        break; // Sai do loop de inimigos, pois o projétil já atingiu seu alvo
                    }
                }

                // Após checar contra todos os inimigos, vemos se o projétil foi marcado para remoção
                if (p.deveSerRemovido) {
                    projetilIterator.remove();
                }
            }

            // --- LÓGICA DOS PROJÉTEIS INIMIGOS ---
            Iterator<ProjetilInimigo> projInimigoIterator = projeteisInimigos.iterator();
            while (projInimigoIterator.hasNext()) {
                ProjetilInimigo p = projInimigoIterator.next();
                p.update(delta, tileMap); // Atualiza a posição do projétil

                // Remove se colidir com uma parede
                if (p.deveSerRemovido) {
                    projInimigoIterator.remove();
                    continue;
                }

                // Checa colisão com o JOGADOR
                if (p.x < jogador.x + 16 && p.x + 8 > jogador.x && p.y < jogador.y + 16 && p.y + 8 > jogador.y) {
                    jogador.sofrerDano(p.dano);
                    projInimigoIterator.remove(); // Remove o projétil ao atingir o jogador
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

            Iterator<Obolo> oboloIterator = obolos.iterator();
            while (oboloIterator.hasNext()) {
                Obolo obolo = oboloIterator.next();
                if (new Vector2(jogador.x, jogador.y).dst(obolo.x, obolo.y) < 24) { // Raio de coleta maior
                    obolosColetadosNaRun += obolo.valor;
                    System.out.println("Óbolos coletados na partida: " + obolosColetadosNaRun); // Para teste
                    oboloIterator.remove();
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

            Iterator<Inimigo> inimigoIteratorMorte = inimigos.iterator();
            while (inimigoIteratorMorte.hasNext()) {
                Inimigo i = inimigoIteratorMorte.next();

                if (i.estaMorto()) {
                    // Se o inimigo estiver morto, processa a morte
                    orbes.add(new OrbeXP(i.x, i.y, texturaOrbeXP));

                    // Chance de 15% de dropar um Óbolo
                    if (randomParaDrops.nextFloat() < 0.15f) {
                        obolos.add(new Obolo(i.x, i.y, texturaObolo));
                    }

                    jogador.curar(jogador.getCuraPorAbate());
                    inimigoIteratorMorte.remove(); // Remove o inimigo da lista com segurança
                }
            }

            // VERIFICA SE O JOGADOR MORREU
            if (jogador.estaMorto()) {
                if (jogador.possuiRessurreicao()) {
                    jogador.usarRessurreicao();
                } else {
                    // Passa o total de óbolos coletados para a próxima tela
                    game.setScreen(new GameOverScreen(game, this.obolosColetadosNaRun));
                    // Não precisa chamar dispose() aqui, pois o 'game' gerencia a tela atual
                    return;
                }
            }

            gameTimer += delta;

            // Lógica para Spawna o portal
            if (portal == null && gameTimer >= bossPortalSpawnTime) {
                // Spawna o portal um pouco à direita do jogador
                portal = new Portal(jogador.x + 100, jogador.y, texturaPortal);
                System.out.println("O portal para a câmara do chefe apareceu!");
            }

            // Lógica de interação com o portal
            if (portal != null) {
                if (portal.getBounds().overlaps(jogador.getSolidArea())) {
                    System.out.println("Entrando no portal...");
                    // Passamos o 'game' e o 'jogador' para que a próxima tela tenha os dados atuais
                    game.setScreen(new BossScreen(game, jogador, spritesheet));
                    return; // Importante para parar a execução desta tela
                }
            }

            // --- LÓGICA DOS RAIOS ---
            Iterator<Raio> raioIterator = raios.iterator();
            while (raioIterator.hasNext()) {
                Raio r = raioIterator.next();
                r.update(delta); // Atualiza o timer de duração do raio

                // Aplica o dano em área UMA VEZ, quando o raio é criado
                if (!r.danoJaAplicado) {
                    for (Inimigo inimigo : inimigos) {
                        if (new Vector2(r.x, r.y).dst2(inimigo.x, inimigo.y) < r.raioDoDano * r.raioDoDano) {
                            inimigo.sofrerDano(r.dano);
                            // Opcional: Adicionar um DamageNumber para o dano do raio
                            damageNumbers.add(new DamageNumber(String.valueOf((int)r.dano), inimigo.x + 8, inimigo.y + 16, Color.YELLOW));
                        }
                    }
                    r.danoJaAplicado = true;
                }

                if (r.deveSerRemovido) {
                    raioIterator.remove();
                }
            }
        }
    }

    private void desenharMundo() {
        // --- 1. DESENHA TODOS OS SPRITES DO MUNDO ---
        game.batch.setProjectionMatrix(camera.combined); // Usa a câmera do mundo
        game.batch.begin();
        tileMap.draw(game.batch);
        jogador.draw(game.batch);
        for (Inimigo inimigo : inimigos) {
            inimigo.draw(game.batch);
        }

        for (Projetil projetil : projeteis) {
            projetil.draw(game.batch);
        }

        for (ProjetilInimigo projetil : projeteisInimigos) {
            projetil.draw(game.batch);
        }

        for (OrbeXP orbe : orbes) {
            orbe.draw(game.batch);
        }

        for (Obolo obolo : obolos) {
            obolo.draw(game.batch);
        }

        for (DamageNumber dn : damageNumbers) {
            dn.draw(game.batch, game.font); // Passa a fonte para o método de desenho
        }

        if (portal != null) {
            portal.draw(game.batch);
        }

        game.batch.end();
    }

    public void invocarRaio() {
        // Se não houver inimigos na tela, não faz nada
        if (inimigos.isEmpty()) {
            return;
        }

        // Escolhe um inimigo aleatório para ser o alvo
        Random random = new Random();
        Inimigo alvo = inimigos.get(random.nextInt(inimigos.size()));

        // Cria um novo raio na posição do inimigo alvo
        raios.add(new Raio(alvo.x, alvo.y));
    }

    public void criarProjetilInimigo(float origemX, float origemY, float alvoX, float alvoY) {
        // A lógica é a mesma do projétil do jogador: calcula a direção e cria o objeto.
        Vector2 direcao = new Vector2(alvoX - origemX, alvoY - origemY).nor();
        // Você pode querer uma textura diferente para o projétil do inimigo no futuro.
        // Por enquanto, podemos usar a mesma do jogador.
        projeteisInimigos.add(new ProjetilInimigo(origemX, origemY, direcao, texturaProjetil, 10f)); // Dano fixo de 10
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
        game.hud.draw(jogador);

        // Se estivermos no estado de level up, desenha a UI de melhorias por cima
        if (estadoAtual == GameState.LEVEL_UP) {
            desenharUiLevelUp();
        }
    }

    @Override
    public void resize(int width, int height) {
        if (viewport != null) {
            viewport.update(width, height, true); // centraliza a câmera
        }
        // Atualiza também a HUD (já tem HUD.resize)
        if (game.hud != null) {
            game.hud.resize(width, height);
        }
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

    public void criarProjetil(Jogador atirador, float x, float y, OrthographicCamera camera) {
        Vector3 mousePosTela = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        Vector3 mousePosMundo = camera.unproject(mousePosTela);
        Vector2 direcao = new Vector2(mousePosMundo.x - x, mousePosMundo.y - y).nor();

        // Pega o dano atual do jogador
        float danoFinal = atirador.getDano();

        // Passa o dano final para o novo projétil
        projeteis.add(new Projetil(x, y, direcao, texturaProjetil, danoFinal));
    }


    @Override
    public void dispose() {
        tileMap.dispose();
        jogador.dispose();
        spritesheet.dispose();
        game.hud.dispose();
    }
}
