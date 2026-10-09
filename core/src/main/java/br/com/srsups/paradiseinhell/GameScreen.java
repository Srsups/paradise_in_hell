package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GameScreen extends ScreenAdapter {
    private enum State { PLAYING, LEVEL_UP, PAUSED }
    private final Main game;
    private State state = State.PLAYING, beforePause = State.PLAYING;
    private final RunSession session;
    private final PlayerState stats;
    private final RunProgress progress;
    private final FitViewport world = new FitViewport(480, 270);
    private final FitViewport hud = new FitViewport(960, 540);
    private final List<Inimigo> enemies = new ArrayList<>();
    private final List<Projetil> projectiles = new ArrayList<>();
    private final List<OrbeXP> orbs = new ArrayList<>();
    private final List<DamageNumber> damageNumbers = new ArrayList<>();
    private final RunChests chests = new RunChests();
    private String chestFeedback = "";
    private float chestFeedbackTimer;
    private final List<Upgrade> choices = new ArrayList<>();
    private final Rectangle[] cards = {
        new Rectangle(35, 180, 280, 160),
        new Rectangle(340, 180, 280, 160),
        new Rectangle(645, 180, 280, 160)
    };
    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private BitmapFont font;
    private Texture atlas;
    private TileMap map;
    private Jogador player;
    private float spawnTimer, slashTimer, slashAngle, slashX, slashY;
    private boolean leavingArena;
    private MiniBossEncounter miniBoss;
    private float miniHitCooldown, miniTime, encounterFeedbackTimer;
    private String encounterFeedback = "";
    private EnemyNavigation navigation;
    private final EnemyWaves waves = new EnemyWaves();
    private final java.util.Random boonRandom = new java.util.Random();
    private boolean disposed;
    public GameScreen(Main game) { this(game, RunSession.demo()); }
    public GameScreen(Main game, RunSession session) {
        this.game = game;
        this.session = session;
        game.profiles().prepareRun(session);
        this.stats = session.player();
        this.progress = session.progress();
    }

    @Override public void show() {
        Gdx.input.setInputProcessor(null);
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        font = new BitmapFont();
        atlas = new Texture(Art.ATLAS);
        atlas.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        map = new TileMap(atlas);
        player = new Jogador(100, 100, atlas, stats, this::attack);
        map.gerarAreaInicialSegura(player.x, player.y, 5);
        followPlayer();
        navigation = new EnemyNavigation(map::ehSolido);
        navigation.update(0, player.x + 8, player.y + 8);
    }
    @Override public void resize(int width, int height) {
        world.update(width, height);
        hud.update(width, height, true);
    }
    private void followPlayer() {
        world.getCamera().position.set(player.x + 8, player.y + 8, 0);
        world.getCamera().update();
        map.update(world.getCamera());
    }
    @Override public void render(float delta) {
        if (disposed) return;
        // Avoid large simulation jumps after dragging the window or a debugger stop.
        delta = Math.min(delta, 0.05f);
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (state == State.PAUSED) state = beforePause;
            else { beforePause = state; state = State.PAUSED; }
        }
        switch (state) {
            case PLAYING -> {
                if (!session.isDemo() && session.arenaComplete()
                        && (Gdx.input.isKeyJustPressed(Input.Keys.E) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER))) prepareExit();
                else updatePlaying(delta);
            }
            case LEVEL_UP -> updateChoices();
            case PAUSED -> {
                if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
                    game.finishRun(session, RunOutcome.ABANDONED);
                }
            }
        }
        // A transition can release this screen during update.
        if (game.getScreen() != this) return;
        draw();
    }
    private void updatePlaying(float delta) {
        session.advance(delta);
        miniTime += delta; miniHitCooldown = Math.max(0, miniHitCooldown - delta);
        encounterFeedbackTimer = Math.max(0, encounterFeedbackTimer - delta);
        slashTimer = Math.max(0, slashTimer - delta);
        player.update(delta, map);
        followPlayer();
        navigation.update(delta, player.x + 8, player.y + 8);
        for (Inimigo enemy : enemies) enemy.update(delta, navigation);
        for (Projetil projectile : projectiles) projectile.update(delta, map);

        for (Projetil projectile : projectiles) {
            if (projectile.deveSerRemovido) continue;
            for (Inimigo enemy : enemies) {
                if (!enemy.estaMorto() && projectile.canHit(enemy) && overlaps(projectile.x - 3, projectile.y - 3, 6, enemy.x, enemy.y, 16)) {
                    enemy.sofrerDano(projectile.dano);
                    damageNumbers.add(new DamageNumber(CombatNumbers.format(projectile.dano), enemy.x + 8, enemy.y + 16));
                    projectile.hit(enemy);
                    redirectRicochet(projectile);
                    break;
                }
            }
            if (!projectile.deveSerRemovido && miniBoss != null && !miniBoss.defeated() && projectile.canHit(miniBoss)
                && overlaps(projectile.x - 3, projectile.y - 3, 6, miniBoss.x - 4, miniBoss.y - 4, 24)) {
                miniBoss.hurt(projectile.dano);
                damageNumbers.add(new DamageNumber(CombatNumbers.format(projectile.dano), miniBoss.centerX(), miniBoss.centerY() + 22));
                projectile.hit(miniBoss); redirectRicochet(projectile);
            }
        }
        projectiles.removeIf(projectile -> projectile.deveSerRemovido);
        if (stats.possuiAura()) {
            for (Inimigo enemy : enemies) {
                if (Vector2.dst2(player.x, player.y, enemy.x, enemy.y) < stats.getRaioAura() * stats.getRaioAura()) {
                    enemy.sofrerDano(stats.getDanoAura() * delta);
                }
            }
        }
        if (miniBoss != null && stats.possuiAura()) {
            float radius = stats.getRaioAura() + MiniBossEncounter.BODY_RADIUS;
            if (Vector2.dst2(player.x + 8, player.y + 8, miniBoss.centerX(), miniBoss.centerY()) <= radius * radius)
                miniBoss.hurt(stats.getDanoAura() * delta);
        }
        if (miniBoss != null && miniBoss.defeated()) {
            orbs.add(new OrbeXP(miniBoss.x, miniBoss.y, Art.cell(atlas, 1, 3), MiniBossEncounter.XP));
            boolean dropped = session.recordMiniBossDefeat();
            if (dropped) dropped = game.profiles().collectForgeMaterials(session) > 0;
            encounterFeedback = dropped ? "Fragmento de Forja obtido e guardado no save!"
                : "Mini-boss derrotado: +20 XP.";
            encounterFeedbackTimer = 4;
            stats.curar(stats.getCuraPorAbate()); miniBoss = null;
        }
        Iterator<Inimigo> enemyIterator = enemies.iterator();
        while (enemyIterator.hasNext()) {
            Inimigo enemy = enemyIterator.next();
            if (enemy.estaMorto()) {
                orbs.add(new OrbeXP(enemy.x, enemy.y, Art.cell(atlas, 1, 3), enemy.getXpDrop()));
                session.recordKill(enemy.getType());
                stats.curar(stats.getCuraPorAbate());
                enemyIterator.remove();
            } else if (Vector2.dst2(player.x, player.y, enemy.x, enemy.y) > 900 * 900) {
                enemyIterator.remove();
            }
        }
        // Resolve defeated enemies first, then contact damage, so healing cannot undo a death.
        for (Inimigo enemy : enemies) {
            if (overlaps(player.x, player.y, 16, enemy.x, enemy.y, 16) && enemy.podeAtacar()) {
                stats.sofrerDano(enemy.getDano());
                enemy.resetarCooldownAtaque();
            }
        }
        if (miniBoss != null) {
            float miniDamage = miniBoss.update(delta, player.x + 8, player.y + 8, navigation);
            if (miniDamage > 0 && miniHitCooldown == 0) { stats.sofrerDano(miniDamage); miniHitCooldown = 0.35f; }
        }
        if (stats.estaMorto()) {
            if (stats.possuiRessurreicao()) stats.usarRessurreicao();
            else { finish(false); return; }
        }
        if (progress.won()) { finish(true); return; }

        Iterator<OrbeXP> orbIterator = orbs.iterator();
        while (orbIterator.hasNext()) {
            OrbeXP orb = orbIterator.next();
            XpAttraction.Motion motion = XpAttraction.pull(player.x + 8, player.y + 8, orb.x + 3, orb.y + 3,
                delta, stats.getRaioAtracaoXp(), stats.getVelocidadeAtracaoXp());
            orb.x = motion.x() - 3; orb.y = motion.y() - 3;
            if (motion.collected()) {
                stats.ganharXP(orb.valorXP);
                orbIterator.remove();
            } else if (Vector2.dst2(player.x + 8, player.y + 8, orb.x + 3, orb.y + 3) > 900 * 900) orbIterator.remove();
        }
        for (DamageNumber number : damageNumbers) number.update(delta);
        damageNumbers.removeIf(DamageNumber::deveSerRemovido);

        spawnTimer += delta;
        while (spawnTimer >= session.spawnInterval()) {
            spawnTimer -= session.spawnInterval();
            for (EnemyType type : waves.next(session.waveSize(), session.difficultyTime(), !session.isDemo())) {
                // Bound simultaneous simulation work, never total enemies generated for the phase.
                if (enemies.size() >= 160) break;
                Inimigo enemy = EnemySpawner.spawnInimigoForaDaTela((OrthographicCamera) world.getCamera(), atlas, map, type, navigation);
                if (enemy != null) { enemy.applyDifficulty(session.enemyDifficulty()); enemies.add(enemy); }
            }
        }
        spawnMiniBoss();
        updateChests(delta);
        if (stats.podeSubirDeNivel()) iniciarLevelUp();
    }
    private void spawnMiniBoss() {
        if (!session.miniBossDue()) return;
        for (int attempt = 0; attempt < 16; attempt++) {
            float angle = attempt * (float) Math.PI / 8;
            float x = player.x + (float) Math.cos(angle) * 135, y = player.y + (float) Math.sin(angle) * 100;
            if (!navigation.canOccupy(x + 8, y + 8) || !navigation.reachable(x + 8, y + 8)) continue;
            miniBoss = new MiniBossEncounter(x, y, session.enemyDifficulty());
            session.markMiniBossSpawned(); return;
        }
    }
    private void redirectRicochet(Projetil projectile) {
        if (!projectile.needsRicochet()) return;
        float bestDistance = 100 * 100, targetX = 0, targetY = 0;
        boolean found = false;
        for (Inimigo enemy : enemies) {
            float distance = Vector2.dst2(projectile.x, projectile.y, enemy.x + 8, enemy.y + 8);
            if (!enemy.estaMorto() && projectile.canHit(enemy) && distance < bestDistance
                && navigation.clearLine(projectile.x, projectile.y, enemy.x + 8, enemy.y + 8)) {
                bestDistance = distance; targetX = enemy.x + 8; targetY = enemy.y + 8; found = true;
            }
        }
        if (miniBoss != null && !miniBoss.defeated() && projectile.canHit(miniBoss)
            && Vector2.dst2(projectile.x, projectile.y, miniBoss.centerX(), miniBoss.centerY()) < bestDistance
            && navigation.clearLine(projectile.x, projectile.y, miniBoss.centerX(), miniBoss.centerY())) {
            targetX = miniBoss.centerX(); targetY = miniBoss.centerY(); found = true;
        }
        if (found) projectile.redirect(targetX, targetY); else projectile.deveSerRemovido = true;
    }
    private void updateChests(float delta) {
        chestFeedbackTimer = Math.max(0, chestFeedbackTimer - delta);
        chests.update(delta, player.x + 8, player.y + 8, navigation, !session.isDemo());
        RunChests.Chest nearby = chests.nearby(player.x + 8, player.y + 8);
        if (nearby != null && Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            chestFeedback = chests.open(nearby, session); chestFeedbackTimer = 3;
        }
    }
    private static boolean overlaps(float x, float y, float size, float otherX, float otherY, float otherSize) {
        return x < otherX + otherSize && x + size > otherX && y < otherY + otherSize && y + size > otherY;
    }
    private void finish(boolean victory) {
        game.finishRun(session, victory ? RunOutcome.VICTORY : RunOutcome.DEATH);
    }
    public void iniciarLevelUp() {
        if (!stats.podeSubirDeNivel()) return;
        stats.subirDeNivel();
        state = State.LEVEL_UP;
        choices.clear();
        choices.addAll(UpgradeChoices.draw(session, 3, boonRandom));
    }
    private void updateChoices() {
        Vector2 pointer = hud.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        for (int index = 0; index < choices.size(); index++) {
            boolean key = Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + index)
                || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1 + index);
            boolean click = Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && cards[index].contains(pointer);
            if (key || click) {
                Upgrade upgrade = choices.get(index);
                session.applyUpgrade(upgrade);
                state = State.PLAYING;
                // All earned levels keep their own selection, even after collecting many orbs.
                if (stats.podeSubirDeNivel()) iniciarLevelUp();
                else if (leavingArena) game.offerRoute(session);
                return;
            }
        }
    }
    private void prepareExit() {
        for (OrbeXP orb : orbs) stats.ganharXP(orb.valorXP);
        orbs.clear();
        enemies.clear();
        miniBoss = null;
        projectiles.clear();
        chests.clear();
        leavingArena = true;
        if (stats.podeSubirDeNivel()) iniciarLevelUp();
        else game.offerRoute(session);
    }
    private void attack(float x, float y) {
        Vector2 direction = world.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY())).sub(x, y);
        if (direction.isZero()) return;
        WeaponType weapon = stats.getWeapon();
        if (weapon.isMelee()) {
            slashX = x; slashY = y; slashAngle = direction.angleDeg(); slashTimer = weapon == WeaponType.FISTS ? 0.10f : 0.14f;
            if (weapon == WeaponType.FISTS) { punch(x, y, direction); return; }
            for (Inimigo enemy : enemies) {
                if (!enemy.estaMorto() && WeaponAttack.swordHits(x, y, direction.x, direction.y, enemy.x + 8, enemy.y + 8, stats.swordReach())
                        && navigation.clearLine(x, y, enemy.x + 8, enemy.y + 8)) {
                    enemy.sofrerDano(stats.getDano());
                    damageNumbers.add(new DamageNumber(CombatNumbers.format(stats.getDano()), enemy.x + 8, enemy.y + 16));
                }
            }
            if (miniBoss != null && !miniBoss.defeated()
                && WeaponAttack.swordHits(x, y, direction.x, direction.y, miniBoss.centerX(), miniBoss.centerY(), stats.swordReach() + MiniBossEncounter.BODY_RADIUS)
                && navigation.clearLine(x, y, miniBoss.centerX(), miniBoss.centerY())) {
                miniBoss.hurt(stats.getDano());
                damageNumbers.add(new DamageNumber(CombatNumbers.format(stats.getDano()), miniBoss.centerX(), miniBoss.centerY() + 22));
            }
        } else {
            Projetil projectile = new Projetil(x, y, direction.nor(), Art.cell(atlas, 0, 3), weapon);
            projectile.configure(stats);
            projectiles.add(projectile);
        }
    }
    private void punch(float x, float y, Vector2 direction) {
        Inimigo target = null; float closest = Float.POSITIVE_INFINITY;
        for (Inimigo enemy : enemies) {
            float distance = Vector2.dst2(x, y, enemy.x + 8, enemy.y + 8);
            if (!enemy.estaMorto() && distance < closest
                && WeaponAttack.meleeHits(x, y, direction.x, direction.y, enemy.x + 8, enemy.y + 8, stats.meleeReach(), 40)
                && navigation.clearLine(x, y, enemy.x + 8, enemy.y + 8)) { target = enemy; closest = distance; }
        }
        if (miniBoss != null && !miniBoss.defeated()
            && Vector2.dst2(x, y, miniBoss.centerX(), miniBoss.centerY()) < closest
            && WeaponAttack.meleeHits(x, y, direction.x, direction.y, miniBoss.centerX(), miniBoss.centerY(),
                stats.meleeReach() + MiniBossEncounter.BODY_RADIUS, 40)
            && navigation.clearLine(x, y, miniBoss.centerX(), miniBoss.centerY())) {
            miniBoss.hurt(stats.getDano());
            damageNumbers.add(new DamageNumber(CombatNumbers.format(stats.getDano()), miniBoss.centerX(), miniBoss.centerY() + 22));
        } else if (target != null) {
            target.sofrerDano(stats.getDano());
            damageNumbers.add(new DamageNumber(CombatNumbers.format(stats.getDano()), target.x + 8, target.y + 16));
        }
    }
    private void draw() {
        ScreenUtils.clear(0.045f, 0.055f, 0.085f, 1);
        world.apply();
        batch.setProjectionMatrix(world.getCamera().combined);
        batch.begin();
        map.draw(batch);
        batch.end();
        drawChests();
        drawMiniBoss();
        batch.begin();
        for (OrbeXP orb : orbs) orb.draw(batch);
        player.draw(batch);
        for (Inimigo enemy : enemies) enemy.draw(batch);
        for (Projetil projectile : projectiles) projectile.draw(batch);
        font.getData().setScale(0.65f);
        for (DamageNumber number : damageNumbers) number.draw(batch, font);
        batch.end();
        shapes.setProjectionMatrix(world.getCamera().combined);
        shapes.begin(ShapeRenderer.ShapeType.Line);
        if (stats.possuiImaXp()) {
            shapes.setColor(0.4f, 0.8f, 0.7f, 1); shapes.circle(player.x + 8, player.y + 8, stats.getRaioAtracaoXp(), 48);
        }
        if (stats.possuiAura()) {
            shapes.setColor(Color.CYAN);
            shapes.circle(player.x + 8, player.y + 8, stats.getRaioAura());
        }
        if (stats.isEscudoAtivo()) {
            shapes.setColor(Color.GOLD);
            shapes.circle(player.x + 8, player.y + 8, 12);
        }
        if (slashTimer > 0) {
            shapes.setColor(Color.GOLD);
            if (stats.getWeapon() == WeaponType.FISTS) {
                float angle = (float) Math.toRadians(slashAngle);
                shapes.circle(slashX + (float) Math.cos(angle) * stats.meleeReach(),
                    slashY + (float) Math.sin(angle) * stats.meleeReach(), 5, 12);
            } else shapes.arc(slashX, slashY, stats.swordReach(),
                slashAngle - WeaponAttack.HALF_ANGLE_DEGREES, WeaponAttack.HALF_ANGLE_DEGREES * 2, 18);
        }
        shapes.end();
        hud.apply();
        shapes.setProjectionMatrix(hud.getCamera().combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.045f, 0.055f, 0.085f, 1);
        shapes.rect(0, 458, 960, 82);
        bar(18, 510, stats.getVida() / stats.getVidaMaxima(), Color.SCARLET);
        bar(18, 485, stats.getEstaminaAtual() / stats.getEstaminaMaxima(), stats.isExausto() ? Color.SALMON : Color.GREEN);
        shapes.end();
        batch.setProjectionMatrix(hud.getCamera().combined);
        batch.begin();
        font.getData().setScale(1);
        font.setColor(Color.WHITE);
        font.draw(batch, "Vida: " + CombatNumbers.format(Math.max(0, stats.getVida())) + " / " + CombatNumbers.format(stats.getVidaMaxima()), 240, 523);
        font.draw(batch, "Nível " + stats.getNivel() + "   XP " + (int) stats.getXpAtual() + "/" + stats.getXpParaProximoNivel(), 240, 495);
        font.draw(batch, RunProgress.formatTime(progress.elapsed()) + (session.isDemo() ? " / 05:00" : "") + "   |   Abates: " + progress.kills(), 530, 523);
        font.draw(batch, session.isDemo() ? "Horda " + progress.phase() + "   |   ESC: pausa"
            : session.arena().title(), 530, 495);
        if (!session.isDemo()) {
            StringBuilder objective = new StringBuilder();
            for (EnemyType type : EnemyType.values()) {
                Integer goal = session.arena().killGoals().get(type);
                if (goal == null) continue;
                if (!objective.isEmpty()) objective.append("   |   ");
                objective.append(type.label).append(": ").append(Math.min(goal, session.kills(type))).append("/").append(goal);
            }
            font.draw(batch, objective.toString(), 18, 445);
            font.setColor(session.arenaComplete() ? Color.GOLD : Color.LIGHT_GRAY);
            font.draw(batch, session.arenaComplete() ? "PASSAGEM LIBERADA | E ou ENTER: escolher destino"
                : "Cumpra todas as metas para liberar a passagem.", 18, 423);
            font.setColor(Color.WHITE);
        }
        if (!session.isDemo()) {
            font.setColor(Color.GOLD); font.draw(batch, "Óbolos ao retornar: " + session.earnedObols() + "   |   Fragmentos guardados: " + game.profiles().profile().forgeMaterials()
                + (game.profiles().profile().canClaimForgeMaterial(session.region().id()) ? "" : " (região já obtida)"), 18, 399);
        }
        RunChests.Chest nearbyChest = chests.nearby(player.x + 8, player.y + 8);
        font.setColor(Color.GOLD);
        if (nearbyChest != null) font.draw(batch, "F: abrir baú", 650, 423);
        if (chestFeedbackTimer > 0) font.draw(batch, chestFeedback, 18, 376);
        if (encounterFeedbackTimer > 0) font.draw(batch, encounterFeedback, 18, 352);
        if (miniBoss != null && Vector2.dst2(player.x, player.y, miniBoss.x, miniBoss.y) < 200 * 200) {
            font.setColor(Color.ORANGE); font.draw(batch, MiniBossEncounter.NAME + "  " + CombatNumbers.format(miniBoss.health())
                + "/" + CombatNumbers.format(miniBoss.maxHealth()) + "  |  "
                + (miniBoss.state() == MiniBossEncounter.State.TELEGRAPH ? "AVISO: " : "")
                + (miniBoss.attack() == MiniBossEncounter.Attack.SPIN ? "Giro de espada" : "Rajada de pedras"), 18, 328);
        }
        if (!session.isDemo() && !game.profiles().error().isEmpty()) {
            font.setColor(Color.SALMON); font.draw(batch, game.profiles().error(), 18, 57);
        }
        font.setColor(Color.WHITE);
        font.draw(batch, stats.getProfession().title + " | " + stats.getWeapon().label + " (" + stats.getWeaponRarity().label + ")   |   Sorte: " + CombatNumbers.format(stats.getLuckMultiplier()) + "x   |   F: abrir baú próximo", 18, 24);
        batch.end();
        if (state != State.PLAYING) drawOverlay();
    }
    private void drawMiniBoss() {
        if (miniBoss == null) return;
        shapes.setProjectionMatrix(world.getCamera().combined);
        shapes.begin(ShapeRenderer.ShapeType.Line);
        boolean warning = miniBoss.state() == MiniBossEncounter.State.TELEGRAPH;
        shapes.setColor(warning ? Color.ORANGE : Color.SCARLET);
        if ((warning || miniBoss.state() == MiniBossEncounter.State.ACTIVE) && miniBoss.attack() == MiniBossEncounter.Attack.SPIN) {
            shapes.circle(miniBoss.centerX(), miniBoss.centerY(), MiniBossEncounter.SPIN_RADIUS, 36);
            if (!warning) {
                float angle = miniTime * 18;
                shapes.line(miniBoss.centerX(), miniBoss.centerY(),
                    miniBoss.centerX() + (float) Math.cos(angle) * MiniBossEncounter.SPIN_RADIUS,
                    miniBoss.centerY() + (float) Math.sin(angle) * MiniBossEncounter.SPIN_RADIUS);
            }
        }
        if (warning && miniBoss.attack() == MiniBossEncounter.Attack.STONES) {
            shapes.arc(miniBoss.centerX(), miniBoss.centerY(), 62, (float) Math.toDegrees(miniBoss.aimAngle()) - 38, 76, 24);
        }
        shapes.end();
        shapes.begin(ShapeRenderer.ShapeType.Filled); shapes.setColor(Color.LIGHT_GRAY);
        for (MiniBossEncounter.Stone stone : miniBoss.stones()) shapes.circle(stone.x, stone.y, 3, 8);
        shapes.end();
        batch.begin(); batch.setColor(miniBoss.flashing() ? Color.WHITE : Color.ORANGE);
        batch.draw(Art.cell(atlas, (int) (miniTime * 4) % 2 + 2, 1), miniBoss.centerX() - 16, miniBoss.centerY() - 16, 32, 32);
        batch.setColor(Color.WHITE); batch.end();
    }
    private void drawChests() {
        shapes.setProjectionMatrix(world.getCamera().combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (RunChests.Chest chest : chests.chests()) {
            shapes.setColor(0.15f, 0.09f, 0.04f, 1); shapes.rect(chest.x() - 10, chest.y() - 7, 20, 14);
            shapes.setColor(0.58f, 0.32f, 0.1f, 1); shapes.rect(chest.x() - 9, chest.y() - 6, 18, 12);
            shapes.setColor(Color.GOLD); shapes.rect(chest.x() - 8, chest.y(), 16, 2); shapes.rect(chest.x() - 2, chest.y() - 3, 4, 6);
        }
        shapes.end();
        RunChests.Chest nearby = chests.nearby(player.x + 8, player.y + 8);
        if (nearby != null) {
            shapes.begin(ShapeRenderer.ShapeType.Line); shapes.setColor(Color.GOLD);
            shapes.circle(nearby.x(), nearby.y(), 14, 20); shapes.end();
        }
    }
    private void bar(float x, float y, float fraction, Color color) {
        shapes.setColor(Color.DARK_GRAY);
        shapes.rect(x, y, 200, 15);
        shapes.setColor(color);
        shapes.rect(x, y, 200 * Math.max(0, Math.min(1, fraction)), 15);
    }
    private void drawOverlay() {
        Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0, 0, 0, 0.85f);
        shapes.rect(0, 0, 960, 458);
        if (state == State.LEVEL_UP) {
            shapes.setColor(0.13f, 0.16f, 0.23f, 1);
            for (int index = 0; index < choices.size(); index++) {
                Rectangle card = cards[index];
                shapes.rect(card.x, card.y, card.width, card.height);
            }
        }
        shapes.end();
        Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        batch.begin();
        font.getData().setScale(1.5f);
        font.setColor(Color.GOLD);
        font.draw(batch, state == State.PAUSED ? "PAUSA" : "ESCOLHA UMA DÁDIVA", 300, 405);
        font.getData().setScale(1);
        font.setColor(Color.WHITE);
        if (state == State.PAUSED) {
            font.draw(batch, "ESC: continuar   |   M: voltar ao menu", 280, 270);
        } else {
            for (int index = 0; index < choices.size(); index++) {
                Upgrade upgrade = choices.get(index);
                Rectangle card = cards[index];
                font.setColor(upgrade.rarity.red, upgrade.rarity.green, upgrade.rarity.blue, 1);
                font.draw(batch, "[" + (index + 1) + "] " + upgrade.title, card.x + 12, card.y + 130, card.width - 24, 8, true);
                font.draw(batch, upgrade.rarity.label, card.x + 12, card.y + 100);
                font.setColor(Color.WHITE);
                font.draw(batch, upgrade.description, card.x + 12, card.y + 80, card.width - 24, 8, true);
                font.draw(batch, "Obtida " + session.rank(upgrade) + " vez(es)", card.x + 12, card.y + 25);
            }
            font.draw(batch, "Clique em uma opção ou use 1, 2, 3.", 305, 135);
        }
        batch.end();
    }
    @Override public void pause() {
        if (state != State.PAUSED) { beforePause = state; state = State.PAUSED; }
    }
    @Override public void dispose() {
        if (disposed) return;
        disposed = true;
        batch.dispose();
        shapes.dispose();
        atlas.dispose();
        font.dispose();
        map.dispose();
        enemies.clear();
        miniBoss = null;
        projectiles.clear();
        orbs.clear();
        chests.clear();
        damageNumbers.clear();
    }
}
