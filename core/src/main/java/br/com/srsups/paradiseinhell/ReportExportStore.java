package br.com.srsups.paradiseinhell;

import java.io.IOException;

/** Storage only: no dependency on profiles, settlement or network access. */
public interface ReportExportStore {
    /** Return the directory only after all three files have committed. */
    String save(RunReportExport export)throws IOException;
    static ReportExportStore memory(){return export->"memória/exports/run-history-"+export.id();}
}
