package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import java.util.EnumMap;
import java.util.EnumSet;

/** Durable character facts and immutable checkpoint data; never a live player, run, texture or screen. */
public final class CampaignProfile {
    public static final class UnsupportedVersion extends IllegalArgumentException {
        public UnsupportedVersion() { super("Unsupported profile version"); }
    }
    public enum Event { CARONTE_MET, BRIBE_JOKE, GUARDIAN_RECOGNIZED, INSURGENCE_SPARK_OBTAINED, UNDERWORLD_EPILOGUE_READ }
    public enum CinematicState { UNSEEN, COMPLETED, SKIPPED, LEGACY }
    private String characterId = java.util.UUID.randomUUID().toString();
    public String characterId() { return characterId; }
    private long attemptSequence,lastSettledSequence;
    private RunCheckpoint checkpoint;
    private RunSettlement pendingSettlement;
    public long attemptSequence(){return attemptSequence;}
    public long lastSettledSequence(){return lastSettledSequence;}
    public RunCheckpoint checkpoint(){return checkpoint;}
    public RunSettlement pendingSettlement(){return pendingSettlement;}
    void ensureCharacterIdentity(){if(characterId.isEmpty())characterId=java.util.UUID.randomUUID().toString();}
    /** Recovered personal files must not reuse an attempt already consumed by the shared ledger. */
    void syncAttemptSequence(long completed){
        if(completed<0||checkpoint!=null||pendingSettlement!=null)throw new IllegalStateException("Attempt is still active");
        attemptSequence=Math.max(attemptSequence,completed);
        lastSettledSequence=Math.max(lastSettledSequence,completed);
    }
    /** Consumes only the attempt identity: no run reward, return count or narrative event is applied. */
    void discardCheckpoint(){
        if(checkpoint==null||pendingSettlement!=null)throw new IllegalStateException("No discardable checkpoint");
        lastSettledSequence=Math.max(lastSettledSequence,checkpoint.sequence());
        checkpoint=null;
    }
    void checkpoint(RunCheckpoint value){
        java.util.Objects.requireNonNull(value);
        if(!professionChosen||characterId.isEmpty()||pendingSettlement!=null||!characterId.equals(value.characterId())
            ||profession!=value.profession()||difficulty!=value.difficulty()||value.sequence()<=lastSettledSequence
            ||value.sequence()<attemptSequence||value.sequence()>Math.addExact(attemptSequence,1))
            throw new IllegalStateException("Checkpoint does not belong to this character or attempt");
        if(checkpoint!=null&&(!checkpoint.runId().equals(value.runId())||value.sequence()!=checkpoint.sequence()
            ||value.revision()<=checkpoint.revision()))throw new IllegalStateException("Stale checkpoint");
        checkpoint=value;attemptSequence=value.sequence();
    }
    void recordSettlement(RunSettlement receipt){
        java.util.Objects.requireNonNull(receipt);
        if(!characterId.equals(receipt.report().characterId())||receipt.sequence()!=attemptSequence
            ||receipt.sequence()<=lastSettledSequence||pendingSettlement!=null||checkpoint==null
            ||!checkpoint.runId().equals(receipt.report().id()))throw new IllegalStateException("Stale settlement");
        recordReturn(receipt.report().outcome(),receipt.report().obols());
        lastSettledSequence=receipt.sequence();checkpoint=null;pendingSettlement=receipt;
    }
    boolean acknowledgeSettlement(RunSettlement receipt){
        if(!characterId.equals(receipt.report().characterId()))return false;
        if(pendingSettlement==null)return receipt.sequence()<=lastSettledSequence;
        if(pendingSettlement.sequence()!=receipt.sequence()||!pendingSettlement.report().id().equals(receipt.report().id()))return false;
        pendingSettlement=null;return true;
    }
    private int obols, attempts, deaths, victories, forgeMaterials;
    private Profession profession = Profession.CACADOR;
    private GameDifficulty difficulty = GameDifficulty.MEDIUM;
    private WeaponRarity weaponRarity = WeaponRarity.COMMON;
    private FarmerQuest farmerQuest = FarmerQuest.NOT_STARTED;
    public FarmerQuest farmerQuest() { return farmerQuest; }
    void acceptFarmerQuest() {
        if (farmerQuest != FarmerQuest.NOT_STARTED) throw new IllegalStateException("Quest already accepted");
        farmerQuest = FarmerQuest.ACCEPTED;
    }
    void advanceFarmerQuest(FarmerQuest value) {
        if (farmerQuest == FarmerQuest.NOT_STARTED) throw new IllegalStateException("Quest not accepted");
        if (value.ordinal() > farmerQuest.ordinal()) farmerQuest = value;
    }
    // Legacy access is retained for migration; currency and new unlocks belong to SharedProgress.
    private boolean professionChosen, necromancerUnlocked;
    public boolean necromancerUnlocked() { return necromancerUnlocked; }
    private final java.util.Set<String> claimedMaterialRegions = new java.util.TreeSet<>();
    private final EnumMap<PermanentAttribute, Integer> ranks = new EnumMap<>(PermanentAttribute.class);
    private final EnumSet<Event> events = EnumSet.noneOf(Event.class);
    private final EnumSet<TutorialTopic> tutorialSeen=EnumSet.noneOf(TutorialTopic.class);
    private boolean tutorialEnabled=true;
    private final java.util.Map<String,CinematicState> cinematics=new java.util.TreeMap<>();
    public boolean openingSeen(){return !openingPending();}
    public boolean openingPending(){return openingState()==CinematicState.UNSEEN;}
    public CinematicState openingState(){return cinematics.getOrDefault("opening-v1",CinematicState.UNSEEN);}
    public CinematicState cinematicState(String id){return cinematics.get(java.util.Objects.requireNonNull(id));}
    boolean markOpening(boolean skipped) {
        CinematicState current=openingState(),next=skipped?CinematicState.SKIPPED:CinematicState.COMPLETED;
        if(current!=CinematicState.UNSEEN)return false;
        cinematics.put("opening-v1",next);return true;
    }
    public boolean tutorialSeen(TutorialTopic topic){return tutorialSeen.contains(java.util.Objects.requireNonNull(topic));}
    public boolean tutorialEnabled(){return tutorialEnabled;}
    void completeTutorial(TutorialTopic topic){tutorialSeen.add(java.util.Objects.requireNonNull(topic));}
    void setTutorialEnabled(boolean enabled){tutorialEnabled=enabled;}

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
            || necromancerUnlocked || farmerQuest != FarmerQuest.NOT_STARTED || !claimedMaterialRegions.isEmpty() || !events.isEmpty()
            || !cinematics.isEmpty() || checkpoint!=null || pendingSettlement!=null || ranks.values().stream().anyMatch(rank -> rank > 0);
    }
    public boolean has(Event event) { return events.contains(event); }
    public int rank(PermanentAttribute attribute) { return ranks.getOrDefault(attribute, 0); }
    public PermanentBonuses bonuses() {
        return new PermanentBonuses(rank(PermanentAttribute.VITALITY), rank(PermanentAttribute.STRENGTH), rank(PermanentAttribute.AGILITY),
            rank(PermanentAttribute.ENDURANCE),rank(PermanentAttribute.RECOVERY),rank(PermanentAttribute.ARMOR));
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
        root.addChild("version", new JsonValue(2L));
        root.addChild("attemptSequence",new JsonValue(attemptSequence));
        root.addChild("lastSettledSequence",new JsonValue(lastSettledSequence));
        root.addChild("checkpoint",checkpoint==null?new JsonValue(JsonValue.ValueType.nullValue):checkpoint.json());
        root.addChild("pendingSettlement",pendingSettlement==null?new JsonValue(JsonValue.ValueType.nullValue):pendingSettlement.json());
        root.addChild("narrativeVersion",new JsonValue(1L));
        root.addChild("characterId", new JsonValue(characterId));
        root.addChild("tutorialEnabled",new JsonValue(tutorialEnabled));
        JsonValue tutorials=new JsonValue(JsonValue.ValueType.array);
        for(TutorialTopic topic:tutorialSeen)tutorials.addChild(new JsonValue(topic.name()));
        root.addChild("tutorialSeen",tutorials);
        JsonValue scenes=new JsonValue(JsonValue.ValueType.array);
        for(var entry:cinematics.entrySet()) {
            JsonValue scene=new JsonValue(JsonValue.ValueType.object);
            scene.addChild("id",new JsonValue(entry.getKey()));scene.addChild("state",new JsonValue(entry.getValue().name()));
            scenes.addChild(scene);
        }
        root.addChild("cinematics",scenes);
        root.addChild("obols", new JsonValue((long) obols));
        root.addChild("forgeMaterials", new JsonValue((long) forgeMaterials));
        root.addChild("weaponRarity", new JsonValue(weaponRarity.name()));
        root.addChild("farmerQuest", new JsonValue(farmerQuest.name()));
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
        int version=nonnegative(root,"version");
        if(version!=1&&version!=2)throw new UnsupportedVersion();
        if(version==2){
            // Inspect known versioned envelopes before interpreting any of their newer contents.
            JsonValue checkpoint=root.get("checkpoint"),receipt=root.get("pendingSettlement");
            rejectFutureSnapshotVersion(checkpoint);
            rejectFutureSnapshotVersion(snapshotChild(snapshotChild(checkpoint,"state"),"player"));
            rejectFutureSnapshotVersion(receipt);
            rejectFutureSnapshotVersion(snapshotChild(receipt,"report"));
        }
        java.util.Set<String> rootFields=new java.util.HashSet<>();
        for(JsonValue field:root)if(!rootFields.add(field.name))throw new IllegalArgumentException("Duplicate profile field");
        boolean legacyNarrative=!root.has("narrativeVersion");
        if(!legacyNarrative && nonnegative(root,"narrativeVersion")!=1)throw new UnsupportedVersion();
        CampaignProfile profile = new CampaignProfile();
        if(root.has("tutorialEnabled")) {
            JsonValue enabled=root.require("tutorialEnabled");
            if(!enabled.isBoolean())throw new IllegalArgumentException("Invalid tutorial setting");
            profile.tutorialEnabled=enabled.asBoolean();
        }
        if(root.has("tutorialSeen")) {
            JsonValue tutorials=root.require("tutorialSeen");
            if(!tutorials.isArray())throw new IllegalArgumentException("Invalid tutorial lessons");
            for(JsonValue entry:tutorials) {
                if(!entry.isString()||!profile.tutorialSeen.add(TutorialTopic.valueOf(entry.asString())))
                    throw new IllegalArgumentException("Invalid tutorial lesson");
            }
        }
        if(root.has("cinematics")) {
            JsonValue scenes=root.require("cinematics");
            if(!scenes.isArray()||scenes.size>64)throw new IllegalArgumentException("Invalid cinematic records");
            for(JsonValue scene:scenes) {
                if(!scene.isObject()||scene.size!=2)throw new IllegalArgumentException("Invalid cinematic record");
                java.util.Set<String> fields=new java.util.HashSet<>();
                for(JsonValue field:scene)
                    if(!java.util.Set.of("id","state").contains(field.name)||!fields.add(field.name))
                        throw new IllegalArgumentException("Invalid cinematic record fields");
                String id=RunReport.string(scene,"id");
                if(id.isBlank()||id.length()>80)throw new IllegalArgumentException("Invalid cinematic id");
                CinematicState state=CinematicState.valueOf(RunReport.string(scene,"state"));
                if(profile.cinematics.putIfAbsent(id,state)!=null)throw new IllegalArgumentException("Duplicate cinematic id");
            }
        }
        if (root.has("farmerQuest")) profile.farmerQuest = FarmerQuest.valueOf(RunReport.string(root, "farmerQuest"));
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
        // The former collection upgrade becomes armor at the same purchased rank.
        // Validate the legacy value even when a current ARMOR value takes precedence.
        int legacyArmor = attributes.has("COLLECTION") ? permanentRank(attributes, "COLLECTION") : 0;
        for (PermanentAttribute attribute : PermanentAttribute.values()) {
            int rank = attributes.has(attribute.name()) ? permanentRank(attributes, attribute.name())
                : attribute == PermanentAttribute.ARMOR ? legacyArmor
                : attribute.optionalInLegacyProfile() ? 0 : permanentRank(attributes, attribute.name());
            profile.ranks.put(attribute, rank);
        }
        for (JsonValue flag : flags) profile.events.add(Event.valueOf(flag.asString()));
        // Before the narrative schema existed, every recorded victory belonged to the Submundo.
        if(legacyNarrative && profile.victories>0)profile.events.add(Event.INSURGENCE_SPARK_OBTAINED);
        // Existing characters are not forced through a newly introduced opening, nor marked as having watched it.
        if(!root.has("cinematics") && profile.attempts>0)profile.cinematics.put("opening-v1",CinematicState.LEGACY);
        if(version==2){
            profile.attemptSequence=nonnegativeLong(root,"attemptSequence");
            profile.lastSettledSequence=nonnegativeLong(root,"lastSettledSequence");
            if(profile.lastSettledSequence>profile.attemptSequence)throw new IllegalArgumentException("Invalid attempt sequence");
            JsonValue checkpoint=root.require("checkpoint"),receipt=root.require("pendingSettlement");
            if(!checkpoint.isNull())profile.checkpoint=RunCheckpoint.fromJson(checkpoint);
            if(!receipt.isNull())profile.pendingSettlement=RunSettlement.fromJson(receipt);
            if(profile.checkpoint!=null){
                RunCheckpoint cp=profile.checkpoint;
                if(!profile.professionChosen||profile.characterId.isEmpty()||!profile.characterId.equals(cp.characterId())
                    ||cp.sequence()!=profile.attemptSequence||cp.sequence()<=profile.lastSettledSequence
                    ||cp.profession()!=profile.profession||cp.difficulty()!=profile.difficulty||profile.pendingSettlement!=null)
                    throw new IllegalArgumentException("Checkpoint identity or lifecycle mismatch");
            }
            if(profile.pendingSettlement!=null){
                RunSettlement pending=profile.pendingSettlement;
                if(!profile.professionChosen||!profile.characterId.equals(pending.report().characterId())
                    ||pending.sequence()!=profile.attemptSequence||pending.sequence()!=profile.lastSettledSequence
                    ||pending.report().profession()!=profile.profession||pending.report().difficulty()!=profile.difficulty)
                    throw new IllegalArgumentException("Settlement identity or lifecycle mismatch");
            }
        }
        return profile;
    }
    private static JsonValue snapshotChild(JsonValue parent,String name){
        return parent!=null&&parent.isObject()?parent.get(name):null;
    }
    private static void rejectFutureSnapshotVersion(JsonValue snapshot){
        JsonValue version=snapshotChild(snapshot,"version");
        if(version!=null&&version.isLong()&&version.asLong()>1)throw new UnsupportedVersion();
    }
    private static int permanentRank(JsonValue attributes, String name) {
        int rank = nonnegative(attributes, name);
        if (rank > PermanentAttribute.MAX_RANK) throw new IllegalArgumentException("Invalid rank: " + name);
        return rank;
    }
    private static int nonnegative(JsonValue parent, String name) {
        JsonValue value = parent.require(name);
        if (!value.isLong() || value.asLong() < 0 || value.asLong() > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Invalid " + name);
        return value.asInt();
    }
    private static long nonnegativeLong(JsonValue parent,String name){
        JsonValue value=parent.require(name);
        if(!value.isLong()||value.asLong()<0)throw new IllegalArgumentException("Invalid "+name);
        return value.asLong();
    }
}
