package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Immutable envelope. JSON is copied at the boundary so a UI cannot mutate an already committed checkpoint. */
public final class RunCheckpoint {
    public enum Stage { ROOM_START,ARENA_REWARD,BLACKSMITH,FARMER,ROUTE }
    public static final class UnsupportedVersion extends IllegalArgumentException {
        public UnsupportedVersion(){super("Versão de checkpoint não suportada.");}
    }
    private final String runId,characterId,regionId,regionFingerprint,stateJson;
    private final long sequence,revision;
    private final Stage stage;
    private final Profession profession;
    private final GameDifficulty difficulty;
    private RunCheckpoint(String runId,String characterId,long sequence,long revision,Stage stage,String regionId,
                          String regionFingerprint,Profession profession,GameDifficulty difficulty,JsonValue state){
        java.util.UUID.fromString(runId);java.util.UUID.fromString(characterId);
        if(sequence<=0||revision<=0||regionId==null||regionId.isBlank()||regionId.length()>200
            ||regionFingerprint==null||!regionFingerprint.matches("[0-9a-f]{64}"))throw SnapshotJson.invalid("identidade do checkpoint");
        this.runId=runId;this.characterId=characterId;this.sequence=sequence;this.revision=revision;
        this.stage=java.util.Objects.requireNonNull(stage);this.regionId=regionId;this.regionFingerprint=regionFingerprint;
        this.profession=java.util.Objects.requireNonNull(profession);this.difficulty=java.util.Objects.requireNonNull(difficulty);
        if(state==null||!state.isObject()||!runId.equals(SnapshotJson.text(state,"id"))
            ||!characterId.equals(SnapshotJson.text(state,"characterId"))||!SnapshotJson.flag(state,"characterBound")
            ||sequence!=SnapshotJson.longInteger(state,"attemptSequence",1,Long.MAX_VALUE)
            ||!regionId.equals(SnapshotJson.text(state,"regionId"))||!regionFingerprint.equals(SnapshotJson.text(state,"regionFingerprint"))
            ||profession!=SnapshotJson.enumValue(state,"profession",Profession.class)||difficulty!=SnapshotJson.enumValue(state,"difficulty",GameDifficulty.class))
            throw SnapshotJson.invalid("cabeçalho da tentativa");
        stateJson=state.toJson(JsonWriter.OutputType.json);
        if(stateJson.length()>1_000_000)throw SnapshotJson.invalid("tamanho do checkpoint");
    }
    public static RunCheckpoint capture(RunSession run,Stage stage,long sequence,long revision){
        java.util.Objects.requireNonNull(run);validateStage(run,stage);
        if(run.attemptSequence()!=0&&run.attemptSequence()!=sequence)throw SnapshotJson.invalid("sequência da tentativa");
        JsonValue state=run.checkpointState();state.get("attemptSequence").set(sequence,null);
        return new RunCheckpoint(run.id(),run.characterId(),sequence,revision,stage,run.region().id(),fingerprint(run.region()),
            run.player().getProfession(),run.difficulty(),state);
    }
    public static RunCheckpoint fromJson(JsonValue value){
        if(value==null||!value.isObject())throw SnapshotJson.invalid("checkpoint");
        if(SnapshotJson.longInteger(value,"version",1,Long.MAX_VALUE)!=1)throw new UnsupportedVersion();
        SnapshotJson.object(value,"version","runId","characterId","sequence","revision","stage","regionId","regionFingerprint","profession","difficulty","state");
        return new RunCheckpoint(SnapshotJson.text(value,"runId"),SnapshotJson.text(value,"characterId"),
            SnapshotJson.longInteger(value,"sequence",1,Long.MAX_VALUE),SnapshotJson.longInteger(value,"revision",1,Long.MAX_VALUE),
            SnapshotJson.enumValue(value,"stage",Stage.class),SnapshotJson.text(value,"regionId"),SnapshotJson.text(value,"regionFingerprint"),
            SnapshotJson.enumValue(value,"profession",Profession.class),SnapshotJson.enumValue(value,"difficulty",GameDifficulty.class),SnapshotJson.required(value,"state"));
    }
    public static RunCheckpoint fromJson(String text){return fromJson(new JsonReader().parse(text));}
    public JsonValue json(){
        JsonValue value=new JsonValue(JsonValue.ValueType.object);SnapshotJson.put(value,"version",1L);
        SnapshotJson.put(value,"runId",runId);SnapshotJson.put(value,"characterId",characterId);SnapshotJson.put(value,"sequence",sequence);
        SnapshotJson.put(value,"revision",revision);SnapshotJson.put(value,"stage",stage.name());SnapshotJson.put(value,"regionId",regionId);
        SnapshotJson.put(value,"regionFingerprint",regionFingerprint);SnapshotJson.put(value,"profession",profession.name());
        SnapshotJson.put(value,"difficulty",difficulty.name());value.addChild("state",state());return value;
    }
    public JsonValue toJson(){return json();}
    public String toJsonString(){return json().prettyPrint(JsonWriter.OutputType.json,100);}
    public JsonValue state(){return new JsonReader().parse(stateJson);}
    public RunSession restore(RegionDefinition region){
        if(!regionId.equals(region.id())||!regionFingerprint.equals(fingerprint(region)))throw SnapshotJson.invalid("conteúdo da região alterado");
        RunSession run=RunSession.fromCheckpointState(region,state());
        run.bindAttemptSequence(sequence);validateStage(run,stage);return run;
    }
    public String runId(){return runId;}
    public String characterId(){return characterId;}
    public long sequence(){return sequence;}
    public long revision(){return revision;}
    public Stage stage(){return stage;}
    public String regionId(){return regionId;}
    public String regionFingerprint(){return regionFingerprint;}
    public Profession profession(){return profession;}
    public GameDifficulty difficulty(){return difficulty;}
    private static void validateStage(RunSession run,Stage stage){
        if(stage==null||run.isDemo()||run.finished()||run.bossDefeated()||run.player().estaMorto()||run.player().podeSubirDeNivel())throw SnapshotJson.invalid("ponto seguro");
        boolean reward=run.canClaimArenaReward(),smith=run.encounters().blacksmithOpen(),farmer=run.encounters().farmerOpen();
        boolean valid=switch(stage){
            case ROOM_START -> !run.arenaComplete()&&run.arenaElapsed()==0&&java.util.Arrays.stream(EnemyType.values()).allMatch(type->run.kills(type)==0);
            case ARENA_REWARD -> run.arenaComplete()&&reward&&!smith&&!farmer;
            case BLACKSMITH -> run.arenaComplete()&&!reward&&smith&&!farmer;
            case FARMER -> run.arenaComplete()&&!reward&&!smith&&farmer;
            case ROUTE -> run.arenaComplete()&&!reward&&!smith&&!farmer&&!run.arena().exits().isEmpty();
        };
        if(!valid)throw SnapshotJson.invalid("etapa do checkpoint");
    }
    static String fingerprint(RegionDefinition region){
        JsonValue root=new JsonValue(JsonValue.ValueType.object);SnapshotJson.put(root,"id",region.id());SnapshotJson.put(root,"title",region.title());SnapshotJson.put(root,"start",region.start());
        JsonValue arenas=new JsonValue(JsonValue.ValueType.array);
        for(ArenaDefinition arena:region.arenas()){
            JsonValue value=new JsonValue(JsonValue.ValueType.object);SnapshotJson.put(value,"id",arena.id());SnapshotJson.put(value,"title",arena.title());
            SnapshotJson.put(value,"description",arena.description());SnapshotJson.put(value,"kind",arena.kind().name());
            SnapshotJson.put(value,"difficultySeconds",arena.difficultySeconds());SnapshotJson.put(value,"reward",arena.reward().name());SnapshotJson.put(value,"waveIntervalMultiplier",arena.waveIntervalMultiplier());
            JsonValue goals=new JsonValue(JsonValue.ValueType.object);for(EnemyType type:EnemyType.values())if(arena.killGoals().containsKey(type))SnapshotJson.put(goals,type.name(),arena.killGoals().get(type).longValue());
            value.addChild("killGoals",goals);JsonValue exits=new JsonValue(JsonValue.ValueType.array);for(String exit:arena.exits())exits.addChild(new JsonValue(exit));value.addChild("exits",exits);arenas.addChild(value);
        }
        root.addChild("arenas",arenas);
        try{return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(root.toJson(JsonWriter.OutputType.json).getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
}
