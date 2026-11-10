package br.com.srsups.paradiseinhell;

import java.io.IOException;
import java.util.List;

/** An explicit export never changes or retries game progression. */
public final class ReportExportService {
    private final ReportExportStore store;
    private String directory="",error="";
    public ReportExportService(ReportExportStore store){this.store=java.util.Objects.requireNonNull(store);}
    public String directory(){return directory;}
    public String error(){return error;}
    public boolean export(List<RunReport> history,RunReport opened) {
        RunReportExport bundle=RunReportExport.create(history,opened);
        try {
            String saved=store.save(bundle);
            if(saved==null||saved.isBlank())throw new IOException("Export store did not confirm a directory");
            directory=saved;error="";return true;
        } catch(IOException | SecurityException failure) {
            error="Não foi possível exportar. Seus relatórios foram preservados; tente novamente.";return false;
        }
    }
}
