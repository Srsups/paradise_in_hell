package br.com.srsups.paradiseinhell;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class AudioSettingsTest {
    @TempDir Path directory;
    @Test void roundTripAndMutePreserveIndividualVolumesAndRejectInvalidValues() {
        AudioSettings settings=new AudioSettings(.65f,.2f,true);
        assertEquals(settings,AudioSettings.fromJson(settings.toJson()));
        assertEquals(0,settings.effectsGain());assertEquals(0,settings.musicGain());
        assertEquals(.65f,settings.effects());
        assertThrows(IllegalArgumentException.class,()->new AudioSettings(Float.NaN,.5f,false));
        assertThrows(IllegalArgumentException.class,()->new AudioSettings(.5f,1.01f,false));
        assertThrows(IllegalArgumentException.class,()->new AudioSettings(-.1f,.5f,false));
        assertThrows(IllegalArgumentException.class,()->AudioSettings.fromJson(settings.toJson().replace("true","7")));
    }
    @Test void persistedSettingsReloadAndAValidBackupRecoversAChangedFile() throws Exception {
        Path path=directory.resolve("audio-settings.json");
        FileAudioSettingsStore store=new FileAudioSettingsStore(path);
        assertEquals(AudioSettings.DEFAULT,store.load());
        AudioSettings first=new AudioSettings(.4f,.6f,false),second=new AudioSettings(.7f,.1f,true);
        store.save(first);store.save(second);assertEquals(second,new FileAudioSettingsStore(path).load());
        Files.writeString(path,"broken");
        assertEquals(first,store.load());store.save(first);
        assertEquals(first,AudioSettings.fromJson(Files.readString(path)));
        assertEquals(first,AudioSettings.fromJson(Files.readString(directory.resolve("audio-settings.json.bak"))));
    }
    @Test void unknownVersionIsPreservedEvenWhenAnOlderBackupExists() throws Exception {
        Path path=directory.resolve("audio-settings.json");
        FileAudioSettingsStore store=new FileAudioSettingsStore(path);
        store.save(AudioSettings.DEFAULT);store.save(AudioSettings.DEFAULT);
        String future=AudioSettings.DEFAULT.toJson().replace("\"version\": 1","\"version\": 2");
        Files.writeString(path,future);
        AudioSettingsService service=new AudioSettingsService(store);
        service.change(new AudioSettings(0,0,true));assertFalse(service.save());
        assertEquals(future,Files.readString(path));assertFalse(service.error().isEmpty());
    }
    @Test void failedWriteAppliesRuntimeVolumeAndCanBeRetriedWithoutLosingTheSavedValue() {
        AudioSettings[] saved={AudioSettings.DEFAULT};boolean[] fail={true};
        AudioSettingsService service=new AudioSettingsService(new AudioSettingsStore(){
            public AudioSettings load(){return saved[0];}
            public void save(AudioSettings settings)throws IOException{if(fail[0])throw new IOException("locked");saved[0]=settings;}
        });
        AudioSettings next=new AudioSettings(.2f,.4f,true);service.change(next);
        assertFalse(service.save());assertEquals(next,service.value());assertTrue(service.pending());
        assertEquals(AudioSettings.DEFAULT,saved[0]);fail[0]=false;
        assertTrue(service.save());assertEquals(next,saved[0]);assertFalse(service.pending());assertEquals("",service.error());
    }
    @Test void unreadablePreferencesRemainUntouchedWhileRuntimeControlsStillWork() throws Exception {
        Path path=directory.resolve("audio-settings.json");Files.writeString(path,"bad");
        AudioSettingsService service=new AudioSettingsService(new FileAudioSettingsStore(path));
        service.change(new AudioSettings(.1f,.2f,true));
        assertFalse(service.save());assertEquals("bad",Files.readString(path));assertTrue(service.pending());
    }
    @Test void resettingCharactersCannotResetAudioOrGlobalCurrency() {
        SaveSlots saves=SaveSlots.memory();AudioSettingsStore store=AudioSettingsStore.memory();
        Main main=new Main(saves,store,new TestAudioBackend());
        main.profileAt(0).chooseProfession(Profession.GUERREIRO);
        AudioSettings next=new AudioSettings(.55f,.12f,true);
        main.audioSettings().change(next);assertTrue(main.audioSettings().save());assertTrue(main.deleteSave(0));
        assertEquals(next,new Main(saves,store,new TestAudioBackend()).audioSettings().value());
        assertFalse(main.profileAt(0).profile().professionChosen());
    }
}

