package io.github.kerozhai.morphling.mapper;

import io.github.kerozhai.morphling.annotation.Mapping.ValueStrategy;

public interface Mapper<Source, Target> {

    Target map(Source source, ValueStrategy valueStrategy, Class<?>... ignoreGroups);

    void map(Source source, Target target, ValueStrategy valueStrategy, Class<?>... ignoreGroups);

    Target map(Source source, Class<?>... ignoreGroups);

    void map(Source source, Target target, Class<?>... ignoreGroups);

    Target map(Source source);

    void map(Source source, Target target);

}
