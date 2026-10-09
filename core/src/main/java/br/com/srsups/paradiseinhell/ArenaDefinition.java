package br.com.srsups.paradiseinhell;

import java.util.List;
import java.util.Map;

public record ArenaDefinition(String id, String title, String description, Kind kind,
                              float difficultySeconds, Map<EnemyType, Integer> killGoals, List<String> exits) {
    public enum Kind { COMBAT, BOSS_ROOM }
    public ArenaDefinition {
        if (id == null || id.isBlank() || title == null || title.isBlank() || description == null
                || kind == null || !Float.isFinite(difficultySeconds) || difficultySeconds < 0
                || killGoals == null || exits == null || exits.size() > 3
                || exits.stream().anyMatch(exit -> exit == null || exit.isBlank())
                || exits.stream().distinct().count() != exits.size()
                || killGoals.entrySet().stream().anyMatch(entry -> entry.getKey() == null || entry.getValue() == null || entry.getValue() <= 0)
                || kind == Kind.COMBAT && (killGoals.isEmpty() || exits.isEmpty())
                || kind == Kind.BOSS_ROOM && (!killGoals.isEmpty() || !exits.isEmpty())) {
            throw new IllegalArgumentException("Invalid arena: " + id);
        }
        killGoals = Map.copyOf(killGoals);
        exits = List.copyOf(exits);
    }
    public String goalDescription() {
        if (kind == Kind.BOSS_ROOM) return "Sala do chefe";
        StringBuilder description = new StringBuilder();
        for (EnemyType type : EnemyType.values()) {
            Integer amount = killGoals.get(type);
            if (amount == null) continue;
            if (!description.isEmpty()) description.append("   |   ");
            description.append(type.label).append(": ").append(amount);
        }
        return description.toString();
    }
}
