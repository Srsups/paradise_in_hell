package br.com.srsups.paradiseinhell;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Complete temporary writes and a valid backup; never touches character/global progress files. */
public final class FileAudioSettingsStore implements AudioSettingsStore {
    private final Path file, backup;
    public FileAudioSettingsStore(Path file) {
        this.file = file.toAbsolutePath();
        backup = this.file.resolveSibling(this.file.getFileName() + ".bak");
    }
    public AudioSettings load() throws IOException {
        if (!Files.exists(file) && !Files.exists(backup)) return AudioSettings.DEFAULT;
        try { return read(file); }
        catch (IOException failure) {
            if(failure.getCause() instanceof AudioSettings.UnsupportedVersion)throw failure;
            try { return read(backup); }
            catch (IOException other) { failure.addSuppressed(other); throw failure; }
        }
    }
    private AudioSettings read(Path path) throws IOException {
        try { return AudioSettings.fromJson(Files.readString(path,StandardCharsets.UTF_8)); }
        catch (RuntimeException invalid) { throw new IOException("Invalid audio preferences",invalid); }
    }
    public void save(AudioSettings value) throws IOException {
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(),"audio-settings-",".tmp");
        try {
            Files.writeString(temporary,value.toJson(),StandardCharsets.UTF_8);
            if (Files.exists(file)) {
                boolean valid = true;
                try { read(file); } catch (IOException invalid) { valid = false; }
                if (valid) Files.copy(file,backup,StandardCopyOption.REPLACE_EXISTING);
            }
            try { Files.move(temporary,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException unsupported) { Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
}

