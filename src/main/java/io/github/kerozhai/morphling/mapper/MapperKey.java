package io.github.kerozhai.morphling.mapper;

import java.lang.reflect.Type;
import java.util.Objects;

public class MapperKey {

    private Type sourceType;

    private Type targetType;

    public static MapperKey of(Type sourceType, Type targetType) {
        MapperKey key = new MapperKey();

        key.sourceType = sourceType;
        key.targetType = targetType;

        return key;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        MapperKey other = (MapperKey) obj;

        return Objects.equals(sourceType, other.sourceType) && Objects.equals(targetType, other.targetType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceType, targetType);
    }

}
