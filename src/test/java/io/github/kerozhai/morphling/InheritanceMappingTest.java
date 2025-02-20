package io.github.kerozhai.morphling;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.github.kerozhai.morphling.mapper.Mapper;
import io.github.kerozhai.morphling.mapper.MapperFactory;
import lombok.Data;
import lombok.EqualsAndHashCode;

public class InheritanceMappingTest {

    private MapperFactory mapperFactory = MapperFactory.defaultMapperFactory();

    @Data
    public static class SourceParent {
        private int id = 1;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Source extends SourceParent {
        private String name = "source";
    }

    @Data
    public static class TargetParent {
        private int id;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Target extends TargetParent {
        private String name;
    }

    @Test
    public void test() {
        Source source = new Source();
        Mapper<Source, Target> mapper = mapperFactory.getMapperFor(Source.class, Target.class);
        Target target = mapper.map(source);

        assertEquals(source.getId(), target.getId());
        assertEquals(source.getName(), target.getName());
    }
}
