package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.util.ArrayList;
import java.util.List;

/** Boss arena owns graphics/entities; the same session supplies player progression. */
public final class BossRoomScreen extends ScreenAdapter {
    private final Main game;
    private final RunSession session;
    private final PlayerState stats;
    private final BoonPanel boonPanel;
    private final AudioPanel audioPanel;
    private final TutorialGuide tutorial;
    private final TutorialPanel tutorialPanel;
    private final VideoPanel videoPanel;
    private final ControlsPanel controlsPanel;
    private final CombatFeedback feedback;
    private SpriteHitbox[] hitboxes;
    private EquippedArt.Body bossBody;
    private final ActorAnimation bossMotion=new ActorAnimation();
    private final ActorReaction bossReaction=new ActorReaction(.18f,1);
    private ReactionArt.Body bossReactionBody;
    private ActorAnimation.Direction bossReactionDirection=ActorAnimation.Direction.DOWN;
    private final BossEncounter boss;
    private final CombatEffects effects;
    private final java.util.Random combatRandom = new java.util.Random();
    private EnemyNavigation navigation;
    private final FitViewport world = new FitViewport(480, 270), hud = new FitViewport(960, 540);
    private final HudScaleRenderer hudScaleRenderer=new HudScaleRenderer();
    private final List<Projetil> projectiles = new ArrayList<>();
    private final List<DamageNumber> damageNumbers = new ArrayList<>();
    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private BitmapFont font;
    private Texture atlas;
    private Animation<TextureRegion> animation;
    private TileMap map;
    private Jogador player;
    private boolean paused, disposed,endingStarted,hideEndingPlayer,hideEndingBoss;
    private float stateTime, hitCooldown, slashTimer, slashAngle, slashX, slashY;
    private WeaponType slashWeapon;
    private float slashReach;

    public BossRoomScreen(Main game, RunSession session) {
        if (session.isDemo() || session.arena().kind() != ArenaDefinition.Kind.BOSS_ROOM)
            throw new IllegalStateException("Not a boss room");
        this.game = game; this.session = session; this.stats = session.player();
        videoPanel = new VideoPanel(game);
        controlsPanel = new ControlsPanel(game);
        tutorial = session.tutorial();
        tutorialPanel = new TutorialPanel(game, tutorial, session.characterId(), false, stats.getProfession());
        audioPanel=new AudioPanel(game);feedback=new CombatFeedback(game.audio());
        effects=new CombatEffects(session.statistics());
        boonPanel = new BoonPanel(session,game.controls());
        boss = new BossEncounter(session.enemyDifficulty());
    }
    @Override public void show() {
        Gdx.input.setInputProcessor(null);
        batch = new SpriteBatch(); shapes = new ShapeRenderer(); font = UiTheme.bodyFont();
        atlas = new Texture(Art.ATLAS);
        atlas.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        animation = new Animation<>(0.25f, Art.cell(atlas, 2, 1), Art.cell(atlas, 3, 1));
        animation.setPlayMode(Animation.PlayMode.LOOP);
        hitboxes = new SpriteHitbox[]{Art.hitbox(animation.getKeyFrames()[0]), Art.hitbox(animation.getKeyFrames()[1])};
        bossBody=game.visuals().equipped().body("guardian");
        bossReactionBody=game.visuals().reactions().body("guardian");
        map = new TileMap(atlas,game.visuals().scenery(),SceneryTheme.GUARDIAN);
        map.gerarAreaInicialSegura(240, 112, 64);
        map.setBounds(BossEncounter.MIN_X, BossEncounter.MIN_Y,
            BossEncounter.MAX_X - BossEncounter.MIN_X, BossEncounter.MAX_Y - BossEncounter.MIN_Y);
        player = new Jogador(232, 48, atlas, stats, this::attack, game.visuals(), game.controls());
        navigation = new EnemyNavigation(map::ehSolido);
        session.necromancy().enterRoom(player.x + 8, player.y + 8);
        world.getCamera().position.set(240, 135, 0); world.getCamera().update();
        map.update(world.getCamera());
        session.statistics().observeBoss(boss);
    }
    @Override public void resize(int width, int height) {
        world.update(width, height); hud.update(width, height, true);
        world.getCamera().position.set(240, 135, 0); world.getCamera().update();
        if (map != null) map.update(world.getCamera());
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
        boolean panelWasOpen = boonPanel.open();
        if (game.controls().justPressed(ControlAction.BOONS)) boonPanel.toggle();
        else if (boonPanel.open() && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) boonPanel.close();
        if (boonPanel.open()) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) boonPanel.movePage(-1);
            if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) boonPanel.movePage(1);
        }
        if (panelWasOpen || boonPanel.open()) { draw(); return; }
        delta = Math.min(delta, 0.05f);
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) paused = !paused;
        if (paused) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
                game.finishRun(session, RunOutcome.ABANDONED); return;
            }
        } else update(delta);
        if (game.getScreen() != this) return;
        draw();
    }
    private void update(float delta) {
        float bossHealthBefore=boss.health();
        bossReaction.advance(delta);
        session.advance(delta);feedback.advance(delta);
        if (game.controls().justPressed(ControlAction.SWITCH_TOOL)) stats.switchTool();
        boolean wasEnraged=boss.enraged(),wasLocked=boss.leapLocked();
        BossEncounter.State beforeState=boss.state();
        stateTime += delta;
        hitCooldown = Math.max(0, hitCooldown - delta);
        slashTimer = Math.max(0, slashTimer - delta);
        stats.setWalkingSpeedLimit(boss.walkingSpeedLimit());
        effects.update(delta,java.util.List.of(boss),navigation::clearLine);
        Vector2 guardAim = world.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        stats.setGuard(game.controls().pressed(ControlAction.GUARD), guardAim.x - player.x - 8, guardAim.y - player.y - 8);
        player.aim(guardAim.x-player.x-8,guardAim.y-player.y-8);
        boolean wasDashing=stats.isDashing();float motionX=player.x+8,motionY=player.y+8;
        player.update(delta,map);
        if (Vector2.dst2(motionX, motionY, player.x + 8, player.y + 8) > .0001f) tutorialPanel.observe(TutorialTopic.MOVE);
        if (!wasDashing && stats.isDashing()) tutorialPanel.observe(TutorialTopic.DASH);
        feedback.dash(stats.getWeapon(),motionX,motionY,player.x+8,player.y+8,!wasDashing && stats.isDashing(),stats.isDashing());
        if(!wasDashing&&stats.isDashing())effects.dash(stats,motionX,motionY);
        for (Projetil projectile : projectiles) {
            float beforeX = projectile.x, beforeY = projectile.y;
            projectile.update(delta, map);
            if(!projectile.deveSerRemovido)feedback.trail(stats.getWeapon(),projectile.x,projectile.y,beforeX,beforeY);
            if (boss.targetable() && !projectile.deveSerRemovido && bossHitbox().sweptHits(beforeX, beforeY,
                    projectile.x, projectile.y, projectile.hitRadius())) {
                hurtBoss(projectile.dano,projectile.bonus); projectile.deveSerRemovido = true;
            }
        }
        projectiles.removeIf(projectile -> projectile.deveSerRemovido);
        if (stats.possuiAura() && boss.targetable()) {
            float radius = stats.getRaioAura() + BossEncounter.RADIUS;
            if (Vector2.dst2(player.x + 8, player.y + 8, boss.x(), boss.y()) <= radius * radius) {
                session.statistics().deal(boss,stats.getDanoAura() * delta,DamageSource.AURA);
                effects.onHit(stats, boss, java.util.List.of(boss), player.x + 8, player.y + 8, navigation::clearLine, false);
            }
        }
        session.necromancy().update(delta, player.x + 8, player.y + 8, java.util.List.of(boss), map::ehSolido, navigation::clearLine);
        float beforeBossX=boss.x(),beforeBossY=boss.y();
        float damage = boss.update(delta, player.x + 8, player.y + 8, player.hitbox().overlaps(bossHitbox()));
        bossMotion.advance(delta,boss.x()-beforeBossX,boss.y()-beforeBossY);
        stats.setWalkingSpeedLimit(boss.walkingSpeedLimit());
        if(beforeState!=BossEncounter.State.TELEGRAPH && boss.state()==BossEncounter.State.TELEGRAPH || !wasEnraged && boss.enraged())
            game.audio().play(SoundCue.BOSS_WARNING);
        if(!wasLocked && boss.leapLocked())game.audio().play(SoundCue.RED_LOCK);
        if (damage > 0 && hitCooldown <= 0) {
            CombatDefense.resolve(stats, boss.hits(), player.x + 8, player.y + 8, boss, combatRandom,session.statistics(),
                (hit,blocked,actual,counter)->feedback.receive(hit,blocked,actual,counter,stats.getWeapon(),player.x+8,player.y+8,boss.x(),boss.y()));
            hitCooldown = 0.35f;
        }
        for (DamageNumber number : damageNumbers) number.update(delta);
        damageNumbers.removeIf(DamageNumber::deveSerRemovido);
        if(!boss.defeated() && boss.health()<bossHealthBefore && bossReaction.hurt())
            bossReactionDirection=EncounterAnimation.boss(boss,bossMotion).direction();
        player.syncReaction();
        if (stats.estaMorto()) {
            if (stats.possuiRessurreicao()) { stats.usarRessurreicao(); player.syncReaction();feedback.resurrect(player.x+8,player.y+8); hitCooldown = 0.35f; }
            else { finish(false); return; }
        }
        if (boss.defeated()) finish(true);
        else tutorialPanel.advance(delta, tutorialContext());
    }
    private TutorialGuide.Context tutorialContext() {
        boolean active = !paused && !endingStarted && !controlsPanel.open() && !videoPanel.open() && !tutorialPanel.open() && !boonPanel.open() && !audioPanel.open();
        return new TutorialGuide.Context(TutorialGuide.Place.GUARDIAN, active,
            boss.state() != BossEncounter.State.RECOVERY || !game.profiles().error().isEmpty(), false,
            stats.getXpAtual() > 0 || stats.getNivel() > 1, false);
    }
    private void attack(float x, float y) {
        if (boss.defeated()) return;
        Vector2 direction = world.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY())).sub(x, y);
        if (direction.isZero()) return;
        tutorialPanel.observe(TutorialTopic.ATTACK);
        WeaponType weapon = stats.getWeapon();
        feedback.attack(weapon,x,y,direction.x,direction.y);
        if (weapon.isMelee()) {
            WeaponBonus bonus=stats.boons().beginAttack();
            slashX = x; slashY = y; slashAngle = direction.angleDeg();
            slashWeapon = weapon; slashReach = stats.meleeReach(); slashTimer = DivineVisuals.slashDuration(weapon);
            if (WeaponAttack.hits(weapon, x, y, direction.x, direction.y, boss.x(), boss.y(),
                    stats.meleeReach(), BossEncounter.RADIUS, navigation::clearLine)) hurtBoss(stats.getDano(),bonus);
        } else {
            Projetil projectile = new Projetil(x, y, direction.nor(), weapon == WeaponType.BOW ? game.visuals().region("arrow-0") : Art.cell(atlas, 0, 3), weapon);
            projectile.configure(stats);projectile.visuals(game.visuals()); projectiles.add(projectile);
        }
    }
    private void hurtBoss(float damage,WeaponBonus bonus) {
        if (!boss.targetable()) return;
        float before=Math.max(0,boss.health());
        session.statistics().deal(boss,damage,DamageSource.WEAPON);
        feedback.hit(stats.getWeapon(),boss.x(),boss.y(),Math.max(0,before-Math.max(0,boss.health())));
        effects.weaponBonus(boss,bonus);
        effects.onHit(stats, boss, java.util.List.of(boss), player.x + 8, player.y + 8, navigation::clearLine, true);
        damageNumbers.add(new DamageNumber(CombatNumbers.format(damage), boss.x(), boss.y() + 28));
    }
    private void finish(boolean victory) {
        if(endingStarted)return;
        endingStarted=true;
        String actor;
        ActorAnimation.Direction direction;
        float x,y,size,duration;
        if(victory) {
            tutorialPanel.observe(TutorialTopic.GUARDIAN);
            session.recordBossDefeat();hideEndingBoss=true;
            actor="guardian";direction=EncounterAnimation.boss(boss,bossMotion).direction();
            x=boss.x();y=boss.y();size=BossEncounter.DRAW_SIZE;duration=1;
        } else {
            hideEndingPlayer=true;
            actor=player.deathActor();direction=player.deathDirection();
            x=player.x+8;y=player.y+8;size=player.drawBodySize();duration=ActorReaction.DEATH_SECONDS;
        }
        feedback.clear();
        RunEndingScreen.Frame frame=RunEndingScreen.capture(world,this::draw,actor,direction,x,y,size);
        game.finishRun(session,victory?RunOutcome.VICTORY:RunOutcome.DEATH,next->new RunEndingScreen(game,frame,duration,next));
    }
    private SpriteHitbox bossHitbox() {
        if(bossHurtVisible())return bossReactionBody.hitbox(bossReaction,bossReactionDirection,boss.x(),boss.y(),BossEncounter.DRAW_SIZE);
        return bossBody.hitbox(EncounterAnimation.boss(boss,bossMotion),boss.x(),boss.y(),BossEncounter.DRAW_SIZE);
    }
    private boolean bossHurtVisible() {
        return (boss.state()==BossEncounter.State.INTRO||boss.state()==BossEncounter.State.RECOVERY)
            && bossReaction.state()==ActorReaction.State.HURT;
    }
    private void draw() {
        ScreenUtils.clear(0.045f, 0.055f, 0.085f, 1);
        world.apply();
        batch.setProjectionMatrix(world.getCamera().combined);
        batch.begin(); map.draw(batch); batch.end();
        shapes.setProjectionMatrix(world.getCamera().combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.03f, 0.04f, 0.06f, 0.9f);
        shapes.rect(0, 0, 480, BossEncounter.MIN_Y);
        shapes.rect(0, BossEncounter.MAX_Y, 480, 270 - BossEncounter.MAX_Y);
        shapes.rect(0, BossEncounter.MIN_Y, BossEncounter.MIN_X, BossEncounter.MAX_Y - BossEncounter.MIN_Y);
        shapes.rect(BossEncounter.MAX_X, BossEncounter.MIN_Y, 480 - BossEncounter.MAX_X, BossEncounter.MAX_Y - BossEncounter.MIN_Y);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
        batch.begin();
        SceneryVisuals.boundary(batch,game.visuals().scenery(),SceneryTheme.GUARDIAN,BossEncounter.MIN_X,BossEncounter.MIN_Y,
            BossEncounter.MAX_X-BossEncounter.MIN_X,BossEncounter.MAX_Y-BossEncounter.MIN_Y);
        TacticalVisuals.boss(batch,game.visuals(),boss);
        for(BossEncounter.Shot shot:boss.shots()) {
            batch.setColor(boss.enraged()?Color.SALMON:Color.WHITE);
            batch.draw(game.visuals().expanded().effect("attack-wand",(stateTime*5)%1),shot.x-5,shot.y-5,10,10);
        }
        batch.setColor(boss.flashing()?Color.SALMON:Color.WHITE);
        if(!hideEndingBoss && boss.spriteVisible()) {
            if(bossHurtVisible())bossReactionBody.draw(batch,bossReaction,bossReactionDirection,boss.x(),boss.y(),BossEncounter.DRAW_SIZE);
            else bossBody.draw(batch,EncounterAnimation.boss(boss,bossMotion),boss.spriteX(),boss.spriteY(),BossEncounter.DRAW_SIZE);
        }
        batch.setColor(Color.WHITE);
        if(!hideEndingPlayer)DivineVisuals.player(batch,game.visuals(),stats,player.x+8,player.y+8,stateTime);
        FeedbackVisuals.sprites(batch,game.visuals(),feedback);
        if(!hideEndingPlayer){player.syncReaction();player.draw(batch);}
        CombatVisuals.allies(batch, game.visuals(), session.necromancy());
        for (Projetil projectile : projectiles) projectile.draw(batch);
        DivineVisuals.effects(batch,game.visuals(),effects,java.util.List.of(boss),stateTime);
        if(!hideEndingPlayer)DivineVisuals.slash(batch,game.visuals(),slashWeapon,slashReach,slashX,slashY,slashAngle,slashTimer);
        font.getData().setScale(0.65f); font.setColor(Color.WHITE);
        for (DamageNumber number : damageNumbers) number.draw(batch, font);
        FeedbackVisuals.labels(batch,font,feedback);
        batch.end();
        drawHud();
    }
    private void drawHud() {
        hud.apply();
        shapes.setProjectionMatrix(hud.getCamera().combined);
        batch.setProjectionMatrix(hud.getCamera().combined);
        HudSettings hudSettings=game.hudSettings().value();
        hudScaleRenderer.begin(batch,shapes,hudSettings,HudLayout.Anchor.LEFT);
        try {CombatHud.player(batch,shapes,font,stats,game.visuals());}finally {hudScaleRenderer.end();}
        hudScaleRenderer.begin(batch,shapes,hudSettings,HudLayout.Anchor.RIGHT);
        try {CombatHud.boss(batch,shapes,font,boss);}finally {hudScaleRenderer.end();}
        FeedbackVisuals.damageBorder(shapes,feedback);
        batch.begin();tutorialPanel.footer(batch,font,tutorialContext());batch.end();
        String cue=boss.state()==BossEncounter.State.INTRO?"Guardião: Credencial de morto, por favor."
            :boss.state()==BossEncounter.State.TELEGRAPH?boss.attack().label:"";
        if(boss.airborne()) cue=boss.leapLocked()
            ?Float.isFinite(boss.walkingSpeedLimit())?"Pressão divina — "+game.controlSettings().value().bindings(ControlAction.DASH).get(0).label()+" + direção!":"Área travada — saia do vermelho!"
            :"Queda — o círculo segue você";
        if(!cue.isEmpty()) {
            hudScaleRenderer.begin(batch,shapes,hudSettings,HudLayout.Anchor.RIGHT);
            try {CombatHud.bossWarning(batch,shapes,font,cue,boss.leapLocked());}finally {hudScaleRenderer.end();}
        }
        if (paused) {
            Gdx.gl.glEnable(GL20.GL_BLEND); Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(0, 0, 0, 0.85f); shapes.rect(0, 0, 960, 458); shapes.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
            batch.begin();
            font.getData().setScale(1.5f); font.setColor(Color.GOLD); font.draw(batch, "PAUSA", 435, 325);
            font.getData().setScale(.95f); font.setColor(Color.WHITE); font.draw(batch, "ESC: continuar | F1: guia | F2: vídeo | F3: controles | O: áudio | M: menu", 225, 265);
            batch.end();
        }
        if (boonPanel.open()) boonPanel.draw(batch, shapes, font,game.visuals());
        if(audioPanel.open())audioPanel.draw(batch,shapes,font);
        if(tutorialPanel.open())tutorialPanel.draw(batch,shapes,font);
        if(videoPanel.open())videoPanel.draw(batch,shapes,font);
        if(controlsPanel.open())controlsPanel.draw(batch,shapes,font);
    }
    @Override public void pause() { videoPanel.suspendInput();paused = true; }
    @Override public void dispose() {
        if (disposed) return;
        disposed = true;
        videoPanel.close();
        controlsPanel.close();
        stats.setWalkingSpeedLimit(Float.POSITIVE_INFINITY);
        batch.dispose(); shapes.dispose(); font.dispose(); atlas.dispose(); map.dispose();
        projectiles.clear(); damageNumbers.clear();bossReaction.clear();effects.clear(); feedback.clear(); audioPanel.close();tutorialPanel.close();
    }
}
