package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import java.util.EnumMap;
import java.util.EnumSet;

/** Only permanent facts. Never contains a player, run, texture or screen. */
public final class CampaignProfile {
    public static final class UnsupportedVersion extends IllegalArgumentException {
        public UnsupportedVersion() { super("Unsupported profile version"); }
    }
    public enum Event { CARONTE_MET, BRIBE_JOKE, GUARDIAN_RECOGNIZED }
    private String characterId = java.util.UUID.randomUUID().toString();
    public String characterId() { return characterId; }
    private int obols, attempts, deaths, victories, forgeMaterials;
    private Profession profession = Profession.CACADOR;
    private GameDifficulty difficulty = GameDifficulty.MEDIUM;
    private WeaponRarity weaponRarity = WeaponRarity.COMMON;
    // Legacy access is retained for migration; currency and new unlocks belong to SharedProgress.
    private boolean professionChosen, necromancerUnlocked;
    public boolean necromancerUnlocked() { return necromancerUnlocked; }
    private final java.util.Set<String> claimedMaterialRegions = new java.util.TreeSet<>();
    private final EnumMap<PermanentAttribute, Integer> ranks = new EnumMap<>(PermanentAttribute.class);
    private final EnumSet<Event> events = EnumSet.noneOf(Event.class);

    public int obols() { return obols; }
    public int forgeMaterials() { return forgeMaterials; }
    public WeaponRarity weaponRarity() { return weaponRarity; }
    public boolean canForgeWeapon() { return weaponRarity == WeaponRarity.COMMON && forgeMaterials > 0; }
    void forgeWeapon() {
        if (!canForgeWeapon()) throw new IllegalStateException("Permanent forge unavailable");
        spendForgeMaterial(); weaponRarity = WeaponRarity.RARE;
    }
    public boolean canClaimForgeMaterial(String regionId) {
        if (regionId == null || regionId.isBlank()) throw new IllegalArgumentException("Invalid material region");
        return !claimedMaterialRegions.contains(regionId);
    }
    boolean claimForgeMaterial(String regionId) {
        if (!canClaimForgeMaterial(regionId)) return false;
        forgeMaterials = Math.addExact(forgeMaterials, 1);
        claimedMaterialRegions.add(regionId); return true;
    }
    void spendForgeMaterial() {
        if (forgeMaterials == 0) throw new IllegalStateException("No forge material");
        forgeMaterials--;
    }
    public int attempts() { return attempts; }
    public int deaths() { return deaths; }
    public int victories() { return victories; }
    public Profession profession() { return profession; }
    public GameDifficulty difficulty() { return difficulty; }
    public boolean professionChosen() { return professionChosen; }
    public boolean hasProgress() {
        return professionChosen || obols > 0 || attempts > 0 || forgeMaterials > 0 || weaponRarity != WeaponRarity.COMMON
            || necromancerUnlocked || !claimedMaterialRegions.isEmpty() || !events.isEmpty()
            || ranks.values().stream().anyMatch(rank -> rank > 0);
    }
    public boolean has(Event event) { return events.contains(event); }
    public int rank(PermanentAttribute attribute) { return ranks.getOrDefault(attribute, 0); }
    public PermanentBonuses bonuses() {
        return new PermanentBonuses(rank(PermanentAttribute.VITALITY), rank(PermanentAttribute.STRENGTH), rank(PermanentAttribute.AGILITY));
    }
    public void chooseProfession(Profession value) { createCharacter(value, GameDifficulty.MEDIUM); }
    public void createCharacter(Profession value, GameDifficulty mode) {
        if (professionChosen) throw new IllegalStateException("Character settings belong to this save");
        java.util.Objects.requireNonNull(value); java.util.Objects.requireNonNull(mode);
        profession = value; difficulty = mode; professionChosen = true;
    }
    public boolean canBuy(PermanentAttribute attribute) {
        int rank = rank(attribute);
        return rank < PermanentAttribute.MAX_RANK && obols >= attribute.price(rank);
    }
    public boolean buy(PermanentAttribute attribute) {
        if (!canBuy(attribute)) return false;
        int rank = rank(attribute);
        obols -= attribute.price(rank); ranks.put(attribute, rank + 1);
        return true;
    }
    public void finishConversation(Event event) {
        if (events.add(event) && event == Event.CARONTE_MET) credit(5);
    }
    void credit(int amount) {
        if (amount < 0) throw new IllegalArgumentException("Negative currency");
        obols = Math.addExact(obols, amount);
    }
    void recordReturn(RunOutcome outcome, int earned) {
        credit(earned); attempts = Math.addExact(attempts, 1);
        if (outcome == RunOutcome.DEATH) deaths = Math.addExact(deaths, 1);
        if (outcome == RunOutcome.VICTORY) victories = Math.addExact(victories, 1);
    }
    public CampaignProfile copy() { return fromJson(toJson()); }
    public String toJson() {
        JsonValue root = new JsonValue(JsonValue.ValueType.object);
        root.addChild("version", new JsonValue(1L));
        root.addChild("characterId", new JsonValue(characterId));
        root.addChild("obols", new JsonValue((long) obols));
        root.addChild("forgeMaterials", new JsonValue((long) forgeMaterials));
        root.addChild("weaponRarity", new JsonValue(weaponRarity.name()));
        JsonValue materialRegions = new JsonValue(JsonValue.ValueType.array);
        for (String region : claimedMaterialRegions) materialRegions.addChild(new JsonValue(region));
        root.addChild("claimedMaterialRegions", materialRegions);
        root.addChild("attempts", new JsonValue((long) attempts));
        root.addChild("deaths", new JsonValue((long) deaths));
        root.addChild("victories", new JsonValue((long) victories));
        root.addChild("profession", new JsonValue(profession.name()));
        root.addChild("difficulty", new JsonValue(difficulty.name()));
        root.addChild("professionChosen", new JsonValue(professionChosen));
        root.addChild("necromancerUnlocked", new JsonValue(necromancerUnlocked));
        JsonValue upgrades = new JsonValue(JsonValue.ValueType.object);
        for (PermanentAttribute attribute : PermanentAttribute.values())
            upgrades.addChild(attribute.name(), new JsonValue((long) rank(attribute)));
        root.addChild("attributes", upgrades);
        JsonValue flags = new JsonValue(JsonValue.ValueType.array);
        for (Event event : events) flags.addChild(new JsonValue(event.name()));
        root.addChild("events", flags);
        return root.prettyPrint(JsonWriter.OutputType.json, 100);
    }
    public static CampaignProfile fromJson(String json) {
        JsonValue root = new JsonReader().parse(json);
        if (!root.isObject()) throw new IllegalArgumentException("Invalid profile");
        if (nonnegative(root, "version") != 1) throw new UnsupportedVersion();
        CampaignProfile profile = new CampaignProfile();
        profile.characterId = root.has("characterId") ? RunReport.string(root, "characterId") : "";
        if (!profile.characterId.isEmpty()) java.util.UUID.fromString(profile.characterId);
        // Existing version-one saves predate materials and remain readable.
        profile.forgeMaterials = root.has("forgeMaterials") ? nonnegative(root, "forgeMaterials") : 0;
        if (root.has("claimedMaterialRegions")) {
            JsonValue materialRegions = root.require("claimedMaterialRegions");
            if (!materialRegions.isArray()) throw new IllegalArgumentException("Invalid material regions");
            for (JsonValue entry : materialRegions) {
                if (!entry.isString() || entry.asString().isBlank() || !profile.claimedMaterialRegions.add(entry.asString()))
                    throw new IllegalArgumentException("Invalid material region");
            }
        } else if (profile.forgeMaterials > 0) {
            // Old material saves only had the Submundo and did not record origins.
            profile.claimedMaterialRegions.add(RegionDefinition.PROTOTYPE_ID);
        }
        if (root.has("weaponRarity")) {
            JsonValue rarity = root.require("weaponRarity");
            if (!rarity.isString()) throw new IllegalArgumentException("Invalid weapon rarity");
            profile.weaponRarity = WeaponRarity.valueOf(rarity.asString());
        } else if (root.has("claimedMaterialRegions") && profile.claimedMaterialRegions.size() > profile.forgeMaterials) {
            // Previous builds recorded collected regions and stock: a spent fragment means a forge.
            profile.weaponRarity = WeaponRarity.RARE;
        }
        profile.obols = nonnegative(root, "obols"); profile.attempts = nonnegative(root, "attempts");
        profile.deaths = nonnegative(root, "deaths"); profile.victories = nonnegative(root, "victories");
        if ((long) profile.deaths + profile.victories > profile.attempts) throw new IllegalArgumentException("Invalid return counts");
        profile.profession = Profession.valueOf(root.getString("profession"));
        profile.difficulty = root.has("difficulty") ? GameDifficulty.valueOf(RunReport.string(root, "difficulty")) : GameDifficulty.MEDIUM;
        profile.professionChosen = root.getBoolean("professionChosen");
        if (root.has("necromancerUnlocked")) {
            JsonValue flag = root.require("necromancerUnlocked");
            if (!flag.isBoolean()) throw new IllegalArgumentException("Invalid legendary unlock");
            profile.necromancerUnlocked = flag.asBoolean();
        }
        JsonValue attributes = root.require("attributes"), flags = root.require("events");
        if (!attributes.isObject() || !flags.isArray()) throw new IllegalArgumentException("Invalid profile fields");
        for (PermanentAttribute attribute : PermanentAttribute.values()) {
            int rank = nonnegative(attributes, attribute.name());
            if (rank > PermanentAttribute.MAX_RANK) throw new IllegalArgumentException("Invalid rank");
            profile.ranks.put(attribute, rank);
        }
        for (JsonValue flag : flags) profile.events.add(Event.valueOf(flag.asString()));
        return profile;
    }
    private static int nonnegative(JsonValue parent, String name) {
        JsonValue value = parent.require(name);
        if (!value.isLong() || value.asLong() < 0 || value.asLong() > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Invalid " + name);
        return value.asInt();
    }
}
