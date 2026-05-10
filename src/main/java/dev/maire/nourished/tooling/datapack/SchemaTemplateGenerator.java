package dev.maire.nourished.tooling.datapack;

import dev.maire.nourished.data.SchemaDefinition;
import dev.maire.nourished.data.SchemaField;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public final class SchemaTemplateGenerator {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private SchemaTemplateGenerator() {}

    public static String generate(SchemaDefinition schema) {
        JsonObject root = new JsonObject();
        for (SchemaField field : schema.getFields()) {
            String fieldName = field.name();
            root.addProperty("_comment_" + fieldName, describeField(field));
            addSampleValue(root, field);
        }
        return GSON.toJson(root);
    }

    private static String describeField(SchemaField field) {
        String required = field.required() ? "required" : "optional";
        if (field.defaultValue() != null) {
            return required + " " + field.type().name() + ", default=" + field.defaultValue();
        }
        return required + " " + field.type().name();
    }

    private static void addSampleValue(JsonObject root, SchemaField field) {
        Object defaultValue = field.defaultValue();
        String name = field.name();
        if (defaultValue != null) {
            addByRuntimeType(root, name, defaultValue);
            return;
        }

        switch (field.type()) {
            case STRING -> root.addProperty(name, "example_value");
            case FLOAT, DOUBLE -> root.addProperty(name, 0.5f);
            case INT -> root.addProperty(name, 0);
            case BOOLEAN -> root.addProperty(name, false);
            case OBJECT -> root.add(name, new JsonObject());
            case ARRAY -> root.add(name, new JsonArray());
            case RESOURCE_LOCATION -> root.addProperty(name, "minecraft:example");
        }
    }

    private static void addByRuntimeType(JsonObject root, String key, Object value) {
        if (value instanceof String stringValue) {
            root.addProperty(key, stringValue);
        } else if (value instanceof Integer intValue) {
            root.addProperty(key, intValue);
        } else if (value instanceof Long longValue) {
            root.addProperty(key, longValue);
        } else if (value instanceof Float floatValue) {
            root.addProperty(key, floatValue);
        } else if (value instanceof Double doubleValue) {
            root.addProperty(key, doubleValue);
        } else if (value instanceof Boolean boolValue) {
            root.addProperty(key, boolValue);
        } else {
            root.addProperty(key, String.valueOf(value));
        }
    }
}
