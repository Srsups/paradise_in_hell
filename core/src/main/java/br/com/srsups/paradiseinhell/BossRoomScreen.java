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
    private final BossEncounter boss;
    private final FitViewport world = new FitViewport(480, 270), hud = new FitViewport(960, 540);
    private final List<Projetil> projectiles = new ArrayList<>();
    private final List<DamageNumber> damageNumbers = new ArrayList<>();
    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private BitmapFont font;
    private Texture atlas;
    private Animation<TextureRegion> animation;
    private TileMap map;
    private Jogador player;
    private boolean paused, disposed;
    private float stateTime, hitCooldown, slashTimer, slashAngle, slashX, slashY;

    public BossRoomScreen(Main game, RunSession session) {
        if (session.isDemo() || session.arena().kind() != ArenaDefinition.Kind.BOSS_ROOM)
            throw new IllegalStateException("Not a boss room");
        this.game = game; this.session = session; this.stats = session.player();
        boss = new BossEncounter(session.enemyDifficulty());
    }
    @Override public void show() {
        Gdx.input.setInputProcessor(null);
        batch = new SpriteBatch(); shapes = new ShapeRenderer(); font = new BitmapFont();
        atlas = new Texture(Art.ATLAS);
        atlas.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        animation = new Animation<>(0.25f, Art.cell(atlas, 2, 1), Art.cell(atlas, 3, 1));
        animation.setPlayMode(Animation.PlayMode.LOOP);
        map = new TileMap(atlas);
        map.gerarAreaInicialSegura(240, 112, 64);
        map.setBounds(BossEncounter.MIN_X, BossEncounter.MIN_Y,
            BossEncounter.MAX_X - BossEncounter.MIN_X, BossEncounter.MAX_Y - BossEncounter.MIN_Y);
        player = new Jogador(232, 48, atlas, stats, this::attack);
        world.getCamera().position.set(240, 135, 0); world.getCamera().update();
        map.update(world.getCamera());
    }
    @Override public void resize(int width, int height) {
        world.update(width, height); hud.update(width, height, true);
        world.getCamera().position.set(240, 135, 0); world.getCamera().update();
        if (map != null) map.update(world.getCamera());
    }
    @Override public void render(float delta) {
        if (disposed) return;
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
        session.advance(delta);
        stateTime += delta;
        hitCooldown = Math.max(0, hitCooldown - delta);
        slashTimer = Math.max(0, slashTimer - delta);
        player.update(delta, map);
        for (Projetil projectile : projectiles) {
            float beforeX = projectile.x, beforeY = projectile.y;
            projectile.update(delta, map);
            if (!projectile.deveSerRemovido && CombatGeometry.sweptCircleHits(beforeX, beforeY,
                    projectile.x, projectile.y, boss.x(), boss.y(), BossEncounter.RADIUS + 3)) {
                hurtBoss(projectile.dano); projectile.deveSerRemovido = true;
            }
        }
        projectiles.removeIf(projectile -> projectile.deveSerRemovido);
        if (stats.possuiAura()) {
            float radius = stats.getRaioAura() + BossEncounter.RADIUS;
            if (Vector2.dst2(player.x + 8, player.y + 8, boss.x(), boss.y()) <= radius * radius)
                boss.hurt(stats.getDanoAura() * delta);
        }
        float damage = boss.update(delta, player.x + 8, player.y + 8);
        if (damage > 0 && hitCooldown <= 0) {
            stats.sofrerDano(damage); hitCooldown = 0.35f;
        }
        for (DamageNumber number : damageNumbers) number.update(delta);
        damageNumbers.removeIf(DamageNumber::deveSerRemovido);
        if (stats.estaMorto()) {
            if (stats.possuiRessurreicao()) { stats.usarRessurreicao(); hitCooldown = 0.35f; }
            else { finish(false); return; }
        }
        if (boss.defeated()) finish(true);
    }
    private void attack(float x, float y) {
        if (boss.defeated()) return;
        Vector2 direction = world.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY())).sub(x, y);
        if (direction.isZero()) return;
        WeaponType weapon = stats.getWeapon();
        if (weapon.isMelee()) {
            slashX = x; slashY = y; slashAngle = direction.angleDeg(); slashTimer = weapon == WeaponType.FISTS ? 0.10f : 0.14f;
            Vector2 closest = new Vector2(boss.x() - x, boss.y() - y);
            float distance = closest.len();
            closest.nor().scl(Math.max(0, distance - BossEncounter.RADIUS));
            if (WeaponAttack.meleeHits(x, y, direction.x, direction.y, x + closest.x, y + closest.y, stats.meleeReach(), weapon.halfAngle())) hurtBoss(stats.getDano());
        } else {
            Projetil projectile = new Projetil(x, y, direction.nor(), Art.cell(atlas, 0, 3), weapon);
            projectile.configure(stats); projectiles.add(projectile);
        }
    }
    private void hurtBoss(float damage) {
        if (boss.defeated()) return;
        boss.hurt(damage);
        damageNumbers.add(new DamageNumber(CombatNumbers.format(damage), boss.x(), boss.y() + 28));
    }
    private void finish(boolean victory) {
        if (victory) {
            session.recordBossDefeat();
            stats.curar(stats.getCuraPorAbate());
        }
        game.finishRun(session, victory ? RunOutcome.VICTORY : RunOutcome.DEATH);
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
        boolean warning = boss.state() == BossEncounter.State.TELEGRAPH;
        if (warning) shapes.setColor(1, 0.65f, 0.1f, 0.22f);
        else shapes.setColor(1, 0.15f, 0.1f, 0.38f);
        for (BossEncounter.Mark mark : boss.marks()) shapes.circle(mark.x(), mark.y(), mark.radius(), 32);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(Color.SLATE);
        shapes.rect(BossEncounter.MIN_X, BossEncounter.MIN_Y, BossEncounter.MAX_X - BossEncounter.MIN_X, BossEncounter.MAX_Y - BossEncounter.MIN_Y);
        shapes.setColor(warning ? Color.ORANGE : Color.SCARLET);
        for (BossEncounter.Mark mark : boss.marks()) {
            shapes.circle(mark.x(), mark.y(), mark.radius(), 32);
            if (warning) shapes.circle(mark.x(), mark.y(), mark.radius() * boss.warningProgress(), 32);
        }
        if ((warning || boss.state() == BossEncounter.State.ACTIVE) && boss.attack() == BossEncounter.Attack.CHARGE)
            drawChargeLane();
        if (warning && boss.attack() == BossEncounter.Attack.VOLLEY) {
            float angle = (float) Math.toDegrees(boss.aimAngle());
            shapes.arc(boss.x(), boss.y(), 58, angle - 55, 110, 24);
            float directionX = (float) Math.cos(boss.aimAngle()), directionY = (float) Math.sin(boss.aimAngle());
            shapes.line(boss.x(), boss.y(), boss.x() + directionX * 82, boss.y() + directionY * 82);
        }
        if (stats.possuiAura()) {
            shapes.setColor(Color.CYAN); shapes.circle(player.x + 8, player.y + 8, stats.getRaioAura(), 32);
        }
        if (stats.isEscudoAtivo()) {
            shapes.setColor(Color.GOLD); shapes.circle(player.x + 8, player.y + 8, 12, 20);
        }
        if (slashTimer > 0) {
            shapes.setColor(Color.GOLD);
            if (stats.getWeapon() == WeaponType.FISTS) {
                float angle = (float) Math.toRadians(slashAngle);
                shapes.circle(slashX + (float) Math.cos(angle) * stats.meleeReach(),
                    slashY + (float) Math.sin(angle) * stats.meleeReach(), 5, 12);
            } else shapes.arc(slashX, slashY, stats.swordReach(), slashAngle - WeaponAttack.HALF_ANGLE_DEGREES,
                WeaponAttack.HALF_ANGLE_DEGREES * 2, 18);
        }
        shapes.end();
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(Color.SCARLET);
        for (BossEncounter.Shot shot : boss.shots()) shapes.circle(shot.x, shot.y, 3.5f, 12);
        shapes.end();
        batch.begin();
        batch.setColor(boss.flashing() ? Color.WHITE : boss.enraged() ? Color.SCARLET : Color.GOLD);
        batch.draw(animation.getKeyFrame(stateTime), boss.x() - 23, boss.y() - 23, 46, 46);
        batch.setColor(Color.WHITE);
        player.draw(batch);
        for (Projetil projectile : projectiles) projectile.draw(batch);
        font.getData().setScale(0.65f); font.setColor(Color.WHITE);
        for (DamageNumber number : damageNumbers) number.draw(batch, font);
        batch.end();
        drawHud();
    }
    private void drawChargeLane() {
        float dx = boss.endX() - boss.startX(), dy = boss.endY() - boss.startY();
        float length = Math.max(0.001f, (float) Math.sqrt(dx * dx + dy * dy));
        float sideX = -dy / length * (BossEncounter.RADIUS + BossEncounter.PLAYER_RADIUS);
        float sideY = dx / length * (BossEncounter.RADIUS + BossEncounter.PLAYER_RADIUS);
        shapes.line(boss.startX() + sideX, boss.startY() + sideY, boss.endX() + sideX, boss.endY() + sideY);
        shapes.line(boss.startX() - sideX, boss.startY() - sideY, boss.endX() - sideX, boss.endY() - sideY);
        shapes.line(boss.endX() - sideX, boss.endY() - sideY, boss.endX() + sideX, boss.endY() + sideY);
    }
    private void drawHud() {
        hud.apply();
        shapes.setProjectionMatrix(hud.getCamera().combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.045f, 0.055f, 0.085f, 1); shapes.rect(0, 458, 960, 82);
        bar(18, 510, 200, stats.getVida() / stats.getVidaMaxima(), Color.SCARLET);
        bar(18, 485, 200, stats.getEstaminaAtual() / stats.getEstaminaMaxima(), Color.GREEN);
        bar(530, 485, 400, boss.health() / boss.maxHealth(), boss.enraged() ? Color.SCARLET : Color.GOLD);
        shapes.end();
        batch.setProjectionMatrix(hud.getCamera().combined);
        batch.begin();
        font.getData().setScale(1); font.setColor(Color.WHITE);
        font.draw(batch, "Vida: " + CombatNumbers.format(Math.max(0, stats.getVida())) + " / " + CombatNumbers.format(stats.getVidaMaxima()), 240, 523);
        font.draw(batch, "Nível " + stats.getNivel() + "   |   " + stats.getProfession().title, 240, 495);
        font.setColor(Color.GOLD);
        font.draw(batch, BossEncounter.NAME + "   |   Fase " + (boss.enraged() ? 2 : 1), 530, 523);
        font.setColor(Color.WHITE); font.getData().setScale(0.85f);
        font.draw(batch, CombatNumbers.format(boss.health()) + " / " + CombatNumbers.format(boss.maxHealth()), 675, 497);
        font.getData().setScale(1);
        String cue = boss.state() == BossEncounter.State.INTRO ? "Guardião: Credencial de morto, por favor."
            : boss.state() == BossEncounter.State.TELEGRAPH ? "PREPARE-SE: " + boss.attack().label
            : boss.state() == BossEncounter.State.ACTIVE ? boss.attack().label : "Recuperação: aproveite para atacar!";
        font.setColor(boss.state() == BossEncounter.State.TELEGRAPH ? Color.ORANGE : Color.WHITE);
        font.draw(batch, cue, 18, 445);
        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, stats.getWeapon().label + " (" + stats.getWeaponRarity().label + ")   |   Mouse: atacar   |   SHIFT: dash   |   ESC: pausa", 18, 25);
        batch.end();
        if (paused) {
            Gdx.gl.glEnable(GL20.GL_BLEND); Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(0, 0, 0, 0.85f); shapes.rect(0, 0, 960, 458); shapes.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
            batch.begin();
            font.getData().setScale(1.5f); font.setColor(Color.GOLD); font.draw(batch, "PAUSA", 435, 325);
            font.getData().setScale(1); font.setColor(Color.WHITE); font.draw(batch, "ESC: continuar   |   M: menu", 345, 265);
            batch.end();
        }
    }
    private void bar(float x, float y, float width, float fraction, Color color) {
        shapes.setColor(Color.DARK_GRAY); shapes.rect(x, y, width, 15);
        shapes.setColor(color); shapes.rect(x, y, width * Math.max(0, Math.min(1, fraction)), 15);
    }
    @Override public void pause() { paused = true; }
    @Override public void dispose() {
        if (disposed) return;
        disposed = true;
        batch.dispose(); shapes.dispose(); font.dispose(); atlas.dispose(); map.dispose();
        projectiles.clear(); damageNumbers.clear();
    }
}
