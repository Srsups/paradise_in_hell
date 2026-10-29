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
    private final TutorialGuide tutorial;
    private final TutorialPanel tutorialPanel;
    private final VideoPanel videoPanel;
    private final ControlsPanel controlsPanel;
    private final CombatFeedback feedback;
    private final ChoiceInputGuard choiceGuard = new ChoiceInputGuard();
    private SpriteHitbox[] miniHitboxes;
    private EquippedArt.Body miniBody;
    private final ActorAnimation miniMotion=new ActorAnimation();
    private final ActorReaction miniReaction=new ActorReaction();
    private ReactionArt.Body miniReactionBody;
    private ActorAnimation.Direction miniReactionDirection=ActorAnimation.Direction.DOWN;
    private final DeathVisuals deaths=new DeathVisuals();
    private final ChestVisuals chestVisuals=new ChestVisuals();
    private float choiceTime,blessingTime;
    private Upgrade blessing;
    private final RunProgress progress;
    private final FitViewport world = new FitViewport(480, 270);
    private final FitViewport hud = new FitViewport(960, 540);
    private final HudScaleRenderer hudScaleRenderer=new HudScaleRenderer();
    private final List<Inimigo> enemies = new ArrayList<>();
    private final List<Projetil> projectiles = new ArrayList<>();
    private final List<OrbeXP> orbs = new ArrayList<>();
    private final List<DamageNumber> damageNumbers = new ArrayList<>();
    private final RunChests chests = new RunChests();
    private String chestFeedback = "";
    private float chestFeedbackTimer;
    private final List<Upgrade> choices = new ArrayList<>();
    private final Rectangle[] cards = BoonChoiceView.cards(3);
    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private BitmapFont font;
    private Texture atlas;
    private TileMap map;
    private Jogador player;
    private float spawnTimer, slashTimer, slashAngle, slashX, slashY;
    private WeaponType slashWeapon;
    private float slashReach;
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
    private boolean disposed,endingStarted,hideEndingPlayer;
    public GameScreen(Main game) { this(game, RunSession.demo()); }
    public GameScreen(Main game, RunSession session) {
        this.game = game;
        this.session = session;
        game.profiles().prepareRun(session);
        this.stats = session.player();
        videoPanel = new VideoPanel(game);
        controlsPanel = new ControlsPanel(game);
        tutorial = session.tutorial();
        tutorialPanel = new TutorialPanel(game, tutorial, session.characterId(), session.isDemo(), stats.getProfession());
        audioPanel=new AudioPanel(game);feedback=new CombatFeedback(game.audio());
        effects=new CombatEffects(session.statistics());
        this.boonPanel = new BoonPanel(session,game.controls());
        this.progress = session.progress();
    }

    @Override public void show() {
        Gdx.input.setInputProcessor(null);
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        font = UiTheme.bodyFont();
        atlas = new Texture(Art.ATLAS);
        atlas.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        map = new TileMap(atlas,game.visuals().scenery(),SceneryTheme.forSession(session));
        miniHitboxes = new SpriteHitbox[]{Art.hitbox(Art.cell(atlas, 2, 1)), Art.hitbox(Art.cell(atlas, 3, 1))};
        miniBody=game.visuals().equipped().body("carrasco");
        miniReactionBody=game.visuals().reactions().body("carrasco");
        player = new Jogador(100, 100, atlas, stats, this::attack, game.visuals(), game.controls());
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
        if (controlsPanel.input(delta,hud.unproject(new Vector2(Gdx.input.getX(),Gdx.input.getY())))) { videoPanel.suspendInput();draw(); return; }
        if (videoPanel.input(delta,hud.unproject(new Vector2(Gdx.input.getX(),Gdx.input.getY())))) { draw(); return; }
        if (tutorialPanel.input(hud.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY())))) { draw(); return; }
        boolean audioWasOpen=audioPanel.open();
        if(audioWasOpen)audioPanel.input(hud.unproject(new Vector2(Gdx.input.getX(),Gdx.input.getY())));
        else if(!boonPanel.open() && Gdx.input.isKeyJustPressed(Input.Keys.O))audioPanel.toggle();
        if(audioWasOpen || audioPanel.open()){draw();return;}
        float uiDelta = delta;
        boolean panelWasOpen = boonPanel.open();
        if (game.controls().justPressed(ControlAction.BOONS)) boonPanel.toggle();
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
                        && game.controls().justPressed(ControlAction.INTERACT)) prepareExit();
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
        float miniHealthBefore=miniBoss==null?0:miniBoss.health();
        deaths.advance(delta);miniReaction.advance(delta);
        session.advance(delta);
        if (game.controls().justPressed(ControlAction.SWITCH_TOOL)) stats.switchTool();
        blessingTime=Math.max(0,blessingTime-delta);
        boolean exitWasOpen=session.arenaComplete();
        feedback.advance(delta);
        miniTime += delta; miniHitCooldown = Math.max(0, miniHitCooldown - delta);
        encounterFeedbackTimer = Math.max(0, encounterFeedbackTimer - delta);
        exitNoticeTimer=Math.max(0,exitNoticeTimer-delta);
        slashTimer = Math.max(0, slashTimer - delta);
        refreshTargets();
        effects.update(delta,targets,navigation::clearLine);
        Vector2 guardAim = world.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        stats.setGuard(game.controls().pressed(ControlAction.GUARD), guardAim.x - player.x - 8, guardAim.y - player.y - 8);
        player.aim(guardAim.x-player.x-8,guardAim.y-player.y-8);
        boolean wasDashing=stats.isDashing();float beforeX=player.x+8,beforeY=player.y+8;
        player.update(delta, map);
        if (Vector2.dst2(beforeX, beforeY, player.x + 8, player.y + 8) > .0001f) tutorialPanel.observe(TutorialTopic.MOVE);
        if (!wasDashing && stats.isDashing()) tutorialPanel.observe(TutorialTopic.DASH);
        feedback.dash(stats.getWeapon(),beforeX,beforeY,player.x+8,player.y+8,!wasDashing && stats.isDashing(),stats.isDashing());
        if(!wasDashing&&stats.isDashing())effects.dash(stats,beforeX,beforeY);
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
                    weaponHit(enemy, projectile.dano,projectile.bonus);
                    damageNumbers.add(new DamageNumber(CombatNumbers.format(projectile.dano), enemy.x + 8, enemy.y + 16));
                    projectile.hit(enemy);
                    redirectRicochet(projectile);
                    break;
                }
            }
            if (!projectile.deveSerRemovido && miniBoss != null && !miniBoss.defeated() && projectile.canHit(miniBoss)
                && miniHitbox().sweptHits(projectile.beforeX, projectile.beforeY, projectile.x, projectile.y, projectile.hitRadius())) {
                weaponHit(miniBoss, projectile.dano,projectile.bonus);
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
            deaths.add("carrasco",EncounterAnimation.mini(miniBoss,miniMotion).direction(),miniBoss.centerX(),miniBoss.centerY(),32);
            game.audio().stopEmitter(miniBoss);
            session.necromancy().resurrect(stats, miniBoss);
            effects.forget(miniBoss);
            orbs.add(new OrbeXP(miniBoss.x, miniBoss.y, Art.cell(atlas, 1, 3), session.xpDrop(MiniBossEncounter.XP)));
            boolean seedPending = session.farmerQuest() == FarmerQuest.ACCEPTED;
            boolean dropped = session.recordMiniBossDefeat();
            game.profiles().collectFarmerQuest(session);
            boolean seedRecovered = seedPending && session.farmerQuest() == FarmerQuest.SEED_FOUND;
            if (dropped) dropped = game.profiles().collectForgeMaterials(session) > 0;
            encounterFeedback = seedRecovered ? "Semente recuperada! Agora derrote o Guardião do Tártaro."
                : dropped ? "Fragmento de Forja obtido e guardado no save!"
                : "Mini-boss derrotado: cristal de XP disponível.";
            encounterFeedbackTimer = 4;
            miniBoss = null;miniReaction.clear();
        }
        Iterator<Inimigo> enemyIterator = enemies.iterator();
        while (enemyIterator.hasNext()) {
            Inimigo enemy = enemyIterator.next();
            if (enemy.estaMorto()) {
                deaths.add(enemy.deathActor(),enemy.deathDirection(),enemy.centerX(),enemy.centerY(),enemy.drawBodySize());
                session.necromancy().resurrect(stats, enemy);
                effects.forget(enemy);
                orbs.add(new OrbeXP(enemy.x, enemy.y, Art.cell(atlas, 1, 3), session.xpDrop(enemy.getXpDrop())));
                session.recordKill(enemy.getType(), enemy.getSoulDrop());
                enemy.dispose();
                enemyIterator.remove();
            } else if (Vector2.dst2(player.x, player.y, enemy.x, enemy.y) > 900 * 900) {
                effects.forget(enemy);
                enemy.dispose();
                enemyIterator.remove();
            }
        }
        if(!session.isDemo() && !exitWasOpen && session.arenaComplete()) {
            tutorialPanel.observe(TutorialTopic.GOALS);
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
            float beforeMiniX=miniBoss.x,beforeMiniY=miniBoss.y;
            float miniDamage = miniBoss.update(delta, player.x + 8, player.y + 8, navigation, player.hitbox().overlaps(miniHitbox()));
            miniMotion.advance(delta,miniBoss.x-beforeMiniX,miniBoss.y-beforeMiniY);
            if(!miniBossVisible()) game.audio().stopEmitter(miniBoss);
            else if(beforeState!=MiniBossEncounter.State.TELEGRAPH && miniBoss.state()==MiniBossEncounter.State.TELEGRAPH)
                game.audio().play(SoundCue.BOSS_WARNING,miniBoss);
            if (miniDamage > 0 && miniHitCooldown == 0) {
                CombatDefense.resolve(stats, miniBoss.hits(), player.x + 8, player.y + 8, miniBoss, combatRandom,session.statistics(),
                    (hit,blocked,actual,counter)->feedback.receive(hit,blocked,actual,counter,stats.getWeapon(),player.x+8,player.y+8,miniBoss.centerX(),miniBoss.centerY()));
                miniHitCooldown = 0.35f;
            }
        }
        if(miniBoss!=null && !miniBoss.defeated() && miniBoss.health()<miniHealthBefore && miniReaction.hurt())
            miniReactionDirection=EncounterAnimation.mini(miniBoss,miniMotion).direction();
        player.syncReaction();
        if (stats.estaMorto()) {
            if (stats.possuiRessurreicao()) {stats.usarRessurreicao();player.syncReaction();feedback.resurrect(player.x+8,player.y+8);}
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
                tutorialPanel.observe(TutorialTopic.EXPERIENCE);
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
                Inimigo enemy = EnemySpawner.spawnInimigoForaDaTela((OrthographicCamera) world.getCamera(), atlas, map, type, navigation, game.visuals());
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
        tutorialPanel.advance(delta, tutorialContext());
    }
    private TutorialGuide.Context tutorialContext() {
        boolean active = state == State.PLAYING && !endingStarted && !controlsPanel.open() && !videoPanel.open() && !tutorialPanel.open() && !boonPanel.open() && !audioPanel.open();
        boolean priority = session.fury().warned() || exitNoticeTimer > 0 || chestFeedbackTimer > 0 || encounterFeedbackTimer > 0
            || chests.nearby(player.x + 8, player.y + 8) != null || !game.profiles().error().isEmpty();
        return new TutorialGuide.Context(TutorialGuide.Place.COMBAT, active, priority, session.arenaComplete(),
            !orbs.isEmpty() || stats.getXpAtual() > 0 || stats.getNivel() > 1, false);
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
        batch.begin();batch.setColor(Color.WHITE);
        batch.draw(game.visuals().scenery().passage((progress.elapsed()%1.2f)/1.2f),exitBeacon.x-16,exitBeacon.y-12,32,32);
        font.getData().setScale(.7f);font.setColor(Color.GOLD);
        font.draw(batch,primaryControl(ControlAction.INTERACT),exitBeacon.x-3,exitBeacon.y+28);
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
    private void weaponHit(CombatTarget target, float damage,WeaponBonus bonus) {
        float before=Math.max(0,target.health());
        session.statistics().deal(target,damage,DamageSource.WEAPON);
        feedback.hit(stats.getWeapon(),target.centerX(),target.centerY(),Math.max(0,before-Math.max(0,target.health())));
        effects.weaponBonus(target,bonus);
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
            miniReaction.clear();
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
        chestVisuals.advance(delta);
        chestFeedbackTimer = Math.max(0, chestFeedbackTimer - delta);
        chests.update(delta, player.x + 8, player.y + 8, navigation, !session.isDemo());
        RunChests.Chest nearby = chests.nearby(player.x + 8, player.y + 8);
        if (nearby != null && game.controls().justPressed(ControlAction.OPEN_CHEST)) {
            float xpBefore = stats.getXpAtual();
            chestFeedback = chests.open(nearby, session);
            if (stats.getXpAtual() > xpBefore) tutorialPanel.observe(TutorialTopic.EXPERIENCE);
            chestVisuals.open(nearby);
            game.audio().play(SoundCue.CHEST); chestFeedbackTimer = 3;
        }
    }

    private void finish(boolean victory) {
        if(endingStarted)return;
        endingStarted=true;
        if(victory) {game.finishRun(session,RunOutcome.VICTORY);return;}
        hideEndingPlayer=true;feedback.clear();
        RunEndingScreen.Frame frame=RunEndingScreen.capture(world,this::draw,player.deathActor(),player.deathDirection(),
            player.x+8,player.y+8,player.drawBodySize());
        game.finishRun(session,RunOutcome.DEATH,next->new RunEndingScreen(game,frame,ActorReaction.DEATH_SECONDS,next));
    }
    public void iniciarLevelUp() {
        if (!stats.podeSubirDeNivel()) return;
        stats.subirDeNivel();
        game.audio().play(SoundCue.LEVEL_UP);
        state = State.LEVEL_UP;
        choiceGuard.reset();
        choices.clear();
        choices.addAll(UpgradeChoices.draw(session, 3, session.boonRandom()));
    }
    private void updateChoices(float delta) {
        choiceTime+=delta;
        choiceGuard.advance(delta, Gdx.input.isButtonPressed(Input.Buttons.LEFT) || game.controls().physicallyPressed(ControlAction.ATTACK));
        if (!choiceGuard.ready()) return;
        Vector2 pointer = hud.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        for (int index = 0; index < choices.size(); index++) {
            boolean key = Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + index)
                || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1 + index);
            boolean click = Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && cards[index].contains(pointer);
            if (key || click) {
                Upgrade upgrade = choices.get(index);
                session.applyUpgrade(upgrade);
                tutorialPanel.observe(TutorialTopic.BOONS);
                blessing=upgrade;blessingTime=.8f;
                state = State.PLAYING;
                // All earned levels keep their own selection, even after collecting many orbs.
                if (stats.podeSubirDeNivel()) iniciarLevelUp();
                else if (leavingArena) game.offerRoute(session);
                return;
            }
        }
    }
    private void prepareExit() {
        tutorialPanel.observe(TutorialTopic.EXIT);
        if (!orbs.isEmpty()) tutorialPanel.observe(TutorialTopic.EXPERIENCE);
        for (OrbeXP orb : orbs) stats.ganharXP(orb.valorXP);
        orbs.clear();
        for (Inimigo enemy : enemies) enemy.dispose();
        enemies.clear();
        if(miniBoss!=null)game.audio().stopEmitter(miniBoss);
        miniBoss = null;
        miniReaction.clear();deaths.clear();
        projectiles.clear();
        chests.clear();chestVisuals.clear();
        effects.clear(); targets.clear(); feedback.clear();
        leavingArena = true;
        if (stats.podeSubirDeNivel()) iniciarLevelUp();
        else game.offerRoute(session);
    }
    private void attack(float x, float y) {
        Vector2 direction = world.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY())).sub(x, y);
        if (direction.isZero()) return;
        tutorialPanel.observe(TutorialTopic.ATTACK);
        WeaponType weapon = stats.getWeapon();
        feedback.attack(weapon,x,y,direction.x,direction.y);
        if (weapon.isMelee()) {
            WeaponBonus bonus=stats.boons().beginAttack();
            slashX = x; slashY = y; slashAngle = direction.angleDeg();
            slashWeapon = weapon; slashReach = stats.meleeReach(); slashTimer = DivineVisuals.slashDuration(weapon);
            if (weapon == WeaponType.FISTS) { punch(x, y, direction,bonus); return; }
            for (Inimigo enemy : enemies) {
                float radius = weapon == WeaponType.RAKE || weapon == WeaponType.SCYTHE
                    ? Math.min(enemy.hitbox().right() - enemy.hitbox().left(), enemy.hitbox().top() - enemy.hitbox().bottom()) / 2 : 0;
                if (!enemy.estaMorto() && WeaponAttack.hits(weapon, x, y, direction.x, direction.y,
                        enemy.x + 8, enemy.y + 8, stats.meleeReach(), radius, navigation::clearLine)) {
                    weaponHit(enemy, stats.getDano(),bonus);
                    damageNumbers.add(new DamageNumber(CombatNumbers.format(stats.getDano()), enemy.x + 8, enemy.y + 16));
                }
            }
            if (miniBoss != null && !miniBoss.defeated()
                && WeaponAttack.hits(weapon, x, y, direction.x, direction.y, miniBoss.centerX(), miniBoss.centerY(),
                    stats.meleeReach(), MiniBossEncounter.BODY_RADIUS, navigation::clearLine)) {
                weaponHit(miniBoss, stats.getDano(),bonus);
                damageNumbers.add(new DamageNumber(CombatNumbers.format(stats.getDano()), miniBoss.centerX(), miniBoss.centerY() + 22));
            }
        } else {
            Projetil projectile = new Projetil(x, y, direction.nor(), weapon == WeaponType.BOW ? game.visuals().region("arrow-0") : Art.cell(atlas, 0, 3), weapon);
            projectile.configure(stats);projectile.visuals(game.visuals());
            projectiles.add(projectile);
        }
    }
    private void punch(float x, float y, Vector2 direction,WeaponBonus bonus) {
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
            weaponHit(miniBoss, stats.getDano(),bonus);
            damageNumbers.add(new DamageNumber(CombatNumbers.format(stats.getDano()), miniBoss.centerX(), miniBoss.centerY() + 22));
        } else if (target != null) {
            weaponHit(target, stats.getDano(),bonus);
            damageNumbers.add(new DamageNumber(CombatNumbers.format(stats.getDano()), target.x + 8, target.y + 16));
        }
    }
    private void draw() {
        if(state==State.LEVEL_UP) {
            hud.apply();shapes.setProjectionMatrix(hud.getCamera().combined);batch.setProjectionMatrix(hud.getCamera().combined);
            if(miniBoss!=null && !miniBossVisible())game.audio().stopEmitter(miniBoss);
            drawOverlay();if(boonPanel.open())boonPanel.draw(batch,shapes,font,game.visuals());if(audioPanel.open())audioPanel.draw(batch,shapes,font);
            if(tutorialPanel.open())tutorialPanel.draw(batch,shapes,font);
            if(videoPanel.open())videoPanel.draw(batch,shapes,font);
            if(controlsPanel.open())controlsPanel.draw(batch,shapes,font);return;
        }
        ScreenUtils.clear(0.045f, 0.055f, 0.085f, 1);
        world.apply();
        batch.setProjectionMatrix(world.getCamera().combined);
        shapes.setProjectionMatrix(world.getCamera().combined);
        batch.begin();
        map.draw(batch);
        deaths.draw(batch,game.visuals().reactions());
        batch.end();
        drawChests();
        drawMiniBoss();
        drawExitBeacon();
        batch.begin();
        for (OrbeXP orb : orbs) orb.draw(batch);
        if(!hideEndingPlayer)DivineVisuals.player(batch,game.visuals(),stats,player.x+8,player.y+8,progress.elapsed());
        FeedbackVisuals.sprites(batch,game.visuals(),feedback);
        if(!hideEndingPlayer){player.syncReaction();player.draw(batch);}
        for (Inimigo enemy : enemies) enemy.draw(batch);
        CombatVisuals.allies(batch, game.visuals(), session.necromancy());
        for (Projetil projectile : projectiles) projectile.draw(batch);
        for(Inimigo enemy:enemies){WeaponVisuals.arrows(batch,game.visuals(),enemy);TacticalVisuals.archer(batch,game.visuals(),enemy);}
        DivineVisuals.effects(batch,game.visuals(),effects,targets,progress.elapsed());
        if(!hideEndingPlayer)DivineVisuals.slash(batch,game.visuals(),slashWeapon,slashReach,slashX,slashY,slashAngle,slashTimer);
        if(!hideEndingPlayer&&blessingTime>0)DivineVisuals.blessing(batch,game.visuals(),blessing,player.x+8,player.y+8,1-blessingTime/.8f);
        font.getData().setScale(0.65f);
        for (DamageNumber number : damageNumbers) number.draw(batch, font);
        FeedbackVisuals.labels(batch,font,feedback);
        batch.end();
        if(miniBossVisible()) {
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            CombatHud.bar(shapes,miniBoss.centerX()-18,miniBoss.centerY()+20,36,3,miniBoss.health()/miniBoss.maxHealth(),Color.ORANGE);
            shapes.end();
        }

        hud.apply();
        shapes.setProjectionMatrix(hud.getCamera().combined);batch.setProjectionMatrix(hud.getCamera().combined);
        // Re-evaluate even while paused/resizing so a source hidden by the new viewport becomes silent.
        if(miniBoss!=null && !miniBossVisible())game.audio().stopEmitter(miniBoss);
        HudSettings hudSettings=game.hudSettings().value();
        hudScaleRenderer.begin(batch,shapes,hudSettings,HudLayout.Anchor.LEFT);
        try {CombatHud.player(batch,shapes,font,stats,game.visuals());}finally {hudScaleRenderer.end();}
        hudScaleRenderer.begin(batch,shapes,hudSettings,HudLayout.Anchor.RIGHT);
        try {CombatHud.goals(batch,shapes,font,session,game.controls());}finally {hudScaleRenderer.end();}
        FeedbackVisuals.damageBorder(shapes,feedback);
        batch.begin();font.getData().setScale(.9f);font.setColor(Color.GOLD);
        if(chests.nearby(player.x+8,player.y+8)!=null)UiTheme.caption(batch,font,primaryControl(ControlAction.OPEN_CHEST)+": abrir baú",650,461,Color.GOLD);
        if(exitNoticeTimer>0)UiTheme.caption(batch,font,"Passagem liberada — "+primaryControl(ControlAction.INTERACT)+" para avançar",320,85,Color.GOLD);
        if(chestFeedbackTimer>0)UiTheme.caption(batch,font,chestFeedback,18,79,Color.GOLD);
        if(encounterFeedbackTimer>0)UiTheme.caption(batch,font,encounterFeedback,18,57,Color.GOLD);
        tutorialPanel.footer(batch, font, tutorialContext());
        if(!session.isDemo() && session.fury().warned()) {
            UiTheme.caption(batch,font,session.fury().level()>0?"Fúria "+session.fury().level()+"/3 — inimigos mais fortes, XP reduzida. "+primaryControl(ControlAction.INTERACT)+": avançar"
                :"O destino observa — avance antes da Fúria.",320,57,session.fury().level()>0?Color.SALMON:Color.ORANGE);
        }
        if(!session.isDemo() && !game.profiles().error().isEmpty()) {
            font.setColor(Color.SALMON);font.draw(batch,game.profiles().error(),18,110,924,8,true);
        }
        font.getData().setScale(1);font.setColor(Color.WHITE);batch.end();
        if (state != State.PLAYING) drawOverlay();
        if (boonPanel.open()) boonPanel.draw(batch, shapes, font,game.visuals());
        if(audioPanel.open())audioPanel.draw(batch,shapes,font);
        if(tutorialPanel.open())tutorialPanel.draw(batch,shapes,font);
        if(videoPanel.open())videoPanel.draw(batch,shapes,font);
        if(controlsPanel.open())controlsPanel.draw(batch,shapes,font);
    }
    private SpriteHitbox miniHitbox() {
        if(miniHurtVisible())return miniReactionBody.hitbox(miniReaction,miniReactionDirection,miniBoss.centerX(),miniBoss.centerY(),32);
        return miniBody.hitbox(EncounterAnimation.mini(miniBoss,miniMotion),miniBoss.centerX(),miniBoss.centerY(),32);
    }
    private boolean miniHurtVisible() {
        return miniBoss!=null && miniBoss.state()==MiniBossEncounter.State.RECOVERY && miniReaction.state()==ActorReaction.State.HURT;
    }
    private void drawMiniBoss() {
        if(miniBoss==null)return;
        batch.begin();
        TacticalVisuals.mini(batch,game.visuals(),miniBoss);
        for(MiniBossEncounter.Stone stone:miniBoss.stones())batch.draw(Art.cell(atlas,3,2),stone.x-3,stone.y-3,6,6);
        batch.setColor(miniBoss.flashing()?Color.SALMON:Color.WHITE);
        if(miniHurtVisible())miniReactionBody.draw(batch,miniReaction,miniReactionDirection,miniBoss.centerX(),miniBoss.centerY(),32);
        else if(miniBoss.state()==MiniBossEncounter.State.RECOVERY)miniBody.draw(batch,miniMotion,miniBoss.centerX(),miniBoss.centerY(),32);
        else miniBody.draw(batch,EncounterAnimation.mini(miniBoss,miniMotion),miniBoss.centerX(),miniBoss.centerY(),32);
        batch.setColor(Color.WHITE);
        if(miniBoss.state()==MiniBossEncounter.State.ACTIVE&&miniBoss.attack()==MiniBossEncounter.Attack.SPIN)
            WeaponVisuals.sprite(batch,game.visuals().expanded().effect("attack-sword",miniBoss.activeProgress()),
                miniBoss.centerX(),miniBoss.centerY(),MiniBossEncounter.SPIN_RADIUS*2,MiniBossEncounter.SPIN_RADIUS*2,miniBoss.activeProgress()*360);
        batch.end();
    }
    private void drawChests() {
        batch.begin();chestVisuals.draw(batch,game.visuals(),chests.chests());batch.end();
    }

    private void drawOverlay() {
        if(state==State.LEVEL_UP) {
            BoonChoiceView.draw(batch,shapes,font,game.visuals(),session,choices,cards,"ESCOLHA UMA DÁDIVA",
                choiceGuard.ready()?"Escolha disponível — compare os efeitos.":choiceGuard.notice(),"pausa",choiceTime,game.controls().label(ControlAction.BOONS));
            return;
        }
        Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA,com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);shapes.setColor(0,0,0,.88f);shapes.rect(0,0,960,540);shapes.end();
        Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        batch.begin();font.getData().setScale(1.5f);font.setColor(Color.GOLD);font.draw(batch,"PAUSA",420,325);
        font.getData().setScale(1);font.setColor(Color.WHITE);
        font.getData().setScale(.9f);
        font.draw(batch,"ESC: continuar | F1: guia | F2: vídeo | F3: controles | O: áudio | "+primaryControl(ControlAction.BOONS)+": dádivas | M: menu",140,270);batch.end();
    }
    @Override public void pause() {
        videoPanel.suspendInput();
        if (state != State.PAUSED) { beforePause = state; state = State.PAUSED; }
    }
    private String primaryControl(ControlAction action) {
        return game.controlSettings().value().bindings(action).get(0).label();
    }
    @Override public void dispose() {
        if (disposed) return;
        disposed = true;
        videoPanel.close();
        controlsPanel.close();
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
        damageNumbers.clear(); deaths.clear();miniReaction.clear();effects.clear(); targets.clear(); feedback.clear(); audioPanel.close();tutorialPanel.close();
    }
}
