package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/** Shared compact HUD. Detailed numbers belong to the paused Tab panel. */
public final class CombatHud {
    private CombatHud() {}
    public static float xpFraction(PlayerState player) {
        return fraction(player.getXpAtual()/player.getXpParaProximoNivel());
    }
    public static float fraction(float value) { return Math.max(0,Math.min(1,value)); }
    public static String dashNotice(PlayerState player) {
        if(player.isDashing()) return "Dash";
        if(player.guarding()) return "Solte o escudo";
        if(player.isExausto() || player.getEstaminaAtual()<20) return "Sem estamina";
        return player.canDash() ? "Dash pronto" : "Dash recarregando";
    }
    public static void player(SpriteBatch batch,ShapeRenderer shapes,BitmapFont font,PlayerState player) {
        player(batch,shapes,font,player,null);
    }
    public static void player(SpriteBatch batch,ShapeRenderer shapes,BitmapFont font,PlayerState player,PilotArt art) {
        panel(shapes,14,432,250,94);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        bar(shapes,72,505,176,12,player.getVida()/player.getVidaMaxima(),Color.SCARLET);
        bar(shapes,72,485,176,8,player.getEstaminaAtual()/player.getEstaminaMaxima(),
            player.isExausto()?Color.SALMON:Color.GREEN);
        // Five segments communicate the standard dash's stamina cost without numeric clutter.
        shapes.setColor(.08f,.10f,.16f,1);
        for(int i=1;i<5;i++) shapes.rect(72+176*i/5f,485,2,8);
        bar(shapes,72,466,176,6,xpFraction(player),Color.CYAN);
        if(player.dashCooldownRemaining()>0) bar(shapes,155,438,93,3,1-player.dashCooldownRemaining(),Color.GOLD);
        shapes.end();
        batch.begin();font.getData().setScale(.85f);font.setColor(Color.LIGHT_GRAY);
        if(art==null){font.draw(batch,"Vida",25,517);font.draw(batch,"Fôlego",25,494);font.draw(batch,"XP",25,475);}
        else {
            batch.draw(art.region("icon-health"),29,501,20,20);
            batch.draw(art.region("icon-stamina"),29,480,20,20);
            batch.draw(art.region("icon-experience"),29,461,20,20);
        }
        font.setColor(Color.WHITE);font.draw(batch,"Nv. "+player.getNivel(),25,449);
        font.setColor(player.canDash()?Color.GOLD:Color.LIGHT_GRAY);
        font.draw(batch,dashNotice(player),99,449);
        font.getData().setScale(1);font.setColor(Color.WHITE);batch.end();
    }
    public static void goals(SpriteBatch batch,ShapeRenderer shapes,BitmapFont font,RunSession run) {
        if(run.isDemo()) {
            panel(shapes,817,491,129,35);
            batch.begin();font.getData().setScale(.95f);font.setColor(Color.LIGHT_GRAY);
            font.draw(batch,RunProgress.formatTime(run.progress().elapsed())+" / 05:00",829,514);
            font.getData().setScale(1);font.setColor(Color.WHITE);batch.end();return;
        }
        panel(shapes,594,484,352,42);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        if(!run.arenaComplete()) {
            int column=0;
            for(EnemyType type:EnemyType.values()) {
                Integer goal=run.arena().killGoals().get(type);if(goal==null)continue;
                bar(shapes,606+column++*112,492,100,4,run.kills(type)/(float)goal,Color.GOLD);
            }
        } else {
            shapes.setColor(Color.GOLD);shapes.rect(609,493,3,23);shapes.rect(627,493,3,23);shapes.rect(609,513,21,3);
        }
        shapes.end();
        batch.begin();font.getData().setScale(.85f);
        if(run.arenaComplete()) {
            font.setColor(Color.GOLD);font.draw(batch,"Passagem liberada",647,514);
            font.setColor(Color.WHITE);font.draw(batch,"E / ENTER",843,514);
        } else {
            int column=0;
            for(EnemyType type:EnemyType.values()) {
                Integer goal=run.arena().killGoals().get(type);if(goal==null)continue;
                font.setColor(run.kills(type)>=goal?Color.GOLD:Color.LIGHT_GRAY);
                font.draw(batch,type.label+" "+Math.min(goal,run.kills(type))+"/"+goal,606+column++*112,515);
            }
        }
        font.getData().setScale(1);font.setColor(Color.WHITE);batch.end();
    }
    public static void boss(SpriteBatch batch,ShapeRenderer shapes,BitmapFont font,BossEncounter boss) {
        panel(shapes,524,484,422,42);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        bar(shapes,538,493,394,8,boss.health()/boss.maxHealth(),boss.enraged()?Color.SCARLET:Color.GOLD);
        shapes.end();
        batch.begin();font.getData().setScale(.85f);font.setColor(Color.GOLD);
        font.draw(batch,BossEncounter.NAME,538,517);font.setColor(Color.WHITE);
        font.draw(batch,"Fase "+(boss.enraged()?2:1),885,517);
        font.getData().setScale(1);font.setColor(Color.WHITE);batch.end();
    }
    public static void panel(ShapeRenderer shapes,float x,float y,float width,float height) {
        Gdx.gl.glEnable(GL20.GL_BLEND);Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA,GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);shapes.setColor(.045f,.055f,.085f,.88f);
        shapes.rect(x,y,width,height);shapes.end();Gdx.gl.glDisable(GL20.GL_BLEND);
    }
    public static void bar(ShapeRenderer shapes,float x,float y,float width,float height,float value,Color color) {
        shapes.setColor(.18f,.21f,.29f,1);shapes.rect(x,y,width,height);
        shapes.setColor(color);shapes.rect(x,y,width*fraction(value),height);
    }
}

