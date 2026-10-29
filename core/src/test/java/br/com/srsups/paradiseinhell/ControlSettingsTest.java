package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ControlSettingsTest {
    @TempDir Path directory;
    private static final InputBinding SPACE = InputBinding.key(Input.Keys.SPACE);
    private static final InputBinding K = InputBinding.key(Input.Keys.K);

    private static final class Store implements ControlSettingsStore {
        ControlSettings saved = ControlSettings.DEFAULT;
        int writes;
        boolean failSave, failLoad;
        public ControlSettings load() throws IOException {
            if (failLoad) throw new IOException("locked");
            return saved;
        }
        public void save(ControlSettings value) throws IOException {
            writes++;
            if (failSave) throw new IOException("locked");
            saved = value;
        }
    }
    private static final class Inputs {
        final Set<Integer> heldKeys = new HashSet<>(), newKeys = new HashSet<>();
        final Set<Integer> heldButtons = new HashSet<>(), newButtons = new HashSet<>();
        final Input proxy = (Input) Proxy.newProxyInstance(Input.class.getClassLoader(), new Class<?>[]{Input.class}, (ignored, method, args) -> switch (method.getName()) {
            case "isKeyPressed" -> heldKeys.contains((Integer) args[0]);
            case "isKeyJustPressed" -> newKeys.contains((Integer) args[0]);
            case "isButtonPressed" -> heldButtons.contains((Integer) args[0]);
            case "isButtonJustPressed" -> newButtons.contains((Integer) args[0]);
            default -> method.getReturnType() == boolean.class ? false : method.getReturnType() == int.class ? 0 : null;
        });
        void key(int key) { heldKeys.add(key); newKeys.add(key); }
        void button(int button) { heldButtons.add(button); newButtons.add(button); }
        void release() { heldKeys.clear(); newKeys.clear(); heldButtons.clear(); newButtons.clear(); }
        void nextFrame() { newKeys.clear(); newButtons.clear(); }
    }
    private static String edited(Consumer<JsonValue> edit) {
        JsonValue value = new JsonReader().parse(ControlSettings.DEFAULT.toJson());
        edit.accept(value);
        return value.prettyPrint(JsonWriter.OutputType.json, 100);
    }

    @Test void defaultsPreserveBothMovementAndDashAliasesAndAreImmutable() {
        ControlSettings defaults = ControlSettings.DEFAULT;
        assertEquals(ControlAction.values().length, 11);
        assertEquals(2, defaults.bindings(ControlAction.MOVE_UP).size());
        assertEquals(InputBinding.key(Input.Keys.UP), defaults.bindings(ControlAction.MOVE_UP).get(1));
        assertEquals(InputBinding.key(Input.Keys.SHIFT_RIGHT), defaults.bindings(ControlAction.DASH).get(1));
        assertEquals(InputBinding.key(Input.Keys.ENTER), defaults.bindings(ControlAction.INTERACT).get(1));
        assertEquals("Mouse esquerdo", defaults.label(ControlAction.ATTACK));
        assertThrows(UnsupportedOperationException.class, () -> defaults.bindings(ControlAction.ATTACK).clear());
        assertEquals(defaults, ControlSettings.fromJson(defaults.toJson()));
    }

    @Test void replacementsAdditionsAndRemovalRetainAtLeastOneBindingWithoutMutatingTheSource() {
        ControlSettings remapped = ControlSettings.DEFAULT.withBinding(ControlAction.DASH, 0, SPACE)
            .withBinding(ControlAction.ATTACK, 0, K)
            .withBinding(ControlAction.ATTACK, 1, InputBinding.mouse(Input.Buttons.MIDDLE));
        assertEquals("Espaço / Shift direito", remapped.label(ControlAction.DASH));
        assertEquals("K / Mouse central", remapped.label(ControlAction.ATTACK));
        assertEquals("Mouse esquerdo", ControlSettings.DEFAULT.label(ControlAction.ATTACK));
        ControlSettings single = remapped.withoutBinding(ControlAction.ATTACK, 0);
        assertEquals("Mouse central", single.label(ControlAction.ATTACK));
        assertThrows(IllegalArgumentException.class, () -> single.withoutBinding(ControlAction.ATTACK, 0));
        assertThrows(IllegalArgumentException.class, () -> single.withBinding(ControlAction.ATTACK, 2, K));
        assertThrows(IllegalArgumentException.class, () -> single.withoutBinding(ControlAction.ATTACK, -1));
        assertEquals(remapped, ControlSettings.fromJson(remapped.toJson()));
        assertEquals(remapped.hashCode(), ControlSettings.fromJson(remapped.toJson()).hashCode());
    }

    @Test void duplicateInputsAcrossActionsAndWithinOneActionAreRejectedWithTheConflictingAction() {
        IllegalArgumentException conflict = assertThrows(IllegalArgumentException.class,
            () -> ControlSettings.DEFAULT.withBinding(ControlAction.ATTACK, 0, InputBinding.key(Input.Keys.W)));
        assertTrue(conflict.getMessage().contains(ControlAction.MOVE_UP.label()));
        assertThrows(IllegalArgumentException.class,
            () -> ControlSettings.DEFAULT.withBinding(ControlAction.MOVE_UP, 1, InputBinding.key(Input.Keys.W)));
        assertThrows(IllegalArgumentException.class,
            () -> ControlSettings.DEFAULT.withBinding(ControlAction.GUARD, 0, InputBinding.mouse(Input.Buttons.LEFT)));
    }

    @Test void menuRecoveryKeysUnsupportedInputsAndEnterForOtherActionsCannotBeBound() {
        for (int key : new int[]{Input.Keys.ESCAPE, Input.Keys.F1, Input.Keys.F2, Input.Keys.F3,
            Input.Keys.O, Input.Keys.H, Input.Keys.C, Input.Keys.L, Input.Keys.P, Input.Keys.R,
            Input.Keys.J, Input.Keys.V, Input.Keys.I, Input.Keys.M, Input.Keys.BACKSPACE,
            Input.Keys.FORWARD_DEL, Input.Keys.POWER, Input.Keys.PRINT_SCREEN, Input.Keys.BUTTON_A})
            assertThrows(IllegalArgumentException.class,
                () -> ControlSettings.DEFAULT.withBinding(ControlAction.ATTACK, 0, InputBinding.key(key)), "key " + key);
        assertThrows(IllegalArgumentException.class,
            () -> ControlSettings.DEFAULT.withBinding(ControlAction.ATTACK, 0, InputBinding.key(Input.Keys.ENTER)));
        assertThrows(IllegalArgumentException.class, () -> InputBinding.key(Input.Keys.ANY_KEY));
        assertThrows(IllegalArgumentException.class, () -> InputBinding.key(256));
        assertThrows(IllegalArgumentException.class, () -> InputBinding.mouse(-1));
        assertThrows(IllegalArgumentException.class, () -> InputBinding.mouse(5));
        assertDoesNotThrow(() -> ControlSettings.DEFAULT.withBinding(ControlAction.DASH, 0, InputBinding.key(Input.Keys.CONTROL_LEFT)));
        assertDoesNotThrow(() -> ControlSettings.DEFAULT.withBinding(ControlAction.ATTACK, 0, InputBinding.mouse(Input.Buttons.FORWARD)));
    }

    @Test void parserRejectsMissingUnknownDuplicateFieldsAndIncompleteActions() {
        for (String json : new String[]{"null", "[]", "{}",
            edited(root -> root.addChild("version", new JsonValue(1L))),
            edited(root -> root.addChild("other", new JsonValue(true))),
            edited(root -> root.remove("bindings")),
            edited(root -> root.get("bindings").remove(0)),
            edited(root -> root.get("bindings").get(0).addChild("other", new JsonValue(1L))),
            edited(root -> root.get("bindings").get(0).get("inputs").get(0).addChild("code", new JsonValue(51L)))})
            assertThrows(RuntimeException.class, () -> ControlSettings.fromJson(json));
        String duplicateAction = edited(root -> {
            JsonValue actions = root.get("bindings");
            actions.addChild(new JsonReader().parse(actions.get(0).toJson(JsonWriter.OutputType.json)));
        });
        assertThrows(IllegalArgumentException.class, () -> ControlSettings.fromJson(duplicateAction));
    }

    @Test void parserRejectsMalformedBindingTypesRangesEnumsDuplicatesAndEmptyActions() {
        for (String json : new String[]{
            edited(root -> root.get("bindings").get(0).get("action").set("FUTURE_ACTION")),
            edited(root -> root.get("bindings").get(0).get("inputs").get(0).get("type").set("JOYSTICK")),
            edited(root -> root.get("bindings").get(0).get("inputs").get(0).get("code").set(1.5,null)),
            edited(root -> root.get("bindings").get(0).get("inputs").get(0).get("code").set("51")),
            edited(root -> root.get("bindings").get(0).get("inputs").get(0).get("code").set(-1L,null)),
            edited(root -> root.get("bindings").get(0).get("inputs").get(1).get("code").set((long) Input.Keys.W,null)),
            edited(root -> { root.get("bindings").get(0).get("inputs").remove(1); root.get("bindings").get(0).get("inputs").remove(0); })})
            assertThrows(RuntimeException.class, () -> ControlSettings.fromJson(json));
        assertThrows(ControlSettings.UnsupportedVersion.class,
            () -> ControlSettings.fromJson("{\"version\":2,\"futureField\":true}"));
        assertThrows(ControlSettings.UnsupportedVersion.class,
            () -> ControlSettings.fromJson("{\"version\":4294967296,\"futureField\":true}"));
        assertThrows(IllegalArgumentException.class,
            () -> ControlSettings.fromJson(edited(root -> root.get("version").set(1.0,null))));
    }

    @Test void applyPersistsOnlyChangesAndSurvivesFailedSaveUntilExplicitRetry() {
        Store store = new Store();
        ControlSettingsService service = new ControlSettingsService(store);
        assertTrue(service.apply(ControlSettings.DEFAULT));
        assertEquals(0, store.writes);
        ControlSettings changed = service.value().withBinding(ControlAction.DASH, 0, SPACE);
        store.failSave = true;
        assertFalse(service.apply(changed));
        assertEquals(changed, service.value());
        assertEquals(ControlSettings.DEFAULT, store.saved);
        assertTrue(service.pendingSave());
        assertFalse(service.error().isEmpty());
        store.failSave = false;
        assertTrue(service.retrySave());
        assertEquals(changed, store.saved);
        assertFalse(service.pendingSave());
        assertEquals("", service.error());
        assertTrue(service.apply(changed));
        assertEquals(2, store.writes);
        assertEquals(changed, new ControlSettingsService(store).value());
    }

    @Test void unreadablePreferencesAllowSessionChangesWithoutOverwritingTheSource() {
        Store store = new Store(); store.failLoad = true;
        ControlSettingsService service = new ControlSettingsService(store);
        assertEquals(ControlSettings.DEFAULT, service.value());
        assertFalse(service.writable());
        ControlSettings changed = service.value().withBinding(ControlAction.DASH, 0, SPACE);
        assertFalse(service.apply(changed));
        assertEquals(changed, service.value());
        assertTrue(service.pendingSave());
        assertFalse(service.retrySave());
        assertEquals(0, store.writes);
    }

    @Test void atomicFileBackupRecoveryPreservesTheLastValidatedBackupAndLeavesNoTemporaryFiles() throws Exception {
        Path file = directory.resolve("controls.json");
        FileControlSettingsStore store = new FileControlSettingsStore(file);
        assertEquals(ControlSettings.DEFAULT, store.load());
        ControlSettings changed = ControlSettings.DEFAULT.withBinding(ControlAction.DASH, 0, SPACE);
        store.save(ControlSettings.DEFAULT); store.save(changed);
        Files.writeString(file, "broken");
        assertEquals(ControlSettings.DEFAULT, store.load());
        store.save(changed);
        assertEquals(changed, store.load());
        assertEquals(ControlSettings.DEFAULT, ControlSettings.fromJson(Files.readString(directory.resolve("controls.json.bak"))));
        try (var entries = Files.list(directory)) { assertEquals(2, entries.count()); }
    }

    @Test void newerVersionWithExtraFieldsNeverFallsBackOrGetsOverwrittenEvenThroughTheStore() throws Exception {
        Path file = directory.resolve("controls.json");
        FileControlSettingsStore store = new FileControlSettingsStore(file);
        store.save(ControlSettings.DEFAULT);
        store.save(ControlSettings.DEFAULT.withBinding(ControlAction.DASH, 0, SPACE));
        String future = "{\"version\":2,\"futureField\":true}";
        Files.writeString(file, future);
        assertThrows(IOException.class, store::load);
        assertThrows(IOException.class, () -> store.save(ControlSettings.DEFAULT));
        ControlSettingsService service = new ControlSettingsService(store);
        assertFalse(service.apply(ControlSettings.DEFAULT.withBinding(ControlAction.ATTACK, 0, K)));
        assertEquals(future, Files.readString(file));
        assertFalse(service.writable());
        assertTrue(service.pendingSave());
    }

    @Test void physicalKeysMouseAliasesAndRemappingAreReadFromTheCurrentSettings() {
        Inputs input = new Inputs();
        ControlSettingsService service = new ControlSettingsService(ControlSettingsStore.memory());
        GameControls controls = new GameControls(service, () -> input.proxy);
        input.key(Input.Keys.UP); input.button(Input.Buttons.LEFT);
        assertTrue(controls.pressed(ControlAction.MOVE_UP));
        assertTrue(controls.justPressed(ControlAction.ATTACK));
        input.nextFrame();
        assertTrue(controls.pressed(ControlAction.ATTACK));
        assertFalse(controls.justPressed(ControlAction.ATTACK));
        service.apply(service.value().withBinding(ControlAction.ATTACK, 0, K));
        assertFalse(controls.pressed(ControlAction.ATTACK));
        input.key(Input.Keys.K);
        assertTrue(controls.justPressed(ControlAction.ATTACK));
        assertEquals("K", controls.label(ControlAction.ATTACK));
        assertFalse(new GameControls(service, () -> null).pressed(ControlAction.ATTACK));
    }

    @Test void heldInputsCannotLeakAfterClosingSettingsUntilReleasedEvenAcrossLaterRemaps() {
        Inputs input = new Inputs();
        ControlSettingsService service = new ControlSettingsService(ControlSettingsStore.memory());
        GameControls controls = new GameControls(service, () -> input.proxy);
        input.key(Input.Keys.SHIFT_LEFT); input.button(Input.Buttons.LEFT);
        controls.blockUntilReleased();
        assertFalse(controls.pressed(ControlAction.ATTACK));
        assertTrue(controls.physicallyPressed(ControlAction.ATTACK));
        assertFalse(controls.justPressed(ControlAction.DASH));
        input.nextFrame();
        assertFalse(controls.pressed(ControlAction.ATTACK));
        // Reassigning a held physical input to another action cannot escape its release guard.
        ControlSettings changed = service.value().withBinding(ControlAction.ATTACK, 0, K)
            .withBinding(ControlAction.GUARD, 0, InputBinding.mouse(Input.Buttons.LEFT));
        service.apply(changed);
        assertFalse(controls.pressed(ControlAction.GUARD));
        input.release(); controls.poll();
        assertFalse(controls.physicallyPressed(ControlAction.GUARD));
        input.button(Input.Buttons.LEFT); input.key(Input.Keys.SHIFT_LEFT);
        assertTrue(controls.pressed(ControlAction.GUARD));
        assertTrue(controls.justPressed(ControlAction.DASH));
    }
}
