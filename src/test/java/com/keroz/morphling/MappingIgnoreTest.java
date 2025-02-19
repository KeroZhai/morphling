package com.keroz.morphling;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.keroz.morphling.annotation.MappingIgnore;
import com.keroz.morphling.mapper.Mapper;
import com.keroz.morphling.mapper.MapperFactory;

import lombok.Data;

public class MappingIgnoreTest {

    private MapperFactory mapperFactory = MapperFactory.defaultMapperFactory();

    @Data
    public static class Source {
        private String name = "source";
    }

    @Data
    public static class Target {

        @MappingIgnore
        private String name;
    }

    @Test
    public void testIgnore() {
        Source source = new Source();
        Target target = new Target();
        Mapper<Source, Target> mapper = mapperFactory.getMapperFor(Source.class, Target.class);

        mapper.map(source, target);

        assertNull(target.getName());
    }

}
