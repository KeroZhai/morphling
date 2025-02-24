package io.github.kerozhai.morphling.mapper;

import io.github.kerozhai.morphling.annotation.Mapping.ValueStrategy;

/**
 * Used to map the source object to the target object.
 */
public interface Mapper<Source, Target> {

    /**
     * Generate a new target object by mapping the source object according to the
     * value strategy and groups.
     *
     * @param source        The source object to be mapped.
     * @param valueStrategy The value strategy to be used.
     * @param groups        The groups to be used.
     * @see ValueStrategy
     * @return The new target object.
     */
    Target map(Source source, ValueStrategy valueStrategy, Class<?>... groups);

    /**
     * Map the source object to the target object according to the value strategy
     * and groups.
     *
     * @param source        The source object to be mapped.
     * @param target        The target object to be mapped to.
     * @param valueStrategy The value strategy to be used.
     * @param groups        The groups to be used.
     *  @see ValueStrategy
     */
    void map(Source source, Target target, ValueStrategy valueStrategy, Class<?>... groups);

    /**
     * Generate a new target object by mapping the source object according to the
     * groups.
     *
     * @param source The source object to be mapped.
     * @param groups The groups to be used.
     * @return
     */
    Target map(Source source, Class<?>... groups);

    /**
     * Map the source object to the target object according to the groups.
     *
     * @param source The source object to be mapped.
     * @param target The target object to be mapped to.
     * @param groups The groups to be used.
     */
    void map(Source source, Target target, Class<?>... groups);

    /**
     * Generate a new target object by mapping the source object.
     *
     * @param source The source object to be mapped.
     * @return The new target object.
     */
    Target map(Source source);

    /**
     * Map the source object to the target object.
     *
     * @param source The source object to be mapped.
     * @param target The target object to be mapped to.
     */
    void map(Source source, Target target);

}
