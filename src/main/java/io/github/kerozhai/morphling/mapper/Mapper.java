package io.github.kerozhai.morphling.mapper;

public interface Mapper<Source, Target> {

    Target map(Source source, Class<?>... ignoreGroups);

    void map(Source source, Target target, Class<?>... ignoreGroups);

    Target map(Source source);

    void map(Source source, Target target);

}
