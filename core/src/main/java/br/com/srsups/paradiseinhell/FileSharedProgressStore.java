package br.com.srsups.paradiseinhell;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/** Separate account file, with atomic replacement and recovery from the previous valid copy. */
public final class FileSharedProgressStore implements SharedProgressStore {
    private final Path file, backup;
    public FileSharedProgressStore(Path file) {
        this.file = file.toAbsolutePath(); backup = this.file.resolveSibling(this.file.getFileName() + ".bak");
    }
    public static Path defaultPath() {
        String local = System.getenv("LOCALAPPDATA");
        Path base = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")
            ? local == null || local.isBlank() ? Path.of(System.getProperty("user.home"), "AppData", "Local") : Path.of(local)
            : Path.of(System.getProperty("user.home"), ".local", "share");
        return base.resolve("ParadiseInHell").resolve("shared-progress.json");
    }
    @Override public SharedProgress load() throws IOException {
        if (!Files.exists(file) && !Files.exists(backup)) return new SharedProgress();
        try { return read(file); }
        catch (IOException primaryFailure) {
            if (primaryFailure.getCause() instanceof SharedProgress.UnsupportedVersion) throw primaryFailure;
            try { return read(backup); }
            catch (IOException backupFailure) {
                primaryFailure.addSuppressed(backupFailure); throw primaryFailure;
            }
        }
    }
    private SharedProgress read(Path path) throws IOException {
        try { return SharedProgress.fromJson(Files.readString(path, StandardCharsets.UTF_8)); }
        catch (RuntimeException invalid) { throw new IOException("Invalid profile: " + path.getFileName(), invalid); }
    }
    @Override public void save(SharedProgress profile) throws IOException {
        rejectFuture(file);rejectFuture(backup);
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "profile-", ".tmp");
        try {
            Files.writeString(temporary, profile.toJson(), StandardCharsets.UTF_8);
            // Never replace a good backup with a corrupt primary file.
            if (Files.exists(file)) {
                boolean valid = true;
                try { read(file); } catch (IOException invalid) { valid = false; }
                if (valid) Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
            }
            try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException unsupported) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
    @Override public void saveSettlement(SharedProgress profile)throws IOException {
        java.util.Objects.requireNonNull(profile);rejectFuture(file);rejectFuture(backup);
        Files.createDirectories(file.getParent());byte[] data=profile.toJson().getBytes(StandardCharsets.UTF_8);
        Path temporary=Files.createTempFile(file.getParent(),"shared-settlement-",".tmp");
        Path temporaryBackup=Files.createTempFile(file.getParent(),"shared-settlement-backup-",".tmp");
        byte[] previousBackup=null;boolean removed=false,committed=false;
        try{
            Files.write(temporary,data);Files.write(temporaryBackup,data);
            if(Files.exists(backup)){previousBackup=Files.readAllBytes(backup);removed=Files.deleteIfExists(backup);}
            replace(temporary,file);committed=true;replace(temporaryBackup,backup);
        }catch(IOException failure){
            if(!committed&&removed)try{Files.write(backup,previousBackup);}catch(IOException rollback){failure.addSuppressed(rollback);}
            throw failure;
        }finally{Files.deleteIfExists(temporary);Files.deleteIfExists(temporaryBackup);}
    }
    private static void replace(Path temporary,Path target)throws IOException {
        try{Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
        catch(AtomicMoveNotSupportedException unsupported){Files.move(temporary,target,StandardCopyOption.REPLACE_EXISTING);}
    }
    private void rejectFuture(Path path)throws IOException {
        if(!Files.exists(path))return;
        try{read(path);}catch(IOException failure){if(failure.getCause() instanceof SharedProgress.UnsupportedVersion)throw failure;}
    }
}

