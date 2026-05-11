package dev.maire.nourished.tooling.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class DatapackValidator {

    private DatapackValidator() {}

    public static List<DatapackDiagnostic> validate(JsonObject json, SchemaDefinition schema, String filePath) {
        List<DatapackDiagnostic> diagnostics = new ArrayList<>();
        Set<String> knownFields = schema.getFields().stream()
                .map(SchemaField::name)
                .collect(Collectors.toSet());

        for (SchemaField field : schema.getFields()) {
            if (!json.has(field.name())) {
                if (field.required()) {
                    diagnostics.add(new DatapackDiagnostic(
                            DatapackDiagnostic.Severity.ERROR,
                            filePath,
                            field.name(),
                            "Missing required field"
                    ));
                }
                continue;
            }

            JsonElement element = json.get(field.name());
            if (!field.type().validate(element)) {
                diagnostics.add(new DatapackDiagnostic(
                        DatapackDiagnostic.Severity.ERROR,
                        filePath,
                        field.name(),
                        "Wrong field type, expected " + field.type().name()
                ));
            }
        }

        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            if (!knownFields.contains(entry.getKey())) {
                diagnostics.add(new DatapackDiagnostic(
                        DatapackDiagnostic.Severity.WARN,
                        filePath,
                        entry.getKey(),
                        "Unknown field for schema type '" + schema.getTypeName() + "'"
                ));
            }
        }

        if (json.has(DatapackSchema.KEY_SCHEMA_VERSION) && json.get(DatapackSchema.KEY_SCHEMA_VERSION).isJsonPrimitive() && json.get(DatapackSchema.KEY_SCHEMA_VERSION).getAsJsonPrimitive().isNumber()) {
            int version = json.get(DatapackSchema.KEY_SCHEMA_VERSION).getAsInt();
            if (version != schema.getVersion()) {
                diagnostics.add(new DatapackDiagnostic(
                        DatapackDiagnostic.Severity.WARN,
                        filePath,
                        DatapackSchema.KEY_SCHEMA_VERSION,
                        "Schema version mismatch, expected " + schema.getVersion() + " but found " + version
                ));
            }
        } else {
            diagnostics.add(new DatapackDiagnostic(
                    DatapackDiagnostic.Severity.WARN,
                    filePath,
                    DatapackSchema.KEY_SCHEMA_VERSION,
                    "Schema version missing or invalid, expected " + schema.getVersion()
            ));
        }

        return diagnostics;
    }
}
