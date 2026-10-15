package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import java.util.Arrays;
import java.util.List;

/** Read-only, paged list of the boons already chosen in this attempt. */
public final class BoonPanel {
    private static final int PER_PAGE = 6;
    private final RunSession session;
    private boolean open;
    private int page;
    public BoonPanel(RunSession session) { this.session = session; }
    public boolean open() { return open; }
    public void toggle() { open = !open; page = 0; }
    public void close() { open = false; }
    public List<Upgrade> acquired() { return Arrays.stream(Upgrade.values()).filter(u -> session.rank(u) > 0).toList(); }
    public int pageCount() { return Math.max(1, (acquired().size() + PER_PAGE - 1) / PER_PAGE); }
    public int page() { return page; }
    public void movePage(int direction) { page = Math.max(0, Math.min(pageCount() - 1, page + direction)); }
    public void draw(SpriteBatch batch, ShapeRenderer shapes, BitmapFont font) {
        Gdx.gl.glEnable(GL20.GL_BLEND); Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0, 0, 0, .94f); shapes.rect(0, 0, 960, 540);
        shapes.setColor(.13f, .16f, .23f, 1);
        List<Upgrade> boons = acquired();
        for (int i = page * PER_PAGE; i < Math.min(boons.size(), (page + 1) * PER_PAGE); i++) {
            int slot = i % PER_PAGE;
            shapes.rect(35 + slot % 2 * 455, 300 - slot / 2 * 105, 435, 92);
        }
        shapes.end(); Gdx.gl.glDisable(GL20.GL_BLEND);
        batch.begin(); font.getData().setScale(1.4f); font.setColor(Color.GOLD);
        font.draw(batch, "DÁDIVAS DA TENTATIVA", 35, 505);
        font.getData().setScale(1); font.setColor(Color.WHITE);
        PlayerState player = session.player();
        font.getData().setScale(.9f);
        font.draw(batch, player.getProfession().title + " | "+player.getWeapon().label+" ("+player.getWeaponRarity().label+")"
            +" | Vida: "+CombatNumbers.format(Math.max(0,player.getVida()))+"/"+CombatNumbers.format(player.getVidaMaxima())
            +" | Dano: "+CombatNumbers.format(player.getDano())+" | Ataques/s: "+CombatNumbers.formatRate(player.attacksPerSecond()),35,462);
        font.draw(batch,"Movimento: "+CombatNumbers.format(player.movementSpeed())+" | Sorte: "+CombatNumbers.format(player.getLuckMultiplier())+"x"
            +" | Estamina: "+CombatNumbers.format(player.getEstaminaAtual())+"/"+CombatNumbers.format(player.getEstaminaMaxima())
            +" | Nível "+player.getNivel()+" | XP: "+CombatNumbers.format(player.getXpAtual())+"/"+player.getXpParaProximoNivel(),35,441);
        font.draw(batch,"Tempo: "+RunProgress.formatTime(session.progress().elapsed())+" | Abates: "+session.progress().kills()
            +(session.isDemo()?"": " | Ao retornar: "+session.earnedObols()+" Óbolos e "+session.earnedSouls()+" Almas | "+session.variation().label)
            +(player.getProfession()==Profession.NECROMANTE?" | Aliados: "+session.necromancy().allies().size()+"/6":""),35,420);
        font.getData().setScale(1);
        if (boons.isEmpty()) font.draw(batch, "Você ainda não escolheu dádivas nesta tentativa.", 190, 280);
        for (int i = page * PER_PAGE; i < Math.min(boons.size(), (page + 1) * PER_PAGE); i++) {
            Upgrade boon = boons.get(i); int slot = i % PER_PAGE;
            float x = 48 + slot % 2 * 455, y = 376 - slot / 2 * 105;
            font.setColor(boon.rarity.red, boon.rarity.green, boon.rarity.blue, 1);
            font.draw(batch, boon.title + " | " + boon.rarity.label, x, y);
            font.setColor(Color.WHITE); font.getData().setScale(.9f);
            font.draw(batch, boon.description, x, y - 22, 405, 8, true);
            font.getData().setScale(1);
            String rank = boon.maxRank == Integer.MAX_VALUE ? "Escolhida " + session.rank(boon) + " vez(es)"
                : "Nível " + session.rank(boon) + "/" + boon.maxRank;
            if (boon == Upgrade.RESSURREICAO && !player.possuiRessurreicao()) rank += " | Consumida";
            if (boon == Upgrade.CURA) rank += " | Cura já aplicada";
            font.draw(batch, rank, x, y - 60);
        }
        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "TAB ou ESC: fechar | Setas: página " + (page + 1) + "/" + pageCount()
            + " | Jogo pausado", 35, 60);
        batch.end();
    }
}

