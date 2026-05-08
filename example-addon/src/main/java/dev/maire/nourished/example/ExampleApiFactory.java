package dev.maire.nourished.example;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Map;

/**
 * Reflection helper for constructing Nourished API definitions while API builders are still placeholder-only.
 */
final class ExampleApiFactory {

    private ExampleApiFactory() {}

    static <T> T instantiate(
            Class<T> targetClass,
            String builderClassName,
            Class<?>[] builderCtorArgTypes,
            Object[] builderCtorArgs,
            Map<String, Object> builderFieldValues
    ) {
        try {
            Class<?> builderClass = Class.forName(builderClassName);
            Constructor<?> builderCtor = builderClass.getDeclaredConstructor(builderCtorArgTypes);
            builderCtor.setAccessible(true);
            Object builder = builderCtor.newInstance(builderCtorArgs);

            for (Map.Entry<String, Object> entry : builderFieldValues.entrySet()) {
                Field field = builderClass.getDeclaredField(entry.getKey());
                field.setAccessible(true);
                field.set(builder, entry.getValue());
            }

            Constructor<T> targetCtor = targetClass.getDeclaredConstructor(builderClass);
            targetCtor.setAccessible(true);
            return targetCtor.newInstance(builder);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to construct API object for " + targetClass.getSimpleName(), e);
        }
    }
}
