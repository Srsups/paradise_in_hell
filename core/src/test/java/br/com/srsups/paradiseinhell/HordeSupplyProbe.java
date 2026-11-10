package br.com.srsups.paradiseinhell;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

/** Ideal supply floor, not an automated playthrough: every generated enemy is killed immediately. */
public final class HordeSupplyProbe {
    public static final int SAMPLES = 1_000;
    private static final float STEP = 1f / 60;
    private static final float LIMIT_SECONDS = 1_800;
    private HordeSupplyProbe() {}

    public record Sample(String arena, ArenaVariation variation, long seed, float seconds,
                         int common, int fast, int tank, int archers, int flyers, int baseXp, int baseSouls,
                         float commonGoalSeconds, float fastGoalSeconds, float tankGoalSeconds) {
        public int kills() { return common + fast + tank; }
    }

    public static RegionDefinition region() {
        try (InputStream stream = HordeSupplyProbe.class.getClassLoader().getResourceAsStream(RegionDefinition.PROTOTYPE_ASSET)) {
            if (stream == null) throw new IllegalStateException("Missing campaign resource: " + RegionDefinition.PROTOTYPE_ASSET);
            return RegionDefinition.fromJson(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException failure) { throw new IllegalStateException("Cannot read campaign baseline resource", failure); }
    }

    /** Uses the live run's timing/goal methods and EnemyWaves; a fixed variation isolates the room's supply. */
    public static Sample sample(RegionDefinition region, String arenaId, ArenaVariation variation, long seed,
                                GameDifficulty difficulty) {
        RunSession run = RunSession.campaign(region, Profession.CACADOR, PermanentBonuses.NONE, difficulty, new RunRandom(seed));
        if (!arenaId.equals(region.start())) {
            // Reach the optional room without charging it the unrelated duration/rewards of the preceding room.
            for (var goal : run.arena().killGoals().entrySet())
                while (run.kills(goal.getKey()) < goal.getValue()) run.recordKill(goal.getKey());
            run.enterArena(arenaId);
        }
        if (run.arena().kind() != ArenaDefinition.Kind.COMBAT) throw new IllegalArgumentException("The probe requires a combat room");
        java.util.Objects.requireNonNull(variation);
        EnemyWaves waves = new EnemyWaves(new Random(seed));
        HordeSchedule hordes=new HordeSchedule(HordeSchedule.seedFor("baseline-"+seed,arenaId));
        int[] generated = new int[EnemyType.values().length];
        float[] goalTimes = new float[EnemyType.values().length];
        Arrays.fill(goalTimes, Float.NaN);
        float timer = 0;
        int archers = 0, flyers=0, xp = 0, souls = 0;
        while (!run.arenaComplete() && run.arenaElapsed() < LIMIT_SECONDS) {
            run.advance(STEP);
            var packs=hordes.update(run.arenaElapsed(),run.progress().elapsed(),true,variation,0);
            if(!hordes.allowsRegularSpawns())timer=0;else timer+=STEP;
            for(var pack:packs)for(int index=0;index<pack.count();index++){
                EnemyType type=pack.kind().enemyType();boolean flyer=pack.kind()==HordeSchedule.Kind.FLYER;
                generated[type.ordinal()]++;if(flyer)flyers++;
                xp+=flyer?StygianFlyer.XP:type.xp;
                int reward=flyer?StygianFlyer.SOULS:SoulRewards.enemy(type);souls+=reward;
                run.recordKill(type,reward);
                Integer goal=run.arena().killGoals().get(type);
                if(goal!=null&&Float.isNaN(goalTimes[type.ordinal()])&&run.kills(type)>=goal)
                    goalTimes[type.ordinal()]=run.arenaElapsed();
            }
            while (timer >= run.spawnInterval()) {
                timer -= run.spawnInterval();
                int waveArchers = 0;
                for (EnemyType type : waves.next(run.waveSize(), run.difficultyTime(), true, variation, 0)) {
                    boolean archer = waves.archer(type, run.progress().elapsed(), true, waveArchers, waveArchers);
                    if (archer) { waveArchers++; archers++; }
                    generated[type.ordinal()]++;
                    xp += archer ? ArcherAttack.XP : type.xp;
                    int reward = archer ? ArcherAttack.SOULS : SoulRewards.enemy(type);
                    souls += reward;
                    run.recordKill(type, reward);
                    Integer goal = run.arena().killGoals().get(type);
                    if (goal != null && Float.isNaN(goalTimes[type.ordinal()]) && run.kills(type) >= goal)
                        goalTimes[type.ordinal()] = run.arenaElapsed();
                }
            }
        }
        if (!run.arenaComplete()) throw new IllegalStateException("Goals not supplied within " + LIMIT_SECONDS + " seconds: " + arenaId);
        return new Sample(arenaId, variation, seed, run.arenaElapsed(), generated[0], generated[1], generated[2], archers, flyers, xp, souls,
            goalTimes[0], goalTimes[1], goalTimes[2]);
    }

    public static String csv() {
        RegionDefinition region = region();
        StringBuilder result = new StringBuilder("arena,variation,samples,mean_seconds,p50_seconds,p90_seconds,mean_common,mean_fast,mean_tank,mean_archers,mean_flyers,mean_excess_kills,mean_base_xp,mean_base_souls,p50_common_goal_seconds,p50_fast_goal_seconds,p50_tank_goal_seconds\n");
        for (ArenaDefinition arena : region.arenas()) {
            if (arena.kind() != ArenaDefinition.Kind.COMBAT) continue;
            for (ArenaVariation variation : ArenaVariation.values()) {
                Sample[] samples = new Sample[SAMPLES];
                for (int seed = 0; seed < samples.length; seed++) samples[seed] = sample(region, arena.id(), variation, seed, GameDifficulty.MEDIUM);
                double[] times = Arrays.stream(samples).mapToDouble(Sample::seconds).sorted().toArray();
                double[] commonTimes = Arrays.stream(samples).mapToDouble(Sample::commonGoalSeconds).sorted().toArray();
                double[] fastTimes = Arrays.stream(samples).mapToDouble(Sample::fastGoalSeconds).sorted().toArray();
                double[] tankTimes = Arrays.stream(samples).mapToDouble(Sample::tankGoalSeconds).sorted().toArray();
                double goals = arena.killGoals().values().stream().mapToInt(Integer::intValue).sum();
                result.append(String.format(Locale.ROOT,
                    "%s,%s,%d,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f%n",
                    arena.id(), variation.name(), samples.length, mean(times), percentile(times, .5), percentile(times, .9),
                    Arrays.stream(samples).mapToInt(Sample::common).average().orElseThrow(),
                    Arrays.stream(samples).mapToInt(Sample::fast).average().orElseThrow(),
                    Arrays.stream(samples).mapToInt(Sample::tank).average().orElseThrow(),
                    Arrays.stream(samples).mapToInt(Sample::archers).average().orElseThrow(),
                    Arrays.stream(samples).mapToInt(Sample::flyers).average().orElseThrow(),
                    Arrays.stream(samples).mapToInt(Sample::kills).average().orElseThrow() - goals,
                    Arrays.stream(samples).mapToInt(Sample::baseXp).average().orElseThrow(),
                    Arrays.stream(samples).mapToInt(Sample::baseSouls).average().orElseThrow(),
                    percentile(commonTimes, .5), percentile(fastTimes, .5), percentile(tankTimes, .5)));
            }
        }
        return result.toString();
    }

    private static double mean(double[] values) { return Arrays.stream(values).average().orElseThrow(); }
    /** Nearest-rank percentiles; the definition is stable even when the sample count changes. */
    private static double percentile(double[] sorted, double percentile) { return sorted[Math.max(0, (int) Math.ceil(sorted.length * percentile) - 1)]; }

    public static String notes() {
        return "Piso de fornecimento: 1.000 sementes fixas (0..999), 60 atualizações/s e métodos reais de EnemyWaves/RunSession para tempo e metas.\n"
            + "Cada sala é isolada com tempo total inicial zero; difficultySeconds e waveIntervalMultiplier configurados continuam aplicados.\n"
            + "A variação é fixada explicitamente. O RNG interno de encontros/dádivas nunca é consultado pela medição.\n"
            + "Todos os inimigos da última onda são contados/derrotados, mesmo se um anterior já completar as metas.\n"
            + "Inimigos morrem imediatamente, sem voo, mira, dano, colisão ou espera pela coleta; rápidos ativos e arqueiros de ondas anteriores são zero.\n"
            + "A conversão de arqueiros usa o RNG compartilhado real das ondas, tempo mínimo e limites por onda; arqueiros contam como COMMON.\n"
            + "HordeSchedule interrompe as ondas normais por 3s de aviso, 5s de bando e 4s de respiro a cada 40s; primeira horda aos 35s.\n"
            + "O primeiro bando da campanha é de voadores; depois os quatro papéis entram em ciclos embaralhados sem repetição na fronteira.\n"
            + "Voadores contam para COMMON e usam XP/Almas próprios de StygianFlyer; hordas homogêneas não convertem membros em arqueiros.\n"
            + "O timer normal é descartado fora da fase NORMAL, sem reposição acumulada. Os bandos também morrem imediatamente neste piso.\n"
            + "Não há mini-boss, baú, dádiva, forja, aliado, farming nem chefe. XP/Almas são potencial base antes dos multiplicadores de dificuldade.\n"
            + "O fornecimento é igual em Fácil/Média/Difícil: multiplicadores de vida/dano/recompensa não atrasam esses abates instantâneos ideais.\n"
            + "p50/p90 usam percentis por posto mais próximo. São pisos de disponibilidade dos inimigos, não tempos esperados de conclusão nem prova de equilíbrio.\n";
    }
}
