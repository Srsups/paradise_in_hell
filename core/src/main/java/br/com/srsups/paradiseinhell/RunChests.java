package br.com.srsups.paradiseinhell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.random.RandomGenerator;

/** Arena-local random supplies. No textures, solid structures or profile writes. */
public final class RunChests {
    public enum Reward { HEALTH, EXPERIENCE, STAMINA, OBOLS }
    public record Chest(float x, float y, Reward reward) {}
    private final RandomGenerator random;
    private final List<Chest> chests = new ArrayList<>();
    private final List<Chest> view = Collections.unmodifiableList(chests);
    private float timer;
    public RunChests() { this(new java.util.Random()); }
    public RunChests(RandomGenerator random) { this.random = random; timer = interval(18, 30); }
    public List<Chest> chests() { return view; }
    public void update(float delta, float playerX, float playerY, EnemyNavigation navigation, boolean campaign) {
        if (!Float.isFinite(delta) || delta < 0) throw new IllegalArgumentException("Invalid time");
        chests.removeIf(chest -> squared(chest.x - playerX, chest.y - playerY) > 600 * 600);
        timer -= delta;
        if (timer > 0) return;
        timer = interval(25, 40);
        if (chests.size() >= 3) return;
        for (int attempt = 0; attempt < 24; attempt++) {
            float x = playerX + random.nextFloat() * 300 - 150;
            float y = playerY + random.nextFloat() * 140 - 80;
            if (squared(x - playerX, y - playerY) < 40 * 40 || !navigation.canOccupy(x, y) || !navigation.reachable(x, y)) continue;
            if (chests.stream().anyMatch(chest -> squared(chest.x - x, chest.y - y) < 30 * 30)) continue;
            Reward reward = Reward.values()[random.nextInt(campaign ? 4 : 3)];
            chests.add(new Chest(x, y, reward)); return;
        }
    }
    public Chest nearby(float playerX, float playerY) {
        Chest closest = null;
        float distance = 24 * 24;
        for (Chest chest : chests) {
            float current = squared(chest.x - playerX, chest.y - playerY);
            if (current <= distance) { closest = chest; distance = current; }
        }
        return closest;
    }
    public String open(Chest chest, RunSession session) {
        if (!chests.contains(chest)) return "";
        PlayerState player = session.player();
        Reward reward = chest.reward;
        if (reward == Reward.HEALTH && player.getVida() >= player.getVidaMaxima()
            || reward == Reward.STAMINA && player.getEstaminaAtual() >= player.getEstaminaMaxima()) reward = Reward.EXPERIENCE;
        String message;
        switch (reward) {
            case HEALTH -> { player.curar(30); message = "+30 de vida"; }
            case EXPERIENCE -> {
                float gained = player.xpGain(12);
                player.ganharXP(12);
                message = "+" + CombatNumbers.format(gained) + " XP";
            }
            case STAMINA -> { player.recuperarEstamina(40); message = "+40 de estamina"; }
            case OBOLS -> {
                int gained = session.previewChestObols(3);
                session.addChestObols(3); message = "+" + gained + " óbolos";
            }
            default -> throw new IllegalStateException();
        }
        chests.remove(chest); return message;
    }
    public void clear() { chests.clear(); }
    private float interval(float min, float max) { return min + random.nextFloat() * (max - min); }
    private static float squared(float x, float y) { return x * x + y * y; }
}
