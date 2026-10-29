package br.com.srsups.paradiseinhell;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/** Atomic HUD preferences with a validated backup, independent of character and video saves. */
public final class FileHudSettingsStore implements HudSettingsStore {
    private final Path file,backup;
    public FileHudSettingsStore(Path file) {
        this.file=Objects.requireNonNull(file).toAbsolutePath();
        backup=this.file.resolveSibling(this.file.getFileName()+".bak");
    }
    public HudSettings load() throws IOException {
        if(!Files.exists(file)&&!Files.exists(backup))return HudSettings.DEFAULT;
        try{return read(file);}
        catch(IOException failure) {
            if(failure.getCause() instanceof HudSettings.UnsupportedVersion)throw failure;
            try{return read(backup);}
            catch(IOException other){failure.addSuppressed(other);throw failure;}
        }
    }
    private HudSettings read(Path path) throws IOException {
        try{return HudSettings.fromJson(Files.readString(path,StandardCharsets.UTF_8));}
        catch(RuntimeException invalid){throw new IOException("Preferência de tamanho do HUD inválida.",invalid);}
    }
    public void save(HudSettings value) throws IOException {
        Objects.requireNonNull(value);
        rejectFuture(file);rejectFuture(backup);
        Files.createDirectories(file.getParent());
        Path temporary=Files.createTempFile(file.getParent(),"hud-settings-",".tmp");
        try {
            Files.writeString(temporary,value.toJson(),StandardCharsets.UTF_8);
            if(Files.exists(file)) {
                boolean valid=true;
                try{read(file);}catch(IOException invalid){valid=false;}
                if(valid)Files.copy(file,backup,StandardCopyOption.REPLACE_EXISTING);
            }
            try{Files.move(temporary,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException unsupported){Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING);}
        } finally {Files.deleteIfExists(temporary);}
    }
    private void rejectFuture(Path path) throws IOException {
        if(!Files.exists(path))return;
        try{read(path);}
        catch(IOException failure) {
            if(failure.getCause() instanceof HudSettings.UnsupportedVersion)throw failure;
        }
    }
}
