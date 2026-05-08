package dev.maire.nourished.data;

import java.util.ArrayList;
import java.util.List;

public final class DatapackDiagnostics {

    private static final DatapackDiagnostics INSTANCE = new DatapackDiagnostics();

    private final List<DatapackDiagnostic> diagnostics = new ArrayList<>();

    private DatapackDiagnostics() {}

    public static DatapackDiagnostics getInstance() {
        return INSTANCE;
    }

    public synchronized void record(DatapackDiagnostic diagnostic) {
        diagnostics.add(diagnostic);
    }

    public synchronized List<DatapackDiagnostic> getAll() {
        return List.copyOf(diagnostics);
    }

    public synchronized List<DatapackDiagnostic> getErrors() {
        return diagnostics.stream()
                .filter(d -> d.severity() == DatapackDiagnostic.Severity.ERROR)
                .toList();
    }

    public synchronized List<DatapackDiagnostic> getWarnings() {
        return diagnostics.stream()
                .filter(d -> d.severity() == DatapackDiagnostic.Severity.WARN)
                .toList();
    }

    public synchronized void clear() {
        diagnostics.clear();
    }

    public synchronized String getSummary() {
        int errors = 0;
        int warnings = 0;
        for (DatapackDiagnostic diagnostic : diagnostics) {
            if (diagnostic.severity() == DatapackDiagnostic.Severity.ERROR) {
                errors++;
            } else if (diagnostic.severity() == DatapackDiagnostic.Severity.WARN) {
                warnings++;
            }
        }
        return errors + " errors, " + warnings + " warnings across last reload";
    }
}
