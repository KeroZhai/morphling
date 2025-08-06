package io.github.kerozhai.morphling.mapper;

import io.github.kerozhai.morphling.annotation.Mapping.ValueStrategy;

/**
 * A generic mapper that can map any source object to any target object. It uses
 * the {@link MapperFactory} to get the appropriate {@link Mapper} for the
 * source and target classes.
 * <p>
 * The generic mapper is useful when you don't know the exact source and target
 * classes at compile time. For example, when you are writing a generic method
 * that can map any object to any other object or when the concrete source and
 * target classes are determined at runtime.
 */
@SuppressWarnings("unchecked")
public class GenericMapper {

    private final MapperFactory mapperFactory;

    public GenericMapper(MapperFactory mapperFactory) {
        this.mapperFactory = mapperFactory;
    }

    /**
     * Generate a new target object by mapping the source object according to the
     * value strategy and groups.
     *
     * @param source        The source object to be mapped.
     * @param targetClass   The target class to be mapped to.
     * @param valueStrategy The value strategy to be used.
     * @param groups        The groups to be used.
     * @see ValueStrategy
     * @return The new target object.
     */
    public <S, T> T map(S source, Class<T> targetClass, ValueStrategy valueStrategy, Class<?>... groups) {
        return ((Mapper<S, T>) mapperFactory.getMapperFor(source.getClass(), targetClass)).map(source, valueStrategy,
                groups);
    }

    /**
     * Map the source object to the target object according to the value strategy
     * and groups.
     *
     * @param <S>           The type of the source object.
     * @param <T>           The type of the target object.
     * @param source        The source object to be mapped.
     * @param target        The target object to be mapped to.
     * @param valueStrategy The value strategy to be used.
     * @param groups        The groups to be used.
     */
    public <S, T> void map(S source, T target, ValueStrategy valueStrategy, Class<?>... groups) {
        ((Mapper<S, T>) mapperFactory.getMapperFor(source.getClass(), target.getClass())).map(source,
                target, valueStrategy, groups);
    }

    /**
     * Generate a new target object by mapping the source object according to the
     * groups.
     *
     * @param source      The source object to be mapped.
     * @param targetClass The target class to be mapped to.
     * @param groups      The groups to be used.
     * @return The new target object.
     */
    public <S, T> T map(S source, Class<T> targetClass, Class<?>... groups) {
        return ((Mapper<S, T>) mapperFactory.getMapperFor(source.getClass(), targetClass)).map(source, groups);
    }

    /**
     * Map the source object to the target object according to the groups.
     *
     * @param source The source object to be mapped.
     * @param target The target object to be mapped to.
     * @param groups The groups to be used.
     */
    public <S, T> void map(S source, T target, Class<?>... groups) {
        ((Mapper<S, T>) mapperFactory.getMapperFor(source.getClass(), target.getClass())).map(source, target, groups);
    }

}
