package io.github.kerozhai.morphling.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class StringUtils {

    public String classNameToPascalCase(String className) {
        String[] parts = className.split("\\.");
        StringBuilder builder = new StringBuilder();

        for (String part : parts) {
            builder.append(capitalize(part));
        }

        return builder.toString();
    }

    public String capitalize(String string) {
        return string.substring(0, 1).toUpperCase() + string.substring(1);
    }
}
