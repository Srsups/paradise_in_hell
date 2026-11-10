package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class RunReportExportTest {
    @TempDir Path directory;
    private static String id(int value){return UUID.nameUUIDFromBytes(("export-fixture-"+value).getBytes(StandardCharsets.UTF_8)).toString();}
    private static RunReport report(int value,String title,String cause,float seconds,Float unlocked) {
        var room=new RunReport.Room("estige",title,seconds,10,100,85,32.75f,15,unlocked,
            value==99?new BossFightSummary(seconds,0,Map.of(BossEncounter.Attack.CHARGE,2)):null);
        return new RunReport(id(value),value%SaveSlots.COUNT+1,false,Profession.values()[value%Profession.values().length],WeaponRarity.RARE,
            RunOutcome.DEATH,seconds,4,10,3,7,cause,Map.of(DamageSource.WEAPON,30.5f,DamageSource.SHOCK,2.25f),15,
            List.of(room),Map.of(Upgrade.FORCA,2,Upgrade.CADENCIA,1),0,id(1000+value),GameDifficulty.HARD);
    }
    private static RunReport report(int value){return report(value,"Margem do Estige","Pedras do Carrasco",12.5f,8f);}
    private static RunReportExport export(List<RunReport> history,RunReport opened) {
        return RunReportExport.create(history,opened,Instant.parse("2026-11-04T12:30:15Z"),id(9000));
    }
    private static String field(List<List<String>> csv,int row,String column){return csv.get(row).get(csv.getFirst().indexOf(column));}

    @Test void jsonKeepsCompleteImmutableReportsAcrossClassesAndTheOpenedReportWithoutDuplicates() {
        List<RunReport> history=new ArrayList<>();for(int i=0;i<25;i++)history.add(report(i));
        List<RunReport> before=List.copyOf(history);RunReport opened=report(100);
        RunReportExport bundle=export(history,opened);JsonValue json=new JsonReader().parse(bundle.json());
        assertEquals(21,bundle.reports().size());assertEquals(21,json.getInt("reportCount"));assertEquals(20,json.getInt("historyLimit"));
        assertTrue(json.getBoolean("includesOpenedReportOutsideHistory"));assertEquals("2026-11-04T12:30:15Z",json.getString("exportedAtUtc"));
        assertEquals("paradise-in-hell-run-history",json.getString("type"));assertEquals(1,json.getInt("version"));
        for(int i=0;i<20;i++)assertEquals(history.get(i),RunReport.fromJson(json.get("reports").get(i)));
        assertEquals(opened,RunReport.fromJson(json.get("reports").get(20)));assertEquals(before,history);
        assertThrows(UnsupportedOperationException.class,()->bundle.reports().clear());
        RunReportExport existing=export(history,history.get(3));
        assertEquals(20,existing.reports().size());assertFalse(new JsonReader().parse(existing.json()).getBoolean("includesOpenedReportOutsideHistory"));
        assertEquals(1,existing.reports().stream().filter(r->r.id().equals(history.get(3).id())).count());
    }

    @Test void attemptsAndRoomsUseInvariantDecimalsAndOnlyRecordedContext() {
        Locale previous=Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);RunReportExport bundle=export(List.of(report(0)),report(0));
            List<List<String>> attempts=csv(bundle.attemptsCsv()),rooms=csv(bundle.roomsCsv());
            assertEquals("12.5",field(attempts,1,"duration_seconds"));assertEquals("32.75",field(attempts,1,"effective_damage_dealt"));
            assertEquals("2.620000",field(attempts,1,"effective_dps"));assertEquals("14.400000",field(attempts,1,"obols_per_minute"));
            assertEquals("33.600000",field(attempts,1,"souls_per_minute"));assertEquals("3",field(attempts,1,"boon_choices"));
            assertEquals("HARD",field(attempts,1,"difficulty"));assertEquals("estige",field(attempts,1,"route_ids"));
            for(String absent:List.of("initial_permanent_bonuses","arena_variations","balance_revision"))assertEquals("",field(attempts,1,absent));
            assertEquals("8.0",field(rooms,1,"unlocked_at_seconds"));assertEquals("4.5",field(rooms,1,"time_after_unlock_seconds"));
            assertEquals("",field(rooms,1,"boss_phase_1_seconds"));assertEquals("",field(rooms,1,"boss_charge_started"));
        } finally {Locale.setDefault(previous);}
    }

    @Test void undefinedRatesUnreachedExitsAndMissingBossMetricsStayEmptyInsteadOfInventingZeros() {
        RunReport empty=report(1,"Sala não concluída","Contato",0,null);
        RunReportExport bundle=export(List.of(empty),empty);List<List<String>> attempts=csv(bundle.attemptsCsv()),rooms=csv(bundle.roomsCsv());
        for(String absent:List.of("effective_dps","obols_per_minute","souls_per_minute"))assertEquals("",field(attempts,1,absent));
        for(String absent:List.of("unlocked_at_seconds","time_after_unlock_seconds","effective_dps","boss_phase_1_seconds","boss_phase_2_seconds","boss_charge_started"))
            assertEquals("",field(rooms,1,absent));
        RunReport boss=report(99,"Guardião","Investida",20,20f);List<List<String>> bossCsv=csv(export(List.of(boss),boss).roomsCsv());
        assertEquals("20.0",field(bossCsv,1,"boss_phase_1_seconds"));assertEquals("0.0",field(bossCsv,1,"boss_phase_2_seconds"));
        assertEquals("2",field(bossCsv,1,"boss_charge_started"));assertEquals("0",field(bossCsv,1,"boss_leap_started"));
        assertEquals("0.0",field(bossCsv,1,"time_after_unlock_seconds"));
    }

    @Test void csvQuotesMultilineUnicodeAndNeutralizesSpreadsheetFormulaTextWhileJsonKeepsOriginalText() {
        String title="  \t=HYPERLINK(\"arquivo\",\"Ação do Guardião\")\nsegunda linha, com vírgula";
        List<RunReport> reports=new ArrayList<>();
        for(int i=0;i<4;i++)reports.add(report(i,i==0?title:new String[]{"=1+1","+1+1","-1+1","@SUM(A1:A2)"}[i],
            new String[]{"=1+1","+1+1","-1+1","@SUM(A1:A2)"}[i],12.5f,8f));
        RunReportExport bundle=export(reports,reports.getFirst());List<List<String>> attempts=csv(bundle.attemptsCsv()),rooms=csv(bundle.roomsCsv());
        assertEquals("'"+title,field(rooms,1,"room_title"));
        for(int i=0;i<4;i++)assertEquals("'"+reports.get(i).deathCause(),field(attempts,i+1,"death_cause"));
        JsonValue json=new JsonReader().parse(bundle.json());
        assertEquals(title,RunReport.fromJson(json.get("reports").get(0)).rooms().getFirst().title());
        assertEquals(reports.getFirst().deathCause(),RunReport.fromJson(json.get("reports").get(0)).deathCause());
    }

    @Test void exportingDoesNotChangeSharedRewardsReportsOrPersonalProgressAndFailureCanBeRetried() {
        SharedProgress shared=new SharedProgress();RunReport report=report(0);shared.record(report);
        CampaignProfile personal=new CampaignProfile();String sharedBefore=shared.toJson(),personalBefore=personal.toJson();
        class Store implements ReportExportStore {
            boolean fail=true;int calls;RunReportExport saved;
            @Override public String save(RunReportExport value)throws IOException {
                calls++;if(fail)throw new IOException("fixture");saved=value;return "memória/Relatórios do Guardião";
            }
        }
        Store store=new Store();ReportExportService service=new ReportExportService(store);
        assertFalse(service.export(shared.history(),report));assertFalse(service.error().isEmpty());assertEquals("",service.directory());
        assertEquals(sharedBefore,shared.toJson());assertEquals(personalBefore,personal.toJson());
        store.fail=false;assertTrue(service.export(shared.history(),report));assertEquals(2,store.calls);
        assertEquals("",service.error());assertEquals("memória/Relatórios do Guardião",service.directory());assertEquals(List.of(report),store.saved.reports());
        assertEquals(sharedBefore,shared.toJson());assertEquals(personalBefore,personal.toJson());
    }

    @Test void fileBundleCommitsThreeUtf8FilesAndNeverOverwritesAnExistingExport()throws Exception {
        RunReportExport bundle=export(List.of(report(0)),report(0));Path base=directory.resolve("exports");
        FileReportExportStore store=new FileReportExportStore(base);Path saved=Path.of(store.save(bundle));
        assertEquals(base.toAbsolutePath(),saved.getParent());assertTrue(saved.getFileName().toString().startsWith("run-history-"));
        assertEquals(bundle.json(),Files.readString(saved.resolve(RunReportExport.JSON_FILE),StandardCharsets.UTF_8));
        assertEquals(bundle.attemptsCsv(),Files.readString(saved.resolve(RunReportExport.ATTEMPTS_FILE),StandardCharsets.UTF_8));
        assertEquals(bundle.roomsCsv(),Files.readString(saved.resolve(RunReportExport.ROOMS_FILE),StandardCharsets.UTF_8));
        try(var files=Files.list(saved)){assertEquals(3,files.count());}
        try(var folders=Files.list(base)){assertEquals(1,folders.count());}
        assertThrows(IOException.class,()->store.save(bundle));assertEquals(bundle.json(),Files.readString(saved.resolve(RunReportExport.JSON_FILE)));
    }

    @Test void partialIoFailureLeavesNoPublishedBundleOrTemporaryFilesAndManualRetryCommitsAllFiles()throws Exception {
        Path base=directory.resolve("exports");boolean[] fail={true};
        FileReportExportStore store=new FileReportExportStore(base,(file,content)->{
            Files.writeString(file,content,StandardCharsets.UTF_8);
            if(fail[0]&&file.getFileName().toString().equals(RunReportExport.ATTEMPTS_FILE))throw new IOException("disk full after partial write");
        });
        ReportExportService service=new ReportExportService(store);RunReport report=report(0);
        assertFalse(service.export(List.of(report),report));try(var folders=Files.list(base)){assertEquals(0,folders.count());}
        fail[0]=false;assertTrue(service.export(List.of(report),report));Path saved=Path.of(service.directory());
        try(var files=Files.list(saved)){assertEquals(3,files.count());}
        assertEquals(report,RunReport.fromJson(new JsonReader().parse(Files.readString(saved.resolve(RunReportExport.JSON_FILE))).get("reports").get(0)));
        try(var folders=Files.list(base)){assertEquals(1,folders.count());}
    }

    /** Independent small RFC-style reader verifies columns and embedded newlines rather than matching writer internals. */
    private static List<List<String>> csv(String text) {
        List<List<String>> rows=new ArrayList<>();List<String> row=new ArrayList<>();StringBuilder cell=new StringBuilder();boolean quoted=false;
        for(int i=0;i<text.length();i++) {
            char ch=text.charAt(i);
            if(ch=='"') {
                if(quoted&&i+1<text.length()&&text.charAt(i+1)=='"'){cell.append('"');i++;}else quoted=!quoted;
            } else if(!quoted&&ch==','){row.add(cell.toString());cell.setLength(0);}
            else if(!quoted&&ch=='\r'&&i+1<text.length()&&text.charAt(i+1)=='\n'){
                row.add(cell.toString());cell.setLength(0);rows.add(List.copyOf(row));row.clear();i++;
            } else cell.append(ch);
        }
        assertFalse(quoted);assertEquals(0,cell.length());assertTrue(row.isEmpty());
        for(List<String> value:rows)assertEquals(rows.getFirst().size(),value.size());return rows;
    }
}
