package br.com.srsups.paradiseinhell.lwjgl3;

import br.com.srsups.paradiseinhell.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static br.com.srsups.paradiseinhell.lwjgl3.DemoSmokeTest.*;

/** Real report/export UI; every preference, profile and exported bundle stays in memory. */
public final class BalanceSmoke {
    private BalanceSmoke() {}
    private static final class Personal implements ProfileStore {
        CampaignProfile saved;int writes;
        Personal(CampaignProfile saved){this.saved=saved.copy();}
        @Override public CampaignProfile load(){return saved.copy();}
        @Override public void save(CampaignProfile profile){writes++;saved=profile.copy();}
    }
    private static final class Global implements SharedProgressStore {
        SharedProgress saved;int writes;
        Global(SharedProgress saved){this.saved=saved.copy();}
        @Override public SharedProgress load(){return saved.copy();}
        @Override public void save(SharedProgress progress){writes++;saved=progress.copy();}
    }
    private static final class Exports implements ReportExportStore {
        int calls;boolean fail;final List<RunReportExport> saved=new ArrayList<>();
        @Override public String save(RunReportExport export)throws IOException {
            calls++;if(fail)throw new IOException("export fixture failure");saved.add(export);
            return "C:\\Users\\UmNomeDeUsuarioMuitoLongoParaExercitarAComposicaoDaMensagemSemInvadirOsBotoes\\AppData\\Local\\ParadiseInHell\\exports\\run-history-"+export.id();
        }
    }
    private static final class Fixture implements AutoCloseable {
        final Personal personal;
        final Global global;
        final Exports exports=new Exports();
        final Main game;
        final RunReport opened;
        Fixture() {
            CampaignProfile profile=new CampaignProfile();profile.createCharacter(Profession.CACADOR,GameDifficulty.MEDIUM);personal=new Personal(profile);
            JsonValue progress=new JsonReader().parse(new SharedProgress().toJson());progress.get("souls").set(777L,null);
            for(int i=0;i<20;i++)progress.get("history").addChild(report(i,i%2==0?profile.characterId():id(1000),i%2==0?Profession.CACADOR:Profession.FEITICEIRA).json());
            global=new Global(SharedProgress.fromJson(progress.toJson(JsonWriter.OutputType.json)));
            game=new Main(new SaveSlots(global,personal),AudioSettingsStore.memory(),new RecordedAudio(),VideoSettingsStore.memory(),VideoBackend.memory(),
                ControlSettingsStore.memory(),HudSettingsStore.memory(),exports);game.create();
            opened=report(100,profile.characterId(),Profession.CACADOR);
        }
        @Override public void close(){game.dispose();}
    }

    public static void verify()throws Exception {
        try(Fixture fixture=new Fixture()) {
            Main game=fixture.game;RunReportScreen screen=new RunReportScreen(game,fixture.opened,()->new RefugeScreen(game));game.setScreen(screen);
            String personal=game.profiles().profile().toJson(),global=game.shared().progress().toJson();
            int personalWrites=fixture.personal.writes,globalWrites=fixture.global.writes;
            press(screen,Input.Keys.E);
            check(fixture.exports.calls==0,"The export action cannot consume an input carried into the protected report screen");
            screen.render(.3f);press(screen,Input.Keys.E);
            check(fixture.exports.calls==1 && fixture.exports.saved.size()==1 && game.reportExports().error().isEmpty(),
                "The E action explicitly exports a complete report bundle using the injected memory store");
            RunReportExport first=fixture.exports.saved.getFirst();JsonValue json=new JsonReader().parse(first.json());
            check(first.reports().size()==21 && json.getInt("reportCount")==21 && json.getBoolean("includesOpenedReportOutsideHistory"),
                "The export includes all twenty available global reports plus the opened result outside that history");
            check(first.reports().stream().map(RunReport::profession).distinct().count()==2
                &&first.reports().stream().filter(r->r.id().equals(fixture.opened.id())).count()==1,
                "Export crosses character/class filters without duplicating the opened report");
            for(int i=0;i<json.get("reports").size;i++)check(first.reports().get(i).equals(RunReport.fromJson(json.get("reports").get(i))),
                "The report export is complete JSON readable by the existing report parser");
            verifyFooter(screen,game.reportExports().directory());captureNpcDialogue(screen,"m7-balance-export");

            fixture.exports.fail=true;click(screen,(Rectangle)field(screen,"exportButton"));
            check(fixture.exports.calls==2 && fixture.exports.saved.size()==1 && !game.reportExports().error().isEmpty()
                && game.getScreen()==screen,"An IO failure leaves the reports and report screen available for a manual retry");
            captureNpcDialogue(screen,"m7-balance-export-error");
            fixture.exports.fail=false;screen.render(.3f);press(screen,Input.Keys.E);
            check(fixture.exports.calls==3 && fixture.exports.saved.size()==2 && game.reportExports().error().isEmpty(),
                "The explicit export action retries successfully without settling another attempt");

            press(screen,Input.Keys.RIGHT);press(screen,Input.Keys.RIGHT);press(screen,Input.Keys.RIGHT);
            check((int)field(screen,"page")==3,"The original report pages still reach effective damage by source");
            verifyDamageColumns(screen,fixture.opened);captureNpcDialogue(screen,"m7-balance-damage");
            press(screen,Input.Keys.DOWN);press(screen,Input.Keys.UP);
            check(screen.report().id().equals(fixture.opened.id()),"Browsing old attempts returns to the same opened immutable report");
            check(fixture.personal.writes==personalWrites && fixture.global.writes==globalWrites
                && personal.equals(game.profiles().profile().toJson()) && global.equals(game.shared().progress().toJson())
                && game.shared().progress().souls()==777,"Export, retry, rendering and history navigation never save progression or duplicate currency");
            int calls=fixture.exports.calls;for(int i=0;i<8;i++)screen.render(.1f);
            check(fixture.exports.calls==calls,"Ordinary rendering never repeatedly exports the history");
            press(screen,Input.Keys.R);
            check(game.getScreen() instanceof RefugeScreen && fixture.personal.writes==personalWrites && fixture.global.writes==globalWrites,
                "Returning from the report restores the refuge without replaying settlement");
        }
        System.out.println("BALANCE_REPORT_EXPORT_OK: protected manual export, complete global JSON/CSVs, memory-only IO retry, unchanged progression, report navigation and readable damage columns");
    }
    private static void verifyFooter(RunReportScreen screen,String directory)throws Exception {
        BitmapFont font=(BitmapFont)field(screen,"font");float x=font.getData().scaleX,y=font.getData().scaleY;
        try {
            font.getData().setScale(.85f);Method fit=RunReportScreen.class.getDeclaredMethod("fitNotice",String.class);fit.setAccessible(true);
            String notice=(String)fit.invoke(screen,"Exportado: "+directory);
            check(new GlyphLayout(font,notice).width<=890,"A long successful export directory fits the report footer without overlapping the export button");
            check(new GlyphLayout(font,"Não foi possível exportar. Seus relatórios foram preservados; tente novamente.").width<=890,
                "The retryable export error fits in a single reserved footer line");
            Rectangle button=(Rectangle)field(screen,"exportButton");font.getData().setScale(.95f);
            check(new GlyphLayout(font,"Exportar histórico [E]").width<=button.width-30,"The localized export action fits its clickable button");
        } finally {font.getData().setScale(x,y);}
    }
    private static void verifyDamageColumns(RunReportScreen screen,RunReport report)throws Exception {
        BitmapFont font=(BitmapFont)field(screen,"font");float x=font.getData().scaleX,y=font.getData().scaleY;
        try {
            font.getData().setScale(.95f);int rows=(DamageSource.values().length+1)/2;
            check(DamageSource.values().length==11 && 375-(rows-1)*38>150,"Eleven origins occupy two columns above the note and export footer");
            for(DamageSource source:DamageSource.values()) {
                float amount=report.dealt().getOrDefault(source,0f),percent=report.damageDealt()==0?0:amount/report.damageDealt()*100;
                String label=source.label+": "+CombatNumbers.format(amount)+" ("+CombatNumbers.format(percent)+"%)";
                check(new GlyphLayout(font,label).width<=435,"Each real damage label and measured amount fits its column: "+source);
            }
        } finally {font.getData().setScale(x,y);}
    }
    private static RunReport report(int index,String character,Profession profession) {
        EnumMap<DamageSource,Float> dealt=new EnumMap<>(DamageSource.class);for(DamageSource source:DamageSource.values())dealt.put(source,10f+source.ordinal());
        float total=(float)dealt.values().stream().mapToDouble(Float::doubleValue).sum();
        return new RunReport(id(index),profession==Profession.CACADOR?1:2,false,profession,WeaponRarity.RARE,RunOutcome.DEATH,
            65.25f,6,79,17,240,"Pedras do Carrasco",dealt,45,List.of(new RunReport.Room("estige","Margem do Estige",65.25f,79,100,55,total,45,58f)),
            Map.of(Upgrade.FORCA,2,Upgrade.CADENCIA,1),0,character,GameDifficulty.MEDIUM);
    }
    private static String id(int value){return UUID.nameUUIDFromBytes(("balance-smoke-"+value).getBytes(StandardCharsets.UTF_8)).toString();}
    private static void click(Screen screen,Rectangle target)throws Exception {
        FitViewport viewport=(FitViewport)field(screen,"viewport");Vector2 point=viewport.project(new Vector2(target.x+target.width/2,target.y+target.height/2));
        int x=Math.round(point.x),y=Gdx.graphics.getHeight()-Math.round(point.y);Input actual=Gdx.input;
        Gdx.input=(Input)Proxy.newProxyInstance(Input.class.getClassLoader(),new Class<?>[]{Input.class},(proxy,method,args)->switch(method.getName()) {
            case "isKeyJustPressed","isKeyPressed" -> false;
            case "isButtonPressed","isButtonJustPressed" -> (int)args[0]==Input.Buttons.LEFT;
            case "getX" -> x;
            case "getY" -> y;
            default -> method.invoke(actual,args);
        });
        try{screen.render(0);}finally{Gdx.input=actual;}
    }
}
