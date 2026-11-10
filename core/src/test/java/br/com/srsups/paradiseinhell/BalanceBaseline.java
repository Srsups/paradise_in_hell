package br.com.srsups.paradiseinhell;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** Developer analysis, deliberately outside the shipped game and without graphics or save writes. */
public final class BalanceBaseline {
    private BalanceBaseline() {}
    public static void main(String[] args)throws Exception {
        if(args.length<1||args.length>2)throw new IllegalArgumentException("Expected output directory and optional shared-progress JSON");
        Path directory=Path.of(args[0]).toAbsolutePath();Files.createDirectories(directory);
        write(directory.resolve("classes.csv"),ClassBalanceProbe.csv());
        write(directory.resolve("hordes.csv"),HordeSupplyProbe.csv());
        String notes="# Baseline técnico do Submundo\n\n"
            +"Esta medição usa os modelos reais em cenários controlados. Não representa uma partida completa, "
            +"habilidade humana, navegação, acertos sob pressão ou taxa de vitórias.\n\n"
            +"## Profissões e armas\n\n"+ClassBalanceProbe.notes()+"\n\n"
            +"## Fornecimento das metas\n\n"+HordeSupplyProbe.notes()+"\n\n"
            +"## Rodada de partidas\n\n"
            +"Comparar a mesma dificuldade e rota, registrando separadamente personagem novo/evoluído. "
            +"Usar o relatório para tempo até a passagem, dano por origem, chefe e moedas; "
            +"anotar manualmente níveis iniciais do altar, variação da sala e percepção dos rápidos. "
            +"Relatórios antigos não registram esses três campos e não podem recebê-los por inferência.\n";
        if(args.length==2)notes+="\n"+historyNotes(SharedProgress.fromJson(Files.readString(Path.of(args[1]),StandardCharsets.UTF_8)).history());
        write(directory.resolve("README.md"),notes);
        System.out.println("BALANCE_BASELINE_OK: "+directory+" (classes.csv, hordes.csv, README.md)");
    }
    private static void write(Path file,String value)throws Exception {Files.writeString(file,value,StandardCharsets.UTF_8);}

    static String historyNotes(List<RunReport> reports) {
        StringBuilder text=new StringBuilder("## Histórico local disponível\n\n");
        long demos=reports.stream().filter(RunReport::demo).count();
        text.append("Amostra disponível: ").append(reports.size()).append(" relatórios; ").append(demos)
            .append(" de demo. Histórico limitado a 20; sem data nem versão de balanceamento. "
                +"Abandonos são listados separadamente e não contam como derrotas. "
                +"Os dados de origem são somente lidos.\n\n");
        Map<String,List<RunReport>> groups=new TreeMap<>();
        for(RunReport report:reports)if(!report.demo())
            groups.computeIfAbsent(report.profession().title+" / "+report.difficulty().label,unused->new ArrayList<>()).add(report);
        text.append("| Profissão / dificuldade | Tentativas | Vitórias | Mortes | Abandonos | Mediana até saída do Estige (s) | Mediana do chefe alcançado (s) |\n")
            .append("| --- | ---: | ---: | ---: | ---: | ---: | ---: |\n");
        for(var entry:groups.entrySet()) {
            List<RunReport> group=entry.getValue();
            List<Double> exits=group.stream().flatMap(report->report.rooms().stream())
                .filter(room->room.id().equals("estige")&&room.unlockedAt()!=null).map(room->(double)room.unlockedAt()).toList();
            List<Double> bosses=group.stream().flatMap(report->report.rooms().stream())
                .filter(room->room.bossFight()!=null).map(room->(double)room.duration()).toList();
            text.append("| ").append(entry.getKey()).append(" | ").append(group.size()).append(" | ")
                .append(count(group,RunOutcome.VICTORY)).append(" | ").append(count(group,RunOutcome.DEATH)).append(" | ")
                .append(count(group,RunOutcome.ABANDONED)).append(" | ").append(median(exits)).append(" | ").append(median(bosses)).append(" |\n");
        }
        Map<String,Integer> causes=new TreeMap<>();
        for(RunReport report:reports)if(!report.demo()&&report.outcome()==RunOutcome.DEATH)
            causes.merge(report.deathCause().isBlank()?"Não registrada":report.deathCause(),1,Integer::sum);
        text.append("\nCausas finais registradas (não são o total de acertos/dano por inimigo):\n\n");
        if(causes.isEmpty())text.append("Nenhuma morte registrada.\n");
        for(var cause:causes.entrySet())text.append("- ").append(cause.getKey().replace('\n',' ').replace('\r',' ')).append(": ").append(cause.getValue()).append(".\n");
        text.append("\nA amostra mistura builds, experiência do jogador e possíveis versões antigas. "
            +"Estes números orientam a próxima coleta; não demonstram que uma profissão está equilibrada ou dominante.\n");
        return text.toString();
    }
    private static long count(List<RunReport> reports,RunOutcome outcome){return reports.stream().filter(r->r.outcome()==outcome).count();}
    private static String median(List<Double> input) {
        if(input.isEmpty())return "—";
        List<Double> values=input.stream().sorted().toList();int middle=values.size()/2;
        double value=values.size()%2==0?(values.get(middle-1)+values.get(middle))/2:values.get(middle);
        return String.format(Locale.ROOT,"%.2f",value);
    }
}
