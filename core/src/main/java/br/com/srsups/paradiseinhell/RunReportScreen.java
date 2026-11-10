package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Inspect immutable summaries; browsing never reruns settlement or changes the character. */
public final class RunReportScreen extends ScreenAdapter {
    private final Main game;
    private final Supplier<Screen> returnTo;
    private final List<RunReport> history;
    private int index,page;
    private final SpriteBatch batch=new SpriteBatch();
    private final ShapeRenderer shapes=new ShapeRenderer();
    private final BitmapFont font=UiTheme.bodyFont();
    private final FitViewport viewport=new FitViewport(960,540);
    private final Rectangle exportButton=new Rectangle(650,16,275,34);
    private float inputDelay=.2f;
    private boolean mouseReleased,exportRequested;
    public RunReportScreen(Main game,RunReport report,Supplier<Screen> returnTo) {
        this.game=game; this.returnTo=returnTo;
        history=new ArrayList<>(game.shared().progress().history(report.saveSlot()-1,report.characterId()));
        if(history.stream().noneMatch(r->r.id().equals(report.id()))) history.addFirst(report);
        for(int i=0;i<history.size();i++) if(history.get(i).id().equals(report.id())) index=i;
    }
    public RunReport report() { return history.get(index); }
    private int roomPages() { return Math.max(1,(report().rooms().size()+4)/5); }
    private int boonPages() { return Math.max(1,(report().boons().size()+9)/10); }
    private List<RunReport.Room> bossRooms() { return report().rooms().stream().filter(r->r.bossFight()!=null).toList(); }
    public int pageCount() { return 2+roomPages()+boonPages()+bossRooms().size(); }
    @Override public void show() { Gdx.input.setInputProcessor(null); }
    @Override public void resize(int width,int height) { viewport.update(width,height,true); }
    @Override public void render(float delta) {
        inputDelay=Math.max(0,inputDelay-delta);
        mouseReleased|=!Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        Vector2 pointer=viewport.unproject(new Vector2(Gdx.input.getX(),Gdx.input.getY()));
        if(inputDelay==0) {
            if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.R)) {
                game.setScreen(returnTo.get()); return;
            }
            if(Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) page=Math.min(pageCount()-1,page+1);
            if(Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) page=Math.max(0,page-1);
            if(Gdx.input.isKeyJustPressed(Input.Keys.DOWN) && index+1<history.size()) { index++; page=0; }
            if(Gdx.input.isKeyJustPressed(Input.Keys.UP) && index>0) { index--; page=0; }
            if(Gdx.input.isKeyJustPressed(Input.Keys.E)
                ||mouseReleased&&Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)&&exportButton.contains(pointer)) {
                game.reportExports().export(game.shared().progress().history(),report());exportRequested=true;
            }
        }
        ScreenUtils.clear(.045f,.055f,.085f,1); viewport.apply(); batch.setProjectionMatrix(viewport.getCamera().combined);
        shapes.setProjectionMatrix(viewport.getCamera().combined);shapes.begin(ShapeRenderer.ShapeType.Filled);
        UiTheme.panel(shapes,exportButton.x,exportButton.y,exportButton.width,exportButton.height,exportButton.contains(pointer));shapes.end();
        batch.begin(); RunReport r=report();
        text("RESUMO DA TENTATIVA",35,504,1.6f,Color.GOLD);
        text("Save "+r.saveSlot()+" | "+r.profession().title+" | "+(r.demo()?"Demo":"Campanha")+" | "+r.resultLabel()+" | "+r.difficulty().label,35,462,1,Color.WHITE);
        if(page==0) overview(r);
        else if(page<=roomPages()) rooms(r,page-1);
        else if(page<=roomPages()+boonPages()) boons(r,page-roomPages()-1);
        else if(page==1+roomPages()+boonPages())damage(r);
        else bossFight(bossRooms().get(page-2-roomPages()-boonPages()));
        text("Esquerda/direita: página "+(page+1)+"/"+pageCount()+" | Acima/baixo: tentativas "+(index+1)+"/"+history.size(),35,60,1,Color.LIGHT_GRAY);
        text("R ou ESC: voltar",35,32,1,Color.LIGHT_GRAY);
        text("Exportar histórico [E]",exportButton.x+44,exportButton.y+23,.95f,UiTheme.IVORY);
        if(exportRequested) {
            ReportExportService service=game.reportExports();boolean failed=!service.error().isEmpty();
            font.getData().setScale(.85f);font.setColor(failed?Color.SALMON:UiTheme.TEAL);
            String notice=failed?service.error():"Exportado: "+service.directory();
            font.draw(batch,fitNotice(notice),35,94);
            if(!failed)text("JSON e CSV de tentativas/salas. Inclui até 20 registros globais e a tentativa aberta.",35,77,.8f,Color.LIGHT_GRAY);
        }
        batch.end();
    }
    private String fitNotice(String value) {
        if(new GlyphLayout(font,value).width<=890)return value;
        int left=Math.min(42,value.length()/3),right=value.length()-left;
        while(right>8) {
            String candidate=value.substring(0,left)+"..."+value.substring(value.length()-right);
            if(new GlyphLayout(font,candidate).width<=890)return candidate;
            right--;
        }
        return value.substring(0,Math.min(80,value.length()));
    }
    private void overview(RunReport r) {
        text("VISÃO GERAL",35,416,1.2f,Color.CYAN);
        text("Tempo: "+RunProgress.formatTime(r.duration())+" | Abates: "+r.kills()+" | Nível final: "+r.level(),35,370,1.15f,Color.WHITE);
        text("Arma: "+r.profession().weapon.label+" ("+r.rarity().label+")",35,330,1.1f,Color.WHITE);
        text("Dano causado: "+CombatNumbers.format(r.damageDealt())+" | Dano recebido: "+CombatNumbers.format(r.received()),35,290,1.1f,Color.WHITE);
        text("Óbolos obtidos: "+r.obols()+" | Almas obtidas: "+r.souls(),35,250,1.1f,Color.GOLD);
        text("Ressurreições usadas: "+r.resurrections()+" | Dádivas distintas: "+r.boons().size(),35,210,1,Color.WHITE);
        if(r.outcome()==RunOutcome.DEATH) {
            text("Causa da morte:",35,165,1,Color.SALMON);
            font.draw(batch,r.deathCause(),35,139,880,8,true);
        }
    }
    private void rooms(RunReport r,int sheet) {
        text("TRAVESSIA POR SALA",35,416,1.2f,Color.CYAN);
        for(int i=sheet*5;i<Math.min(r.rooms().size(),(sheet+1)*5);i++) {
            RunReport.Room room=r.rooms().get(i); float y=375-(i%5)*58;
            text(room.title()+" | "+RunProgress.formatTime(room.duration())+" | "+room.kills()+" abates",35,y,1.05f,Color.GOLD);
            text("Vida: "+CombatNumbers.format(room.entryLife())+" para "+CombatNumbers.format(room.exitLife())
                +" | Dano: "+CombatNumbers.format(room.dealt())+" causado / "+CombatNumbers.format(room.received())+" recebido"
                +(room.unlockedAt()==null?" | Meta não concluída":" | Meta em "+RunProgress.formatTime(room.unlockedAt())),35,y-23,.9f,Color.WHITE);
        }
    }
    private void boons(RunReport r,int sheet) {
        text("DÁDIVAS ESCOLHIDAS",35,416,1.2f,Color.CYAN);
        List<Upgrade> choices=java.util.Arrays.stream(Upgrade.values()).filter(r.boons()::containsKey).toList();
        if(choices.isEmpty()) text("Nenhuma dádiva escolhida nesta tentativa.",35,360,1,Color.WHITE);
        for(int i=sheet*10;i<Math.min(choices.size(),(sheet+1)*10);i++) {
            Upgrade boon=choices.get(i);
            text(boon.title+" | "+boon.rarity.label+" | "+r.boons().get(boon)+" escolha(s)",35,375-(i%10)*27,1,
                new Color(boon.rarity.red,boon.rarity.green,boon.rarity.blue,1));
        }
    }
    private void damage(RunReport r) {
        text("DANO EFETIVO POR ORIGEM",35,416,1.2f,Color.CYAN);
        DamageSource[] sources=DamageSource.values();int rows=(sources.length+1)/2;
        for(int index=0;index<sources.length;index++) {
            DamageSource source=sources[index];float x=index<rows?35:490,y=375-(index%rows)*38;
            float amount=r.dealt().getOrDefault(source,0f), percent=r.damageDealt()==0?0:amount/r.damageDealt()*100;
            text(source.label+": "+CombatNumbers.format(amount)+" ("+CombatNumbers.format(percent)+"%)",x,y,.95f,Color.WHITE);
        }
        text("Bloqueios, invulnerabilidade e dano além da vida restante não entram no total.",35,125,.95f,Color.LIGHT_GRAY);
    }
    private void bossFight(RunReport.Room room) {
        BossFightSummary fight=room.bossFight();
        text("PADRÕES DO CHEFE",35,416,1.2f,Color.CYAN);
        text(room.title(),35,375,1.1f,Color.GOLD);
        text("Tempo na fase 1: "+CombatNumbers.format(fight.phaseOneSeconds())+"s | Fase 2: "
            +CombatNumbers.format(fight.phaseTwoSeconds())+"s",35,337,1,Color.WHITE);
        float duration=fight.phaseOneSeconds()+fight.phaseTwoSeconds();
        text("DPS médio efetivo: "+CombatNumbers.format(duration>0?room.dealt()/duration:0)
            +" | Vida de entrada: "+CombatNumbers.format(room.entryLife())+" | Dano recebido: "+CombatNumbers.format(room.received()),35,306,1,Color.WHITE);
        float y=258;
        for(BossEncounter.Attack attack:BossEncounter.Attack.values()) {
            text(attack.label+": "+fight.attacks().getOrDefault(attack,0)+" iniciado(s)",35,y,1,Color.WHITE);y-=31;
        }
        text("Contagem de avisos iniciados; não representa acertos ou ataques concluídos.",35,125,.9f,Color.LIGHT_GRAY);
    }
    private void text(String value,float x,float y,float scale,Color color) {
        font.getData().setScale(scale); font.setColor(color); font.draw(batch,value,x,y);
    }
    @Override public void dispose() { batch.dispose(); shapes.dispose(); font.dispose(); }
}

