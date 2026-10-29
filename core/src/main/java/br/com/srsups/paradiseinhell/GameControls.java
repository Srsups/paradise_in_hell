package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/** Runtime input reader. Captured/held controls require a fresh release after returning from settings. */
public final class GameControls {
    private final ControlSettingsService service;
    private final Supplier<Input> input;
    private final Set<InputBinding> blocked = new HashSet<>();

    public GameControls() { this(new ControlSettingsService(ControlSettingsStore.memory())); }
    public GameControls(ControlSettingsService service) { this(service, () -> Gdx.input); }
    public GameControls(ControlSettingsService service, Supplier<Input> input) {
        this.service = Objects.requireNonNull(service);
        this.input = Objects.requireNonNull(input);
    }
    public String label(ControlAction action) { return service.value().label(action); }
    /** Release guards must observe the physical input even while gameplay queries are blocked. */
    public boolean physicallyPressed(ControlAction action) {
        Input state = input.get();
        for (InputBinding binding : service.value().bindings(action))
            if (binding.pressed(state)) return true;
        return false;
    }
    public void poll() {
        Input state = input.get();
        blocked.removeIf(binding -> !binding.pressed(state));
    }
    public boolean pressed(ControlAction action) {
        poll();
        Input state = input.get();
        for (InputBinding binding : service.value().bindings(action))
            if (!blocked.contains(binding) && binding.pressed(state)) return true;
        return false;
    }
    public boolean justPressed(ControlAction action) {
        poll();
        Input state = input.get();
        for (InputBinding binding : service.value().bindings(action))
            if (!blocked.contains(binding) && binding.justPressed(state)) return true;
        return false;
    }
    public void blockUntilReleased() {
        Input state = input.get();
        for (ControlAction action : ControlAction.values())
            for (InputBinding binding : service.value().bindings(action))
                if (binding.pressed(state)) blocked.add(binding);
    }
}
