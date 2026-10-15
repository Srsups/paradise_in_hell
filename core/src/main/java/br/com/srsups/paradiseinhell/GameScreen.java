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
    private final BoonPanel boonPanel;
    private final AudioPanel audioPanel;
    private final CombatFeedback feedback;
    private final ChoiceInputGuard choiceGuard = new ChoiceInputGuard();
    private SpriteHitbox[] miniHitboxes;
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
    private Vector2 exitBeacon;
    private float exitNoticeTimer;
    private float miniHitCooldown, miniTime, encounterFeedbackTimer;
    private String encounterFeedback = "";
    private EnemyNavigation navigation;
    private final CombatEffects effects;
    private final List<CombatTarget> targets = new ArrayList<>();
    private final java.util.Random combatRandom = new java.util.Random();
    private final EnemyWaves waves = new EnemyWaves();
    private final java.util.Random boonRandom = new java.util.Random();
    private boolean disposed;
    public GameScreen(Main game) { this(game, RunSession.demo()); }
    public GameScreen(Main game, RunSession session) {
        this.game = game;
        this.session = session;
        game.profiles().prepareRun(session);
        this.stats = session.player();
        audioPanel=new AudioPanel(game);feedback=new CombatFeedback(game.audio());
        effects=new CombatEffects(session.statistics());
        this.boonPanel = new BoonPanel(session);
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
        miniHitboxes = new SpriteHitbox[]{Art.hitbox(Art.cell(atlas, 2, 1)), Art.hitbox(Art.cell(atlas, 3, 1))};
        player = new Jogador(100, 100, atlas, stats, this::attack);
        map.gerarAreaInicialSegura(player.x, player.y, 5);
        followPlayer();
        navigation = new EnemyNavigation(map::ehSolido);
        navigation.update(0, player.x + 8, player.y + 8);
        session.necromancy().enterRoom(player.x + 8, player.y + 8);
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
        boolean audioWasOpen=audioPanel.open();
        if(audioWasOpen)audioPanel.input(hud.unproject(new Vector2(Gdx.input.getX(),Gdx.input.getY())));
        else if(!boonPanel.open() && Gdx.input.isKeyJustPressed(Input.Keys.O))audioPanel.toggle();
        if(audioWasOpen || audioPanel.open()){draw();return;}
        float uiDelta = delta;
        boolean panelWasOpen = boonPanel.open();
        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) boonPanel.toggle();
        else if (boonPanel.open() && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) boonPanel.close();
        if (boonPanel.open()) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) boonPanel.movePage(-1);
            if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) boonPanel.movePage(1);
        }
        if (panelWasOpen || boonPanel.open()) { draw(); return; }
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
            case LEVEL_UP -> updateChoices(uiDelta);
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
        boolean exitWasOpen=session.arenaComplete();
        feedback.advance(delta);
        miniTime += delta; miniHitCooldown = Math.max(0, miniHitCooldown - delta);
        encounterFeedbackTimer = Math.max(0, encounterFeedbackTimer - delta);
        exitNoticeTimer=Math.max(0,exitNoticeTimer-delta);
        slashTimer = Math.max(0, slashTimer - delta);
        refreshTargets();
        effects.update(delta);
        Vector2 guardAim = world.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        stats.setGuard(Gdx.input.isButtonPressed(Input.Buttons.RIGHT), guardAim.x - player.x - 8, guardAim.y - player.y - 8);
        boolean wasDashing=stats.isDashing();float beforeX=player.x+8,beforeY=player.y+8;
        player.update(delta, map);
        feedback.dash(stats.getWeapon(),beforeX,beforeY,player.x+8,player.y+8,!wasDashing && stats.isDashing(),stats.isDashing());
        followPlayer();
        navigation.update(delta, player.x + 8, player.y + 8);
        for (Inimigo enemy : enemies) {
            boolean aiming=enemy.isArcher() && enemy.archer().phase()==ArcherAttack.Phase.AIMING;
            enemy.update(delta,navigation,archerVisible(enemy));
            if(enemy.isArcher() && !aiming && enemy.archer().phase()==ArcherAttack.Phase.AIMING)game.audio().play(SoundCue.ARCHER_WARNING);
        }
        for (Projetil projectile : projectiles) {
            projectile.update(delta,map);
            if(!projectile.deveSerRemovido)feedback.trail(stats.getWeapon(),projectile.x,projectile.y,projectile.beforeX,projectile.beforeY);
        }

        for (Projetil projectile : projectiles) {
            if (projectile.deveSerRemovido) continue;
            for (Inimigo enemy : enemies) {
                if (!enemy.estaMorto() && projectile.canHit(enemy) && enemy.hitbox().sweptHits(projectile.beforeX, projectile.beforeY, projectile.x, projectile.y, projectile.hitRadius())) {
                    weaponHit(enemy, projectile.dano);
                    damageNumbers.add(new DamageNumber(CombatNumbers.format(projectile.dano), enemy.x + 8, enemy.y + 16));
                    projectile.hit(enemy);
                    redirectRicochet(projectile);
                    break;
                }
            }
            if (!projectile.deveSerRemovido && miniBoss != null && !miniBoss.defeated() && projectile.canHit(miniBoss)
                && miniHitbox().sweptHits(projectile.beforeX, projectile.beforeY, projectile.x, projectile.y, projectile.hitRadius())) {
                weaponHit(miniBoss, projectile.dano);
                damageNumbers.add(new DamageNumber(CombatNumbers.format(projectile.dano), miniBoss.centerX(), miniBoss.centerY() + 22));
                projectile.hit(miniBoss); redirectRicochet(projectile);
            }
        }
        projectiles.removeIf(projectile -> projectile.deveSerRemovido);
        if (stats.possuiAura()) {
            for (Inimigo enemy : enemies) {
                if (Vector2.dst2(player.x, player.y, enemy.x, enemy.y) < stats.getRaioAura() * stats.getRaioAura()) {
                    session.statistics().deal(enemy,stats.getDanoAura() * delta,DamageSource.AURA);
                    effects.onHit(stats, enemy, targets, player.x + 8, player.y + 8, navigation::clearLine, false);
                }
            }
        }
        if (miniBoss != null && stats.possuiAura()) {
            float radius = stats.getRaioAura() + MiniBossEncounter.BODY_RADIUS;
            if (Vector2.dst2(player.x + 8, player.y + 8, miniBoss.centerX(), miniBoss.centerY()) <= radius * radius) {
                session.statistics().deal(miniBoss,stats.getDanoAura() * delta,DamageSource.AURA);
                effects.onHit(stats, miniBoss, targets, player.x + 8, player.y + 8, navigation::clearLine, false);
            }
        }
        session.necromancy().update(delta, player.x + 8, player.y + 8, targets, map::ehSolido, navigation::clearLine);
        if (miniBoss != null && miniBoss.defeated()) {
            game.audio().stopEmitter(miniBoss);
            session.necromancy().resurrect(stats, miniBoss);
            effects.forget(miniBoss);
            orbs.add(new OrbeXP(miniBoss.x, miniBoss.y, Art.cell(atlas, 1, 3), session.xpDrop(MiniBossEncounter.XP)));
            boolean dropped = session.recordMiniBossDefeat();
            if (dropped) dropped = game.profiles().collectForgeMaterials(session) > 0;
            encounterFeedback = dropped ? "Fragmento de Forja obtido e guardado no save!"
                : "Mini-boss derrotado: cristal de XP disponível.";
            encounterFeedbackTimer = 4;
            stats.curar(stats.getCuraPorAbate()); miniBoss = null;
        }
        Iterator<Inimigo> enemyIterator = enemies.iterator();
        while (enemyIterator.hasNext()) {
            Inimigo enemy = enemyIterator.next();
            if (enemy.estaMorto()) {
                session.necromancy().resurrect(stats, enemy);
                effects.forget(enemy);
                orbs.add(new OrbeXP(enemy.x, enemy.y, Art.cell(atlas, 1, 3), session.xpDrop(enemy.getXpDrop())));
                session.recordKill(enemy.getType(), enemy.getSoulDrop());
                stats.curar(stats.getCuraPorAbate());
                enemy.dispose();
                enemyIterator.remove();
            } else if (Vector2.dst2(player.x, player.y, enemy.x, enemy.y) > 900 * 900) {
                effects.forget(enemy);
                enemy.dispose();
                enemyIterator.remove();
            }
        }
        if(!session.isDemo() && !exitWasOpen && session.arenaComplete()) {
            game.audio().play(SoundCue.PASSAGE);exitNoticeTimer=3.5f;
        }
        updateExitBeacon();
        // Resolve defeated enemies first, then contact damage, so healing cannot undo a death.
        for (Inimigo enemy : enemies) {
            if (player.hitbox().overlaps(enemy.hitbox()) && enemy.podeAtacar()) {
                receiveHit(new CombatHit(enemy.getDano(), enemy.centerX(), enemy.centerY(), CombatHit.Kind.CONTACT,enemy.displayName()+" (contato)"), enemy);
                enemy.resetarCooldownAtaque();
            }
        }
        // Resolve arrows only from living enemies, after player attacks and death cleanup.
        for (Inimigo enemy : enemies) {
            if (!enemy.isArcher() || enemy.estaMorto()) continue;
            for (CombatHit hit : enemy.archer().advanceArrows(delta, navigation, player.hitbox()::sweptHits))
                receiveHit(hit, enemy);
        }
        if (miniBoss != null) {
            MiniBossEncounter.State beforeState=miniBoss.state();
            float miniDamage = miniBoss.update(delta, player.x + 8, player.y + 8, navigation, player.hitbox().overlaps(miniHitbox()));
            if(!miniBossVisible()) game.audio().stopEmitter(miniBoss);
            else if(beforeState!=MiniBossEncounter.State.TELEGRAPH && miniBoss.state()==MiniBossEncounter.State.TELEGRAPH)
                game.audio().play(SoundCue.BOSS_WARNING,miniBoss);
            if (miniDamage > 0 && miniHitCooldown == 0) {
                CombatDefense.resolve(stats, miniBoss.hits(), player.x + 8, player.y + 8, miniBoss, combatRandom,session.statistics(),
                    (hit,blocked,actual,counter)->feedback.receive(hit,blocked,actual,counter,stats.getWeapon(),player.x+8,player.y+8,miniBoss.centerX(),miniBoss.centerY()));
                miniHitCooldown = 0.35f;
            }
        }
        if (stats.estaMorto()) {
            if (stats.possuiRessurreicao()) {stats.usarRessurreicao();feedback.resurrect(player.x+8,player.y+8);}
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
                feedback.xp(orb.x+3,orb.y+3);
                orbIterator.remove();
            } else if (Vector2.dst2(player.x + 8, player.y + 8, orb.x + 3, orb.y + 3) > 900 * 900) orbIterator.remove();
        }
        for (DamageNumber number : damageNumbers) number.update(delta);
        damageNumbers.removeIf(DamageNumber::deveSerRemovido);

        spawnTimer += delta;
        while (spawnTimer >= session.spawnInterval()) {
            spawnTimer -= session.spawnInterval();
            int activeArchers = (int) enemies.stream().filter(Inimigo::isArcher).count(), waveArchers = 0;
            for (EnemyType type : waves.next(session.waveSize(), session.difficultyTime(), !session.isDemo(), session.variation(),
                    (int) enemies.stream().filter(enemy -> enemy.getType() == EnemyType.FAST).count())) {
                // Bound simultaneous simulation work, never total enemies generated for the phase.
                if (enemies.size() >= 160) break;
                Inimigo enemy = EnemySpawner.spawnInimigoForaDaTela((OrthographicCamera) world.getCamera(), atlas, map, type, navigation);
                if (enemy != null) {
                    if (waves.archer(type, progress.elapsed(), !session.isDemo(), activeArchers, waveArchers)) {
                        enemy.configureArcher(); activeArchers++; waveArchers++;
                    }
                    enemy.applyDifficulty(session.enemyDifficulty()); enemies.add(enemy);
                }
            }
        }
        spawnMiniBoss();
        updateChests(delta);
        if (stats.podeSubirDeNivel()) iniciarLevelUp();
    }
    private boolean miniBossVisible() {
        return miniBoss!=null && VisibleArea.sprite(world,miniHitbox());
    }
    private void updateExitBeacon() {
        if(session.isDemo() || !session.arenaComplete() || exitBeacon!=null) return;
        for(int i=0;i<16;i++) {
            float angle=i*(float)Math.PI/8;
            float x=player.x+8+(float)Math.cos(angle)*64,y=player.y+8+(float)Math.sin(angle)*48;
            if(navigation.canOccupy(x,y) && navigation.reachable(x,y)) {exitBeacon=new Vector2(x,y);return;}
        }
    }
    private void drawExitBeacon() {
        if(exitBeacon==null) return;
        shapes.setProjectionMatrix(world.getCamera().combined);
        shapes.begin(ShapeRenderer.ShapeType.Line);shapes.setColor(Color.GOLD);
        float radius=12+(float)Math.sin(progress.elapsed()*3)*1.5f;
        shapes.circle(exitBeacon.x,exitBeacon.y,radius,24);
        shapes.rect(exitBeacon.x-5,exitBeacon.y-7,10,14);shapes.end();
        batch.begin();font.getData().setScale(.7f);font.setColor(Color.GOLD);
        font.draw(batch,"E",exitBeacon.x-3,exitBeacon.y+24);
        font.setColor(Color.WHITE);batch.end();
    }
    private boolean archerVisible(Inimigo enemy) {
        return Math.abs(enemy.centerX() - world.getCamera().position.x) < world.getWorldWidth() / 2 - 20
            && Math.abs(enemy.centerY() - world.getCamera().position.y) < world.getWorldHeight() / 2 - 20;
    }
    private void refreshTargets() {
        targets.clear(); targets.addAll(enemies);
        if (miniBoss != null) targets.add(miniBoss);
    }
    private void weaponHit(CombatTarget target, float damage) {
        float before=Math.max(0,target.health());
        session.statistics().deal(target,damage,DamageSource.WEAPON);
        feedback.hit(stats.getWeapon(),target.centerX(),target.centerY(),Math.max(0,before-Math.max(0,target.health())));
        effects.onHit(stats, target, targets, player.x + 8, player.y + 8, navigation::clearLine, true);
    }
    private void receiveHit(CombatHit hit, CombatTarget source) {
        CombatDefense.resolve(stats,List.of(hit),player.x+8,player.y+8,source,combatRandom,session.statistics(),
            (resolved,blocked,actual,counter)->feedback.receive(resolved,blocked,actual,counter,
                stats.getWeapon(),player.x+8,player.y+8,source.centerX(),source.centerY()));
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
            chestFeedback = chests.open(nearby, session);
            game.audio().play(SoundCue.CHEST); chestFeedbackTimer = 3;
        }
    }

    private void finish(boolean victory) {
        game.finishRun(session, victory ? RunOutcome.VICTORY : RunOutcome.DEATH);
    }
    public void iniciarLevelUp() {
        if (!stats.podeSubirDeNivel()) return;
        stats.subirDeNivel();
        game.audio().play(SoundCue.LEVEL_UP);
        state = State.LEVEL_UP;
        choiceGuard.reset();
        choices.clear();
        choices.addAll(UpgradeChoices.draw(session, 3, boonRandom));
    }
    private void updateChoices(float delta) {
        choiceGuard.advance(delta, Gdx.input.isButtonPressed(Input.Buttons.LEFT));
        if (!choiceGuard.ready()) return;
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
        for (Inimigo enemy : enemies) enemy.dispose();
        enemies.clear();
        if(miniBoss!=null)game.audio().stopEmitter(miniBoss);
        miniBoss = null;
        projectiles.clear();
        chests.clear();
        effects.clear(); targets.clear(); feedback.clear();
        leavingArena = true;
        if (stats.podeSubirDeNivel()) iniciarLevelUp();
        else game.offerRoute(session);
    }
    private void attack(float x, float y) {
        Vector2 direction = world.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY())).sub(x, y);
        if (direction.isZero()) return;
        WeaponType weapon = stats.getWeapon();
        feedback.attack(weapon,x,y,direction.x,direction.y);
        if (weapon.isMelee()) {
            slashX = x; slashY = y; slashAngle = direction.angleDeg(); slashTimer = weapon == WeaponType.FISTS ? 0.10f : 0.14f;
            if (weapon == WeaponType.FISTS) { punch(x, y, direction); return; }
            for (Inimigo enemy : enemies) {
                if (!enemy.estaMorto() && WeaponAttack.swordHits(x, y, direction.x, direction.y, enemy.x + 8, enemy.y + 8, stats.swordReach())
                        && navigation.clearLine(x, y, enemy.x + 8, enemy.y + 8)) {
                    weaponHit(enemy, stats.getDano());
                    damageNumbers.add(new DamageNumber(CombatNumbers.format(stats.getDano()), enemy.x + 8, enemy.y + 16));
                }
            }
            if (miniBoss != null && !miniBoss.defeated()
                && WeaponAttack.swordHits(x, y, direction.x, direction.y, miniBoss.centerX(), miniBoss.centerY(), stats.swordReach() + MiniBossEncounter.BODY_RADIUS)
                && navigation.clearLine(x, y, miniBoss.centerX(), miniBoss.centerY())) {
                weaponHit(miniBoss, stats.getDano());
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
            weaponHit(miniBoss, stats.getDano());
            damageNumbers.add(new DamageNumber(CombatNumbers.format(stats.getDano()), miniBoss.centerX(), miniBoss.centerY() + 22));
        } else if (target != null) {
            weaponHit(target, stats.getDano());
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
        drawExitBeacon();
        batch.begin();
        for (OrbeXP orb : orbs) orb.draw(batch);
        player.draw(batch);
        for (Inimigo enemy : enemies) enemy.draw(batch);
        CombatVisuals.allies(batch, atlas, session.necromancy());
        for (Projetil projectile : projectiles) projectile.draw(batch);
        font.getData().setScale(0.65f);
        for (DamageNumber number : damageNumbers) number.draw(batch, font);
        FeedbackVisuals.labels(batch,font,feedback);
        batch.end();
        if(miniBossVisible()) {
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            CombatHud.bar(shapes,miniBoss.centerX()-18,miniBoss.centerY()+20,36,3,miniBoss.health()/miniBoss.maxHealth(),Color.ORANGE);
            shapes.end();
        }
        shapes.setProjectionMatrix(world.getCamera().combined);
        shapes.begin(ShapeRenderer.ShapeType.Line);
        for (Inimigo enemy : enemies) ArcherVisuals.draw(shapes, enemy);
        FeedbackVisuals.pulses(shapes,feedback);
        CombatVisuals.effects(shapes, effects, targets);
        CombatVisuals.guard(shapes, stats, player.x + 8, player.y + 8);
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
            shapes.setColor(FeedbackVisuals.color(stats.getWeapon()));
            if (stats.getWeapon() == WeaponType.FISTS) {
                float angle = (float) Math.toRadians(slashAngle);
                shapes.circle(slashX + (float) Math.cos(angle) * stats.meleeReach(),
                    slashY + (float) Math.sin(angle) * stats.meleeReach(), 5, 12);
            } else shapes.arc(slashX, slashY, stats.swordReach(),
                slashAngle - WeaponAttack.HALF_ANGLE_DEGREES, WeaponAttack.HALF_ANGLE_DEGREES * 2, 18);
        }
        shapes.end();
        hud.apply();
        shapes.setProjectionMatrix(hud.getCamera().combined);batch.setProjectionMatrix(hud.getCamera().combined);
        // Re-evaluate even while paused/resizing so a source hidden by the new viewport becomes silent.
        if(miniBoss!=null && !miniBossVisible())game.audio().stopEmitter(miniBoss);
        CombatHud.player(batch,shapes,font,stats);CombatHud.goals(batch,shapes,font,session);
        FeedbackVisuals.damageBorder(shapes,feedback);
        batch.begin();font.getData().setScale(.9f);font.setColor(Color.GOLD);
        if(chests.nearby(player.x+8,player.y+8)!=null)font.draw(batch,"F: abrir baú",650,461);
        if(exitNoticeTimer>0)font.draw(batch,"Passagem liberada — E para avançar",320,85);
        if(chestFeedbackTimer>0)font.draw(batch,chestFeedback,18,79);
        if(encounterFeedbackTimer>0)font.draw(batch,encounterFeedback,18,57);
        if(progress.elapsed()<8) {
            font.setColor(Color.LIGHT_GRAY);font.draw(batch,"WASD: mover | Mouse: atacar | SHIFT: dash | TAB: detalhes | ESC: pausa",230,25);
        }
        if(!session.isDemo() && session.fury().warned()) {
            font.setColor(session.fury().level()>0?Color.SALMON:Color.ORANGE);
            font.draw(batch,session.fury().level()>0?"Fúria "+session.fury().level()+"/3 — inimigos mais fortes, XP reduzida. E: avançar"
                :"O destino observa — avance antes da Fúria.",320,57);
        }
        if(!session.isDemo() && !game.profiles().error().isEmpty()) {
            font.setColor(Color.SALMON);font.draw(batch,game.profiles().error(),18,110,924,8,true);
        }
        font.getData().setScale(1);font.setColor(Color.WHITE);batch.end();
        if (state != State.PLAYING) drawOverlay();
        if (boonPanel.open()) boonPanel.draw(batch, shapes, font);
        if(audioPanel.open())audioPanel.draw(batch,shapes,font);
    }
    private SpriteHitbox miniHitbox() {
        return miniHitboxes[(int) (miniTime * 4) % 2].place(miniBoss.centerX() - 16, miniBoss.centerY() - 16, 32, 32, false, false);
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
            font.draw(batch, "ESC: continuar | O: áudio | TAB: dádivas | M: voltar ao menu", 280, 270);
        } else {
            for (int index = 0; index < choices.size(); index++) {
                Upgrade upgrade = choices.get(index);
                Rectangle card = cards[index];
                font.setColor(upgrade.rarity.red, upgrade.rarity.green, upgrade.rarity.blue, 1);
                font.draw(batch, "[" + (index + 1) + "] " + upgrade.title, card.x + 12, card.y + 130, card.width - 24, 8, true);
                font.draw(batch, upgrade.rarity.label, card.x + 12, card.y + 100);
                font.setColor(Color.WHITE);
                font.draw(batch, upgrade.description(stats), card.x + 12, card.y + 80, card.width - 24, 8, true);
                font.draw(batch, "Obtida " + session.rank(upgrade) + " vez(es)", card.x + 12, card.y + 25);
            }
            font.draw(batch, choiceGuard.notice(), 230, 135);
            font.draw(batch, "TAB: consultar dádivas já escolhidas", 300, 105);
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
        for (Inimigo enemy : enemies) enemy.dispose();
        enemies.clear();
        if(miniBoss!=null)game.audio().stopEmitter(miniBoss);
        miniBoss = null;
        projectiles.clear();
        orbs.clear();
        chests.clear();
        damageNumbers.clear(); effects.clear(); targets.clear(); feedback.clear(); audioPanel.close();
    }
}
