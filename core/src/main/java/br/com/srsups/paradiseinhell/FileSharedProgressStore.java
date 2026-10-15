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
}

