package br.com.srsups.paradiseinhell;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Atomic preference writes and a validated backup, separate from every character and shared currency. */
public final class FileVideoSettingsStore implements VideoSettingsStore {
    private final Path file,backup;
    public FileVideoSettingsStore(Path file) {
        this.file=file.toAbsolutePath();backup=this.file.resolveSibling(this.file.getFileName()+".bak");
    }
    public VideoSettings load() throws IOException {
        if(!Files.exists(file) && !Files.exists(backup))return VideoSettings.DEFAULT;
        try{return read(file);}
        catch(IOException failure) {
            if(failure.getCause() instanceof VideoSettings.UnsupportedVersion)throw failure;
            try{return read(backup);}catch(IOException other){failure.addSuppressed(other);throw failure;}
        }
    }
    private VideoSettings read(Path path) throws IOException {
        try{return VideoSettings.fromJson(Files.readString(path,StandardCharsets.UTF_8));}
        catch(RuntimeException invalid){throw new IOException("Invalid video preferences",invalid);}
    }
    public void save(VideoSettings value) throws IOException {
        java.util.Objects.requireNonNull(value);rejectFuture(file);rejectFuture(backup);
        Files.createDirectories(file.getParent());Path temporary=Files.createTempFile(file.getParent(),"video-settings-",".tmp");
        try {
            Files.writeString(temporary,value.toJson(),StandardCharsets.UTF_8);
            if(Files.exists(file)) {
                boolean valid=true;try{read(file);}catch(IOException invalid){valid=false;}
                if(valid)Files.copy(file,backup,StandardCopyOption.REPLACE_EXISTING);
            }
            try{Files.move(temporary,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException unsupported){Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING);}
        } finally {Files.deleteIfExists(temporary);}
    }
    private void rejectFuture(Path path) throws IOException {
        if(!Files.exists(path))return;
        try{read(path);}
        catch(IOException failure){if(failure.getCause() instanceof VideoSettings.UnsupportedVersion)throw failure;}
    }
}
