package br.com.srsups.paradiseinhell.lwjgl3;

import br.com.srsups.paradiseinhell.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

/** Real OpenGL integration checks in an invisible window; no simulated renderer. */
public final class DemoSmokeTest {
    private static Throwable failure;
    private static final SaveSlots TEST_SAVES = SaveSlots.memory();

    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) return;
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setWindowedMode(960, 540);
        config.setInitialVisible(false);
        config.disableAudio(true);
        config.setForegroundFPS(60);
        new Lwjgl3Application(new Main(TEST_SAVES) {
            @Override public void startCampaign() {
                CampaignProfile p = profiles().profile();
                setScreen(new GameScreen(this, RunSession.campaign(campaignRegion(), p.profession(), p.bonuses(), p.difficulty(), new Rolls(0.5f))));
            }
            @Override public void render() {
                try {
                    verify(this);
                } catch (Throwable error) { failure = error; }
                Gdx.app.exit();
            }
        }, config);
        if (failure != null) throw new AssertionError("Demo integration check failed", failure);
        System.out.println("DEMO_SMOKE_OK: menu, upgrades, pause, resize, victory, defeat, retry, professions, weapons, XP by type, kill goals, continuous waves, boss access, boss attacks, phase two, boss weapons, resurrection, refuge, dialogue, permanent purchases, reset, profile reload, chests, save slots, locked profession, timed notice, magnet pickup, percentage bonuses, enemy scaling, mini-boss spin/stones, persistent material, blacksmith forging, rare weapon powers, fourth save, fists, weighted boon rarities, permanent region material cap, permanent forged weapons, route rewards, fixed arena variations, eight save slots, second profession page, legendary unlock, spell splash, curses, resurrected allies, shield guard and retaliation, elemental synergies, divine fury, boss leap, continuous tracking circle, red lock, airborne immunity, phase-two walking pressure, timed dash escape, visible sprite contact, attacks per second, protected boon input, paged Tab boon list, area curses, shared Souls, 1000-Soul legendary unlock, run reports, damage origins, report history browsing, report persistence, Stygian archers, fixed aim, arrow walls, shield arrow retaliation, archer rewards, archer weapons for all eight professions, simultaneous guard and attacks in hordes and boss, confirmed save reset, cancelled reset, slot reuse, reset on second page, shared progress preserved, character history isolation, fast burst recovery and crowd caps, audio controls and persistence, mute and live gain, audio panel freezes both arenas and choice input, weapon sounds and hit effects, actual block/hurt/invulnerability cues, boss warning once, cleanup, fixed difficulty creation and cancellation, reward fractions, difficulty persistence and reports");
    }

    private static void verify(Main game) throws Exception {
        game.getScreen().render(1f / 60);
        screenshot("menu");
        press(game.getScreen(), Input.Keys.ENTER);
        check(game.getScreen() instanceof SaveSelectionScreen, "First game must choose a save");
        game.getScreen().render(0); screenshot("saves");
        press(game.getScreen(), Input.Keys.NUM_1);
        check(game.getScreen() instanceof ProfessionScreen, "Demo must start with the narrator profession question");
        game.getScreen().render(0);
        screenshot("professions");
        press(game.getScreen(), Input.Keys.NUM_1);
        confirmCharacter(game);
        GameScreen screen = (GameScreen) game.getScreen();
        PlayerState player = (PlayerState) field(screen, "stats");
        RunProgress run = (RunProgress) field(screen, "progress");
        for (int frame = 0; frame < 120; frame++) screen.render(1f / 60);
        screenshot("gameplay");
        // Render real textures and exercise viewport changes without recreating the run.
        screen.resize(800, 600);
        screen.render(1f / 60);
        screen.resize(960, 540);

        player.ganharXP(100);
        screen.render(1f / 60);
        check(field(screen, "state").toString().equals("LEVEL_UP"), "XP must open the choice screen");
        screenshot("upgrades");
        int selections = 0;
        while (field(screen, "state").toString().equals("LEVEL_UP") && selections++ < 12) {
            press(screen, Input.Keys.NUM_1);
        }
        check(player.getNivel() >= 5, "Multiple earned levels must not discard their selections");
        check(field(screen, "state").toString().equals("PLAYING"), "Choice queue must finish");

        press(screen, Input.Keys.ESCAPE);
        float pausedAt = run.elapsed();
        for (int frame = 0; frame < 120; frame++) screen.render(1f / 60);
        check(run.elapsed() == pausedAt, "Pause must freeze simulation time");
        screenshot("pause");
        press(screen, Input.Keys.ESCAPE);
        check(run.elapsed() > pausedAt, "Resume must advance simulation time");

        // Repeated resistance must never convert incoming damage into healing.
        for (int count = 0; count < 20; count++) player.aumentarResistencia(0.1f);
        set(player, "escudoAtivo", false);
        set(player, "protecaoRessurreicao", 0f);
        float lifeBeforeDamage = player.getVida();
        player.sofrerDano(20);
        check(player.getVida() < lifeBeforeDamage, "Resistance must still take positive damage");
        set(run, "elapsed", 299.99f);
        screen.render(0.05f);
        check(game.getScreen() instanceof GameOverScreen, "Survival must reach a result screen");
        check((boolean) field(game.getScreen(), "victory"), "Survival must count as victory");
        game.getScreen().render(0.05f);
        screenshot("victory");
        check((boolean) field(screen, "disposed"), "Previous combat screen must be disposed");

        for (int frame = 0; frame < 30; frame++) game.getScreen().render(1f / 60);
        press(game.getScreen(), Input.Keys.ENTER);
        check(game.getScreen() instanceof GameScreen, "Enter must start a fresh run");
        screen = (GameScreen) game.getScreen();
        player = (PlayerState) field(screen, "stats");
        check(player.getNivel() == 1, "Retry must reset progress");
        player.sofrerDano(10000);
        screen.render(0.05f);
        check(game.getScreen() instanceof GameOverScreen, "Death must transition safely");
        check(!(boolean) field(game.getScreen(), "victory"), "Death must count as defeat");
        game.getScreen().render(0.05f);
        screenshot("defeat");
        for (int frame = 0; frame < 30; frame++) game.getScreen().render(1f / 60);
        press(game.getScreen(), Input.Keys.ESCAPE);
        check(game.getScreen() instanceof MenuScreen, "Escape must return to menu");
        game.getScreen().render(1f / 60);
        verifyCampaign(game);
        verifyCombatReadability(game);
        verifyAthenaExperience(game);
        verifySharedSoulsAndReports(game);
        verifyArchers(game);
        verifyRequestedChanges();
        verifyAudioAndFeedback();
        verifyMilestoneOne();
        verifyMilestoneTwo();
        verifyMilestoneThree();
        verifyExpandedVisuals();
        verifyEquippedVisuals();
    }
    @SuppressWarnings("unchecked")
    private static void verifyCampaign(Main game) throws Exception {
        press(game.getScreen(), Input.Keys.C);
        check(game.getScreen() instanceof SaveSelectionScreen, "Campaign must select a save");
        press(game.getScreen(), Input.Keys.NUM_2);
        check(game.getScreen() instanceof ProfessionScreen, "An empty save must ask the profession");
        press(game.getScreen(), Input.Keys.NUM_3);
        confirmCharacter(game);
        check(game.getScreen() instanceof RefugeScreen, "Campaign profession must lead to the refuge");
        game.getScreen().render(0); screenshot("refuge");
        press(game.getScreen(), Input.Keys.C);
        screenshot("caronte");
        finishDialogue((RefugeScreen)game.getScreen());
        check(game.profiles().profile().has(CampaignProfile.Event.CARONTE_MET)
            && game.profiles().profile().obols() == 5, "Caronte must give his first gift once");
        check(!field(game.getScreen(), "notice").toString().isEmpty(), "Conversation must show a brief acknowledgement");
        game.getScreen().render(4);
        check(field(game.getScreen(), "notice").toString().isEmpty(), "Caronte acknowledgement must expire");
        press(game.getScreen(), Input.Keys.ENTER);
        GameScreen screen = (GameScreen) game.getScreen();
        RunSession session = (RunSession) field(screen, "session");
        PlayerState player = session.player();
        check(player.getProfession() == Profession.VIGILANTE, "Third profession must start with the pistol");
        session.applyUpgrade(Upgrade.FORCA); session.applyUpgrade(Upgrade.POSEIDON); session.applyUpgrade(Upgrade.RESSURREICAO);
        player.sofrerDano(20);
        fire(screen);
        java.util.List<Projetil> bullets = (java.util.List<Projetil>) field(screen, "projectiles");
        check(!bullets.isEmpty() && Math.abs(bullets.get(0).velocidade.len() - 420) < 0.1f, "Pistol must use its actual firing speed");
        screenshot("campaign");
        java.util.List<Inimigo> enemies = (java.util.List<Inimigo>) field(screen, "enemies");
        com.badlogic.gdx.graphics.Texture atlas = (com.badlogic.gdx.graphics.Texture) field(screen, "atlas");
        for (EnemyType type : EnemyType.values()) {
            Inimigo enemy = new Inimigo(180 + type.ordinal() * 20, 180, atlas);
            enemy.configurarTipo(type); enemy.sofrerDano(1000); enemies.add(enemy);
        }
        screen.render(0);
        java.util.List<OrbeXP> orbs = (java.util.List<OrbeXP>) field(screen, "orbs");
        check(orbs.stream().mapToInt(orb -> orb.valorXP).sum() == 8, "Common, fast and tank must drop 1, 2 and 5 XP");
        check(session.progress().kills() == 3, "Each defeated enemy must count once by its type");
        screen.render(0);
        check(session.progress().kills() == 3, "Removed enemies must not be counted twice");
        session.advance(400);
        screen.render(0);
        check(!session.arenaComplete() && game.getScreen() == screen, "Elapsed time must not unlock the boss");
        player.ganharXP(9);
        meetGoals(session);
        check(session.arenaComplete(), "All three quotas must unlock access");
        int before = enemies.size();
        set(screen, "spawnTimer", session.spawnInterval() * 2);
        screen.render(0);
        check(enemies.size() > before, "Hordes must continue after goals until the player chooses to leave");
        check(game.getScreen() == screen && field(screen, "state").toString().equals("PLAYING"), "Living enemies must not block unlocked access");
        screenshot("boss-unlocked");
        press(screen, Input.Keys.E);
        check(game.getScreen() == screen && field(screen, "state").toString().equals("LEVEL_UP"), "Leaving must collect XP and resolve pending upgrades");
        check(player.getNivel() == 2 && player.getXpAtual() == 7, "Remaining weighted XP must be preserved");
        press(screen, Input.Keys.NUM_1);
        check(game.getScreen() instanceof RouteScreen, "Resolving exit rewards must show available destinations");
        check((boolean) field(screen, "disposed"), "Exit must release arena resources");
        float carriedLife = player.getVida(), carriedDamage = player.getDano(); int carriedRank = session.rank(Upgrade.FORCA);
        game.getScreen().render(0.3f); screenshot("routes");
        press(game.getScreen(), Input.Keys.NUM_3);
        screen = (GameScreen) game.getScreen();
        check(field(screen, "session") == session && field(screen, "stats") == player, "Optional path must keep the same attempt and stats");
        check(session.arena().id().equals("sombras") && session.kills(EnemyType.COMMON) == 0, "New combat path must start its own goals");
        check(player.getVida() == carriedLife && player.getDano() == carriedDamage && player.possuiAura()
            && player.possuiRessurreicao() && session.rank(Upgrade.FORCA) == carriedRank, "Stats and powers must survive path selection");
        check(player.getWeapon() == WeaponType.PISTOL, "Profession weapon must survive transitions");
        screen.render(0); screenshot("campaign-next");
        meetGoals(session);
        press(screen, Input.Keys.E);
        check(game.getScreen() instanceof ArenaRewardScreen, "Sombras must grant its rare boon before the route");
        game.getScreen().render(0.3f);
        press(game.getScreen(), Input.Keys.NUM_1);
        check(game.getScreen() instanceof RouteScreen, "Claiming the extra boon must resume route selection");
        game.getScreen().render(0.3f);
        press(game.getScreen(), Input.Keys.NUM_1);
        check(game.getScreen() instanceof BossRoomScreen, "Completed goals must allow entering the boss room");
        check(!session.progress().won(), "Boss access alone must not count as campaign victory");
        verifyBossBattle(game, session);
        game.getScreen().render(0.5f);
        press(game.getScreen(), Input.Keys.ENTER);
        check(game.getScreen() instanceof RefugeScreen, "Campaign result must return to the refuge");
        press(game.getScreen(), Input.Keys.ENTER);
        screen = (GameScreen) game.getScreen();
        RunSession retry = (RunSession) field(screen, "session");
        check(retry.player().getProfession() == Profession.VIGILANTE && retry.arena().id().equals("estige"), "Retry must retain profession and return to start");
        check(retry.player().getVida() == Profession.VIGILANTE.baseHealth && retry.rank(Upgrade.FORCA) == 0 && retry.progress().kills() == 0, "Retry must clear temporary gains and kill goals");
        retry.player().sofrerDano(10000); screen.render(0);
        check(game.getScreen() instanceof GameOverScreen && !(boolean) field(game.getScreen(), "victory"), "Campaign death must end the attempt");
        game.getScreen().render(0.5f); press(game.getScreen(), Input.Keys.ENTER);
        check(game.getScreen() instanceof RefugeScreen, "Death must return to the refuge");
        press(game.getScreen(), Input.Keys.ENTER);
        check(((RunSession) field(game.getScreen(), "session")).player().getProfession() == Profession.VIGILANTE, "Death retry must preserve the chosen profession");
        verifySword(game);
        verifyBossWeapons(game);
        verifyBossResurrection(game);
        verifyMetaProgress(game);
        verifyChests(game);
        verifySaveLockAndMagnet(game);
        verifyMiniBossAndForge(game);
        verifyRareWeapons(game);
        verifyLuckyFistsAndRegionCap(game);
        verifyArenaRewards(game);
        verifyNewProfessionsAndCombat(game);
        verifyBossLeap(game);
    }

    private static void verifyBossBattle(Main game, RunSession session) throws Exception {
        BossRoomScreen room = (BossRoomScreen) game.getScreen();
        BossEncounter boss = (BossEncounter) field(room, "boss");
        PlayerState stats = session.player();
        float carriedLife = stats.getVida();
        check(field(room, "stats") == stats && stats.possuiAura() && stats.possuiRessurreicao(),
            "The boss must retain the same player and powers");
        room.render(0); screenshot("boss-room");
        press(room, Input.Keys.ENTER);
        check(game.getScreen() == room && !session.bossDefeated(), "Enter must not bypass the boss fight");
        check(stats.getVida() == carriedLife, "Entering the boss must not reset or heal the player");
        press(room, Input.Keys.ESCAPE);
        float pausedAt = session.progress().elapsed(), bossPausedAt = boss.elapsed();
        for (int frame = 0; frame < 120; frame++) room.render(1f / 60);
        check(session.progress().elapsed() == pausedAt && boss.elapsed() == bossPausedAt,
            "Pause must freeze both the attempt and boss attacks");
        screenshot("boss-pause");
        press(room, Input.Keys.ESCAPE);
        check(boss.elapsed() > bossPausedAt, "Boss attacks must resume");
        room.resize(800, 600); room.render(0); room.resize(960, 540);

        renderUntil(game, room, boss, BossEncounter.Attack.MARKS, BossEncounter.State.TELEGRAPH);
        screenshot("boss-marks");
        Jogador actor = (Jogador) field(room, "player");
        actor.x = 64; actor.y = 176; // Move away from the locked warning.
        renderUntil(game, room, boss, BossEncounter.Attack.CHARGE, BossEncounter.State.TELEGRAPH);
        screenshot("boss-charge");
        actor.x = 392; actor.y = 40; // Dodge the marked charge.
        boss.hurt(boss.health() - boss.maxHealth() / 2);
        renderUntil(game, room, boss, BossEncounter.Attack.VOLLEY, BossEncounter.State.TELEGRAPH);
        screenshot("boss-phase-two");
        renderUntil(game, room, boss, BossEncounter.Attack.VOLLEY, BossEncounter.State.ACTIVE);
        check(boss.shots().size() == 9, "The second phase must emit nine real projectiles");
        room.render(0); screenshot("boss-volley");

        actor.x = boss.x() + 38 - 8; actor.y = boss.y() - 8;
        // Randomly selected elemental boons may also proc from the aura: isolate its own damage here.
        int shockRank = (int) field(stats, "shockRank"), burnRank = (int) field(stats, "burnRank");
        set(stats, "shockRank", 0); set(stats, "burnRank", 0);
        ((CombatEffects) field(room, "effects")).clear();
        float beforeAura = boss.health();
        try {
            room.render(0.05f);
            check(Math.abs(beforeAura - boss.health() - stats.getDanoAura() * 0.05f) < 0.01f,
                "The carried aura must damage the boss in range");
        } finally { set(stats, "shockRank", shockRank); set(stats, "burnRank", burnRank); }
        set(stats, "auraDePoseidonAtiva", false); // Isolate the lethal weapon hit from aura damage.
        actor.x = boss.x() + 40 - 8; actor.y = boss.y() - 8;
        set(stats, "cooldownTiro", 0f);
        boss.hurt(boss.health() - 1);
        int killsBefore = session.progress().kills();
        fireAtBoss(room, boss);
        for (int frame = 0; frame < 5 && !boss.defeated(); frame++) room.render(.02f);
        check(game.getScreen() instanceof GameOverScreen && (boolean) field(game.getScreen(), "victory"),
            "A real weapon hit must defeat the boss and show victory");
        check(session.bossDefeated() && session.progress().kills() == killsBefore + 1,
            "Boss defeat must count exactly once");
        check((boolean) field(room, "disposed"), "Boss victory must release its graphics resources");
        game.getScreen().render(0); screenshot("boss-victory");
    }

    private static RunSession bossSession(Profession profession) {
        RegionDefinition region = RegionDefinition.fromJson(
            Gdx.files.internal(RegionDefinition.PROTOTYPE_ASSET).readString("UTF-8"));
        RunSession session = RunSession.campaign(region, profession);
        meetGoals(session); session.enterArena("portoes");
        return session;
    }


    private static void verifyBossLeap(Main game) throws Exception {
        for (int scenario = 0; scenario < 3; scenario++) {
            boolean powered = scenario > 0, dash = scenario == 2;
            RunSession session = bossSession(powered ? Profession.GUERREIRO : Profession.CACADOR);
            BossRoomScreen room = new BossRoomScreen(game, session); game.setScreen(room);
            BossEncounter boss = (BossEncounter) field(room, "boss");
            Jogador actor = (Jogador) field(room, "player");
            actor.x = 172; actor.y = 92;
            if (powered) boss.hurt(boss.maxHealth() / 2);
            for (int frame = 0; frame < 3000 && !boss.airborne(); frame++)
                boss.update(.01f, actor.x + 8, actor.y + 8);
            check(boss.airborne(), "The fourth pattern must be the leap");
            for (int frame = 0; frame < 6; frame++) room.render(.05f);
            check(!boss.spriteVisible(), "Boss sprite must leave the arena during the leap");
            float health = boss.health();
            // A real projectile and aura must not damage the old, empty position.
            actor.x = boss.x() - 8; actor.y = boss.y() - 8;
            session.player().ativarAuraDePoseidon();
            fireAtBoss(room, boss);
            check(boss.health() == health && !boss.targetable(), "Airborne boss must be unreachable by weapons and aura");
            set(session.player(), "auraDePoseidonAtiva", false);

            float markerX = boss.leapX(), markerY = boss.leapY();
            actor.x = 392; actor.y = 172;
            room.render(.05f);
            check(Math.hypot(boss.leapX() - markerX, boss.leapY() - markerY) <= boss.leapTrackSpeed() * .05f + .001f,
                "Tracking marker must not teleport to the player's new position");
            screenshot(powered ? "boss-leap-phase-two-tracking" : "boss-leap-tracking");
            press(room, Input.Keys.ESCAPE);
            markerX = boss.leapX(); markerY = boss.leapY();
            float radius = boss.leapRadius(), remaining = boss.leapSecondsRemaining();
            room.render(.05f); room.resize(800,600); room.render(.05f); room.resize(960,540);
            check(boss.leapX() == markerX && boss.leapY() == markerY && boss.leapRadius() == radius
                && boss.leapSecondsRemaining() == remaining, "Pause and resize must preserve the entire leap warning");
            screenshot("boss-leap-paused");
            actor.x = 172; actor.y = 92; press(room, Input.Keys.ESCAPE);
            for (int frame = 0; frame < 300 && !boss.leapLocked(); frame++) room.render(.01f);
            check(boss.leapLocked() && boss.airborne() && boss.leapSecondsRemaining() <= 1.00001f
                && boss.leapSecondsRemaining() >= .989f, "The marker must turn red one second before landing");
            check(Math.abs(boss.leapX() - 180) < .01f && Math.abs(boss.leapY() - 100) < .01f, "The marker must catch a stationary player before locking");
            check(powered ? session.player().movementSpeed() <= BossEncounter.LEAP_WALKING_LIMIT
                : session.player().movementSpeed() == Profession.CACADOR.baseSpeed, "Only phase-two red warning must slow walking");
            screenshot(powered ? "boss-leap-phase-two-red" : "boss-leap-red");
            markerX = boss.leapX(); markerY = boss.leapY();
            float before = session.player().getVida();
            Input actual = Gdx.input;
            for (int frame = 0; frame < 25 && boss.airborne(); frame++) {
                boolean first = frame == 0;
                Gdx.input = (Input) Proxy.newProxyInstance(Input.class.getClassLoader(), new Class<?>[]{Input.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("isKeyPressed")) return (int) args[0] == Input.Keys.D;
                        if (method.getName().equals("isKeyJustPressed")) return dash && first && (int) args[0] == Input.Keys.SHIFT_LEFT;
                        if (method.getName().equals("isButtonPressed") || method.getName().equals("isButtonJustPressed")) return false;
                        return method.invoke(actual, args);
                    });
                try { room.render(.05f); } finally { Gdx.input = actual; }
            }
            check(!boss.airborne(), "The boss must land after the final second");
            check(boss.leapX() == markerX && boss.leapY() == markerY, "Red marker must remain fixed while the player moves or dashes");
            check(boss.x() == markerX && boss.y() == markerY, "Boss must land at the marker's center");
            check(powered && !dash ? session.player().getVida() < before : session.player().getVida() == before,
                "Phase one permits walking; phase two requires a dash to escape the impact");
            check(session.player().movementSpeed() == session.player().getProfession().baseSpeed, "Landing must release the temporary walking penalty");
            screenshot(powered ? dash ? "boss-leap-phase-two-dodged" : "boss-leap-phase-two-impact" : "boss-leap-landed");
            game.finishRun(session, RunOutcome.ABANDONED);
        }
    }

    private static void verifyBossWeapons(Main game) throws Exception {
        for (Profession profession : Profession.values()) {
            RunSession session = bossSession(profession);
            BossRoomScreen room = new BossRoomScreen(game, session);
            game.setScreen(room);
            BossEncounter boss = (BossEncounter) field(room, "boss");
            Jogador actor = (Jogador) field(room, "player");
            actor.x = boss.x() + 40 - 8; actor.y = boss.y() - 8;
            for (int frame = 0; frame < 8 && boss.health() == boss.maxHealth(); frame++)
                fireAtBoss(room, boss);
            check(boss.health() == boss.maxHealth() - profession.weapon.damage,
                profession + " must deal its weapon's real damage to the boss");
            screenshot("boss-" + profession.name().toLowerCase(java.util.Locale.ROOT));
        }
    }

    private static void verifyBossResurrection(Main game) throws Exception {
        game.selectSave(2, false); press(game.getScreen(), Input.Keys.NUM_2); confirmCharacter(game);
        RunSession session = bossSession(Profession.FERREIRO);
        session.player().ganharRessurreicao(); session.player().sofrerDano(session.player().getVida() - 1);
        BossRoomScreen room = new BossRoomScreen(game, session);
        game.setScreen(room);
        BossEncounter boss = (BossEncounter) field(room, "boss");
        renderUntil(game, room, boss, BossEncounter.Attack.MARKS, BossEncounter.State.ACTIVE);
        room.render(0.05f);
        check(game.getScreen() == room && !session.player().possuiRessurreicao()
            && session.player().getVida() == 65, "A lethal boss attack must consume resurrection and restore half health");
        session.player().sofrerDano(10000);
        check(session.player().getVida() == 65, "Resurrection must protect against immediate repeat damage");
        session.player().advance(2.1f); session.player().sofrerDano(10000);
        room.render(0);
        check(game.getScreen() instanceof GameOverScreen && !(boolean) field(game.getScreen(), "victory")
            && !session.bossDefeated(), "Death without another resurrection must end in defeat");
        check((boolean) field(room, "disposed"), "Boss defeat must release resources");
        game.getScreen().render(0.5f); press(game.getScreen(), Input.Keys.ENTER);
        check(game.getScreen() instanceof RefugeScreen, "Boss death must return to the refuge");
        press(game.getScreen(), Input.Keys.ENTER);
        RunSession retry = (RunSession) field(game.getScreen(), "session");
        check(retry.player().getProfession() == Profession.FERREIRO && !retry.player().possuiRessurreicao()
            && !retry.bossDefeated(), "A fresh attempt must preserve profession and reset boss progression");
    }

    private static void renderUntil(Main game, BossRoomScreen room, BossEncounter boss,
            BossEncounter.Attack attack, BossEncounter.State state) {
        for (int frame = 0; frame < 1000; frame++) {
            check(game.getScreen() == room, "The test must stay alive until " + attack + "/" + state);
            if (boss.attack() == attack && boss.state() == state) return;
            room.render(0.02f);
        }
        throw new AssertionError("Boss did not reach " + attack + "/" + state);
    }

    private static void fireAtBoss(BossRoomScreen room, BossEncounter boss) throws Exception {
        var viewport = (com.badlogic.gdx.utils.viewport.FitViewport) field(room, "world");
        var target = viewport.project(new com.badlogic.gdx.math.Vector2(boss.x(), boss.y()));
        fire(room, Math.round(target.x), Gdx.graphics.getHeight() - Math.round(target.y));
    }


    private static void verifyMetaProgress(Main game) throws Exception {
        GameScreen screen = (GameScreen) game.getScreen();
        ((RunSession) field(screen, "session")).player().sofrerDano(10000); screen.render(0);
        game.getScreen().render(0.5f); press(game.getScreen(), Input.Keys.ENTER);
        game.selectSave(1, false);
        RefugeScreen refuge = (RefugeScreen) game.getScreen();
        int coins = game.profiles().profile().obols();
        check(coins >= 10, "Boss victory must provide spendable permanent currency");
        press(refuge, Input.Keys.L); press(refuge, Input.Keys.NUM_1); press(refuge, Input.Keys.NUM_2);
        check(game.profiles().profile().obols() == coins - 10 && game.profiles().profile().bonuses().vitality() == 1
            && game.profiles().profile().bonuses().strength() == 1, "Shop must spend exact costs and apply persistent ranks");
        refuge.render(0); screenshot("permanent-shop");
        refuge.resize(800, 600); refuge.render(0); refuge.resize(960, 540);
        press(refuge, Input.Keys.ESCAPE);
        press(refuge, Input.Keys.C); screenshot("caronte-return");
        finishDialogue(refuge);
        check(game.profiles().profile().has(CampaignProfile.Event.BRIBE_JOKE), "Caronte must recognize death");
        press(refuge, Input.Keys.C);
        finishDialogue(refuge);
        check(game.profiles().profile().has(CampaignProfile.Event.GUARDIAN_RECOGNIZED), "Caronte must recognize boss victory");
        CampaignProfile reloaded = new ProfileService(TEST_SAVES.store(game.activeSlot())).profile();
        check(reloaded.bonuses().vitality() == 1 && reloaded.has(CampaignProfile.Event.BRIBE_JOKE),
            "Purchases and narrative flags must survive profile reload");

        // The isolated survival demo does not inherit campaign purchases.
        game.setScreen(new MenuScreen(game)); press(game.getScreen(), Input.Keys.ENTER);
        check(game.getScreen() instanceof SaveSelectionScreen, "Demo must allow selecting an existing save");
        press(game.getScreen(), Input.Keys.NUM_2);
        RunSession demo = (RunSession) field(game.getScreen(), "session");
        check(demo.player().getVidaMaxima() == 85 && demo.player().getDano() == 2, "Demo must keep its independent starting stats");

        game.setScreen(new MenuScreen(game)); press(game.getScreen(), Input.Keys.C);
        check(game.getScreen() instanceof SaveSelectionScreen, "Campaign must keep its save selection");
        press(game.getScreen(), Input.Keys.NUM_2);
        check(game.getScreen() instanceof RefugeScreen, "Returning player must enter the saved refuge");
        press(game.getScreen(), Input.Keys.ENTER);
        screen = (GameScreen) game.getScreen();
        RunSession run = (RunSession) field(screen, "session");
        check(run.player().getVidaMaxima() == 93.5f && Math.abs(run.player().getDano() - 2.1f) < 0.0001f, "Campaign must apply purchased attributes");
        run.applyUpgrade(Upgrade.VIDA); run.applyUpgrade(Upgrade.FORCA); run.applyUpgrade(Upgrade.POSEIDON); run.applyUpgrade(Upgrade.RESSURREICAO);
        run.player().ganharXP(10); run.player().subirDeNivel();
        press(screen, Input.Keys.ESCAPE); press(screen, Input.Keys.M);
        check(game.getScreen() instanceof RefugeScreen && (boolean) field(screen, "disposed"), "Abandoning must safely return to refuge");
        PlayerState preview = (PlayerState) field(game.getScreen(), "stats");
        check(preview.getVida() == 93.5f && Math.abs(preview.getDano() - 2.1f) < 0.0001f && preview.getNivel() == 1 && !preview.possuiAura()
            && !preview.possuiRessurreicao(), "Refuge must contain fresh stats with permanent bonuses only");
        press(game.getScreen(), Input.Keys.ENTER);
        RunSession fresh = (RunSession) field(game.getScreen(), "session");
        check(fresh.player().getVida() == 93.5f && Math.abs(fresh.player().getDano() - 2.1f) < 0.0001f && fresh.player().getXpAtual() == 0
            && fresh.rank(Upgrade.FORCA) == 0 && !fresh.player().possuiAura() && !fresh.player().possuiRessurreicao(),
            "The next run must reset all divine and temporary progress");
    }

    private static void verifyChests(Main game) throws Exception {
        GameScreen screen = (GameScreen) game.getScreen();
        RunSession run = (RunSession) field(screen, "session");
        RunChests chests = (RunChests) field(screen, "chests");
        for (int attempt = 0; attempt < 10 && chests.chests().isEmpty(); attempt++) {
            set(chests, "timer", 0f); screen.render(0);
        }
        check(!chests.chests().isEmpty(), "A real arena must spawn a reachable chest");
        RunChests.Chest chest = chests.chests().get(0);
        press(screen, Input.Keys.F);
        check(chests.chests().contains(chest), "A distant chest must not be opened");
        Jogador actor = (Jogador) field(screen, "player");
        actor.x = chest.x() - 8; actor.y = chest.y() - 8;
        screen.render(0); screenshot("chest");
        set(chests, "timer", 10f);
        press(screen, Input.Keys.ESCAPE);
        float pausedTimer = (float) field(chests, "timer");
        for (int frame = 0; frame < 120; frame++) screen.render(1f / 60);
        check((float) field(chests, "timer") == pausedTimer, "Pause must freeze chest generation");
        press(screen, Input.Keys.ESCAPE);
        run.player().sofrerDano(30); run.player().tryDash();
        float healthBefore = run.player().getVida(); int pendingCoins = run.earnedObols(), savedCoins = game.profiles().profile().obols();
        press(screen, Input.Keys.F);
        check(!chests.chests().contains(chest), "F must consume the chest exactly once");
        switch (chest.reward()) {
            case HEALTH -> check(run.player().getVida() == healthBefore + 30, "Chest must heal the real player");
            case EXPERIENCE -> check(run.player().getNivel() == 2 && run.player().getXpAtual() == 2, "Chest XP must trigger a real level choice");
            case STAMINA -> check(run.player().getEstaminaAtual() == 100, "Chest must restore stamina");
            case OBOLS -> check(run.earnedObols() == pendingCoins + 3, "Chest currency must be added to the run");
        }
        check(game.profiles().profile().obols() == savedCoins, "Chest currency must wait for return before banking");
        screen.render(0); screenshot("chest-reward");
        while (field(screen, "state").toString().equals("LEVEL_UP")) press(screen, Input.Keys.NUM_1);
        press(screen, Input.Keys.ESCAPE); press(screen, Input.Keys.M);
        check(game.getScreen() instanceof RefugeScreen && chests.chests().isEmpty(), "Returning must discard arena chests");
    }


    @SuppressWarnings("unchecked")
    private static void verifySaveLockAndMagnet(Main game) throws Exception {
        RefugeScreen refuge = (RefugeScreen) game.getScreen();
        Profession locked = game.profiles().profile().profession();
        press(refuge, Input.Keys.P);
        check(game.getScreen() instanceof SaveSelectionScreen, "P must select saves instead of changing profession");
        game.getScreen().render(0); screenshot("saves-progress");
        press(game.getScreen(), Input.Keys.NUM_2);
        check(game.getScreen() instanceof RefugeScreen && game.profiles().profile().profession() == locked,
            "Continuing an existing save must retain its profession");
        check(!game.profiles().chooseProfession(Profession.CACADOR), "Profile service must reject a profession change");
        check(game.profileAt(0).profile().profession() == Profession.CACADOR
            && game.profileAt(2).profile().profession() == Profession.FERREIRO, "Independent saves must keep their own characters");
        press(game.getScreen(), Input.Keys.ENTER);
        GameScreen screen = (GameScreen) game.getScreen();
        RunSession session = (RunSession) field(screen, "session");
        Jogador actor = (Jogador) field(screen, "player");
        java.util.List<OrbeXP> orbs = (java.util.List<OrbeXP>) field(screen, "orbs");
        var atlas = (com.badlogic.gdx.graphics.Texture) field(screen, "atlas");
        OrbeXP orb = new OrbeXP(actor.x + 65, actor.y + 5, Art.cell(atlas, 1, 3), 1);
        orbs.add(orb);
        float originalX = orb.x;
        screen.render(0.05f);
        check(orb.x == originalX && session.player().getXpAtual() == 0, "A distant crystal must stay outside the base attraction");
        session.applyUpgrade(Upgrade.MAGNETISMO);
        screen.render(0.05f); screenshot("xp-magnet");
        check(orb.x < originalX, "The magnet must move an actual XP crystal toward the player");
        for (int i = 0; i < 12 && !orbs.isEmpty(); i++) screen.render(0.05f);
        check(orbs.isEmpty() && session.player().getXpAtual() == 1, "The magnet must collect and award XP once");
        session.advance(600); set(screen, "spawnTimer", session.spawnInterval() * 2); screen.render(0);
        java.util.List<Inimigo> enemies = (java.util.List<Inimigo>) field(screen, "enemies");
        check(!enemies.isEmpty(), "Continuous hordes must still spawn with scaled stats");
        for (Inimigo enemy : enemies) {
            check(Math.abs(enemy.vida - (enemy.isArcher() ? ArcherAttack.HEALTH : enemy.getType().health) * session.enemyDifficulty().healthMultiplier()) < 0.001f,
                "New enemy health must use total attempt time");
            check(Math.abs(enemy.getDano() - (enemy.isArcher() ? ArcherAttack.CONTACT_DAMAGE : enemy.getType().damage) * session.enemyDifficulty().damageMultiplier()) < 0.001f,
                "New enemy damage must use the linear curve");
        }
        Inimigo target = enemies.get(0); target.sofrerDano(1); float damagedLife = target.vida;
        session.advance(60); screen.render(0);
        check(target.vida == damagedLife, "Time scaling must not heal a damaged existing enemy");
        press(screen, Input.Keys.ESCAPE); press(screen, Input.Keys.M);
        press(game.getScreen(), Input.Keys.ENTER);
        check(!((RunSession) field(game.getScreen(), "session")).player().possuiImaXp(), "Returning must reset the magnet");
    }


    private static final class Rolls extends java.util.Random {
        private final float[] values; private int index;
        Rolls(float... values) { this.values = values; }
        @Override public float nextFloat() { return index < values.length ? values[index++] : 0.5f; }
    }
    @SuppressWarnings("unchecked")
    private static void verifyMiniBossAndForge(Main game) throws Exception {
        RunSession session = RunSession.campaign(game.campaignRegion(), Profession.VIGILANTE, PermanentBonuses.NONE, new Rolls(0.01f, 0.01f));
        GameScreen screen = new GameScreen(game, session); game.setScreen(screen);
        session.advance(45); set(screen, "spawnTimer", session.spawnInterval() * 2); screen.render(0);
        MiniBossEncounter mini = (MiniBossEncounter) field(screen, "miniBoss");
        check(mini != null && mini.health() > 180, "The campaign must spawn a scaled mini-boss");
        java.util.List<Inimigo> enemies = (java.util.List<Inimigo>) field(screen, "enemies");
        check(!enemies.isEmpty(), "Normal hordes must remain alongside the mini-boss");
        set(session.player(), "escudoAtivo", true);
        for (int frame = 0; frame < 100 && mini.state() != MiniBossEncounter.State.TELEGRAPH; frame++) screen.render(0.02f);
        check(mini.attack() == MiniBossEncounter.Attack.SPIN && mini.state() == MiniBossEncounter.State.TELEGRAPH, "The mini-boss must signal the spin");
        screenshot("mini-boss-spin");
        press(screen, Input.Keys.ESCAPE);
        float pausedTime = session.progress().elapsed(), pausedX = mini.x, pausedY = mini.y;
        for (int frame = 0; frame < 20; frame++) screen.render(0.05f);
        check(session.progress().elapsed() == pausedTime && mini.x == pausedX && mini.y == pausedY, "Pause must freeze mini-boss combat");
        press(screen, Input.Keys.ESCAPE);
        for (int frame = 0; frame < 400 && mini.stones().isEmpty(); frame++) screen.render(0.02f);
        check(!mini.stones().isEmpty(), "The mini-boss must throw a handful of stones");
        screenshot("mini-boss-stones");
        enemies.clear();
        var map = (TileMap) field(screen, "map"); map.gerarAreaInicialSegura(mini.x, mini.y, 8);
        Jogador actor = (Jogador) field(screen, "player"); actor.x = mini.x - 60; actor.y = mini.y;
        mini.hurt(mini.health() - 1); set(session.player(), "cooldownTiro", 0f);
        int materialsBefore = game.profiles().profile().forgeMaterials(), killsBefore = session.progress().kills();
        for (int frame = 0; frame < 20 && field(screen, "miniBoss") != null; frame++) {
            var viewport = (com.badlogic.gdx.utils.viewport.FitViewport) field(screen, "world");
            var target = viewport.project(new com.badlogic.gdx.math.Vector2(mini.centerX(), mini.centerY()));
            fire(screen, Math.round(target.x), Gdx.graphics.getHeight() - Math.round(target.y));
        }
        check(field(screen, "miniBoss") == null && session.progress().kills() == killsBefore + 1, "A real weapon hit must defeat the mini-boss once");
        check(game.profiles().profile().forgeMaterials() == materialsBefore + 1, "The rare material must be banked immediately");
        check(new ProfileService(TEST_SAVES.store(game.activeSlot())).profile().forgeMaterials() == materialsBefore + 1, "Materials must survive profile reload");
        check(mini.stones().isEmpty() && !session.miniBossDue(), "Defeat must clear stones without spawning a second mini-boss");
        screenshot("forge-material");
        while (field(screen, "state").toString().equals("LEVEL_UP")) press(screen, Input.Keys.NUM_1);
        meetGoals(session); press(screen, Input.Keys.E);
        while (game.getScreen() == screen && field(screen, "state").toString().equals("LEVEL_UP")) press(screen, Input.Keys.NUM_1);
        check(game.getScreen() instanceof BlacksmithScreen, "The scheduled blacksmith must open before choosing the next room");
        BlacksmithScreen smith = (BlacksmithScreen) game.getScreen();
        smith.render(0.5f); screenshot("blacksmith");
        float time = session.progress().elapsed(), damage = session.player().getDano();
        press(smith, Input.Keys.NUM_1);
        check(session.player().getWeaponRarity() == WeaponRarity.RARE
            && Math.abs(session.player().getDano() - damage * 1.25f) < 0.001f, "Forging must upgrade the actual weapon");
        check(game.profiles().profile().forgeMaterials() == materialsBefore, "Forging must consume one material");
        press(smith, Input.Keys.NUM_1);
        check(game.profiles().profile().forgeMaterials() == materialsBefore, "Repeated input must not spend again");
        check(session.progress().elapsed() == time, "The safe encounter must not increase enemy difficulty");
        smith.resize(800, 600); smith.render(0); smith.resize(960, 540); smith.render(0); screenshot("blacksmith-forged");
        press(smith, Input.Keys.ENTER); check(game.getScreen() instanceof RouteScreen, "Leaving the forge must continue the route");
        game.getScreen().render(0.5f); press(game.getScreen(), Input.Keys.NUM_1);
        check(game.getScreen() instanceof BossRoomScreen && session.player().getWeaponRarity() == WeaponRarity.RARE, "Rarity must continue into the boss room");
        BossRoomScreen room = (BossRoomScreen) game.getScreen(); BossEncounter boss = (BossEncounter) field(room, "boss");
        actor = (Jogador) field(room, "player"); actor.x = boss.x() + 40 - 8; actor.y = boss.y() - 8;
        set(session.player(), "auraDePoseidonAtiva", false); // Isolate forged damage from random level rewards.
        set(session.player(), "shockRank", 0); set(session.player(), "burnRank", 0); set(session.player(), "impactRank", 0);
        set(session.player(), "cooldownTiro", 0f); float life = boss.health(), realDamage = session.player().getDano();
        for (int frame = 0; frame < 8 && boss.health() == life; frame++) fireAtBoss(room, boss);
        check(Math.abs(life - boss.health() - realDamage) < 0.001f, "Forged damage must reach the floor boss");
        screenshot("boss-rare-weapon");
        press(room, Input.Keys.ESCAPE); press(room, Input.Keys.M); press(game.getScreen(), Input.Keys.ENTER);
        check(((RunSession) field(game.getScreen(), "session")).player().getWeaponRarity() == WeaponRarity.RARE, "A new attempt must keep permanent weapon rarity");
        check(new ProfileService(TEST_SAVES.store(game.activeSlot())).profile().weaponRarity() == WeaponRarity.RARE,
            "Permanent forging must survive profile reload");
        game.getScreen().render(0); screenshot("permanent-forge-retry");
    }
    @SuppressWarnings("unchecked")
    private static void verifyRareWeapons(Main game) throws Exception {
        for (Profession profession : new Profession[]{Profession.CACADOR, Profession.FERREIRO, Profession.VIGILANTE, Profession.APOSTADOR}) {
            RunSession session = RunSession.campaign(game.campaignRegion(), profession, PermanentBonuses.NONE, new Rolls(0.5f));
            session.player().forgeWeapon();
            GameScreen screen = new GameScreen(game, session); game.setScreen(screen);
            var atlas = (com.badlogic.gdx.graphics.Texture) field(screen, "atlas");
            Inimigo first = new Inimigo(profession == Profession.APOSTADOR ? 132 : 124, 100, atlas);
            Inimigo second = new Inimigo(145, profession == Profession.VIGILANTE ? 122 : 100, atlas);
            first.vida = second.vida = 100;
            var enemies = (java.util.List<Inimigo>) field(screen, "enemies"); enemies.add(first); enemies.add(second);
            fire(screen);
            if (profession == Profession.APOSTADOR) {
                check(first.vida == 100 - session.player().getDano() && second.vida == 100,
                    "Rare fists must extend range while hitting only one target");
                check(((java.util.List<?>) field(screen, "projectiles")).isEmpty(), "Fists must never emit projectiles");
                screenshot("rare-apostador"); continue;
            }
            for (int frame = 0; frame < 30 && second.vida == 100; frame++) screen.render(0.02f);
            check(first.vida < 100 && second.vida < 100, profession + " must apply its rare weapon effect to real enemies");
            check(Math.abs(100 - second.vida - session.player().getDano()) < 0.001f, "A projectile must not hit the same target twice");
            screenshot("rare-" + profession.name().toLowerCase(java.util.Locale.ROOT));
        }
    }


    @SuppressWarnings("unchecked")
    private static void verifyLuckyFistsAndRegionCap(Main game) throws Exception {
        game.setScreen(new MenuScreen(game)); press(game.getScreen(), Input.Keys.C);
        game.getScreen().render(0); screenshot("saves-four");
        press(game.getScreen(), Input.Keys.NUM_4); check(game.getScreen() instanceof ProfessionScreen, "Fourth save must create a new character");
        game.getScreen().render(0); screenshot("professions-four");
        press(game.getScreen(), Input.Keys.NUM_4); confirmCharacter(game); check(game.getScreen() instanceof RefugeScreen, "Fourth profession must enter refuge");
        check(game.profiles().profile().profession() == Profession.APOSTADOR
            && new ProfileService(TEST_SAVES.store(3)).profile().profession() == Profession.APOSTADOR, "Lucky profession must persist");
        press(game.getScreen(), Input.Keys.ENTER);
        GameScreen screen = (GameScreen) game.getScreen(); RunSession session = (RunSession) field(screen, "session");
        check(session.player().getWeapon() == WeaponType.FISTS && session.player().getLuckMultiplier() == 4, "Apostador must use fists and class luck");
        var atlas = (com.badlogic.gdx.graphics.Texture) field(screen, "atlas");
        Inimigo nearest = new Inimigo(120, 100, atlas), next = new Inimigo(124, 100, atlas), behind = new Inimigo(88, 100, atlas);
        nearest.vida = next.vida = behind.vida = 100;
        var enemies = (java.util.List<Inimigo>) field(screen, "enemies"); enemies.add(nearest); enemies.add(next); enemies.add(behind);
        fire(screen);
        check(nearest.vida == 96 && next.vida == 100 && behind.vida == 100, "Punch must hit only the nearest target in aim direction");
        check(((java.util.List<?>) field(screen, "projectiles")).isEmpty(), "Normal punches must not launch arrows");
        screenshot("fists");
        enemies.clear();
        session.player().ganharXP(10); screen.render(0);
        java.util.List<Upgrade> choices = (java.util.List<Upgrade>) field(screen, "choices");
        check(choices.size() == 3 && new java.util.HashSet<>(choices).size() == 3, "Weighted boon choices must be distinct");
        screen.render(0); screenshot("boon-rarities");
        while (field(screen, "state").toString().equals("LEVEL_UP")) press(screen, Input.Keys.NUM_1);

        // Rolls that fail for ordinary classes must succeed with this profession.
        RunSession lucky = RunSession.campaign(game.campaignRegion(), Profession.APOSTADOR, PermanentBonuses.NONE, new Rolls(0.10f, 0.05f));
        game.setScreen(new GameScreen(game, lucky));
        lucky.advance(45); lucky.markMiniBossSpawned(); check(lucky.recordMiniBossDefeat(), "Class luck must raise the real material chance");
        check(game.profiles().collectForgeMaterials(lucky) == 1, "Lucky material must be banked exactly once");
        meetGoals(lucky); game.offerRoute(lucky);
        check(game.getScreen() instanceof BlacksmithScreen, "Class luck must raise the real blacksmith event chance");
        game.getScreen().render(0.5f); screenshot("blacksmith-fists"); press(game.getScreen(), Input.Keys.NUM_1);
        check(lucky.player().getWeaponRarity() == WeaponRarity.RARE && lucky.player().getWeapon() == WeaponType.FISTS,
            "Blacksmith must strengthen fists without replacing the fighting style");
        check(game.profiles().profile().forgeMaterials() == 0, "Forging fists must spend the material");
        game.finishRun(lucky, RunOutcome.ABANDONED);

        // A new session in a different room of the same region remains blocked.
        ProfileService reloaded = new ProfileService(TEST_SAVES.store(3));
        check(!reloaded.profile().canClaimForgeMaterial(game.campaignRegion().id()), "Regional claim must persist after spending and returning");
        check(reloaded.profile().weaponRarity() == WeaponRarity.RARE, "Forged fists must persist in their own save");
        RunSession retry = RunSession.campaign(game.campaignRegion(), Profession.APOSTADOR, PermanentBonuses.NONE, new Rolls(0.5f, 0));
        game.setScreen(new GameScreen(game, retry));
        check(retry.player().getWeaponRarity() == WeaponRarity.RARE && retry.player().meleeReach() == 34,
            "A new run must restore the saved fists and their range");
        meetGoals(retry); retry.enterArena("sombras");
        game.setScreen(new GameScreen(game, retry)); retry.advance(45); retry.markMiniBossSpawned();
        check(!retry.recordMiniBossDefeat() && game.profiles().collectForgeMaterials(retry) == 0, "A different room or run must not rearm the region");
        check(retry.encounters().materialChance() == 0 && game.profiles().profile().forgeMaterials() == 0, "Luck must respect the persistent material cap");
        game.getScreen().render(0); screenshot("material-region-cap");
        game.finishRun(retry, RunOutcome.ABANDONED);
        check(!game.profiles().chooseProfession(Profession.CACADOR), "Apostador must remain fixed to its save");
    }

    @SuppressWarnings("unchecked")
    private static void verifyArenaRewards(Main game) throws Exception {
        // Reach Cinzas through the real route screen and check its announced event stays fixed.
        RunSession recovery = RunSession.campaign(game.campaignRegion(), game.profiles().profile().profession(),
            game.profiles().profile().bonuses(), new Rolls(0.5f));
        GameScreen screen = new GameScreen(game, recovery); game.setScreen(screen);
        meetGoals(recovery); press(screen, Input.Keys.E);
        check(game.getScreen() instanceof RouteScreen, "Starting arena must still offer all routes");
        ArenaVariation announced = recovery.variation("cinzas");
        game.getScreen().render(0.5f); screenshot("routes-rewards-events");
        press(game.getScreen(), Input.Keys.NUM_2);
        screen = (GameScreen) game.getScreen();
        check(recovery.arena().id().equals("cinzas") && recovery.variation() == announced, "The chosen room must keep its forecast event");
        recovery.player().sofrerDano(50); meetGoals(recovery);
        press(screen, Input.Keys.E);
        check(game.getScreen() instanceof ArenaRewardScreen, "Cinzas must grant its guaranteed chest after the exit");
        float time = recovery.progress().elapsed(), health = recovery.player().getVida();
        game.getScreen().render(1); screenshot("cinzas-recovery");
        check(recovery.progress().elapsed() == time, "Completion rewards must be safe and freeze difficulty");
        press(game.getScreen(), Input.Keys.F);
        check(game.getScreen() instanceof RouteScreen && recovery.player().getVida() == health + 40,
            "F must open the guaranteed chest and restore health");
        check(!recovery.canClaimArenaReward(), "Recovery chest must be consumed once");
        game.offerRoute(recovery); check(recovery.player().getVida() == health + 40, "Reopening the route must not repeat healing");
        game.finishRun(recovery, RunOutcome.ABANDONED);

        RunSession full = RunSession.campaign(game.campaignRegion(), Profession.APOSTADOR, PermanentBonuses.NONE, new Rolls(0.5f));
        meetGoals(full); full.enterArena("cinzas"); game.setScreen(new GameScreen(game, full));
        meetGoals(full); press(game.getScreen(), Input.Keys.E); game.getScreen().render(0.5f);
        int earned = full.earnedObols(); press(game.getScreen(), Input.Keys.F);
        check(full.earnedObols() == earned + 5, "A guaranteed recovery chest at full health must grant currency");
        game.finishRun(full, RunOutcome.ABANDONED);

        RunSession risk = RunSession.campaign(game.campaignRegion(), Profession.APOSTADOR, PermanentBonuses.NONE, new Rolls(0.5f));
        meetGoals(risk); risk.enterArena("sombras"); screen = new GameScreen(game, risk); game.setScreen(screen);
        risk.player().ganharXP(10); meetGoals(risk); press(screen, Input.Keys.E);
        check(game.getScreen() == screen && field(screen, "state").toString().equals("LEVEL_UP"),
            "Regular XP choices must be resolved before the route's extra boon");
        press(screen, Input.Keys.NUM_1);
        check(game.getScreen() instanceof ArenaRewardScreen, "Sombras must show a separate extra boon");
        var options = risk.arenaRewardChoices();
        check(!options.isEmpty() && options.stream().allMatch(b -> b.rarity.ordinal() >= BoonRarity.RARE.ordinal()),
            "Every extra option must respect the guaranteed minimum rarity");
        var originalOptions = java.util.List.copyOf(options);
        game.setScreen(new ArenaRewardScreen(game, risk));
        check(risk.arenaRewardChoices().equals(originalOptions), "Reopening a reward must not reroll options");
        int level = risk.player().getNivel(), rank = risk.rank(options.get(0)); float xp = risk.player().getXpAtual();
        game.getScreen().render(0.5f); screenshot("sombras-rare-reward");
        press(game.getScreen(), Input.Keys.NUM_1);
        check(game.getScreen() instanceof RouteScreen && risk.rank(options.get(0)) == rank + 1,
            "Claiming the rare reward must apply its effect exactly once");
        check(risk.player().getNivel() == level && risk.player().getXpAtual() == xp, "Extra boon must not invent a level or consume XP");
        game.getScreen().render(0.5f); press(game.getScreen(), Input.Keys.NUM_1);
        check(game.getScreen() instanceof BossRoomScreen, "The completed optional route must continue to the boss");
        game.finishRun(risk, RunOutcome.ABANDONED);

        RunSession exhausted = RunSession.campaign(game.campaignRegion(), Profession.APOSTADOR, PermanentBonuses.NONE, new Rolls(0.5f));
        meetGoals(exhausted); exhausted.enterArena("sombras");
        for (Upgrade boon : Upgrade.values()) if (boon.rarity != BoonRarity.COMMON)
            for (int i = 0; i < boon.maxRank; i++) exhausted.applyUpgrade(boon);
        game.setScreen(new GameScreen(game, exhausted)); meetGoals(exhausted); press(game.getScreen(), Input.Keys.E);
        game.getScreen().render(0.5f); screenshot("sombras-exhausted-reward");
        earned = exhausted.earnedObols(); press(game.getScreen(), Input.Keys.NUM_1);
        check(game.getScreen() instanceof RouteScreen && exhausted.earnedObols() == earned + 8,
            "Exhausted rare categories must turn the extra reward into currency");
        game.finishRun(exhausted, RunOutcome.ABANDONED);
    }


    @SuppressWarnings("unchecked")
    private static void verifyNewProfessionsAndCombat(Main game) throws Exception {
        game.setScreen(new SaveSelectionScreen(game,false));
        press(game.getScreen(),Input.Keys.RIGHT); screenshot("saves-page-two");
        press(game.getScreen(),Input.Keys.NUM_3);
        press(game.getScreen(),Input.Keys.RIGHT); screenshot("professions-page-two-locked");
        press(game.getScreen(),Input.Keys.NUM_3);
        check(game.getScreen() instanceof ProfessionScreen && !game.profiles().profile().professionChosen(),
            "Legendary class must remain locked before purchase");
        game.selectSave(3,false);
        RefugeScreen refuge = (RefugeScreen) game.getScreen(); press(refuge,Input.Keys.L);
        set(game.profiles().profile(),"obols",9); game.profiles().retrySave();
        set(game.shared().progress(),"souls",1009); game.shared().retrySave();
        press(refuge,Input.Keys.N);
        check(game.necromancerUnlocked() && game.shared().progress().souls()==9 && game.profiles().profile().obols()==9,
            "Altar must debit the legendary unlock atomically");
        press(refuge,Input.Keys.N); check(game.shared().progress().souls()==9 && game.profiles().profile().obols()==9,"Repeated unlock must not spend again");
        check(new SharedProgressService(TEST_SAVES.sharedStore()).progress().necromancerUnlocked(),"Unlock must survive reloading shared progress");
        screenshot("legendary-unlock");

        for(int index=4;index<8;index++) {
            game.setScreen(new SaveSelectionScreen(game,false)); press(game.getScreen(),Input.Keys.RIGHT);
            press(game.getScreen(),Input.Keys.NUM_1+index-4);
            check(game.getScreen() instanceof ProfessionScreen,"New slots must ask for a profession");
            press(game.getScreen(),Input.Keys.RIGHT); screenshot("professions-page-two");
            press(game.getScreen(),Input.Keys.NUM_1+index-4);
            confirmCharacter(game);
            Profession profession = Profession.values()[index];
            check(game.getScreen() instanceof RefugeScreen && game.profiles().profile().profession()==profession,"Second profession page must create the selected character");
            check(new ProfileService(TEST_SAVES.store(index)).profile().profession()==profession,"New professions must persist");
            press(game.getScreen(),Input.Keys.ENTER);
            GameScreen screen = (GameScreen) game.getScreen(); RunSession run = (RunSession) field(screen,"session");
            var atlas=(com.badlogic.gdx.graphics.Texture)field(screen,"atlas");
            var enemies=(java.util.List<Inimigo>)field(screen,"enemies");
            Inimigo first=new Inimigo(130,100,atlas), second=new Inimigo(145,100,atlas);
            first.vida=second.vida=100; enemies.add(first); enemies.add(second);
            fire(screen);
            for(int frame=0;frame<8 && first.vida==100;frame++) screen.render(.02f);
            check(first.vida<100,profession+" weapon must hit real enemies");
            CombatEffects fx=(CombatEffects)field(screen,"effects");
            if(profession==Profession.FEITICEIRA) check(second.vida<100,"Spell impact must deal splash damage");
            if(profession==Profession.SACERDOTE) {
                check(fx.cursed(first) && fx.cursed(second),"Priest must curse both enemies in the impact area"); float life=first.vida;
                screen.render(.05f); check(first.vida<life,"Curse must tick during combat");
            }
            if(profession==Profession.NECROMANTE) {
                Necromancy army=run.necromancy();
                Field random=Necromancy.class.getDeclaredField("random"); random.setAccessible(true);
                random.set(army,new java.util.Random(){@Override public double nextDouble(){return 0;}});
                first.sofrerDano(1000); screen.render(0);
                check(army.allies().size()==1 && run.progress().kills()==1,"Necromancer must convert the defeated enemy and count its death only once");
                float life=second.vida; screen.render(.05f); check(second.vida<life,"Resurrected ally must fight alongside the player");
                screenshot("necromancer-allies");
                meetGoals(run); press(screen,Input.Keys.E); game.getScreen().render(.5f); press(game.getScreen(),Input.Keys.NUM_1);
                check(game.getScreen() instanceof BossRoomScreen && army.allies().size()==1,"Allies must carry into the guardian room");
                ((BossEncounter)field(game.getScreen(),"boss")).hurt(100000);
                game.getScreen().render(0); check(army.allies().isEmpty(),"Ending a run must release allies");
                continue;
            }
            if(profession==Profession.GUERREIRO) {
                enemies.clear();
                Inimigo contact=new Inimigo(109,100,atlas); contact.vida=100; enemies.add(contact);
                PlayerState player=run.player(); float hp=player.getVida();
                holdGuard(screen,700,270);
                check(player.guarding() && player.getVida()==hp && player.getEstaminaAtual()<100,"Right mouse must block frontal contact in real arena combat");
                screenshot("shield-guard");
                player.forgeWeapon();
                Field random=GameScreen.class.getDeclaredField("combatRandom");random.setAccessible(true);
                random.set(screen,new java.util.Random(){@Override public double nextDouble(){return 0;}});
                set(contact,"attackTimer",0f); float life=contact.vida;
                holdGuard(screen,700,270);
                check(contact.vida<life,"Forged shield must retaliate in arena combat");
                screenshot("shield-retaliation");
            }
            screen.render(0); screenshot("class-"+profession.name().toLowerCase(java.util.Locale.ROOT));
            game.finishRun(run,RunOutcome.ABANDONED);
        }

        RunSession run=RunSession.campaign(game.campaignRegion(),Profession.FEITICEIRA,PermanentBonuses.NONE,new Rolls(.5f));
        GameScreen screen=new GameScreen(game,run);game.setScreen(screen);
        var atlas=(com.badlogic.gdx.graphics.Texture)field(screen,"atlas");
        var enemies=(java.util.List<Inimigo>)field(screen,"enemies");
        Inimigo target=new Inimigo(130,100,atlas), nearby=new Inimigo(145,100,atlas);
        target.vida=nearby.vida=300;enemies.add(target);enemies.add(nearby);
        run.applyUpgrade(Upgrade.CHOQUE);run.applyUpgrade(Upgrade.QUEIMADURA);run.applyUpgrade(Upgrade.IMPACTO);
        run.applyUpgrade(Upgrade.POSEIDON);
        fire(screen);for(int frame=0;frame<8 && target.vida==300;frame++)screen.render(.02f);
        screen.render(.05f);screenshot("elemental-synergies");
        meetGoals(run);run.advance(20);screen.render(0);screenshot("divine-fury-warning");
        press(screen,Input.Keys.ESCAPE);float pressure=run.fury().farmingSeconds();screen.render(1);
        check(run.fury().farmingSeconds()==pressure,"Pause must freeze divine fury");
        press(screen,Input.Keys.ESCAPE);run.advance(20);screen.render(0);screenshot("divine-fury-active");
        check(run.fury().level()>0 && run.fury().healthMultiplier()>1,"Post-unlock farming must strengthen future hordes");
        press(screen,Input.Keys.E);game.getScreen().render(.5f);press(game.getScreen(),Input.Keys.NUM_1);
        check(game.getScreen() instanceof BossRoomScreen && run.fury().level()==0,"Advancing must clear farming pressure");
        game.finishRun(run,RunOutcome.ABANDONED);
    }
    private static void holdGuard(Screen screen,int mouseX,int mouseY) {
        Input actual=Gdx.input;
        Gdx.input=(Input)Proxy.newProxyInstance(Input.class.getClassLoader(),new Class<?>[]{Input.class},
            (proxy,method,args)->{
                if(method.getName().equals("isButtonPressed"))return (int)args[0]==Input.Buttons.RIGHT;
                if(method.getName().equals("getX"))return mouseX;
                if(method.getName().equals("getY"))return mouseY;
                return method.invoke(actual,args);
            });
        try {screen.render(.05f);}finally {Gdx.input=actual;}
    }

    @SuppressWarnings("unchecked")
    private static void verifyCombatReadability(Main game) throws Exception {
        RunSession run = RunSession.demo(); GameScreen room = new GameScreen(game,run); game.setScreen(room);
        Jogador actor = (Jogador) field(room,"player");
        var atlas = (com.badlogic.gdx.graphics.Texture) field(room,"atlas");
        var enemies = (java.util.List<Inimigo>) field(room,"enemies");
        for (EnemyType type : EnemyType.values()) {
            enemies.clear();
            Inimigo enemy = new Inimigo(actor.x+15,actor.y,atlas); enemy.configurarTipo(type); enemies.add(enemy);
            check(!actor.hitbox().overlaps(enemy.hitbox()),"Transparent borders must not overlap for "+type);
            float hp = run.player().getVida(); room.render(0);
            check(run.player().getVida()==hp,"The old 16x16 overlap must not cause invisible contact damage");
            enemy.x=actor.x;
            check(actor.hitbox().overlaps(enemy.hitbox()),"Visible sprites at the same position must overlap");
            room.render(0);
            check(run.player().getVida()==hp-type.damage,"Visible contact must still damage the player");
        }
        enemies.clear(); run.player().ganharXP(10); room.render(0);
        ChoiceInputGuard guard=(ChoiceInputGuard)field(room,"choiceGuard");
        clickChoice(room,.2f,true,true);
        check(field(room,"state").toString().equals("LEVEL_UP"),"A combat click must not select a newly opened boon");
        press(room,Input.Keys.TAB); float remaining=guard.remaining(), elapsed=run.progress().elapsed();
        room.render(1); room.resize(800,600); room.render(.5f); room.resize(960,540);
        check(guard.remaining()==remaining && run.progress().elapsed()==elapsed,"Tab must freeze choices and combat");
        screenshot("boons-empty"); press(room,Input.Keys.ESCAPE);
        check(field(room,"state").toString().equals("LEVEL_UP"),"Closing Tab must restore the pending choices");
        clickChoice(room,2,true,true);
        check(field(room,"state").toString().equals("LEVEL_UP") && !guard.ready(),"Held attack must stay blocked beyond the delay");
        clickChoice(room,0,false,false); check(guard.ready(),"Releasing attack must unlock the choice");
        screenshot("upgrades-ready");
        clickChoice(room,.01f,true,true);
        check(field(room,"state").toString().equals("PLAYING"),"A fresh click after release must choose once");
        check(java.util.Arrays.stream(Upgrade.values()).mapToInt(run::rank).sum()==1,"One click must award one boon");
        for(Upgrade boon:Upgrade.values()) if(run.rank(boon)==0)run.applyUpgrade(boon);
        press(room,Input.Keys.TAB);
        BoonPanel panel=(BoonPanel)field(room,"boonPanel");
        elapsed=run.progress().elapsed(); room.render(.5f);
        check(run.progress().elapsed()==elapsed && panel.acquired().size()==15,"Tab must list every chosen boon without advancing combat");
        screenshot("boons-page-one"); press(room,Input.Keys.RIGHT); screenshot("boons-page-two");
        check(panel.page()==1,"Tab list must page with arrow keys");
        press(room,Input.Keys.TAB); room.render(.05f);
        check(run.progress().elapsed()>elapsed,"Combat must resume after closing Tab");
        press(room,Input.Keys.ESCAPE); press(room,Input.Keys.TAB); press(room,Input.Keys.ESCAPE);
        check(field(room,"state").toString().equals("PAUSED"),"Tab from pause must preserve pause after closing");
        screenshot("hud-attack-rate"); game.finishRun(run,RunOutcome.ABANDONED);

        RunSession bossRun=bossSession(Profession.SACERDOTE); bossRun.applyUpgrade(Upgrade.CADENCIA);
        BossRoomScreen bossRoom=new BossRoomScreen(game,bossRun); game.setScreen(bossRoom);
        BossEncounter boss=(BossEncounter)field(bossRoom,"boss"); press(bossRoom,Input.Keys.TAB);
        float bossTime=boss.elapsed(); elapsed=bossRun.progress().elapsed(); bossRoom.render(.5f);
        check(boss.elapsed()==bossTime && bossRun.progress().elapsed()==elapsed,"Tab must freeze boss attacks and difficulty too");
        screenshot("boss-boons"); press(bossRoom,Input.Keys.ESCAPE); bossRoom.render(.05f);
        check(boss.elapsed()>bossTime,"Closing boss Tab must resume attacks"); game.finishRun(bossRun,RunOutcome.ABANDONED);
    }
    @SuppressWarnings("unchecked")
    private static void verifyAthenaExperience(Main game) throws Exception {
        RunSession run = RunSession.demo();
        GameScreen room = new GameScreen(game, run); game.setScreen(room);
        java.util.Random rng = (java.util.Random) field(room, "boonRandom");
        int seed = 0;
        while (!UpgradeChoices.draw(run, 3, new java.util.Random(seed)).contains(Upgrade.INTELIGENCIA)) {
            check(++seed < 1000, "An eligible Epic Athena must be drawable");
        }
        rng.setSeed(seed);
        run.player().ganharXP(11); room.render(0);
        var choices = (java.util.List<Upgrade>) field(room, "choices");
        int index = choices.indexOf(Upgrade.INTELIGENCIA);
        check(index >= 0 && Upgrade.INTELIGENCIA.rarity == BoonRarity.EPIC, "Athena must appear as Epic in real choices");
        screenshot("athena-epic-choice");
        press(room, Input.Keys.NUM_1 + index);
        check(run.rank(Upgrade.INTELIGENCIA) == 1 && run.player().getXpAtual() == 1, "Choosing Athena must leave banked XP intact");
        press(room, Input.Keys.TAB); room.render(0); screenshot("athena-acquired"); press(room, Input.Keys.TAB);
        Jogador actor = (Jogador) field(room, "player");
        var atlas = (com.badlogic.gdx.graphics.Texture) field(room, "atlas");
        var orbs = (java.util.List<OrbeXP>) field(room, "orbs");
        orbs.add(new OrbeXP(actor.x + 5, actor.y + 5, Art.cell(atlas, 1, 3), EnemyType.COMMON.xp));
        room.render(0);
        check(orbs.isEmpty() && run.player().getXpAtual() == 3.5f, "A common crystal must award 2.5 XP and retain the fraction");
        screenshot("athena-fractional-xp");
        orbs.add(new OrbeXP(actor.x + 5, actor.y + 5, Art.cell(atlas, 1, 3), EnemyType.TANK.xp));
        room.render(0);
        check(run.player().getNivel() == 3 && run.player().getXpAtual() == 1, "Boosted crystals must earn a real level without losing overflow");
        check(field(room, "state").toString().equals("LEVEL_UP") && !choices.contains(Upgrade.INTELIGENCIA), "Athena must not be offered a second time");
        ChoiceInputGuard guard = (ChoiceInputGuard) field(room, "choiceGuard");
        check(!guard.ready(), "The faster level must retain its input protection");
        clickChoice(room, .2f, true, true);
        check(field(room, "state").toString().equals("LEVEL_UP"), "An attack click must not consume the new boon");
        press(room, Input.Keys.NUM_1);
        game.finishRun(run, RunOutcome.ABANDONED);
    }
    @SuppressWarnings("unchecked")
    private static void verifySharedSoulsAndReports(Main game) throws Exception {
        game.selectSave(0,false); game.startCampaign();
        GameScreen room=(GameScreen)game.getScreen(); RunSession run=(RunSession)field(room,"session");
        var atlas=(com.badlogic.gdx.graphics.Texture)field(room,"atlas");
        var enemies=(java.util.List<Inimigo>)field(room,"enemies");
        Jogador actor=(Jogador)field(room,"player");
        run.applyUpgrade(Upgrade.FORCA); run.applyUpgrade(Upgrade.CADENCIA); run.advance(12);
        Inimigo enemy=new Inimigo(actor.x+20,actor.y,atlas); enemy.vida=1; enemies.add(enemy);
        int souls=game.shared().progress().souls();
        fire(room);
        for(int i=0;i<20 && run.progress().kills()==0;i++) room.render(.02f);
        check(run.progress().kills()==1,"A real weapon must create the report's kill and Soul reward");
        run.player().receive(new CombatHit(10000,actor.x,actor.y,CombatHit.Kind.GROUND,"Guardião / Queda do Tártaro"),actor.x,actor.y);
        room.render(0);
        check(game.getScreen() instanceof GameOverScreen,"A lethal hit must open the result and save a report");
        RunReport report=game.shared().progress().latest(0);
        check(report!=null && report.outcome()==RunOutcome.DEATH && report.souls()==1
            && report.deathCause().equals("Guardião / Queda do Tártaro"),"The report must preserve result, source and reward");
        check(Math.abs(report.damageDealt()-1)<.001f && report.received()>0,"Effective damage must exclude weapon overkill");
        check(game.shared().progress().souls()==souls+1,"Finishing the run must credit the shared balance once");
        game.getScreen().render(.5f); press(game.getScreen(),Input.Keys.R);
        check(game.getScreen() instanceof RunReportScreen,"R on the result must open the detailed report");
        RunReportScreen screen=(RunReportScreen)game.getScreen();
        screen.render(.3f); screenshot("run-report-overview");
        press(screen,Input.Keys.RIGHT); screenshot("run-report-rooms");
        press(screen,Input.Keys.RIGHT); screenshot("run-report-boons");
        press(screen,Input.Keys.RIGHT); screenshot("run-report-damage");
        screen.resize(800,600); screen.render(0); screen.resize(960,540);
        check(game.shared().progress().souls()==souls+1,"Inspecting a report must not grant rewards again");
        press(screen,Input.Keys.ESCAPE); game.getScreen().render(.5f); press(game.getScreen(),Input.Keys.ENTER);
        check(game.getScreen() instanceof RefugeScreen,"Closing the report must preserve the route back to the refuge");
        press(game.getScreen(),Input.Keys.R);
        check(((RunReportScreen)game.getScreen()).report().id().equals(report.id()),"Refuge R must reopen the saved report");
        game.getScreen().render(.3f); press(game.getScreen(),Input.Keys.ESCAPE);
        game.startCampaign(); RunSession second=(RunSession)field(game.getScreen(),"session");
        second.advance(5); second.recordKill(EnemyType.FAST); game.finishRun(second,RunOutcome.ABANDONED);
        check(game.shared().progress().souls()==souls+3,"Abandonment must also preserve earned Souls");
        press(game.getScreen(),Input.Keys.R); screen=(RunReportScreen)game.getScreen();
        screen.render(.3f); check(screen.report().outcome()==RunOutcome.ABANDONED,"The latest summary must include abandonment");
        press(screen,Input.Keys.DOWN);
        check(screen.report().id().equals(report.id()),"Older summaries must remain browsable without changing progress");
        screenshot("run-report-history"); press(screen,Input.Keys.ESCAPE);
        game.selectSave(1,false);
        check(game.shared().progress().souls()==souls+3,"Changing character must preserve the shared Soul balance");
        game.getScreen().render(0); screenshot("shared-souls-refuge");
        Main reloaded=new Main(TEST_SAVES);
        check(reloaded.shared().progress().souls()==souls+3 && reloaded.necromancerUnlocked(),"Souls and legendary access must survive reload");
        check(reloaded.shared().progress().latest(0).outcome()==RunOutcome.ABANDONED,"Saved history must survive reload");
    }

    @SuppressWarnings("unchecked")
    private static void verifyArchers(Main game) throws Exception {
        // Eight actual weapon paths, rather than direct hurt calls, must defeat the new common variant.
        for (Profession profession : Profession.values()) {
            RunSession run=RunSession.campaign(game.campaignRegion(),profession);
            GameScreen room=new GameScreen(game,run); game.setScreen(room);
            var atlas=(com.badlogic.gdx.graphics.Texture)field(room,"atlas");
            var enemies=(java.util.List<Inimigo>)field(room,"enemies");
            Jogador actor=(Jogador)field(room,"player");
            Inimigo archer=new Inimigo(actor.x+20,actor.y,atlas); archer.configureArcher(); enemies.add(archer);
            check(archer.getType()==EnemyType.COMMON && archer.getXpDrop()==3 && archer.getSoulDrop()==3,
                "Archers must keep common goals and distinct rewards");
            check(Math.abs(archer.strength()-1.8f)<.001,"Necromancy must use the archer's own threat strength");
            for(int i=0;i<12 && archer.vida==ArcherAttack.HEALTH;i++) fire(room);
            check(archer.vida<ArcherAttack.HEALTH,profession+" must hit an archer through the real weapon path");
            archer.vida=1; run.player().advance(2);
            for(int i=0;i<12 && run.progress().kills()==0;i++) fire(room);
            check(run.progress().kills()==1 && run.kills(EnemyType.COMMON)==1 && run.earnedSouls()==3,
                "A real archer defeat must count once and grant three Souls");
            check(archer.archer().arrows().isEmpty(),"Death cleanup must remove pending archer arrows");
            actor.x=archer.x; actor.y=archer.y; room.render(.05f);
            check(run.player().getXpAtual()==3,"An archer must drop three collectable XP");
            room.render(.05f);
            check(run.progress().kills()==1 && run.earnedSouls()==3,"Cleanup must not award the archer twice");
        }
        RunSession run=RunSession.campaign(game.campaignRegion(),Profession.VIGILANTE);
        GameScreen room=new GameScreen(game,run); game.setScreen(room);
        var atlas=(com.badlogic.gdx.graphics.Texture)field(room,"atlas");
        var enemies=(java.util.List<Inimigo>)field(room,"enemies");
        Jogador actor=(Jogador)field(room,"player");
        Inimigo archer=new Inimigo(actor.x+64,actor.y,atlas); archer.configureArcher(); enemies.add(archer);
        // This test observes an evaded arrow for a full second; random cover must not consume it first.
        ((TileMap)field(room,"map")).gerarAreaInicialSegura(actor.x,actor.y,20);
        archer.applyDifficulty(new EnemyDifficulty(2,1.5f));
        check(archer.maxHealth()==28 && archer.getDano()==9 && archer.arrowDamage()==15,
            "Scaling must affect archer health, contact and arrows");
        room.render(.01f); screenshot("archer-warning");
        float aimX=archer.archer().aimX(),aimY=archer.archer().aimY();
        for(int i=0;i<10;i++)room.render(.05f);
        press(room,Input.Keys.TAB); float progress=archer.archer().aimProgress();
        room.render(1); check(progress==archer.archer().aimProgress(),"Tab must freeze the archer's warning");
        press(room,Input.Keys.TAB);
        press(room,Input.Keys.ESCAPE); progress=archer.archer().aimProgress();
        room.render(1); check(progress==archer.archer().aimProgress(),"Pause must freeze the archer's warning");
        press(room,Input.Keys.ESCAPE); room.resize(800,600); room.render(0); room.resize(960,540);
        actor.y+=32;
        for(int i=0;i<30 && archer.archer().arrows().isEmpty();i++)room.render(.05f);
        check(archer.archer().aimX()==aimX && archer.archer().aimY()==aimY,"Moving must not drag the locked target");
        check(!archer.archer().arrows().isEmpty() && archer.archer().arrows().getFirst().directionY()==0,
            "The real arrow must fly toward the initial position");
        screenshot("archer-arrow");
        float life=run.player().getVida();
        for(int i=0;i<20;i++)room.render(.05f);
        check(run.player().getVida()==life && !archer.archer().arrows().isEmpty()
            && archer.archer().arrows().getFirst().directionY()==0,"Leaving the mark must evade the arrow without redirecting it");
        archer.vida=0; room.render(.05f);
        check(archer.archer().arrows().isEmpty(),"Defeating the archer must also clear an arrow already in flight");

        // The real player's alpha mask and collision pipeline receive the scaled arrow once.
        run=RunSession.campaign(game.campaignRegion(),Profession.VIGILANTE);
        room=new GameScreen(game,run); game.setScreen(room);
        atlas=(com.badlogic.gdx.graphics.Texture)field(room,"atlas"); actor=(Jogador)field(room,"player");
        enemies=(java.util.List<Inimigo>)field(room,"enemies");
        archer=new Inimigo(actor.x+64,actor.y,atlas); archer.configureArcher();
        archer.applyDifficulty(new EnemyDifficulty(2,1.5f)); enemies.add(archer);
        life=run.player().getVida();
        for(int i=0;i<35;i++)room.render(.05f);
        check(run.player().getVida()==life-15 && archer.archer().arrows().isEmpty(),
            "A scaled arrow must collide with the visible player and disappear after one hit");
        run.end(); RunReport report=run.report(RunOutcome.DEATH,0);
        check(report.received()==15 && report.deathCause().equals("Arqueiro do Estige / Flecha"),
            "The report must identify incoming archer damage");

        // A real frontal guard also routes a guaranteed rare counterattack to the firing enemy.
        run=RunSession.campaign(game.campaignRegion(),Profession.GUERREIRO);
        room=new GameScreen(game,run); game.setScreen(room);
        atlas=(com.badlogic.gdx.graphics.Texture)field(room,"atlas"); actor=(Jogador)field(room,"player");
        enemies=(java.util.List<Inimigo>)field(room,"enemies");
        archer=new Inimigo(actor.x+64,actor.y,atlas); archer.configureArcher(); enemies.add(archer);
        if(run.player().canForgeWeapon())run.player().forgeWeapon();
        set(room,"combatRandom",new java.util.Random(){@Override public double nextDouble(){return 0;}});
        life=run.player().getVida();
        for(int i=0;i<35;i++)holdGuard(room,700,270);
        check(run.player().getVida()==life && archer.vida<ArcherAttack.HEALTH,
            "The frontal shield must block and a rare counter must hurt the firing archer");
        screenshot("archer-shield");

        // An off-screen archer must approach without starting a warning or shooting.
        run=RunSession.demo(); room=new GameScreen(game,run); game.setScreen(room);
        atlas=(com.badlogic.gdx.graphics.Texture)field(room,"atlas"); actor=(Jogador)field(room,"player");
        enemies=(java.util.List<Inimigo>)field(room,"enemies");
        archer=new Inimigo(actor.x,actor.y+125,atlas); archer.configureArcher(); enemies.add(archer);
        room.render(.05f);
        check(archer.archer().phase()==ArcherAttack.Phase.SEEKING && archer.archer().arrows().isEmpty(),
            "Archers outside the visible safe margin must approach instead of shooting");
        game.setScreen(new MenuScreen(game));
        check(archer.archer().arrows().isEmpty() && (boolean)field(room,"disposed"),"Screen changes must clear archer hazards");
    }

    @SuppressWarnings("unchecked")
    private static void verifyRequestedChanges() throws Exception {
        Main game=new Main(SaveSlots.memory());
        try {
            game.create();
            for(int i=0;i<SaveSlots.COUNT;i++)game.profileAt(i).chooseProfession(Profession.values()[i],true);
            set(game.shared().progress(),"souls",202); game.shared().retrySave();
            set(game.profileAt(0).profile(),"obols",123); game.profileAt(0).retrySave();
            game.selectSave(0,false);
            RunSession oldRun=RunSession.campaign(game.campaignRegion());
            game.setScreen(new GameScreen(game,oldRun)); game.finishRun(oldRun,RunOutcome.ABANDONED);
            String oldId=game.profiles().profile().characterId();
            String other=game.profileAt(1).profile().toJson();
            SaveSelectionScreen screen=new SaveSelectionScreen(game,false,true); game.setScreen(screen);
            screen.render(0); screenshot("saves-delete-buttons");
            clickAt(screen,401,303);
            check((int)field(screen,"pendingDelete")==0 && game.profiles().profile().characterId().equals(oldId),
                "Clicking delete must first open a modal, without resetting or selecting a character");
            screenshot("save-delete-confirm");
            clickAt(screen,325,180);
            check((int)field(screen,"pendingDelete")==-1 && game.profiles().profile().obols()==123,
                "Cancel must preserve the complete character");
            clickAt(screen,401,303); press(screen,Input.Keys.RIGHT);
            screen.render(.25f); screen.resize(800,600); screen.render(0); screen.resize(960,540);
            press(screen,Input.Keys.ENTER);
            check(!game.profiles().profile().professionChosen() && game.profiles().profile().obols()==0,
                "Confirmed deletion must leave the slot empty");
            check(game.shared().progress().souls()==202 && game.necromancerUnlocked()
                && game.profileAt(1).profile().toJson().equals(other),"Reset must preserve global unlocks/currency and other saves");
            check(game.latestReport()==null,"An empty slot must not show the previous character's report");
            screenshot("save-empty-after-delete");
            press(screen,Input.Keys.ESCAPE);
            check(game.getScreen() instanceof MenuScreen,"Leaving after deleting the active refuge save must return safely");
            game.setScreen(new SaveSelectionScreen(game,false));
            press(game.getScreen(),Input.Keys.NUM_1);
            check(game.getScreen() instanceof ProfessionScreen,"Reusing the slot must ask for a new profession");
            game.getScreen().render(0); screenshot("professions-without-numbers");
            clickAt(game.getScreen(),707,340);
            confirmCharacter(game);
            check(game.getScreen() instanceof RefugeScreen && game.profiles().profile().profession()==Profession.FERREIRO
                && !game.profiles().profile().characterId().equals(oldId),"A recreated character must have a new profession and identity");
            press(game.getScreen(),Input.Keys.R);
            check(game.getScreen() instanceof RefugeScreen,"A new character must not browse the deleted character's summaries");
            press(game.getScreen(),Input.Keys.P); screen=(SaveSelectionScreen)game.getScreen();
            press(screen,Input.Keys.RIGHT); clickAt(screen,856,143);
            check((int)field(screen,"pendingDelete")==7,"The second page must target the eighth slot");
            screen.render(.21f); press(screen,Input.Keys.ENTER);
            check(!game.profileAt(7).profile().professionChosen() && game.profiles().profile().profession()==Profession.FERREIRO,
                "Deleting the eighth slot must preserve the active character");
            String fifth=game.profileAt(5).profile().characterId();
            press(screen,Input.Keys.FORWARD_DEL); press(screen,Input.Keys.NUM_2);
            check((int)field(screen,"pendingDelete")==5,"Keyboard deletion mode must use the current page");
            press(screen,Input.Keys.ESCAPE);
            check(game.profileAt(5).profile().characterId().equals(fifth),"Escape in the confirmation must preserve the requested slot");

            game.profileAt(7).chooseProfession(Profession.GUERREIRO,true); game.selectSave(7,false);
            RunSession run=RunSession.campaign(game.campaignRegion(),Profession.GUERREIRO);
            GameScreen room=new GameScreen(game,run); game.setScreen(room);
            var atlas=(com.badlogic.gdx.graphics.Texture)field(room,"atlas");
            var enemies=(java.util.List<Inimigo>)field(room,"enemies");
            Inimigo front=new Inimigo(109,100,atlas); front.vida=100; enemies.add(front);
            float life=run.player().getVida();
            holdGuardAndAttack(room,700,270);
            check(run.player().guarding() && front.vida<100 && run.player().getVida()==life,
                "Both mouse buttons must attack and block frontal contact in the real horde screen");
            screenshot("shield-attacking-while-guarding");
            RunSession bossRun=bossSession(Profession.GUERREIRO);
            BossRoomScreen bossRoom=new BossRoomScreen(game,bossRun); game.setScreen(bossRoom);
            BossEncounter boss=(BossEncounter)field(bossRoom,"boss"); Jogador actor=(Jogador)field(bossRoom,"player");
            actor.x=boss.x()+30-8; actor.y=boss.y()-8;
            var view=(com.badlogic.gdx.utils.viewport.FitViewport)field(bossRoom,"world");
            // Render once to follow the repositioned player before projecting the mouse target.
            bossRoom.render(0);
            var point=view.project(new com.badlogic.gdx.math.Vector2(boss.x(),boss.y()));
            holdGuardAndAttack(bossRoom,Math.round(point.x),Gdx.graphics.getHeight()-Math.round(point.y));
            check(bossRun.player().guarding() && boss.health()<boss.maxHealth(),
                "Guarded sword attacks must also damage the floor boss");
            screenshot("boss-shield-attack");

            run=RunSession.campaign(game.campaignRegion(),Profession.GUERREIRO);
            room=new GameScreen(game,run); game.setScreen(room);
            atlas=(com.badlogic.gdx.graphics.Texture)field(room,"atlas");
            enemies=(java.util.List<Inimigo>)field(room,"enemies");
            var navigation=(EnemyNavigation)field(room,"navigation");
            Inimigo fast=new Inimigo(164,100,atlas); fast.configurarTipo(EnemyType.FAST);
            float offset=(float)field(fast,"pursuitOffset");
            float burst=.5f-offset; if(burst<0)burst+=2;
            set(fast,"stateTime",burst); float x=fast.x;
            fast.update(.05f,navigation); check(Math.abs(x-fast.x-3.5f)<.001,"Fast burst must use 70 units/s");
            fast.x=164; fast.y=100; float recovery=1.5f-offset; if(recovery<0)recovery+=2;
            set(fast,"stateTime",recovery); fast.update(.05f,navigation);
            check(Math.abs(164-fast.x-1.2f)<.001,"Recovery must slow the real fast enemy to 24 units/s");
            for(int i=0;i<EnemyWaves.MAX_FAST;i++) {
                fast=new Inimigo(164+i%4*3,150+i/4*3,atlas); fast.configurarTipo(EnemyType.FAST); enemies.add(fast);
            }
            run.advance(15);
            set(room,"waves",new EnemyWaves(new java.util.Random(){@Override public float nextFloat(){return .6f;}}));
            set(room,"spawnTimer",run.spawnInterval()); room.render(0);
            check(enemies.stream().filter(e->e.getType()==EnemyType.FAST).count()==24 && enemies.size()>24,
                "A crowded arena must replace excess fast spawns while continuing to produce other enemies");
            screenshot("fast-horde-balanced");
        } finally {game.dispose();}
    }
    private static void clickAt(Screen screen,float x,float y) throws Exception {
        var viewport=(com.badlogic.gdx.utils.viewport.FitViewport)field(screen,"viewport");
        var point=viewport.project(new com.badlogic.gdx.math.Vector2(x,y));
        Input actual=Gdx.input;
        Gdx.input=(Input)Proxy.newProxyInstance(Input.class.getClassLoader(),new Class<?>[]{Input.class},(proxy,method,args)->{
            if(method.getName().equals("getX"))return Math.round(point.x);
            if(method.getName().equals("getY"))return Gdx.graphics.getHeight()-Math.round(point.y);
            if(method.getName().equals("isButtonJustPressed"))return (int)args[0]==Input.Buttons.LEFT;
            if(method.getName().equals("isButtonPressed") || method.getName().equals("isKeyJustPressed") || method.getName().equals("isKeyPressed"))return false;
            return method.invoke(actual,args);
        });
        try {screen.render(1f/60);}finally {Gdx.input=actual;}
    }
    private static void holdGuardAndAttack(Screen screen,int mouseX,int mouseY) {
        Input actual=Gdx.input;
        Gdx.input=(Input)Proxy.newProxyInstance(Input.class.getClassLoader(),new Class<?>[]{Input.class},(proxy,method,args)->{
            if(method.getName().equals("getX"))return mouseX;
            if(method.getName().equals("getY"))return mouseY;
            if(method.getName().equals("isButtonPressed"))return (int)args[0]==Input.Buttons.LEFT || (int)args[0]==Input.Buttons.RIGHT;
            if(method.getName().equals("isKeyJustPressed") || method.getName().equals("isKeyPressed"))return false;
            return method.invoke(actual,args);
        });
        try {screen.render(.05f);}finally {Gdx.input=actual;}
    }

    private static final class RecordedAudio implements AudioBackend {
        final java.util.List<SoundCue> played=new java.util.ArrayList<>();
        float musicGain,effectsGain; boolean disposed;
        Track track;
        public boolean available(){return !disposed;}
        public long play(SoundCue cue,float volume){played.add(cue);effectsGain=volume;return played.size();}
        final java.util.List<Long> stopped=new java.util.ArrayList<>();
        public void stop(SoundCue cue,long id){stopped.add(id);}
        public void volume(SoundCue cue,long id,float volume){effectsGain=volume;}
        public void music(Track track,float volume){this.track=track;musicGain=volume;}
        public void musicVolume(float volume){musicGain=volume;}
        public void dispose(){disposed=true;}
    }
    @SuppressWarnings("unchecked")
    private static void verifyAudioAndFeedback() throws Exception {
        AudioSettingsStore preferences=AudioSettingsStore.memory();RecordedAudio output=new RecordedAudio();
        Main game=new Main(SaveSlots.memory(),preferences,output);
        try {
            game.create();check(output.track==AudioBackend.Track.REFUGE,"Menus must start the calm loop");
            press(game.getScreen(),Input.Keys.O);
            check(game.getScreen() instanceof AudioSettingsScreen,"O in the menu must open audio controls");
            game.getScreen().render(0);screenshot("audio-settings");
            clickAt(game.getScreen(),495,354);
            check(game.audioSettings().value().effects()==.5f,"A real slider click must set and save effects volume");
            press(game.getScreen(),Input.Keys.DOWN);press(game.getScreen(),Input.Keys.RIGHT);
            check(game.audioSettings().value().music()==.35f && output.musicGain==.35f,"Music adjustment must apply immediately");
            press(game.getScreen(),Input.Keys.M);int count=output.played.size();
            press(game.getScreen(),Input.Keys.T);
            check(game.audioSettings().value().muted() && output.musicGain==0 && output.played.size()==count,
                "Mute must silence the preview without losing the chosen levels");
            screenshot("audio-muted");
            press(game.getScreen(),Input.Keys.M);press(game.getScreen(),Input.Keys.T);
            check(output.played.getLast()==SoundCue.BLOCK && output.musicGain==.35f,"Unmute must restore gains and preview");
            press(game.getScreen(),Input.Keys.ESCAPE);
            check(game.getScreen() instanceof MenuScreen,"Audio back must return to the menu");
            Main loaded=new Main(SaveSlots.memory(),preferences,new RecordedAudio());
            check(loaded.audioSettings().value().equals(game.audioSettings().value()),"Volumes must persist across a new application");
            game.profileAt(0).chooseProfession(Profession.GUERREIRO,true);
            for(Profession profession:Profession.values()) {
                RunSession run=RunSession.demo(profession);GameScreen room=new GameScreen(game,run);game.setScreen(room);
                var atlas=(com.badlogic.gdx.graphics.Texture)field(room,"atlas");
                var enemies=(java.util.List<Inimigo>)field(room,"enemies");
                Inimigo enemy=new Inimigo(120,100,atlas);enemy.vida=100;enemies.add(enemy);
                count=output.played.size();
                for(int i=0;i<12 && enemy.vida==100;i++){game.audio().advance(.05f);fire(room);}
                check(enemy.vida<100 && output.played.subList(count,output.played.size()).contains(SoundCue.attack(profession.weapon)),
                    profession+" must emit its own weapon cue only on a real attack");
                CombatFeedback feedback=(CombatFeedback)field(room,"feedback");
                check(feedback.pulses().stream().anyMatch(pulse->pulse.kind==CombatFeedback.Kind.HIT),
                    "Actual weapon damage must create a visible impact");
                room.render(0);screenshot("feedback-"+profession.name().toLowerCase(java.util.Locale.ROOT));
            }
            RunSession run=RunSession.demo(Profession.GUERREIRO);
            GameScreen room=new GameScreen(game,run);game.setScreen(room);
            var atlas=(com.badlogic.gdx.graphics.Texture)field(room,"atlas");
            var enemies=(java.util.List<Inimigo>)field(room,"enemies");
            Inimigo front=new Inimigo(109,100,atlas);front.vida=100;enemies.add(front);
            game.audio().advance(1);count=output.played.size();holdGuardAndAttack(room,700,270);
            CombatFeedback feedback=(CombatFeedback)field(room,"feedback");
            check(output.played.subList(count,output.played.size()).contains(SoundCue.BLOCK) && feedback.hurtFlash()==0,
                "A real block must have a defense cue rather than a hurt flash");
            screenshot("feedback-block");
            run.player().ganharRessurreicao();run.player().usarRessurreicao();set(front,"attackTimer",0f);
            game.audio().advance(1);room.render(.05f);
            check(output.played.getLast()==SoundCue.IMMUNE && feedback.hurtFlash()==0,"Invulnerability must be distinguishable from a shield block");
            run.player().advance(2.1f);set(front,"attackTimer",0f);
            game.audio().advance(1);room.render(.05f);
            check(output.played.getLast()==SoundCue.PLAYER_HURT && feedback.hurtFlash()>0,"Real life loss must create the hurt cue and border");
            screenshot("feedback-hurt");

            press(room,Input.Keys.ESCAPE);press(room,Input.Keys.O);
            float elapsed=run.progress().elapsed(),remaining=feedback.pulses().isEmpty()?0:feedback.pulses().getFirst().remaining;
            press(room,Input.Keys.M);room.render(2);
            check(game.getScreen()==room && !run.finished() && run.progress().elapsed()==elapsed,
                "M in audio controls must mute rather than abandon, and the arena clock must remain frozen");
            check(feedback.pulses().isEmpty() || feedback.pulses().getFirst().remaining==remaining,"Audio controls must also freeze visual effects");
            room.resize(800,600);room.render(0);room.resize(960,540);screenshot("audio-in-pause");
            press(room,Input.Keys.ESCAPE);
            check(field(room,"state").toString().equals("PAUSED"),"Closing audio must preserve the existing pause");
            press(room,Input.Keys.ESCAPE);run.player().ganharXP(100);room.render(.01f);
            check(field(room,"state").toString().equals("LEVEL_UP"),"The next scenario must be a protected boon choice");
            press(room,Input.Keys.O);room.render(2);press(room,Input.Keys.ESCAPE);
            ChoiceInputGuard guard=(ChoiceInputGuard)field(room,"choiceGuard");
            check(!guard.ready() && field(room,"state").toString().equals("LEVEL_UP"),"Audio must not advance or consume the boon choice guard");

            RunSession bossRun=bossSession(Profession.GUERREIRO);BossRoomScreen bossRoom=new BossRoomScreen(game,bossRun);game.setScreen(bossRoom);
            BossEncounter boss=(BossEncounter)field(bossRoom,"boss");
            game.audioSettings().change(new AudioSettings(.5f,.35f,false));game.audio().refreshVolume();
            count=output.played.size();
            for(int i=0;i<200 && boss.state()!=BossEncounter.State.TELEGRAPH;i++){game.audio().advance(.05f);bossRoom.render(.05f);}
            check(boss.state()==BossEncounter.State.TELEGRAPH && output.played.subList(count,output.played.size()).contains(SoundCue.BOSS_WARNING),
                "The actual boss telegraph transition must produce an audible warning");
            count=output.played.size();for(int i=0;i<10;i++)bossRoom.render(0);
            check(output.played.size()==count,"Drawing a telegraph repeatedly must not replay its warning");
            press(bossRoom,Input.Keys.O);float bossElapsed=boss.elapsed(),runElapsed=bossRun.progress().elapsed();
            bossRoom.render(2);check(boss.elapsed()==bossElapsed && bossRun.progress().elapsed()==runElapsed,"Boss audio controls must freeze the attack and run clocks");
            press(bossRoom,Input.Keys.ESCAPE);bossRoom.render(.01f);
            check(boss.elapsed()>bossElapsed,"Closing boss audio must resume the same encounter");

            // Real crystal collection, not just an animation request, emits the XP cue.
            run=RunSession.demo();room=new GameScreen(game,run);game.setScreen(room);
            Jogador actor=(Jogador)field(room,"player");atlas=(com.badlogic.gdx.graphics.Texture)field(room,"atlas");
            var orbs=(java.util.List<OrbeXP>)field(room,"orbs");
            orbs.add(new OrbeXP(actor.x+5,actor.y+5,Art.cell(atlas,1,3),1));
            game.audio().advance(1);room.render(.01f);
            check(run.player().getXpAtual()==1 && output.played.getLast()==SoundCue.XP,"Collecting a crystal must emit XP feedback once");
            run.player().sofrerDano(10000);room.render(0);
            check(game.getScreen() instanceof GameOverScreen && output.played.getLast()==SoundCue.DEATH,"The result transition must emit a death cue");
            int afterDeath=output.played.size();game.getScreen().render(0);game.getScreen().render(0);
            check(output.played.size()==afterDeath,"Result rendering must not replay the death cue");
        }finally{game.dispose();}
        check(output.disposed,"Audio buffers must be released with the application");
    }

    @SuppressWarnings("unchecked")
    private static void verifyMilestoneOne() throws Exception {
        RecordedAudio output=new RecordedAudio();
        Main game=new Main(SaveSlots.memory(),AudioSettingsStore.memory(),output);
        try {
            game.create();game.profiles().chooseProfession(Profession.CACADOR);
            RunSession run=RunSession.campaign(game.campaignRegion());
            GameScreen room=new GameScreen(game,run);game.setScreen(room);
            run.advance(45);run.markMiniBossSpawned();
            Jogador actor=(Jogador)field(room,"player");
            MiniBossEncounter mini=new MiniBossEncounter(actor.x+400,actor.y,EnemyDifficulty.BASE);
            set(room,"miniBoss",mini);set(mini,"timer",0f);
            long warnings=output.played.stream().filter(c->c==SoundCue.BOSS_WARNING).count();
            room.render(.01f);
            check(mini.state()==MiniBossEncounter.State.TELEGRAPH,"An invisible Carrasco must still simulate its attack");
            check(output.played.stream().filter(c->c==SoundCue.BOSS_WARNING).count()==warnings,"Offscreen attacks must be silent");
            mini.x=actor.x+70;mini.y=actor.y;room.render(0);
            check(output.played.stream().filter(c->c==SoundCue.BOSS_WARNING).count()==warnings,"Returning to view must not replay an old warning");
            set(mini,"state",MiniBossEncounter.State.RECOVERY);set(mini,"timer",0f);
            game.audio().advance(1);room.render(.01f);
            check(output.played.getLast()==SoundCue.BOSS_WARNING,"A fresh visible warning must be audible");
            long ownVoice=output.played.size();
            game.audio().advance(.4f);game.audio().play(SoundCue.BOSS_WARNING);long otherVoice=output.played.size();
            game.audio().play(SoundCue.PLAYER_HURT);long playerVoice=output.played.size();
            mini.x=actor.x+400;room.render(0);
            check(output.stopped.contains(ownVoice),"Leaving the viewport must stop this Carrasco's playing voice");
            check(!output.stopped.contains(otherVoice) && !output.stopped.contains(playerVoice),"Culling cannot silence other emitters or the player");
            warnings=output.played.stream().filter(c->c==SoundCue.BOSS_WARNING).count();
            mini.x=actor.x+70;room.render(0);
            check(output.played.stream().filter(c->c==SoundCue.BOSS_WARNING).count()==warnings,"Only a new telegraph can restart the sound");
            var world=(com.badlogic.gdx.utils.viewport.FitViewport)field(room,"world");
            var camera=(com.badlogic.gdx.graphics.OrthographicCamera)world.getCamera();
            var visible=GameScreen.class.getDeclaredMethod("miniBossVisible");visible.setAccessible(true);
            room.resize(800,600);room.render(0);
            check((boolean)visible.invoke(room),"Letterboxing must preserve world visibility");
            game.audio().advance(1);set(mini,"state",MiniBossEncounter.State.RECOVERY);set(mini,"timer",0f);
            room.render(.01f);ownVoice=output.played.size();
            camera.zoom=.2f;camera.update();room.render(0);
            check(!(boolean)visible.invoke(room) && output.stopped.contains(ownVoice),"Camera zoom must cull the actual source during drawing");
            camera.zoom=1;room.resize(960,540);room.render(0);
            float right=camera.position.x+camera.viewportWidth/2;
            mini.x=right-12;room.render(0);check((boolean)visible.invoke(room),"A partially visible body remains audible");
            mini.x=right+32;room.render(0);check(!(boolean)visible.invoke(room),"A body outside the edge is invisible");
            set(room,"miniBoss",null);game.audio().stopEmitter(mini);
            run.applyUpgrade(Upgrade.INTELIGENCIA);run.player().ganharXP(1);
            room.render(0);check(CombatHud.xpFraction(run.player())==.25f,"Real 2.5 XP must fill one quarter of the initial bar");
            screenshot("m1-hud-campaign");
            press(room,Input.Keys.TAB);float time=run.progress().elapsed();room.render(1);
            check(run.progress().elapsed()==time,"Detailed Tab stats must keep combat frozen");screenshot("m1-tab-details");press(room,Input.Keys.TAB);
            meetGoals(run);room.render(.01f);
            var beacon=(com.badlogic.gdx.math.Vector2)field(room,"exitBeacon");
            check(beacon!=null,"Goal completion must produce a visible exit beacon");
            screenshot("m1-passage");
            actor.x-=60;press(room,Input.Keys.E);
            check(game.getScreen()!=room,"Exit remains usable without finding or reaching the beacon");
            game.finishRun(run,RunOutcome.ABANDONED);

            RunSession bossRun=bossSession(Profession.GUERREIRO);
            BossRoomScreen bossRoom=new BossRoomScreen(game,bossRun);game.setScreen(bossRoom);
            BossEncounter boss=(BossEncounter)field(bossRoom,"boss");
            for(int i=0;i<40;i++)bossRoom.render(.05f);
            boss.hurt(boss.health()/2);
            for(int i=0;i<30;i++)bossRoom.render(.05f);
            bossRoom.render(0);screenshot("m1-boss-hud");
            game.finishRun(bossRun,RunOutcome.ABANDONED);
            RunReport report=bossRun.report(RunOutcome.ABANDONED,0);
            check(report.rooms().getLast().bossFight()!=null,"Ending a real boss room must snapshot its phase metrics");
            check(report.rooms().getLast().bossFight().phaseTwoSeconds()>0,"The report must retain second-phase time");
            RunReportScreen result=new RunReportScreen(game,report,()->new MenuScreen(game));game.setScreen(result);
            result.render(.25f);set(result,"page",result.pageCount()-1);result.render(0);screenshot("m1-boss-report");
        } finally {game.dispose();}
    }

    private static void clickChoice(Screen screen,float delta,boolean held,boolean clicked) {
        Input actual=Gdx.input;
        Gdx.input=(Input)Proxy.newProxyInstance(Input.class.getClassLoader(),new Class<?>[]{Input.class},(proxy,method,args)->{
            if(method.getName().equals("getX"))return 175;
            if(method.getName().equals("getY"))return 280;
            if(method.getName().equals("isButtonPressed"))return held && (int)args[0]==Input.Buttons.LEFT;
            if(method.getName().equals("isButtonJustPressed"))return clicked && (int)args[0]==Input.Buttons.LEFT;
            if(method.getName().equals("isKeyJustPressed") || method.getName().equals("isKeyPressed"))return false;
            return method.invoke(actual,args);
        });
        try {screen.render(delta);} finally {Gdx.input=actual;}
    }

    private static void meetGoals(RunSession session) {
        for (var goal : session.arena().killGoals().entrySet())
            while (session.kills(goal.getKey()) < goal.getValue()) session.recordKill(goal.getKey());
    }
    @SuppressWarnings("unchecked")
    private static void verifySword(Main game) throws Exception {
        game.setScreen(new GameScreen(game, RunSession.demo(Profession.FERREIRO)));
        GameScreen screen = (GameScreen) game.getScreen();
        PlayerState stats = (PlayerState) field(screen, "stats");
        check(stats.getWeapon() == WeaponType.SWORD, "Second profession must use the sword");
        com.badlogic.gdx.graphics.Texture atlas = (com.badlogic.gdx.graphics.Texture) field(screen, "atlas");
        Inimigo front = new Inimigo(124, 100, atlas), behind = new Inimigo(76, 100, atlas), far = new Inimigo(160, 100, atlas);
        java.util.List<Inimigo> enemies = (java.util.List<Inimigo>) field(screen, "enemies");
        enemies.add(front); enemies.add(behind); enemies.add(far);
        fire(screen);
        check(front.vida == 2 && behind.vida == 10 && far.vida == 10, "Sword must hit nearby enemies in aim direction");
        check(((java.util.List<?>) field(screen, "projectiles")).isEmpty(), "Sword must not emit a projectile");
        screenshot("sword");
    }
    private static void fire(Screen screen) { fire(screen, 700, 270); }
    private static void fire(Screen screen, int mouseX, int mouseY) {
        Input actual = Gdx.input;
        Gdx.input = (Input) Proxy.newProxyInstance(Input.class.getClassLoader(), new Class<?>[]{Input.class},
            (proxy, method, args) -> {
                if (method.getName().equals("isButtonPressed")) return (int) args[0] == Input.Buttons.LEFT;
                if (method.getName().equals("getX")) return mouseX;
                if (method.getName().equals("getY")) return mouseY;
                return method.invoke(actual, args);
            });
        try { screen.render(0.05f); } finally { Gdx.input = actual; }
    }
    private static Object field(Object owner, String name) throws Exception {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }
    private static void set(Object owner, String name, Object value) throws Exception {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(owner, value);
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    @SuppressWarnings("unchecked")
    private static void verifyMilestoneTwo() throws Exception {
        SaveSlots slots = SaveSlots.memory();
        Main game = new Main(slots, AudioSettingsStore.memory(), new RecordedAudio());
        try {
            game.create();
            for (GameDifficulty mode : GameDifficulty.values()) {
                game.selectSave(mode.ordinal(), false);
                press(game.getScreen(), Input.Keys.NUM_3);
                check(game.getScreen() instanceof DifficultyScreen, "New character needs difficulty confirmation");
                press(game.getScreen(), Input.Keys.NUM_1 + mode.ordinal());
                check(((DifficultyScreen)game.getScreen()).selected() == mode, "All three modes must be selectable");
                check(!slots.store(mode.ordinal()).load().professionChosen(), "Selection alone must not save");
                press(game.getScreen(), Input.Keys.ESCAPE);
                check(game.getScreen() instanceof ProfessionScreen && !game.profiles().profile().professionChosen(), "Cancelling must discard both choices");
                press(game.getScreen(), Input.Keys.NUM_3);
                press(game.getScreen(), Input.Keys.NUM_1 + mode.ordinal());
                game.getScreen().render(.21f); screenshot("m2-difficulty-" + mode.name().toLowerCase(java.util.Locale.ROOT));
                press(game.getScreen(), Input.Keys.ENTER);
                check(game.getScreen() instanceof RefugeScreen && game.profiles().profile().difficulty() == mode, "Confirmation must create the chosen mode");
                check(new ProfileService(slots.store(mode.ordinal())).profile().difficulty() == mode, "Difficulty must reload from storage");
                press(game.getScreen(), Input.Keys.ENTER);
                GameScreen room = (GameScreen)game.getScreen(); RunSession run = (RunSession)field(room,"session");
                check(run.difficulty() == mode, "Main must start with the character difficulty");
                var atlas = (com.badlogic.gdx.graphics.Texture)field(room,"atlas");
                for (EnemyType type : EnemyType.values()) {
                    Inimigo enemy = new Inimigo(160,100,atlas); enemy.configurarTipo(type); enemy.applyDifficulty(run.enemyDifficulty());
                    check(Math.abs(enemy.maxHealth() - type.health * mode.healthMultiplier) < .001f, "Every enemy type must scale health");
                    check(Math.abs(enemy.getDano() - type.damage * mode.damageMultiplier) < .001f, "Every enemy type must scale damage");
                    check(enemy.getXpDrop() == type.xp, "Difficulty must preserve XP drops");
                }
                Inimigo archer = new Inimigo(160,100,atlas); archer.configureArcher(); archer.applyDifficulty(run.enemyDifficulty());
                check(Math.abs(archer.arrowDamage() - 10 * mode.damageMultiplier) < .001f, "Archer arrows must scale");
                check(Math.abs(archer.maxHealth() - 14 * mode.healthMultiplier) < .001f, "Archer health must scale");
                MiniBossEncounter mini = new MiniBossEncounter(160,100,run.enemyDifficulty());
                check(Math.abs(mini.maxHealth() - 180 * mode.healthMultiplier) < .001f, "Miniboss health must scale");
                check(Math.abs((float)field(mini,"damageMultiplier") - mode.damageMultiplier) < .001f, "Miniboss attacks must scale");
                run.recordKill(EnemyType.COMMON); run.addChestObols(3);
                game.finishRun(run,RunOutcome.DEATH);
                RunReport report = game.latestReport();
                check(report.difficulty() == mode && report.obols() == mode.reward(3), "Result report must keep scaled rewards and difficulty");
                game.setScreen(new RunReportScreen(game,report,()->new RefugeScreen(game)));
                game.getScreen().render(0); screenshot("m2-report-" + mode.name().toLowerCase(java.util.Locale.ROOT));
                game.startCampaign();
                room = (GameScreen)game.getScreen(); run = (RunSession)field(room,"session");
                check(run.difficulty() == mode && run.earnedSouls() == 0, "Restart preserves mode and clears pending rewards");
                meetGoals(run); run.enterArena("portoes"); game.setScreen(new BossRoomScreen(game,run));
                BossEncounter boss = (BossEncounter)field(game.getScreen(),"boss");
                check(Math.abs(boss.maxHealth() - 500 * run.enemyDifficulty().healthMultiplier()) < .001f, "Boss room uses composed health scaling");
                check(Math.abs((float)field(boss,"damageMultiplier") - run.enemyDifficulty().damageMultiplier()) < .001f, "Boss attacks use composed damage scaling");
                game.finishRun(run,RunOutcome.ABANDONED);
                game.startDemo(); room = (GameScreen)game.getScreen(); run = (RunSession)field(room,"session");
                check(run.isDemo() && run.difficulty() == mode && run.earnedObols() == 0, "Demo honors mode without campaign rewards");
                game.finishRun(run,RunOutcome.ABANDONED);
            }
            game.setScreen(new SaveSelectionScreen(game,false)); game.getScreen().render(0); screenshot("m2-saves");
            game.getScreen().resize(800,600); game.getScreen().render(0); screenshot("m2-saves-800");
            game.getScreen().resize(960,540);
        } finally { game.dispose(); }

        boolean[] fail = {true};
        ProfileStore first = new ProfileStore() {
            private CampaignProfile saved = new CampaignProfile();
            public CampaignProfile load() { return saved.copy(); }
            public void save(CampaignProfile profile) throws java.io.IOException {
                if (fail[0]) throw new java.io.IOException("test"); saved = profile.copy();
            }
        };
        Main retryGame = new Main(new SaveSlots(first), AudioSettingsStore.memory(), new RecordedAudio());
        try {
            retryGame.create(); retryGame.selectSave(0,false); press(retryGame.getScreen(),Input.Keys.NUM_1);
            press(retryGame.getScreen(),Input.Keys.NUM_3); retryGame.getScreen().render(.21f);
            press(retryGame.getScreen(),Input.Keys.ENTER);
            check(retryGame.getScreen() instanceof DifficultyScreen && !retryGame.profiles().profile().professionChosen(), "Failed disk creation must stay on the confirmation screen");
            check(((DifficultyScreen)retryGame.getScreen()).selected() == GameDifficulty.HARD, "Failure preserves pending selection for retry");
            retryGame.getScreen().render(0); screenshot("m2-creation-error");
            fail[0] = false; press(retryGame.getScreen(),Input.Keys.ENTER);
            check(retryGame.getScreen() instanceof RefugeScreen && first.load().difficulty() == GameDifficulty.HARD, "Successful retry commits both settings");
        } finally { retryGame.dispose(); }
    }

    @SuppressWarnings("unchecked")
    private static void verifyMilestoneThree() throws Exception {
        Main game=new Main(SaveSlots.memory(),AudioSettingsStore.memory(),new RecordedAudio());
        PilotArt art=null;
        try {
            game.create();game.getScreen().render(0);screenshot("m3-menu");
            game.profiles().chooseProfession(Profession.CACADOR);game.selectSave(0,false);
            RefugeScreen refuge=(RefugeScreen)game.getScreen();
            var font=(com.badlogic.gdx.graphics.g2d.BitmapFont)field(refuge,"font");
            for(char glyph:"áàâãéêíóôõúüçÁÉÍÓÚÇ×—".toCharArray())
                check(font.getData().hasGlyph(glyph),"Bundled body font must support Portuguese and interface symbols: "+glyph);
            art=game.visuals();check(art==game.visuals(),"Visuals must be shared across screens");
            press(refuge,Input.Keys.C);refuge.render(.1f);
            DialoguePlayback playback=(DialoguePlayback)field(refuge,"playback");
            check(!playback.complete() && playback.visibleText().length()>0,"Dialogue must type progressively");
            Input actual=Gdx.input;
            Gdx.input=(Input)Proxy.newProxyInstance(Input.class.getClassLoader(),new Class<?>[]{Input.class},(proxy,method2,args)-> {
                if(method2.getName().equals("isButtonJustPressed"))return (int)args[0]==Input.Buttons.LEFT;
                if(method2.getName().equals("isKeyJustPressed"))return false;
                return method2.invoke(actual,args);
            });
            try {refuge.render(0);}finally {Gdx.input=actual;}
            check(playback.line()==0 && playback.complete(),"First click reveals without advancing");
            refuge.render(0);screenshot("m3-charon");
            float previewX=((Jogador)field(refuge,"player")).x;
            refuge.render(.5f);check(((Jogador)field(refuge,"player")).x==previewX,"Dialogue does not move the refuge player");
            press(refuge,Input.Keys.ENTER);check(playback.line()==1,"Second action advances one line");
            refuge.render(5);screenshot("m3-protagonist");
            check(game.profiles().profile().obols()==0,"Reading a partial conversation must not credit the gift");
            press(refuge,Input.Keys.ESCAPE);press(refuge,Input.Keys.C);
            playback=(DialoguePlayback)field(refuge,"playback");check(playback.line()==0,"Reopening starts from the beginning");
            press(refuge,Input.Keys.T);check(playback.complete() && game.instantDialogue(),"Instant-text option reveals without skipping");
            for(var line:CaronteDialogue.select(game.profiles().profile()).lines()) {
                var layout=new com.badlogic.gdx.graphics.g2d.GlyphLayout(font,line.text(),com.badlogic.gdx.graphics.Color.WHITE,670,com.badlogic.gdx.utils.Align.left,true);
                check(layout.height<95,"Dialogue copy must fit its panel");
            }
            refuge.resize(800,600);refuge.render(0);screenshot("m3-charon-800");
            refuge.resize(960,540);finishDialogue(refuge);
            check(game.profiles().profile().obols()==5,"Finishing credits the first gift once");
            press(refuge,Input.Keys.C);finishDialogue(refuge);check(game.profiles().profile().obols()==5,"Repeat dialogue cannot grant another gift");
            press(refuge,Input.Keys.ENTER);
            GameScreen room=(GameScreen)game.getScreen();
            Jogador player=(Jogador)field(room,"player");
            check(field(player,"pilot")==art,"Caçador uses the shared pilot atlas");
            var actors=(java.util.List<Inimigo>)field(room,"enemies");
            var oldAtlas=(com.badlogic.gdx.graphics.Texture)field(room,"atlas");
            Inimigo common=new Inimigo(player.x+40,player.y+8,oldAtlas,art);actors.add(common);
            room.render(.05f);check(field(common,"pilot")==art,"Common enemy uses the pilot");
            screenshot("m3-combat");
            ActorAnimation motion=(ActorAnimation)field(player,"motion");
            motion.advance(.13f,1,0);float phase=motion.phase();motion.advance(0,0,1);
            check(motion.phase()==phase && motion.direction()==ActorAnimation.Direction.UP,"Turning must preserve gait phase");
            motion.advance(1,0,0);check(motion.direction()==ActorAnimation.Direction.UP,"Idle retains facing");
            PilotArt stableArt=art;
            var texture=art.region("hero-down-0").getTexture();var data=texture.getTextureData();
            if(!data.isPrepared())data.prepare();Pixmap pixels=data.consumePixmap();
            try {
                for(boolean hero:new boolean[]{true,false}) {
                    SpriteHitbox[] masks=art.actorMasks(hero);
                    for(ActorAnimation.Direction direction:java.util.Arrays.copyOf(ActorAnimation.Direction.values(),4))for(int frame=0;frame<4;frame++) {
                        String name=(hero?"hero":"common")+"-"+direction.name().toLowerCase(java.util.Locale.ROOT)+"-"+frame;
                        var region=art.region(name);
                        SpriteHitbox mask=masks[direction.ordinal()*4+frame].place(0,0,32,32,false,false);
                        for(int y=0;y<32;y++)for(int x=0;x<32;x++)
                            check(mask.contains(x+.5f,31-y+.5f)==((pixels.getPixel(region.getRegionX()+x,region.getRegionY()+y)&255)>=96),
                                "Body contact mask must follow the exact rendered pixels in "+name);
                    }
                }
            } finally {if(data.disposePixmap())pixels.dispose();}
            CombatFeedback feedback=(CombatFeedback)field(room,"feedback");
            feedback.hit(WeaponType.BOW,player.x+22,player.y+10,5);feedback.xp(player.x+8,player.y+20);
            feedback.dash(WeaponType.BOW,player.x,player.y,player.x+15,player.y,true,true);
            room.render(0);screenshot("m3-effects");
            press(room,Input.Keys.TAB);float time=((RunSession)field(room,"session")).progress().elapsed();
            room.render(.5f);check(((RunSession)field(room,"session")).progress().elapsed()==time,"Visuals must not unpause combat");
            press(room,Input.Keys.TAB);
            ((RunSession)field(room,"session")).player().ganharXP(10);room.render(.01f);screenshot("m3-boon-cards");
            game.startDemo();room=(GameScreen)game.getScreen();set(room,"spawnTimer",2f);room.render(0);
            actors=(java.util.List<Inimigo>)field(room,"enemies");
            check(!actors.isEmpty() && field(actors.getFirst(),"pilot")==art,"Production spawning must attach the pilot atlas");
            game.setScreen(new MenuScreen(game));game.getScreen().render(0);
            check(game.visuals()==stableArt && !art.disposed(),"Changing screens must not dispose shared visuals");
        } finally {game.dispose();}
        check(art!=null && art.disposed(),"Application shutdown must dispose the pilot atlas once");
    }

    @SuppressWarnings("unchecked")
    private static void verifyExpandedVisuals() throws Exception {
        Main game=new Main(SaveSlots.memory(),AudioSettingsStore.memory(),new RecordedAudio());
        try {
            game.create();game.profiles().chooseProfession(Profession.CACADOR);
            PilotArt art=game.visuals();ExpandedArt expanded=art.expanded();
            var texture=expanded.region("class-cacador-down-0").getTexture();var data=texture.getTextureData();
            if(!data.isPrepared())data.prepare();Pixmap pixels=data.consumePixmap();
            try {
                var names=new java.util.ArrayList<String>();
                for(Profession profession:Profession.values())names.add(ExpandedArt.actor(profession));
                names.addAll(java.util.List.of(ExpandedArt.ENEMIES));
                for(String name:names) {
                    int count=name.startsWith("class-")?6:4;ExpandedArt.Body body=expanded.body(name);
                    ActorAnimation motion=new ActorAnimation();
                    for(ActorAnimation.Direction direction:java.util.Arrays.copyOf(ActorAnimation.Direction.values(),4))for(int frame=0;frame<count;frame++) {
                        set(motion,"direction",direction);set(motion,"previous",direction);set(motion,"turn",ActorAnimation.TURN_SECONDS);
                        set(motion,"moving",true);set(motion,"phase",(frame+.1f)*.09f);
                        var region=expanded.frame(name,direction.ordinal(),frame);int size=region.getRegionWidth();
                        SpriteHitbox mask=body.hitbox(motion,size/2f,size/2f,size);
                        for(int y=0;y<size;y++)for(int x=0;x<size;x++)
                            check(mask.contains(x+.5f,size-y-.5f)==((pixels.getPixel(region.getRegionX()+x,region.getRegionY()+y)&255)>=96),
                                "Expanded contact must match the dominant body pixels: "+name);
                    }
                }
                var hashes=new java.util.HashSet<Long>();
                for(Upgrade boon:Upgrade.values()) {
                    var region=art.boonIcon(boon);java.util.zip.CRC32 hash=new java.util.zip.CRC32();
                    for(int y=0;y<region.getRegionHeight();y++)for(int x=0;x<region.getRegionWidth();x++) {
                        int color=pixels.getPixel(region.getRegionX()+x,region.getRegionY()+y);
                        hash.update(color);hash.update(color>>>8);hash.update(color>>>16);hash.update(color>>>24);
                    }
                    check(hashes.add(hash.getValue()),"Each boon must have its own icon: "+boon);
                }
            } finally {if(data.disposePixmap())pixels.dispose();}
            for(Profession profession:Profession.values()) {
                RunSession run=RunSession.demo(profession);GameScreen room=new GameScreen(game,run);game.setScreen(room);
                Jogador player=(Jogador)field(room,"player");
                check(field(player,"body")!=null,"Every class uses its own animated body");
                var atlas=(com.badlogic.gdx.graphics.Texture)field(room,"atlas");
                var enemies=(java.util.List<Inimigo>)field(room,"enemies");
                for(EnemyType type:EnemyType.values()) {
                    Inimigo enemy=new Inimigo(player.x+45+type.ordinal()*35,player.y+15,atlas,art);
                    enemy.configurarTipo(type);enemies.add(enemy);
                    check(enemy.visualKind().equals(type.name().toLowerCase(java.util.Locale.ROOT)),"Enemy roles use distinct sheets");
                }
                Inimigo archer=new Inimigo(player.x-55,player.y+20,atlas,art);archer.configureArcher();enemies.add(archer);
                check(archer.visualKind().equals("archer"),"Archer uses a unique body");
                fire(room);screenshot("visual-class-"+profession.name().toLowerCase(java.util.Locale.ROOT));
            }
            RunSession run=RunSession.campaign(game.campaignRegion(),Profession.SACERDOTE);
            GameScreen room=new GameScreen(game,run);game.setScreen(room);
            Jogador player=(Jogador)field(room,"player");RunChests chests=(RunChests)field(room,"chests");
            var chestList=(java.util.List<RunChests.Chest>)field(chests,"chests");
            chestList.add(new RunChests.Chest(player.x+22,player.y+8,RunChests.Reward.OBOLS));set(chests,"timer",10f);
            room.render(0);screenshot("visual-chest-closed");press(room,Input.Keys.F);
            check(run.earnedObols()==3 && chests.chests().isEmpty(),"Opening keeps exactly one reward and removes the model");
            check(((ChestVisuals)field(room,"chestVisuals")).active()==1,"Opening animation persists independently of the reward");
            for(int i=0;i<5;i++)room.render(.05f);screenshot("visual-chest-opening");press(room,Input.Keys.F);
            check(run.earnedObols()==3,"Opening animation must not duplicate the chest reward");
            for(Upgrade boon:Upgrade.values())run.applyUpgrade(boon);
            var atlas=(com.badlogic.gdx.graphics.Texture)field(room,"atlas");
            var enemies=(java.util.List<Inimigo>)field(room,"enemies");
            Inimigo first=new Inimigo(player.x+36,player.y,atlas,art),second=new Inimigo(player.x+55,player.y+10,atlas,art);
            first.vida=second.vida=10000;enemies.add(first);enemies.add(second);
            for(int i=0;i<10;i++)fire(room);
            room.render(0);screenshot("visual-divine-effects");
            press(room,Input.Keys.TAB);room.render(0);screenshot("visual-boon-list");press(room,Input.Keys.TAB);
            run.player().ganharXP(10);room.render(.01f);
            check(field(room,"state").toString().equals("LEVEL_UP"),"XP still opens protected choices");
            room.render(0);screenshot("visual-boon-fullscreen");
            Pixmap frame=Pixmap.createFromFrameBuffer(90,510,1,1);
            try {check((frame.getPixel(0,0)>>>24)<40,"Opaque boon selection must cover the health bar");} finally {frame.dispose();}
            var cards=(com.badlogic.gdx.math.Rectangle[])field(room,"cards");
            check(cards[0].width>800 && cards[0].y>cards[1].y && !cards[0].overlaps(cards[1]),"Choice hit areas match full-width rows");
            room.resize(800,600);room.render(0);screenshot("visual-boon-fullscreen-800");room.resize(960,540);
            RunSession late=RunSession.demo();
            for(Upgrade boon:Upgrade.values())if(boon!=Upgrade.CURA)for(int i=0;i<boon.maxRank;i++)late.applyUpgrade(boon);
            room=new GameScreen(game,late);game.setScreen(room);late.player().ganharXP(4);room.render(.01f);
            check(((java.util.List<?>)field(room,"choices")).size()==1,"Late-run selection can contain only the remaining heal");
            room.render(0);screenshot("visual-boon-single-choice");press(room,Input.Keys.NUM_1);
            check(late.rank(Upgrade.CURA)==1,"The remaining single row stays selectable");
            makeVisualCatalogs(art);
        } finally {game.dispose();}
    }
    private static void makeVisualCatalogs(PilotArt art) {
        var viewport=new com.badlogic.gdx.utils.viewport.FitViewport(960,540);viewport.update(960,540,true);viewport.apply();
        var batch=new com.badlogic.gdx.graphics.g2d.SpriteBatch();var font=UiTheme.bodyFont();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        try {
            com.badlogic.gdx.utils.ScreenUtils.clear(.035f,.052f,.075f,1);batch.begin();font.setColor(com.badlogic.gdx.graphics.Color.GOLD);
            font.getData().setScale(1.6f);font.draw(batch,"PROFISSÕES E EQUIPAMENTOS",35,510);font.getData().setScale(1);
            for(Profession profession:Profession.values()) {
                int i=profession.ordinal();float x=30+i%4*235,y=420-i/4*200;
                font.setColor(com.badlogic.gdx.graphics.Color.GOLD);font.draw(batch,profession.title,x,y);
                batch.draw(art.equipped().frame(EquippedArt.actor(profession)+"-walk",ActorAnimation.Direction.DOWN,1),x+35,y-135,112,112);
                font.setColor(com.badlogic.gdx.graphics.Color.LIGHT_GRAY);font.draw(batch,profession.weapon.label,x,y-150);
            }
            batch.end();screenshot("visual-catalog-classes");
            com.badlogic.gdx.utils.ScreenUtils.clear(.035f,.052f,.075f,1);batch.begin();font.setColor(com.badlogic.gdx.graphics.Color.GOLD);
            font.getData().setScale(1.6f);font.draw(batch,"INIMIGOS DO SUBMUNDO",35,510);font.getData().setScale(1);
            String[] labels={"Comum","Rápido","Tank","Arqueiro","Carrasco","Guardião do Tártaro"};
            for(int i=0;i<ExpandedArt.ENEMIES.length;i++) {
                float x=45+i%3*310,y=420-i/3*200;font.draw(batch,labels[i],x,y);
                float size=i==5?110:i==4?90:i==2?75:65;
                if(i>=3)batch.draw(art.equipped().frame(ExpandedArt.ENEMIES[i]+"-walk",ActorAnimation.Direction.DOWN,1),x+80-size*.65f,y-145,size*1.3f,size*1.3f);
                else batch.draw(art.expanded().frame(ExpandedArt.ENEMIES[i],0,1),x+80-size/2,y-125,size,size);
            }
            batch.end();screenshot("visual-catalog-enemies");
            com.badlogic.gdx.utils.ScreenUtils.clear(.035f,.052f,.075f,1);batch.begin();
            for(Upgrade boon:Upgrade.values()) {
                int i=boon.ordinal();float x=15+i%5*190,y=505-i/5*165;
                batch.draw(art.boonIcon(boon),x+48,y-84,70,70);font.setColor(com.badlogic.gdx.graphics.Color.WHITE);
                font.draw(batch,boon.title,x,y-105,175,com.badlogic.gdx.utils.Align.center,true);
            }
            batch.end();screenshot("visual-catalog-boons");
        } finally {batch.dispose();font.dispose();}
    }

    @SuppressWarnings("unchecked")
    private static void verifyEquippedVisuals() throws Exception {
        Main game=new Main(SaveSlots.memory(),AudioSettingsStore.memory(),new RecordedAudio());
        try {
            game.create();game.profiles().chooseProfession(Profession.CACADOR);
            PilotArt art=game.visuals();EquippedArt equipped=art.equipped();
            var sources=(java.util.Map<String,?>)field(equipped,"clips");
            var pixels=new java.util.IdentityHashMap<com.badlogic.gdx.graphics.Texture,Pixmap>();
            var bodies=new java.util.HashMap<String,EquippedArt.Body>();int frames=0,outside=0;
            try {
                for(var entry:sources.entrySet()){
                    String name=entry.getKey(),actor=name.substring(0,name.lastIndexOf('-'));
                    int count=(int)field(entry.getValue(),"count"),directions=(int)field(entry.getValue(),"directions");
                    EquippedArt.Body body=bodies.computeIfAbsent(actor,equipped::body);
                    for(int d=0;d<directions;d++)for(int f=0;f<count;f++){
                        var direction=ActorAnimation.Direction.values()[d];var region=equipped.frame(name,direction,f);
                        var texture=region.getTexture();Pixmap image=pixels.get(texture);
                        if(image==null){var data=texture.getTextureData();if(!data.isPrepared())data.prepare();image=data.consumePixmap();pixels.put(texture,image);}
                        int width=region.getRegionWidth();check(width==64,"Equipped masks retain exact pixel resolution");
                        var mask=body.hitbox(new EquippedArt.Pose(name,direction,f),32,32,64/EquippedArt.DRAW_SCALE);
                        int core=0;
                        for(int y=0;y<64;y++)for(int x=0;x<64;x++){
                            boolean opaque=(regionPixel(region,image,x,y)&255)>=96;
                            boolean expected=opaque&&EquippedArt.bodyPixel(region.isFlipX()?63-x:x,y,64,64);
                            check(mask.contains(x+.5f,63-y+.5f)==expected,"Equipped body mask excludes weapon/margins and follows its pose: "+name);
                            if(expected)core++;else if(opaque)outside++;
                        }
                        check(core>10,"Every pose needs a visible physical body: "+name);frames++;
                    }
                }
                check(frames==1280 && outside>1000,"All original and mirrored poses and equipment must be verified");
                for(Profession profession:Profession.values()){
                    var hashes=new java.util.HashSet<Long>();
                    for(var direction:ActorAnimation.Direction.values()){
                        var region=equipped.frame(EquippedArt.actor(profession)+"-walk",direction,1);
                        check(hashes.add(regionHash(region,pixels.get(region.getTexture()))),"Diagonals must be distinct authored frames: "+profession);
                    }
                    var breathing=new java.util.HashSet<Long>();
                    for(int f=0;f<4;f++){
                        var region=equipped.frame(EquippedArt.actor(profession)+"-idle",ActorAnimation.Direction.DOWN,f);
                        breathing.add(regionHash(region,pixels.get(region.getTexture())));
                    }
                    check(breathing.size()>=3,"Every profession needs visible breathing: "+profession);
                    var left=equipped.frame(EquippedArt.actor(profession)+"-walk",ActorAnimation.Direction.DOWN_LEFT,1);
                    var right=equipped.frame(EquippedArt.actor(profession)+"-walk",ActorAnimation.Direction.DOWN_RIGHT,1);
                    check(!left.isFlipX()&&right.isFlipX()&&left.getTexture()==right.getTexture()
                        &&Math.min(left.getU(),left.getU2())==Math.min(right.getU(),right.getU2()),"SE must mirror the same SW sprite without swapping rows");
                }
                var idleHashes=new java.util.HashSet<Long>();
                for(int f=0;f<4;f++){var region=equipped.frame("guardian-idle",ActorAnimation.Direction.DOWN,f);idleHashes.add(regionHash(region,pixels.get(region.getTexture())));}
                check(idleHashes.size()>=3,"The guardian must visibly breathe between attacks");
            } finally {for(var entry:pixels.entrySet())if(entry.getKey().getTextureData().disposePixmap())entry.getValue().dispose();}
            makeEquippedCatalogs(art);
            verifyPeacefulRefuge(game);
            RunSession run=bossSession(Profession.CACADOR);BossRoomScreen room=new BossRoomScreen(game,run);game.setScreen(room);
            BossEncounter boss=(BossEncounter)field(room,"boss");
            room.render(0);screenshot("equipped-boss-breath-a");
            for(int i=0;i<8;i++)room.render(.05f);screenshot("equipped-boss-breath-b");
            press(room,Input.Keys.TAB);
            var motion=(ActorAnimation)field(room,"bossMotion");var pose=EncounterAnimation.boss(boss,motion);float elapsed=boss.elapsed();
            room.render(.5f);check(boss.elapsed()==elapsed&&EncounterAnimation.boss(boss,motion).equals(pose),"Tab freezes breathing and encounter animation");
            press(room,Input.Keys.TAB);
            renderUntil(game,room,boss,BossEncounter.Attack.CHARGE,BossEncounter.State.ACTIVE);room.render(0);screenshot("equipped-boss-run");
            renderUntil(game,room,boss,BossEncounter.Attack.CHARGE,BossEncounter.State.RECOVERY);room.render(0);screenshot("equipped-boss-shoulder");
            run.player().curar(10000);
            renderUntil(game,room,boss,BossEncounter.Attack.LEAP,BossEncounter.State.TELEGRAPH);room.render(0);screenshot("equipped-boss-takeoff");
            run.player().curar(10000);
            renderUntil(game,room,boss,BossEncounter.Attack.LEAP,BossEncounter.State.ACTIVE);room.render(0);screenshot("equipped-boss-landing");
        } finally {game.dispose();}
    }
    private static int regionPixel(com.badlogic.gdx.graphics.g2d.TextureRegion region,Pixmap image,int x,int y){
        int left=Math.round(Math.min(region.getU(),region.getU2())*image.getWidth());
        int top=Math.round(Math.min(region.getV(),region.getV2())*image.getHeight());
        return image.getPixel(left+(region.isFlipX()?region.getRegionWidth()-1-x:x),
            top+(region.isFlipY()?region.getRegionHeight()-1-y:y));
    }
    private static void verifyPeacefulRefuge(Main game)throws Exception{
        RefugeScreen refuge=new RefugeScreen(game);game.setScreen(refuge);
        Jogador player=(Jogador)field(refuge,"player");PlayerState stats=(PlayerState)field(refuge,"stats");
        PlayerAnimation animation=(PlayerAnimation)field(player,"visual");
        float beforeX=player.x;Input actual=Gdx.input;
        Gdx.input=(Input)Proxy.newProxyInstance(Input.class.getClassLoader(),new Class<?>[]{Input.class},(proxy,method,args)->{
            if(method.getName().equals("isButtonPressed"))return (int)args[0]==Input.Buttons.LEFT;
            if(method.getName().equals("isKeyPressed"))return (int)args[0]==Input.Keys.D;
            if(method.getName().equals("isButtonJustPressed")||method.getName().equals("isKeyJustPressed"))return false;
            return method.invoke(actual,args);
        });
        try{refuge.render(.05f);}finally{Gdx.input=actual;}
        check(player.x>beforeX,"Refuge movement remains enabled while holding attack");
        check(!animation.attacking()&&animation.current().clip().endsWith("-walk")&&stats.tryShoot(),
            "Held mouse in refuge cannot attack, animate attacks or consume attack cooldown");
        for(int i=0;i<10;i++)refuge.render(.05f);
        check(animation.current().clip().endsWith("-idle"),"Stationary refuge player must breathe");
        screenshot("polish-refuge");
        press(refuge,Input.Keys.C);float x=player.x,y=player.y;
        fire(refuge);
        check(player.x==x&&player.y==y&&!animation.attacking(),"Dialogue clicks cannot fire or move the player");
        finishDialogue(refuge);
    }
    private static long regionHash(com.badlogic.gdx.graphics.g2d.TextureRegion region,Pixmap image){
        var hash=new java.util.zip.CRC32();
        for(int y=0;y<region.getRegionHeight();y++)for(int x=0;x<region.getRegionWidth();x++){
            int color=regionPixel(region,image,x,y);
            hash.update(color);hash.update(color>>>8);hash.update(color>>>16);hash.update(color>>>24);
        }
        return hash.getValue();
    }
    private static void makeEquippedCatalogs(PilotArt art){
        var view=new com.badlogic.gdx.utils.viewport.FitViewport(960,540);view.update(960,540,true);view.apply();
        var batch=new com.badlogic.gdx.graphics.g2d.SpriteBatch();var font=UiTheme.bodyFont();batch.setProjectionMatrix(view.getCamera().combined);
        try{
            com.badlogic.gdx.utils.ScreenUtils.clear(.035f,.052f,.075f,1);batch.begin();font.setColor(com.badlogic.gdx.graphics.Color.GOLD);
            String[] labels={"S","N","L","O","SE","SO","NE","NO"};
            for(int d=0;d<8;d++)font.draw(batch,labels[d],179+d*96,515);
            for(Profession profession:Profession.values()){
                float y=434-profession.ordinal()*54;font.draw(batch,profession.title,12,y+29);
                for(var direction:ActorAnimation.Direction.values())
                    batch.draw(art.equipped().frame(EquippedArt.actor(profession)+"-walk",direction,1),150+direction.ordinal()*96,y,72,72);
            }
            batch.end();screenshot("equipped-eight-directions");
            com.badlogic.gdx.utils.ScreenUtils.clear(.035f,.052f,.075f,1);batch.begin();
            for(Profession profession:Profession.values()){
                float y=434-profession.ordinal()*54;font.draw(batch,profession.title,12,y+29);
                for(int f=0;f<4;f++)batch.draw(art.equipped().frame(EquippedArt.actor(profession)+"-idle",
                    ActorAnimation.Direction.DOWN,f),180+f*180,y,72,72);
            }
            batch.end();screenshot("polish-idle-classes");
            com.badlogic.gdx.utils.ScreenUtils.clear(.035f,.052f,.075f,1);batch.begin();
            for(var direction:ActorAnimation.Direction.values()){
                int col=direction.ordinal()%4,row=direction.ordinal()/4;float x=30+col*235,y=265-row*245;
                font.draw(batch,direction.key(),x+35,y+220);
                batch.draw(art.equipped().frame("guardian-cast",direction,4),x,y,215,215);
            }
            batch.end();screenshot("polish-boss-cast-directions");
            com.badlogic.gdx.utils.ScreenUtils.clear(.035f,.052f,.075f,1);batch.begin();
            String[] clips={"guardian-idle","guardian-charge","guardian-cast","guardian-jump","guardian-land"};
            String[] titles={"Respiração","Corrida / ombrada","Conjuração","Salto","Aterrissagem"};
            for(int row=0;row<clips.length;row++){
                float y=420-row*98;font.draw(batch,titles[row],12,y+40);
                for(int f=0;f<(row==0?4:6);f++)batch.draw(art.equipped().frame(clips[row],ActorAnimation.Direction.DOWN,f),180+f*125,y,96,96);
            }
            batch.end();screenshot("equipped-guardian-actions");
            com.badlogic.gdx.utils.ScreenUtils.clear(.035f,.052f,.075f,1);batch.begin();
            String[] attacks={"archer-attack","carrasco-spin","carrasco-stones"};
            String[] names={"Arqueiro: disparo","Carrasco: giro","Carrasco: pedras"};
            for(int row=0;row<attacks.length;row++){
                float y=375-row*145;font.draw(batch,names[row],12,y+50);
                for(int f=0;f<6;f++)batch.draw(art.equipped().frame(attacks[row],ActorAnimation.Direction.DOWN,f),180+f*125,y,96,96);
            }
            batch.end();screenshot("equipped-enemy-actions");
        }finally{batch.dispose();font.dispose();}
    }

    private static void finishDialogue(RefugeScreen refuge) throws Exception {
        for(int count=0;count<70 && field(refuge,"state").toString().equals("DIALOGUE");count++)
            press(refuge,Input.Keys.ENTER);
        check(!field(refuge,"state").toString().equals("DIALOGUE"),"Conversation must end without repeated actions granting extra rewards");
    }
    private static void confirmCharacter(Main game) throws Exception {
        check(game.getScreen() instanceof DifficultyScreen, "Profession must lead to difficulty confirmation");
        check(!game.profiles().profile().professionChosen(), "Pending choices must not create the save");
        game.getScreen().render(.21f);
        press(game.getScreen(), Input.Keys.ENTER);
    }
    private static void press(Screen screen, int key) throws Exception {
        if (key >= Input.Keys.NUM_1 && key <= Input.Keys.NUM_3
            && (screen instanceof GameScreen && field(screen,"state").toString().equals("LEVEL_UP")
                || screen instanceof ArenaRewardScreen && !(boolean) field(screen,"recovery"))) screen.render(1.51f);
        Input actual = Gdx.input;
        Gdx.input = (Input) Proxy.newProxyInstance(Input.class.getClassLoader(), new Class<?>[]{Input.class},
            (proxy, method, args) -> {
                if (method.getName().equals("isKeyJustPressed")) return (int) args[0] == key;
                if (method.getName().equals("isButtonJustPressed")) return false;
                return method.invoke(actual, args);
            });
        try { screen.render(1f / 60); } finally { Gdx.input = actual; }
    }
    private static void screenshot(String name) {
        Pixmap pixmap = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        try { PixmapIO.writePNG(Gdx.files.local("build/smoke/" + name + ".png"), pixmap, -1, true); }
        finally { pixmap.dispose(); }
    }
}
