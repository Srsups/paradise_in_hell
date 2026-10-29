package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Complete immutable bindings; changes never leave an action without a usable input. */
public final class ControlSettings {
    public static final int MAX_BINDINGS = 2;
    public static final class UnsupportedVersion extends IllegalArgumentException {
        public UnsupportedVersion() { super("Versão de controles não suportada."); }
    }
    public static final ControlSettings DEFAULT = defaults();
    private final Map<ControlAction, List<InputBinding>> bindings;

    public ControlSettings(Map<ControlAction, List<InputBinding>> source) {
        Objects.requireNonNull(source, "Controles ausentes.");
        if (source.size() != ControlAction.values().length)
            throw new IllegalArgumentException("Todas as ações devem ter um comando.");
        EnumMap<ControlAction, List<InputBinding>> copy = new EnumMap<>(ControlAction.class);
        Map<InputBinding, ControlAction> occupied = new HashMap<>();
        for (ControlAction action : ControlAction.values()) {
            List<InputBinding> inputs = source.get(action);
            if (inputs == null || inputs.isEmpty() || inputs.size() > MAX_BINDINGS)
                throw new IllegalArgumentException(action.label() + ": escolha um ou dois comandos.");
            for (InputBinding input : inputs) {
                validateBinding(action, input);
                ControlAction other = occupied.putIfAbsent(input, action);
                if (other != null) throw new IllegalArgumentException(input.label() + " já está em " + other.label() + ".");
            }
            copy.put(action, List.copyOf(inputs));
        }
        bindings = Map.copyOf(copy);
    }
    public List<InputBinding> bindings(ControlAction action) { return bindings.get(Objects.requireNonNull(action)); }
    public String label(ControlAction action) { return bindings(action).stream().map(InputBinding::label).collect(Collectors.joining(" / ")); }
    public ControlSettings withBinding(ControlAction action, int index, InputBinding input) {
        List<InputBinding> current = bindings(action);
        if (index < 0 || index >= MAX_BINDINGS || index > current.size())
            throw new IllegalArgumentException("Posição do comando inválida.");
        ArrayList<InputBinding> next = new ArrayList<>(current);
        if (index == next.size()) next.add(input); else next.set(index, input);
        EnumMap<ControlAction, List<InputBinding>> copy = new EnumMap<>(bindings);
        copy.put(action, next);
        return new ControlSettings(copy);
    }
    public ControlSettings withoutBinding(ControlAction action, int index) {
        List<InputBinding> current = bindings(action);
        if (index < 0 || index >= current.size()) throw new IllegalArgumentException("Posição do comando inválida.");
        if (current.size() == 1) throw new IllegalArgumentException("Mantenha ao menos um comando para " + action.label() + ".");
        ArrayList<InputBinding> next = new ArrayList<>(current);
        next.remove(index);
        EnumMap<ControlAction, List<InputBinding>> copy = new EnumMap<>(bindings);
        copy.put(action, next);
        return new ControlSettings(copy);
    }
    public static void validateBinding(ControlAction action, InputBinding input) {
        Objects.requireNonNull(action, "Ação ausente.");
        if (input == null) throw new IllegalArgumentException("Comando ausente.");
        if (input.type() == InputBinding.Type.MOUSE) return;
        int code = input.code();
        if (code == Input.Keys.ENTER) {
            if (action != ControlAction.INTERACT)
                throw new IllegalArgumentException("Enter é reservado para confirmar e interagir.");
            return;
        }
        if (reserved(code)) throw new IllegalArgumentException(input.label() + " é reservado para menus e atalhos.");
        if (!supported(code)) throw new IllegalArgumentException("Esta tecla não está disponível para remapeamento.");
    }
    private static boolean reserved(int code) {
        return switch (code) {
            case Input.Keys.ESCAPE, Input.Keys.F1, Input.Keys.F2, Input.Keys.F3,
                Input.Keys.O, Input.Keys.H, Input.Keys.C, Input.Keys.L, Input.Keys.P,
                Input.Keys.R, Input.Keys.J, Input.Keys.V, Input.Keys.I, Input.Keys.M,
                Input.Keys.BACKSPACE, Input.Keys.FORWARD_DEL -> true;
            default -> false;
        };
    }
    private static boolean supported(int code) {
        if (code >= Input.Keys.A && code <= Input.Keys.Z
            || code >= Input.Keys.NUM_0 && code <= Input.Keys.NUM_9
            || code >= Input.Keys.NUMPAD_0 && code <= Input.Keys.NUMPAD_9
            || code >= Input.Keys.F4 && code <= Input.Keys.F12) return true;
        return switch (code) {
            case Input.Keys.UP, Input.Keys.DOWN, Input.Keys.LEFT, Input.Keys.RIGHT,
                Input.Keys.SHIFT_LEFT, Input.Keys.SHIFT_RIGHT, Input.Keys.CONTROL_LEFT,
                Input.Keys.CONTROL_RIGHT, Input.Keys.ALT_LEFT, Input.Keys.ALT_RIGHT,
                Input.Keys.SPACE, Input.Keys.TAB, Input.Keys.COMMA, Input.Keys.PERIOD,
                Input.Keys.APOSTROPHE, Input.Keys.BACKSLASH, Input.Keys.EQUALS,
                Input.Keys.GRAVE, Input.Keys.LEFT_BRACKET, Input.Keys.RIGHT_BRACKET,
                Input.Keys.MINUS, Input.Keys.SEMICOLON, Input.Keys.SLASH,
                Input.Keys.HOME, Input.Keys.END, Input.Keys.INSERT,
                Input.Keys.PAGE_UP, Input.Keys.PAGE_DOWN -> true;
            default -> false;
        };
    }
    private static ControlSettings defaults() {
        EnumMap<ControlAction, List<InputBinding>> inputs = new EnumMap<>(ControlAction.class);
        inputs.put(ControlAction.MOVE_UP, List.of(InputBinding.key(Input.Keys.W), InputBinding.key(Input.Keys.UP)));
        inputs.put(ControlAction.MOVE_DOWN, List.of(InputBinding.key(Input.Keys.S), InputBinding.key(Input.Keys.DOWN)));
        inputs.put(ControlAction.MOVE_LEFT, List.of(InputBinding.key(Input.Keys.A), InputBinding.key(Input.Keys.LEFT)));
        inputs.put(ControlAction.MOVE_RIGHT, List.of(InputBinding.key(Input.Keys.D), InputBinding.key(Input.Keys.RIGHT)));
        inputs.put(ControlAction.ATTACK, List.of(InputBinding.mouse(Input.Buttons.LEFT)));
        inputs.put(ControlAction.DASH, List.of(InputBinding.key(Input.Keys.SHIFT_LEFT), InputBinding.key(Input.Keys.SHIFT_RIGHT)));
        inputs.put(ControlAction.GUARD, List.of(InputBinding.mouse(Input.Buttons.RIGHT)));
        inputs.put(ControlAction.INTERACT, List.of(InputBinding.key(Input.Keys.E), InputBinding.key(Input.Keys.ENTER)));
        inputs.put(ControlAction.OPEN_CHEST, List.of(InputBinding.key(Input.Keys.F)));
        inputs.put(ControlAction.SWITCH_TOOL, List.of(InputBinding.key(Input.Keys.Q)));
        inputs.put(ControlAction.BOONS, List.of(InputBinding.key(Input.Keys.TAB)));
        return new ControlSettings(inputs);
    }
    public String toJson() {
        JsonValue root = new JsonValue(JsonValue.ValueType.object);
        root.addChild("version", new JsonValue(1L));
        JsonValue actions = new JsonValue(JsonValue.ValueType.array);
        root.addChild("bindings", actions);
        for (ControlAction action : ControlAction.values()) {
            JsonValue entry = new JsonValue(JsonValue.ValueType.object);
            entry.addChild("action", new JsonValue(action.name()));
            JsonValue inputs = new JsonValue(JsonValue.ValueType.array);
            entry.addChild("inputs", inputs);
            for (InputBinding binding : bindings(action)) {
                JsonValue value = new JsonValue(JsonValue.ValueType.object);
                value.addChild("type", new JsonValue(binding.type().name()));
                value.addChild("code", new JsonValue((long) binding.code()));
                inputs.addChild(value);
            }
            actions.addChild(entry);
        }
        return root.prettyPrint(JsonWriter.OutputType.json, 100);
    }
    public static ControlSettings fromJson(String json) {
        JsonValue root = new JsonReader().parse(json);
        if (root == null || !root.isObject()) throw new IllegalArgumentException("Objeto de controles inválido.");
        // Inspect the version before this version's exact field schema: a newer file can add fields.
        JsonValue version = root.require("version");
        if (!version.isLong()) throw new IllegalArgumentException("Versão de controles inválida.");
        if (version.asLong() != 1) throw new UnsupportedVersion();
        fields(root, Set.of("version", "bindings"));
        JsonValue actions = root.require("bindings");
        if (!actions.isArray()) throw new IllegalArgumentException("Lista de comandos inválida.");
        EnumMap<ControlAction, List<InputBinding>> values = new EnumMap<>(ControlAction.class);
        for (JsonValue entry : actions) {
            fields(entry, Set.of("action", "inputs"));
            ControlAction action;
            try { action = ControlAction.valueOf(string(entry, "action")); }
            catch (IllegalArgumentException invalid) { throw new IllegalArgumentException("Ação de controle desconhecida.", invalid); }
            if (values.containsKey(action)) throw new IllegalArgumentException("Ação de controle duplicada.");
            JsonValue inputs = entry.require("inputs");
            if (!inputs.isArray()) throw new IllegalArgumentException("Comandos da ação inválidos.");
            List<InputBinding> parsed = new ArrayList<>();
            for (JsonValue value : inputs) {
                fields(value, Set.of("type", "code"));
                InputBinding.Type type;
                try { type = InputBinding.Type.valueOf(string(value, "type")); }
                catch (IllegalArgumentException invalid) { throw new IllegalArgumentException("Tipo de comando desconhecido.", invalid); }
                parsed.add(new InputBinding(type, integer(value, "code")));
            }
            values.put(action, parsed);
        }
        return new ControlSettings(values);
    }
    private static void fields(JsonValue value, Set<String> allowed) {
        if (value == null || !value.isObject()) throw new IllegalArgumentException("Objeto de controles inválido.");
        Set<String> found = new HashSet<>();
        for (JsonValue child : value) {
            if (!allowed.contains(child.name) || !found.add(child.name))
                throw new IllegalArgumentException("Campo de controles desconhecido ou duplicado.");
        }
        if (!found.equals(allowed)) throw new IllegalArgumentException("Campo de controles ausente.");
    }
    private static String string(JsonValue value, String key) {
        JsonValue field = value.require(key);
        if (!field.isString()) throw new IllegalArgumentException("Texto de controles inválido.");
        return field.asString();
    }
    private static int integer(JsonValue value, String key) {
        JsonValue field = value.require(key);
        if (!field.isLong() || field.asLong() < 0 || field.asLong() > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Número de controles inválido.");
        return field.asInt();
    }
    @Override public boolean equals(Object other) { return other instanceof ControlSettings settings && bindings.equals(settings.bindings); }
    @Override public int hashCode() { return bindings.hashCode(); }
}
