package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Validated, immutable content; can be loaded and tested without starting libGDX. */
public final class RegionDefinition {
    public static final String PROTOTYPE_ID = "submundo-prototype";
    public static final String PROTOTYPE_ASSET = "campaign/submundo-prototype.json";
    private final String id, title, start;
    private final Map<String, ArenaDefinition> arenas;

    public RegionDefinition(String id, String title, String start, List<ArenaDefinition> definitions) {
        if (id == null || id.isBlank() || title == null || title.isBlank()
                || start == null || definitions == null || definitions.isEmpty()) {
            throw new IllegalArgumentException("Invalid region");
        }
        this.id = id;
        this.title = title;
        this.start = start;
        Map<String, ArenaDefinition> byId = new LinkedHashMap<>();
        for (ArenaDefinition arena : definitions) {
            if (byId.putIfAbsent(arena.id(), arena) != null) throw new IllegalArgumentException("Duplicate arena: " + arena.id());
        }
        arenas = Map.copyOf(byId);
        arena(start);
        for (ArenaDefinition arena : definitions) for (String exit : arena.exits()) arena(exit);
        Set<String> visited = new HashSet<>();
        visit(start, visited, new HashSet<>());
        if (visited.size() != arenas.size()) throw new IllegalArgumentException("Unreachable arena");
    }
    private void visit(String arenaId, Set<String> visited, Set<String> stack) {
        if (stack.contains(arenaId)) throw new IllegalArgumentException("Cyclic route: " + arenaId);
        if (!visited.add(arenaId)) return;
        stack.add(arenaId);
        for (String exit : arena(arenaId).exits()) visit(exit, visited, stack);
        stack.remove(arenaId);
    }
    public static RegionDefinition fromJson(String text) {
        JsonValue root = new JsonReader().parse(text);
        List<ArenaDefinition> definitions = new ArrayList<>();
        for (JsonValue arena : root.require("arenas")) {
            EnumMap<EnemyType, Integer> goals = new EnumMap<>(EnemyType.class);
            for (JsonValue goal : arena.require("killGoals")) goals.put(EnemyType.valueOf(goal.name()), goal.asInt());
            definitions.add(new ArenaDefinition(arena.getString("id"), arena.getString("title"),
                arena.getString("description"), ArenaDefinition.Kind.valueOf(arena.getString("kind")),
                arena.getFloat("difficultySeconds"), goals, List.of(arena.require("exits").asStringArray())));
        }
        return new RegionDefinition(root.getString("id"), root.getString("title"), root.getString("start"), definitions);
    }
    public ArenaDefinition arena(String arenaId) {
        ArenaDefinition arena = arenas.get(arenaId);
        if (arena == null) throw new IllegalArgumentException("Unknown arena: " + arenaId);
        return arena;
    }
    public String id() { return id; }
    public String title() { return title; }
    public String start() { return start; }
}
