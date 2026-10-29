package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Input;
import java.util.Objects;

/** One physical input. Keyboard chords and mouse wheel gestures are not bindings. */
public record InputBinding(Type type, int code) {
    public enum Type { KEY, MOUSE }

    public InputBinding {
        Objects.requireNonNull(type, "Tipo de comando ausente.");
        if (type == Type.KEY && (code <= Input.Keys.UNKNOWN || code > Input.Keys.MAX_KEYCODE))
            throw new IllegalArgumentException("Tecla inválida.");
        if (type == Type.MOUSE && (code < Input.Buttons.LEFT || code > Input.Buttons.FORWARD))
            throw new IllegalArgumentException("Botão do mouse inválido.");
    }
    public static InputBinding key(int code) { return new InputBinding(Type.KEY, code); }
    public static InputBinding mouse(int code) { return new InputBinding(Type.MOUSE, code); }
    public boolean pressed(Input input) {
        return input != null && (type == Type.KEY ? input.isKeyPressed(code) : input.isButtonPressed(code));
    }
    public boolean justPressed(Input input) {
        return input != null && (type == Type.KEY ? input.isKeyJustPressed(code) : input.isButtonJustPressed(code));
    }
    public String label() {
        if (type == Type.MOUSE) return switch (code) {
            case Input.Buttons.LEFT -> "Mouse esquerdo";
            case Input.Buttons.RIGHT -> "Mouse direito";
            case Input.Buttons.MIDDLE -> "Mouse central";
            case Input.Buttons.BACK -> "Mouse lateral 1";
            case Input.Buttons.FORWARD -> "Mouse lateral 2";
            default -> throw new IllegalStateException("Botão inválido.");
        };
        return switch (code) {
            case Input.Keys.UP -> "Seta cima";
            case Input.Keys.DOWN -> "Seta baixo";
            case Input.Keys.LEFT -> "Seta esquerda";
            case Input.Keys.RIGHT -> "Seta direita";
            case Input.Keys.SHIFT_LEFT -> "Shift esquerdo";
            case Input.Keys.SHIFT_RIGHT -> "Shift direito";
            case Input.Keys.CONTROL_LEFT -> "Ctrl esquerdo";
            case Input.Keys.CONTROL_RIGHT -> "Ctrl direito";
            case Input.Keys.ALT_LEFT -> "Alt esquerdo";
            case Input.Keys.ALT_RIGHT -> "Alt direito";
            case Input.Keys.SPACE -> "Espaço";
            case Input.Keys.ENTER -> "Enter";
            case Input.Keys.TAB -> "Tab";
            case Input.Keys.PAGE_UP -> "Page Up";
            case Input.Keys.PAGE_DOWN -> "Page Down";
            default -> {
                String name = Input.Keys.toString(code);
                yield name == null ? "Tecla " + code : name;
            }
        };
    }
}
