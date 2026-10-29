package br.com.srsups.paradiseinhell;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class VideoSettingsTest {
    @TempDir Path directory;
    private static final VideoSettings HD=new VideoSettings(1280,720,false,false);
    private static final class Store implements VideoSettingsStore {
        VideoSettings saved=VideoSettings.DEFAULT;boolean fail;int writes;
        public VideoSettings load(){return saved;}
        public void save(VideoSettings value) throws IOException {writes++;if(fail)throw new IOException("locked");saved=value;}
    }
    private static final class Backend implements VideoBackend {
        VideoSettings live=VideoSettings.DEFAULT;int calls;boolean supports=true,ignore;
        Predicate<VideoSettings> reject=ignored->false;Runnable callback=()->{};
        public VideoSettings current(){return live;}
        public boolean supportsFullscreen(){return supports;}
        public boolean apply(VideoSettings value) {
            calls++;callback.run();if(reject.test(value))return false;if(!ignore)live=value;return true;
        }
    }
    @Test void settingsRoundTripManualDimensionsAndRejectInvalidTypesAndFutureVersions() {
        VideoSettings manual=new VideoSettings(1111,777,true,false);
        assertEquals(manual,VideoSettings.fromJson(manual.toJson()));assertEquals(5,VideoSettings.PRESETS.size());
        assertEquals(HD,VideoSettings.DEFAULT.withSize(new VideoSettings.Size(1280,720)).withVsync(false));
        assertThrows(IllegalArgumentException.class,()->new VideoSettings(639,540,false,true));
        assertThrows(IllegalArgumentException.class,()->new VideoSettings(960,359,false,true));
        assertThrows(IllegalArgumentException.class,()->new VideoSettings(7681,4320,false,true));
        for(String invalid:new String[]{"null","[]","{}","{\"version\":1.0}",
            manual.toJson().replace("\"vsync\": false","\"vsync\": 7"),manual.toJson().replace("1111","\"1111\"")})
            assertThrows(RuntimeException.class,()->VideoSettings.fromJson(invalid));
        assertThrows(VideoSettings.UnsupportedVersion.class,()->VideoSettings.fromJson(manual.toJson().replace("\"version\": 2","\"version\": 3")));
    }
    @Test void previewDoesNotSaveAndCancelRestoresTheActualManuallyResizedWindow() {
        Store store=new Store();Backend backend=new Backend();VideoSettingsService service=new VideoSettingsService(store,backend);
        service.initialize();VideoSettings manual=new VideoSettings(1111,777,false,true);backend.live=manual;
        assertTrue(service.preview(HD.withFullscreen(true)));assertTrue(service.previewing());assertEquals(0,store.writes);
        assertFalse(service.preview(VideoSettings.DEFAULT));assertTrue(service.cancelPreview());
        assertEquals(manual,backend.live);assertEquals(VideoSettings.DEFAULT,store.saved);assertEquals(0,store.writes);
        assertFalse(service.previewing());assertTrue(service.cancelPreview());assertEquals(2,backend.calls);
    }
    @Test void onlyConfirmationPersistsAndNoOpEditsCannotWrite() {
        Store store=new Store();Backend backend=new Backend();VideoSettingsService service=new VideoSettingsService(store,backend);
        assertFalse(service.preview(VideoSettings.DEFAULT));assertFalse(service.confirm());assertEquals(0,store.writes);
        assertTrue(service.preview(HD));assertEquals(VideoSettings.DEFAULT,service.value());
        assertTrue(service.confirm());assertEquals(HD,service.value());assertEquals(HD,store.saved);
        assertEquals(1,store.writes);assertFalse(service.previewing());assertFalse(service.pendingSave());
        assertFalse(service.confirm());assertTrue(service.retrySave());assertEquals(1,store.writes);
    }
    @Test void timeoutUsesMonotonicWallTimeEvenWithNoSimulationUpdates() {
        Store store=new Store();Backend backend=new Backend();long[] nanos={Long.MAX_VALUE-5_000_000_000L};
        VideoSettingsService service=new VideoSettingsService(store,backend,()->nanos[0]);
        service.preview(HD);nanos[0]+=14_900_000_000L;service.tick();
        assertTrue(service.previewing());assertEquals(.1f,service.remainingSeconds(),.001f);
        nanos[0]+=100_000_000L;service.tick();assertFalse(service.previewing());assertEquals(VideoSettings.DEFAULT,backend.live);
        assertEquals(0,store.writes);
        service.preview(HD);nanos[0]+=16_000_000_000L;
        assertFalse(service.confirm());assertEquals(VideoSettings.DEFAULT,backend.live);assertEquals(0,store.writes);
    }
    @Test void failedPersistenceKeepsConfirmedImageAndCanBeRetriedOrUsedAsNextCancelBaseline() {
        Store store=new Store();Backend backend=new Backend();VideoSettingsService service=new VideoSettingsService(store,backend);
        store.fail=true;service.preview(HD);assertFalse(service.confirm());
        assertEquals(HD,backend.live);assertEquals(HD,service.value());assertTrue(service.pendingSave());assertFalse(service.previewing());
        assertEquals(VideoSettings.DEFAULT,store.saved);assertFalse(service.error().isEmpty());
        service.preview(HD.withFullscreen(true));service.cancelPreview();assertEquals(HD,backend.live);assertTrue(service.pendingSave());
        store.fail=false;assertTrue(service.retrySave());assertEquals(HD,store.saved);assertFalse(service.pendingSave());assertEquals("",service.error());
    }
    @Test void unsupportedAndUnacknowledgedNativeChangesCannotBeConfirmed() {
        Store store=new Store();Backend backend=new Backend();VideoSettingsService service=new VideoSettingsService(store,backend);
        backend.supports=false;assertFalse(service.preview(HD.withFullscreen(true)));assertEquals(0,backend.calls);
        backend.ignore=true;assertTrue(service.preview(HD));assertFalse(service.confirm());
        assertFalse(service.previewing());assertEquals(0,store.writes);assertEquals(VideoSettings.DEFAULT,backend.live);
    }
    @Test void failedRestoreUsesSafeWindowWithoutReplacingTheLastConfirmedPreference() {
        Store store=new Store();store.saved=HD;Backend backend=new Backend();VideoSettingsService service=new VideoSettingsService(store,backend);
        assertTrue(service.initialize());VideoSettings fullscreen=HD.withFullscreen(true);assertTrue(service.preview(fullscreen));
        backend.reject=HD::equals;assertTrue(service.cancelPreview());
        assertEquals(VideoSettings.DEFAULT,backend.live);assertEquals(HD,store.saved);assertEquals(HD,service.value());
        assertEquals(0,store.writes);assertTrue(service.error().contains("janela segura"));
    }
    @Test void rejectedApplyAndExceptionsRestoreAndNeverPersistPartialModes() {
        Store store=new Store();Backend backend=new Backend();VideoSettingsService service=new VideoSettingsService(store,backend);
        backend.reject=HD::equals;assertFalse(service.preview(HD));assertEquals(VideoSettings.DEFAULT,backend.live);assertEquals(0,store.writes);
        backend.reject=ignored->false;
        backend.callback=()->{if(backend.calls==3){backend.live=HD;throw new IllegalStateException("Native change failed halfway");}};
        assertFalse(service.preview(HD));assertEquals(VideoSettings.DEFAULT,backend.live);assertEquals(0,store.writes);
    }
    @Test void reentrantResizeCannotApplyTwiceOrCommitAndCancellationIsDeferredSafely() {
        Store store=new Store();Backend backend=new Backend();VideoSettingsService service=new VideoSettingsService(store,backend);
        backend.callback=()->{
            assertTrue(service.busy());assertTrue(service.previewing());
            assertFalse(service.preview(VideoSettings.DEFAULT));assertFalse(service.confirm());
            service.tick();assertFalse(service.cancelPreview());
        };
        // Only the first native call represents the resize callback. The restore has no new input.
        Runnable callback=backend.callback;backend.callback=()->{if(backend.calls==1)callback.run();};
        assertFalse(service.preview(HD));assertFalse(service.previewing());assertFalse(service.busy());
        assertEquals(VideoSettings.DEFAULT,backend.live);assertEquals(2,backend.calls);assertEquals(0,store.writes);
    }
    @Test void startupFallbackDoesNotOverwriteUnavailableSavedMode() {
        Store store=new Store();store.saved=HD.withFullscreen(true);Backend backend=new Backend();backend.reject=store.saved::equals;
        VideoSettingsService service=new VideoSettingsService(store,backend);assertFalse(service.initialize());
        assertEquals(VideoSettings.DEFAULT,backend.live);assertEquals(VideoSettings.DEFAULT,service.value());
        assertEquals(HD.withFullscreen(true),store.saved);assertEquals(0,store.writes);assertFalse(service.notice().isEmpty());
    }
    @Test void fileBackupRecoveryAndUnknownVersionPreserveOriginalFiles() throws Exception {
        Path path=directory.resolve("video-settings.json");FileVideoSettingsStore store=new FileVideoSettingsStore(path);
        assertEquals(VideoSettings.DEFAULT,store.load());store.save(VideoSettings.DEFAULT);store.save(HD);
        Files.writeString(path,"broken");assertEquals(VideoSettings.DEFAULT,store.load());store.save(HD);
        assertEquals(VideoSettings.DEFAULT,VideoSettings.fromJson(Files.readString(directory.resolve("video-settings.json.bak"))));
        String future=HD.toJson().replace("\"version\": 2","\"version\": 3");Files.writeString(path,future);
        VideoSettingsService service=new VideoSettingsService(store,VideoBackend.memory());
        service.preview(HD);assertFalse(service.confirm());assertEquals(HD,service.current());
        assertFalse(service.retrySave());assertEquals(future,Files.readString(path));assertTrue(service.pendingSave());
    }
    @Test void videoPreferencesAreIndependentOfCharactersAudioAndSharedProgress() {
        SaveSlots saves=SaveSlots.memory();VideoSettingsStore store=VideoSettingsStore.memory();AudioSettingsStore audio=AudioSettingsStore.memory();
        Main first=new Main(saves,audio,new TestAudioBackend(),store,VideoBackend.memory());
        first.profileAt(0).chooseProfession(Profession.GUERREIRO);first.video().preview(HD);assertTrue(first.video().confirm());
        assertTrue(first.deleteSave(0));Main second=new Main(saves,audio,new TestAudioBackend(),store,VideoBackend.memory());
        assertEquals(HD,second.video().value());assertEquals(AudioSettings.DEFAULT,second.audioSettings().value());
        assertFalse(second.profileAt(0).profile().professionChosen());assertEquals(0,second.shared().progress().souls());
    }
    @Test void threeModesRoundTripAndBorderlessRemainsDifferentFromExclusiveFullscreen() {
        for(VideoSettings.Mode mode:VideoSettings.Mode.values()) {
            VideoSettings value=new VideoSettings(1111,777,mode,false);
            assertEquals(value,VideoSettings.fromJson(value.toJson()));
            assertEquals(mode,value.withSize(new VideoSettings.Size(1280,720)).mode());
            assertEquals(mode,value.withVsync(true).mode());
            assertEquals(mode==VideoSettings.Mode.FULLSCREEN,value.fullscreen());
            assertEquals(1111,value.width());assertEquals(777,value.height());
            assertFalse(mode.label().isBlank());
        }
        assertEquals(VideoSettings.Mode.WINDOWED,new VideoSettings(960,540,false,true).mode());
        assertEquals(VideoSettings.Mode.FULLSCREEN,new VideoSettings(960,540,true,true).mode());
        assertEquals(VideoSettings.Mode.WINDOWED,HD.withMode(VideoSettings.Mode.BORDERLESS).withFullscreen(false).mode());
    }
    @Test void versionOnePreferencesMigrateWithoutLosingWindowDimensionsOrVsync() {
        for(boolean fullscreen:new boolean[]{false,true}) {
            String legacy="{\"version\":1,\"width\":1111,\"height\":777,\"fullscreen\":"+fullscreen+",\"vsync\":false}";
            VideoSettings migrated=VideoSettings.fromJson(legacy);
            assertEquals(new VideoSettings(1111,777,fullscreen,false),migrated);
            assertTrue(migrated.toJson().contains("\"version\": 2"));
            assertFalse(migrated.toJson().contains("\"fullscreen\""));
            assertEquals(migrated,VideoSettings.fromJson(migrated.toJson()));
        }
    }
    @Test void strictModeSchemaRejectsMissingDuplicatedUnknownOrWronglyTypedFields() {
        String valid=HD.withMode(VideoSettings.Mode.BORDERLESS).toJson();
        for(String invalid:new String[]{
            valid.replace("\"BORDERLESS\"","\"MAXIMIZED\""),
            valid.replace("\"BORDERLESS\"","true"),
            valid.replace("\"BORDERLESS\"","null"),
            valid.replace("\"version\": 2","\"version\": 2.0"),
            valid.replace("\"width\": 1280","\"width\": 1280, \"width\": 960"),
            valid.replace("\"mode\": \"BORDERLESS\"","\"mode\": \"BORDERLESS\", \"mode\": \"WINDOWED\""),
            valid.replace("\"mode\": \"BORDERLESS\"","\"fullscreen\": false"),
            valid.replace("\"height\": 720,",""),
            valid.replace("\"vsync\": false","\"vsync\": false, \"extra\": 3")})
            assertThrows(RuntimeException.class,()->VideoSettings.fromJson(invalid),invalid);
        assertThrows(VideoSettings.UnsupportedVersion.class,()->VideoSettings.fromJson("{\"version\":3,\"new-schema\":true}"));
    }
    @Test void legacyFileMigratesOnExplicitSaveAndFutureBackupIsProtectedFromDirectWrites() throws Exception {
        Path path=directory.resolve("video-settings.json"),backup=directory.resolve("video-settings.json.bak");
        String legacy="{\"version\":1,\"width\":1280,\"height\":720,\"fullscreen\":false,\"vsync\":false}";
        Files.writeString(path,legacy);FileVideoSettingsStore store=new FileVideoSettingsStore(path);
        assertEquals(HD,store.load());assertEquals(legacy,Files.readString(path));
        store.save(HD.withMode(VideoSettings.Mode.BORDERLESS));
        assertEquals(legacy,Files.readString(backup));assertEquals(HD.withMode(VideoSettings.Mode.BORDERLESS),store.load());
        String future="{\"version\":3,\"new-schema\":true}";Files.writeString(backup,future);
        String primary=Files.readString(path);
        assertThrows(IOException.class,()->store.save(VideoSettings.DEFAULT));
        assertEquals(primary,Files.readString(path));assertEquals(future,Files.readString(backup));
    }
    @Test void previewAdaptsToMonitorBeforeComparingOrConfirmingTheTarget() {
        Store store=new Store();
        VideoBackend backend=new VideoBackend() {
            VideoSettings value=VideoSettings.DEFAULT;
            public VideoSettings current(){return value;}
            public VideoSettings adapt(VideoSettings settings){return settings.withSize(new VideoSettings.Size(Math.min(settings.width(),1280),Math.min(settings.height(),720)));}
            public boolean apply(VideoSettings settings){value=settings;return true;}
        };
        VideoSettingsService service=new VideoSettingsService(store,backend);
        assertTrue(service.preview(new VideoSettings(1920,1080,VideoSettings.Mode.WINDOWED,false)));
        assertEquals(HD,service.current());assertEquals(VideoSettings.DEFAULT,service.value());
        assertTrue(service.notice().contains("ajustado"));
        assertTrue(service.confirm());assertEquals(HD,service.value());assertEquals(HD,store.saved);
        assertEquals(1,store.writes);
        assertFalse(service.preview(new VideoSettings(1920,1080,VideoSettings.Mode.WINDOWED,false)));
        assertEquals(1,store.writes);
    }
    @Test void startupFitsSavedWindowWithoutSilentlyRewritingThePreference() {
        Store store=new Store();store.saved=new VideoSettings(1920,1080,VideoSettings.Mode.WINDOWED,false);
        VideoBackend backend=new VideoBackend() {
            VideoSettings value=VideoSettings.DEFAULT;
            public VideoSettings current(){return value;}
            public VideoSettings adapt(VideoSettings settings){return settings.withSize(new VideoSettings.Size(1280,720));}
            public boolean apply(VideoSettings settings){value=settings;return true;}
        };
        VideoSettingsService service=new VideoSettingsService(store,backend);
        assertTrue(service.initialize());assertEquals(HD,service.current());assertEquals(HD,service.value());
        assertEquals(1920,store.saved.width());assertEquals(0,store.writes);assertFalse(service.pendingSave());
        assertTrue(service.notice().contains("ajustado"));
    }
    @Test void borderlessDoesNotRequireExclusiveDisplayModeSupportAndRollbackUsesOpaquePlacementSnapshot() {
        record Placement(VideoSettings settings,int x,int y) implements VideoBackend.Snapshot {}
        class Positioned implements VideoBackend {
            VideoSettings value=VideoSettings.DEFAULT;int x=-1700,y=90,restores;
            public VideoSettings current(){return value;}
            public boolean supportsFullscreen(){return false;}
            public boolean apply(VideoSettings settings){value=settings;x=-1920;y=0;return true;}
            public Snapshot snapshot(){return new Placement(value,x,y);}
            public boolean restore(Snapshot state){Placement saved=(Placement)state;value=saved.settings();x=saved.x();y=saved.y();restores++;return true;}
            public String monitorDescription(){return "Monitor esquerdo: 1920 × 1080";}
        }
        Store store=new Store();Positioned backend=new Positioned();long[] clock={0};
        VideoSettingsService service=new VideoSettingsService(store,backend,()->clock[0]);
        assertTrue(service.monitorDescription().contains("esquerdo"));
        assertTrue(service.preview(HD.withMode(VideoSettings.Mode.BORDERLESS)));
        assertFalse(service.current().fullscreen());assertEquals(-1920,backend.x);
        clock[0]=15_000_000_000L;service.tick();
        assertEquals(-1700,backend.x);assertEquals(90,backend.y);assertEquals(1,backend.restores);
        assertEquals(VideoSettings.DEFAULT,backend.value);assertEquals(0,store.writes);
        assertFalse(service.preview(HD.withMode(VideoSettings.Mode.FULLSCREEN)));assertEquals(1,backend.restores);
    }
    @Test void invalidAdaptationOrSnapshotFailureCannotMutateTheCurrentImageOrPreference() {
        Store store=new Store();int[] calls={0};
        VideoBackend invalid=new VideoBackend() {
            public VideoSettings current(){return VideoSettings.DEFAULT;}
            public VideoSettings adapt(VideoSettings value){return value.withMode(VideoSettings.Mode.FULLSCREEN);}
            public boolean apply(VideoSettings value){calls[0]++;return true;}
        };
        VideoSettingsService service=new VideoSettingsService(store,invalid);
        assertFalse(service.preview(HD));assertFalse(service.previewing());assertEquals(0,calls[0]);assertEquals(0,store.writes);
        VideoBackend failedSnapshot=new VideoBackend() {
            public VideoSettings current(){return VideoSettings.DEFAULT;}
            public Snapshot snapshot(){throw new IllegalStateException("Monitor removed while reading placement");}
            public boolean apply(VideoSettings value){calls[0]++;return true;}
        };
        service=new VideoSettingsService(store,failedSnapshot);
        assertFalse(service.preview(HD));assertFalse(service.busy());assertFalse(service.previewing());
        assertEquals(VideoSettings.DEFAULT,service.current());assertEquals(0,calls[0]);assertEquals(0,store.writes);
    }
    @Test void failedSafeWindowProviderNeverCrashesStartupOrRollbackAndCannotPersistAnUnconfirmedMode() {
        Store startupStore=new Store();startupStore.saved=HD;
        VideoBackend unavailable=new VideoBackend() {
            public VideoSettings current(){return VideoSettings.DEFAULT;}
            public boolean apply(VideoSettings settings){return false;}
            public VideoSettings safeDefaults(){throw new IllegalArgumentException("No usable area on the monitor");}
        };
        VideoSettingsService startup=new VideoSettingsService(startupStore,unavailable);
        assertFalse(startup.initialize());assertFalse(startup.busy());assertFalse(startup.error().isEmpty());
        assertEquals(HD,startupStore.saved);assertEquals(0,startupStore.writes);

        Store store=new Store();VideoSettings[] actual={VideoSettings.DEFAULT};
        VideoBackend disconnected=new VideoBackend() {
            public VideoSettings current(){return actual[0];}
            public boolean apply(VideoSettings settings){actual[0]=settings;return true;}
            public boolean restore(Snapshot snapshot){throw new IllegalStateException("Original monitor disappeared");}
            public VideoSettings safeDefaults(){throw new IllegalArgumentException("No usable area on the remaining monitor");}
        };
        VideoSettingsService service=new VideoSettingsService(store,disconnected);
        assertTrue(service.preview(HD));assertFalse(service.cancelPreview());
        assertFalse(service.previewing());assertFalse(service.busy());assertFalse(service.confirm());
        assertEquals(VideoSettings.DEFAULT,service.value());assertEquals(VideoSettings.DEFAULT,store.saved);
        assertEquals(0,store.writes);assertTrue(service.error().contains("Não foi possível restaurar"));
    }
}
