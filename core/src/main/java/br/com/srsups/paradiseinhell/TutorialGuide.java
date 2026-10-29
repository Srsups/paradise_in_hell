package br.com.srsups.paradiseinhell;

import java.util.EnumSet;
import java.util.Objects;
import java.util.function.Predicate;

/** Presentation facts and reading time only. Never advances combat, spends currency or reads input. */
public final class TutorialGuide {
    public static final float READ_SECONDS = 7;
    public enum Place { REFUGE, COMBAT, GUARDIAN }
    public record Context(Place place, boolean active, boolean priorityNotice,
                          boolean exitUnlocked, boolean experienceAvailable, boolean returned) {
        public Context { Objects.requireNonNull(place); }
    }
    private static final EnumSet<TutorialTopic> BASICS = EnumSet.of(TutorialTopic.MOVE,
        TutorialTopic.ATTACK, TutorialTopic.DASH, TutorialTopic.EXPERIENCE, TutorialTopic.BOONS);
    private static final TutorialTopic[] FIRST_ACTIONS = {TutorialTopic.MOVE, TutorialTopic.ATTACK, TutorialTopic.DASH};
    private final boolean demo;
    private final EnumSet<TutorialTopic> seen = EnumSet.noneOf(TutorialTopic.class);
    private final EnumSet<TutorialTopic> completed = EnumSet.noneOf(TutorialTopic.class);
    private final float[] reading = new float[TutorialTopic.values().length];
    private boolean enabled = true;

    public TutorialGuide(boolean demo) { this.demo = demo; }
    public boolean demo() { return demo; }
    public boolean enabled() { return enabled; }
    public boolean seen(TutorialTopic topic) { return seen.contains(Objects.requireNonNull(topic)); }
    public float readingSeconds(TutorialTopic topic) { return reading[Objects.requireNonNull(topic).ordinal()]; }
    public void synchronize(boolean enabled, Predicate<TutorialTopic> saved) {
        this.enabled = enabled;
        Objects.requireNonNull(saved);
        if (!demo) for (TutorialTopic topic : TutorialTopic.values()) if (saved.test(topic)) seen.add(topic);
    }
    /** An observed action counts even when its hint has not been displayed yet. */
    public void action(TutorialTopic topic) {
        Objects.requireNonNull(topic);
        if ((!demo || BASICS.contains(topic)) && seen.add(topic)) completed.add(topic);
    }
    public TutorialTopic pollCompleted() {
        if (completed.isEmpty()) return null;
        TutorialTopic topic = completed.iterator().next(); completed.remove(topic); return topic;
    }
    public TutorialTopic current(Context context) {
        Objects.requireNonNull(context);
        if (!enabled || !context.active || context.priorityNotice) return null;
        if (context.place == Place.REFUGE) {
            if (demo) return null;
            if (context.returned && !seen(TutorialTopic.RETURN)) return TutorialTopic.RETURN;
            return unseen(TutorialTopic.REFUGE);
        }
        if (context.place == Place.GUARDIAN && !demo && !seen(TutorialTopic.GUARDIAN))
            return TutorialTopic.GUARDIAN;
        if (context.exitUnlocked && !demo) return unseen(TutorialTopic.EXIT);
        for (TutorialTopic topic : FIRST_ACTIONS)
            if (!seen(topic)) return topic;
        if (context.place == Place.COMBAT && !demo && !seen(TutorialTopic.GOALS)) return TutorialTopic.GOALS;
        if (context.experienceAvailable) {
            if (!seen(TutorialTopic.EXPERIENCE)) return TutorialTopic.EXPERIENCE;
            return unseen(TutorialTopic.BOONS);
        }
        return null;
    }
    private TutorialTopic unseen(TutorialTopic topic) { return seen(topic) ? null : topic; }
    public void advance(float delta, Context context) {
        if (!Float.isFinite(delta) || delta < 0) throw new IllegalArgumentException("Invalid tutorial delta");
        TutorialTopic topic = current(context);
        if (topic == null) return;
        int index = topic.ordinal();
        reading[index] = Math.min(READ_SECONDS, reading[index] + delta);
        if (reading[index] >= READ_SECONDS) action(topic);
    }
    public static String title(TutorialTopic topic) {
        return switch (topic) {
            case REFUGE -> "Preparação no refúgio";
            case MOVE -> "Movimento";
            case ATTACK -> "Mira e ataque";
            case DASH -> "Dash e estamina";
            case EXPERIENCE -> "Cristais de experiência";
            case BOONS -> "Dádivas e consulta";
            case GOALS -> "As três metas";
            case EXIT -> "Passagem e rotas";
            case GUARDIAN -> "Guardião do Tártaro";
            case RETURN -> "Retorno e melhorias";
        };
    }
    public static String hint(TutorialTopic topic) {
        return switch (topic) {
            case REFUGE -> "C: converse com Caronte. L: altar. ENTER: iniciar a travessia.";
            case MOVE -> "WASD ou setas: mova-se para manter distância dos inimigos.";
            case ATTACK -> "Mire com o mouse e segure o botão esquerdo para atacar.";
            case DASH -> "SHIFT + direção: dash. Ele gasta estamina e precisa recarregar.";
            case EXPERIENCE -> "Aproxime-se dos cristais: a barra azul mostra a XP para o próximo nível.";
            case BOONS -> "Ao subir de nível, escolha uma dádiva. TAB pausa e mostra sua build.";
            case GOALS -> "Cumpra as três metas no canto superior. Um tipo não substitui os outros.";
            case EXIT -> "Passagem liberada: E ou ENTER avança de qualquer posição. As hordas continuam.";
            case GUARDIAN -> "Observe os avisos do Guardião. Saia das áreas marcadas e use o dash para escapar.";
            case RETURN -> "Dádivas reiniciam; Óbolos, arma e compras ficam. L abre o altar permanente.";
        };
    }
    public static String hint(TutorialTopic topic,ControlSettings controls) {
        Objects.requireNonNull(controls);
        return switch (topic) {
            case MOVE -> changedMovement(controls)?"Mover: "+movementKeys(controls)+". Mantenha distância dos inimigos.":hint(topic);
            case ATTACK -> changed(controls,ControlAction.ATTACK)?"Mire com o mouse e segure "+controls.label(ControlAction.ATTACK)+" para atacar.":hint(topic);
            case DASH -> changed(controls,ControlAction.DASH)?controls.label(ControlAction.DASH)+" + direção: dash. Gasta estamina e precisa recarregar.":hint(topic);
            case BOONS -> changed(controls,ControlAction.BOONS)?"Ao subir de nível, escolha uma dádiva. "+controls.label(ControlAction.BOONS)+" pausa e mostra sua build.":hint(topic);
            case EXIT -> changed(controls,ControlAction.INTERACT)?"Passagem liberada: "+controls.label(ControlAction.INTERACT)+" avança de qualquer posição. As hordas continuam.":hint(topic);
            default -> hint(topic);
        };
    }
    public static String detail(TutorialTopic topic) {
        return switch (topic) {
            case REFUGE -> "C: Caronte | L: altar | P: saves | ENTER: partir | E: interação próxima. J: promessa | R: relatório | I: abertura | O: áudio | ESC: menu.";
            case MOVE -> "Use WASD ou as setas. Movimento diagonal mantém a velocidade. Obstáculos e limites bloqueiam o corpo.";
            case ATTACK -> "Mire com o mouse e segure o botão esquerdo. Sua profissão define a arma e o ritmo dos golpes; dádivas modificam os atributos da tentativa.";
            case DASH -> "Use SHIFT enquanto se move. O dash consome estamina, tem recarga e exige fôlego. Com o Guerreiro, solte a defesa antes.";
            case EXPERIENCE -> "Aproxime-se dos cristais de XP deixados pelos inimigos. A barra azul indica o progresso para o próximo nível.";
            case BOONS -> "Escolha uma dádiva ao subir de nível. Aguarde 1,5 s e solte o ataque antes de clicar. TAB consulta atributos e poderes com o combate pausado.";
            case GOALS -> "Derrote as quantidades pedidas de comuns, rápidos e tanks. As três metas são independentes; tempo não libera a passagem.";
            case EXIT -> "Depois das metas, E ou ENTER funciona em qualquer posição. A saída recolhe a XP no chão. Farmar depois de liberar a passagem atrai a Fúria.";
            case GUARDIAN -> "Responda aos avisos e ataque nas aberturas. Na queda, o círculo trava em vermelho; na fase 2, a caminhada fica lenta, mas o dash mantém sua força.";
            case RETURN -> "Nível e dádivas reiniciam. Óbolos, Almas, arma forjada e atributos comprados permanecem. Use L no refúgio para comprar atributos permanentes.";
        };
    }
    public static String detail(TutorialTopic topic,ControlSettings controls) {
        Objects.requireNonNull(controls);
        return switch (topic) {
            case REFUGE -> changed(controls,ControlAction.INTERACT)?"C: Caronte | L: altar | P: saves | ENTER: partir. Interação próxima: "+nearbyInteraction(controls)+". J: promessa | R: relatório | I: abertura | O: áudio | ESC: menu.":detail(topic);
            case MOVE -> changedMovement(controls)?"Mover: "+movementKeys(controls)+". A diagonal mantém a velocidade; obstáculos e limites bloqueiam o corpo.":detail(topic);
            case ATTACK -> changed(controls,ControlAction.ATTACK)?"Mire com o mouse e segure "+controls.label(ControlAction.ATTACK)+". Sua profissão define a arma e o ritmo dos golpes; dádivas modificam os atributos da tentativa.":detail(topic);
            case DASH -> changed(controls,ControlAction.DASH)?"Use "+controls.label(ControlAction.DASH)+" enquanto se move. O dash consome estamina e tem recarga. Com o Guerreiro, solte a defesa antes.":detail(topic);
            case BOONS -> changed(controls,ControlAction.BOONS)?"Escolha uma dádiva ao subir de nível. Aguarde 1,5 s e solte o ataque antes de clicar. "+controls.label(ControlAction.BOONS)+" consulta atributos e poderes com o combate pausado.":detail(topic);
            case EXIT -> changed(controls,ControlAction.INTERACT)?"Depois das metas, "+controls.label(ControlAction.INTERACT)+" funciona em qualquer posição. A saída recolhe a XP no chão. Farmar depois da liberação atrai a Fúria.":detail(topic);
            default -> detail(topic);
        };
    }
    static boolean changed(ControlSettings controls,ControlAction action) {
        return !controls.bindings(action).equals(ControlSettings.DEFAULT.bindings(action));
    }
    private static boolean changedMovement(ControlSettings controls) {
        return changed(controls,ControlAction.MOVE_UP)||changed(controls,ControlAction.MOVE_DOWN)
            ||changed(controls,ControlAction.MOVE_LEFT)||changed(controls,ControlAction.MOVE_RIGHT);
    }
    private static String movementKeys(ControlSettings controls) {
        return "cima "+controls.bindings(ControlAction.MOVE_UP).get(0).label()+", baixo "+controls.bindings(ControlAction.MOVE_DOWN).get(0).label()
            +", esquerda "+controls.bindings(ControlAction.MOVE_LEFT).get(0).label()+", direita "+controls.bindings(ControlAction.MOVE_RIGHT).get(0).label();
    }
    private static String nearbyInteraction(ControlSettings controls) {
        for(InputBinding binding:controls.bindings(ControlAction.INTERACT))
            if(!binding.equals(InputBinding.key(com.badlogic.gdx.Input.Keys.ENTER)))return binding.label();
        return "C ou L, conforme o serviço";
    }
}
