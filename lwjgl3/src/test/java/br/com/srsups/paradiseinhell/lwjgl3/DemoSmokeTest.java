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
                setScreen(new GameScreen(this, RunSession.campaign(campaignRegion(), p.profession(), p.bonuses(), new Rolls(0.5f))));
            }
            @Override public void render() {
                try {
                    verify(this);
                } catch (Throwable error) { failure = error; }
                Gdx.app.exit();
            }
        }, config);
        if (failure != null) throw new AssertionError("Demo integration check failed", failure);
        System.out.println("DEMO_SMOKE_OK: menu, upgrades, pause, resize, victory, defeat, retry, professions, weapons, XP by type, kill goals, continuous waves, boss access, boss attacks, phase two, boss weapons, resurrection, refuge, dialogue, permanent purchases, reset, profile reload, chests, save slots, locked profession, timed notice, magnet pickup, percentage bonuses, enemy scaling, mini-boss spin/stones, persistent material, blacksmith forging, rare weapon powers, fourth save, fists, weighted boon rarities, permanent region material cap, cleanup");
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
    }
    @SuppressWarnings("unchecked")
    private static void verifyCampaign(Main game) throws Exception {
        press(game.getScreen(), Input.Keys.C);
        check(game.getScreen() instanceof SaveSelectionScreen, "Campaign must select a save");
        press(game.getScreen(), Input.Keys.NUM_2);
        check(game.getScreen() instanceof ProfessionScreen, "An empty save must ask the profession");
        press(game.getScreen(), Input.Keys.NUM_3);
        check(game.getScreen() instanceof RefugeScreen, "Campaign profession must lead to the refuge");
        game.getScreen().render(0); screenshot("refuge");
        press(game.getScreen(), Input.Keys.C);
        screenshot("caronte");
        for (int line = 0; line < 4; line++) press(game.getScreen(), Input.Keys.ENTER);
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
        check(boss.shots().size() == 7, "The second phase must emit seven real projectiles");
        room.render(0); screenshot("boss-volley");

        actor.x = boss.x() + 38 - 8; actor.y = boss.y() - 8;
        float beforeAura = boss.health();
        room.render(0.05f);
        check(Math.abs(beforeAura - boss.health() - stats.getDanoAura() * 0.05f) < 0.01f,
            "The carried aura must damage the boss in range");
        set(stats, "auraDePoseidonAtiva", false); // Isolate the lethal weapon hit from aura damage.
        actor.x = boss.x() + 40 - 8; actor.y = boss.y() - 8;
        set(stats, "cooldownTiro", 0f);
        boss.hurt(boss.health() - 1);
        int killsBefore = session.progress().kills();
        fireAtBoss(room, boss);
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
        game.selectSave(2, false); press(game.getScreen(), Input.Keys.NUM_2);
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
        for (int line = 0; line < 4; line++) press(refuge, Input.Keys.ENTER);
        check(game.profiles().profile().has(CampaignProfile.Event.BRIBE_JOKE), "Caronte must recognize death");
        press(refuge, Input.Keys.C);
        for (int line = 0; line < 3; line++) press(refuge, Input.Keys.ENTER);
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
            check(Math.abs(enemy.vida - enemy.getType().health * session.enemyDifficulty().healthMultiplier()) < 0.001f,
                "New enemy health must use total attempt time");
            check(Math.abs(enemy.getDano() - enemy.getType().damage * session.enemyDifficulty().damageMultiplier()) < 0.001f,
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
        set(session.player(), "cooldownTiro", 0f); float life = boss.health(), realDamage = session.player().getDano();
        for (int frame = 0; frame < 8 && boss.health() == life; frame++) fireAtBoss(room, boss);
        check(Math.abs(life - boss.health() - realDamage) < 0.001f, "Forged damage must reach the floor boss");
        screenshot("boss-rare-weapon");
        press(room, Input.Keys.ESCAPE); press(room, Input.Keys.M); press(game.getScreen(), Input.Keys.ENTER);
        check(((RunSession) field(game.getScreen(), "session")).player().getWeaponRarity() == WeaponRarity.COMMON, "A new attempt must reset weapon rarity");
    }
    @SuppressWarnings("unchecked")
    private static void verifyRareWeapons(Main game) throws Exception {
        for (Profession profession : Profession.values()) {
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
        press(game.getScreen(), Input.Keys.NUM_4); check(game.getScreen() instanceof RefugeScreen, "Fourth profession must enter refuge");
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
        RunSession retry = RunSession.campaign(game.campaignRegion(), Profession.APOSTADOR, PermanentBonuses.NONE, new Rolls(0.5f, 0));
        game.setScreen(new GameScreen(game, retry)); meetGoals(retry); retry.enterArena("sombras");
        game.setScreen(new GameScreen(game, retry)); retry.advance(45); retry.markMiniBossSpawned();
        check(!retry.recordMiniBossDefeat() && game.profiles().collectForgeMaterials(retry) == 0, "A different room or run must not rearm the region");
        check(retry.encounters().materialChance() == 0 && game.profiles().profile().forgeMaterials() == 0, "Luck must respect the persistent material cap");
        game.getScreen().render(0); screenshot("material-region-cap");
        game.finishRun(retry, RunOutcome.ABANDONED);
        check(!game.profiles().chooseProfession(Profession.CACADOR), "Apostador must remain fixed to its save");
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
    private static void press(Screen screen, int key) {
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
