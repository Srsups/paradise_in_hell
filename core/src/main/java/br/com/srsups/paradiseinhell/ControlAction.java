package br.com.srsups.paradiseinhell;

/** Actions that belong to movement and combat; menu navigation keeps its own safe keys. */
public enum ControlAction {
    MOVE_UP("Mover para cima"), MOVE_DOWN("Mover para baixo"),
    MOVE_LEFT("Mover para a esquerda"), MOVE_RIGHT("Mover para a direita"),
    ATTACK("Atacar"), DASH("Dash"), GUARD("Defender"),
    INTERACT("Interagir / atravessar"), OPEN_CHEST("Abrir baú"),
    SWITCH_TOOL("Trocar ferramenta"), BOONS("Ver dádivas");

    private final String label;
    ControlAction(String label) { this.label = label; }
    public String label() { return label; }
}
