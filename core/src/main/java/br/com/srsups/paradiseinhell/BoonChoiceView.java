package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import java.util.List;

/** Shared opaque, full-screen choices. Hit rectangles and rendering use the same row layout. */
public final class BoonChoiceView {
    private BoonChoiceView() {}
    public static Rectangle[] cards(int count) {
        if(count<1||count>3)throw new IllegalArgumentException("Invalid boon count");
        Rectangle[] result=new Rectangle[count];
        for(int i=0;i<count;i++)result[i]=new Rectangle(45,330-i*125,870,112);
        return result;
    }
    public static void draw(SpriteBatch batch,ShapeRenderer shapes,BitmapFont font,PilotArt art,
                            RunSession session,List<Upgrade> choices,Rectangle[] cards,String title,String footer,String escapeHint,float time) {
        draw(batch,shapes,font,art,session,choices,cards,title,footer,escapeHint,time,"TAB");
    }
    public static void draw(SpriteBatch batch,ShapeRenderer shapes,BitmapFont font,PilotArt art,
                            RunSession session,List<Upgrade> choices,Rectangle[] cards,String title,String footer,String escapeHint,float time,String boonsKey) {
        com.badlogic.gdx.utils.ScreenUtils.clear(UiTheme.INK);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(UiTheme.INK);shapes.rect(0,0,960,540);
        int rows=choices.isEmpty()?cards.length:Math.min(cards.length,choices.size());
        for(int i=0;i<rows;i++){Rectangle card=cards[i];UiTheme.panel(shapes,card.x,card.y,card.width,card.height,true);}
        shapes.end();
        batch.begin();font.setColor(UiTheme.IVORY);font.getData().setScale(1.6f);
        font.draw(batch,title,45,502);
        font.getData().setScale(.95f);font.setColor(Color.LIGHT_GRAY);
        font.draw(batch,"Escolha um poder para esta tentativa.",45,467);
        for(int i=0;i<rows;i++) {
            Rectangle card=cards[i];Upgrade boon=choices.isEmpty()?null:choices.get(i);
            batch.setColor(1,1,1,.92f+.08f*(float)Math.sin(time*3));
            batch.draw(art.boonIcon(boon),card.x+16,card.y+17,76,76);batch.setColor(Color.WHITE);
            font.getData().setScale(1.12f);
            font.setColor(boon==null?Color.GOLD:new Color(boon.rarity.red,boon.rarity.green,boon.rarity.blue,1));
            font.draw(batch,"["+(i+1)+"] "+(boon==null?"Reserva de Óbolos":boon.title),card.x+112,card.y+90);
            font.getData().setScale(.9f);
            String rank=boon==null?"Recompensa":session.rank(boon)==0?"NOVA": "Obtida "+session.rank(boon)+" vez(es)";
            font.draw(batch,rank,card.x+725,card.y+89);
            font.setColor(UiTheme.IVORY);font.getData().setScale(1);
            String description=boon==null?"Dádivas superiores no limite. Receba "+session.previewChestObols(8)+" Óbolos ao retornar."
                :boon.description(session.player());
            font.draw(batch,description,card.x+112,card.y+62,715,com.badlogic.gdx.utils.Align.left,true);
            font.setColor(Color.LIGHT_GRAY);font.getData().setScale(.85f);
            if(boon!=null)font.draw(batch,boon.rarity.label+(boon==Upgrade.ECO_DO_ESTIGE?" | "+boon.originLabel():""),card.x+112,card.y+20);
        }
        font.getData().setScale(.95f);font.setColor(Color.GOLD);font.draw(batch,footer,45,53);
        font.setColor(Color.LIGHT_GRAY);font.getData().setScale(.9f);
        font.draw(batch,"1, 2, 3 ou clique: escolher | "+boonsKey+": suas dádivas | ESC: "+escapeHint,45,27,870,8,true);
        font.getData().setScale(1);font.setColor(Color.WHITE);batch.end();
    }
}

