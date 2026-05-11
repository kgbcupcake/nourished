package dev.maire.nourished.tooling.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.resources.ResourceLocation;

public enum SchemaType {
    STRING {
        @Override
        public boolean validate(JsonElement element) {
            return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString();
        }
    },
    FLOAT {
        @Override
        public boolean validate(JsonElement element) {
            return isNumber(element);
        }
    },
    DOUBLE {
        @Override
        public boolean validate(JsonElement element) {
            return isNumber(element);
        }
    },
    INT {
        @Override
        public boolean validate(JsonElement element) {
            if (!isNumber(element)) {
                return false;
            }
            double number = element.getAsDouble();
            return Math.floor(number) == number;
        }
    },
    BOOLEAN {
        @Override
        public boolean validate(JsonElement element) {
            return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean();
        }
    },
    OBJECT {
        @Override
        public boolean validate(JsonElement element) {
            return element != null && element.isJsonObject();
        }
    },
    ARRAY {
        @Override
        public boolean validate(JsonElement element) {
            return element != null && element.isJsonArray();
        }
    },
    RESOURCE_LOCATION {
        @Override
        public boolean validate(JsonElement element) {
            if (!STRING.validate(element)) {
                return false;
            }
            try {
                ResourceLocation.parse(element.getAsString());
                return true;
            } catch (Exception ignored) {
                return false;
            }
        }
    };

    public abstract boolean validate(JsonElement element);

    private static boolean isNumber(JsonElement element) {
        if (element == null || !element.isJsonPrimitive()) {
            return false;
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        return primitive.isNumber();
    }
}
