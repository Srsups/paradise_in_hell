package br.com.srsups.paradiseinhell;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Stage files in a private temporary directory; publish a complete, uniquely named bundle. */
public final class FileReportExportStore implements ReportExportStore {
    @FunctionalInterface interface Writer {void write(Path file,String content)throws IOException;}
    private final Path base;
    private final Writer writer;
    public FileReportExportStore(Path base){this(base,FileReportExportStore::write);}
    FileReportExportStore(Path base,Writer writer){
        this.base=java.util.Objects.requireNonNull(base).toAbsolutePath().normalize();this.writer=java.util.Objects.requireNonNull(writer);
    }
    @Override public String save(RunReportExport export)throws IOException {
        java.util.Objects.requireNonNull(export);Files.createDirectories(base);
        Path destination=base.resolve("run-history-"+export.id());
        if(Files.exists(destination))throw new IOException("Export directory already exists");
        Path temporary=Files.createTempDirectory(base,".run-history-");boolean committed=false;
        try {
            writer.write(temporary.resolve(RunReportExport.JSON_FILE),export.json());
            writer.write(temporary.resolve(RunReportExport.ATTEMPTS_FILE),export.attemptsCsv());
            writer.write(temporary.resolve(RunReportExport.ROOMS_FILE),export.roomsCsv());
            try{Files.move(temporary,destination,java.nio.file.StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException unsupported){Files.move(temporary,destination);}
            committed=true;return destination.toString();
        } finally {
            if(!committed) {
                // These are the only files created by this operation; never recursively delete user content.
                IOException cleanup=null;
                for(String name:new String[]{RunReportExport.JSON_FILE,RunReportExport.ATTEMPTS_FILE,RunReportExport.ROOMS_FILE})
                    try{Files.deleteIfExists(temporary.resolve(name));}catch(IOException failure){cleanup=failure;}
                try{Files.deleteIfExists(temporary);}catch(IOException failure){cleanup=failure;}
                if(cleanup!=null)throw cleanup;
            }
        }
    }
    private static void write(Path file,String content)throws IOException {
        Files.writeString(file,content,StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE);
    }
}
