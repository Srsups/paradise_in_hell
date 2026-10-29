package br.com.srsups.paradiseinhell;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/** UTF-8 profile outside the installation; replace only after a complete write. */
public final class FileProfileStore implements ProfileStore {
    private final Path file, backup;
    public FileProfileStore(Path file) {
        this.file = file.toAbsolutePath(); backup = this.file.resolveSibling(this.file.getFileName() + ".bak");
    }
    public static Path defaultPath() {
        String local = System.getenv("LOCALAPPDATA");
        Path base = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")
            ? local == null || local.isBlank() ? Path.of(System.getProperty("user.home"), "AppData", "Local") : Path.of(local)
            : Path.of(System.getProperty("user.home"), ".local", "share");
        return base.resolve("ParadiseInHell").resolve("profile.json");
    }
    @Override public CampaignProfile load() throws IOException {
        if (!Files.exists(file) && !Files.exists(backup)) return new CampaignProfile();
        try { return read(file); }
        catch (IOException primaryFailure) {
            if (primaryFailure.getCause() instanceof CampaignProfile.UnsupportedVersion) throw primaryFailure;
            try { return read(backup); }
            catch (IOException backupFailure) {
                primaryFailure.addSuppressed(backupFailure); throw primaryFailure;
            }
        }
    }
    private CampaignProfile read(Path path) throws IOException {
        try { return CampaignProfile.fromJson(Files.readString(path, StandardCharsets.UTF_8)); }
        catch (RuntimeException invalid) { throw new IOException("Invalid profile: " + path.getFileName(), invalid); }
    }
    @Override public void save(CampaignProfile profile) throws IOException {
        if(profile.checkpoint()==null&&profile.lastSettledSequence()>0){saveTerminal(profile);return;}
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
    @Override public void saveTerminal(CampaignProfile profile)throws IOException {
        if(profile.checkpoint()!=null)throw new IllegalArgumentException("A terminal profile cannot contain an active checkpoint");
        rejectFuture(file);rejectFuture(backup);
        Files.createDirectories(file.getParent());
        byte[] data=profile.toJson().getBytes(StandardCharsets.UTF_8);
        Path temporary=Files.createTempFile(file.getParent(),"profile-terminal-",".tmp");
        Path temporaryBackup=Files.createTempFile(file.getParent(),"profile-terminal-backup-",".tmp");
        byte[] previousBackup=null;boolean removed=false,committed=false;
        try {
            Files.write(temporary,data);Files.write(temporaryBackup,data);
            if(Files.exists(backup)){previousBackup=Files.readAllBytes(backup);removed=Files.deleteIfExists(backup);}
            replace(temporary,file);committed=true;
            // Mirror the terminal receipt/ack. Never leave an active attempt as the fallback.
            replace(temporaryBackup,backup);
        }catch(IOException failure){
            if(!committed&&removed)try{Files.write(backup,previousBackup);}catch(IOException rollback){failure.addSuppressed(rollback);}
            throw failure;
        }finally{Files.deleteIfExists(temporary);Files.deleteIfExists(temporaryBackup);}
    }
    private static void replace(Path temporary,Path destination)throws IOException {
        try{Files.move(temporary,destination,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
        catch(AtomicMoveNotSupportedException unsupported){Files.move(temporary,destination,StandardCopyOption.REPLACE_EXISTING);}
    }
    private void rejectFuture(Path path)throws IOException {
        if(!Files.exists(path))return;
        try{read(path);}catch(IOException failure){if(failure.getCause() instanceof CampaignProfile.UnsupportedVersion)throw failure;}
    }
    @Override public void reset(CampaignProfile empty) throws IOException {
        if (empty.professionChosen()) throw new IllegalArgumentException("Reset requires an empty character");
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "profile-reset-", ".tmp");
        byte[] previousBackup = null;
        boolean backupRemoved = false;
        try {
            Files.writeString(temporary, empty.toJson(), StandardCharsets.UTF_8);
            // Invalidate the old fallback before committing: a deleted character must never revive from it.
            if (Files.exists(backup)) {
                previousBackup = Files.readAllBytes(backup);
                backupRemoved = Files.deleteIfExists(backup);
            }
            try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException unsupported) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException failure) {
            if (backupRemoved) {
                try { Files.write(backup, previousBackup); }
                catch (IOException rollbackFailure) { failure.addSuppressed(rollbackFailure); }
            }
            throw failure;
        } finally { Files.deleteIfExists(temporary); }
    }

}
