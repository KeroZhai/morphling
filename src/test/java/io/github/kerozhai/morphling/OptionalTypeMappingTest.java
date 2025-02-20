package io.github.kerozhai.morphling;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.github.kerozhai.morphling.annotation.Mapping;
import io.github.kerozhai.morphling.mapper.Mapper;
import io.github.kerozhai.morphling.mapper.MapperFactory;
import lombok.Data;

public class OptionalTypeMappingTest {

    private MapperFactory mapperFactory = MapperFactory.defaultMapperFactory();

    @Data
    public static class Foo {

        private Optional<String> name;

        private Optional<Integer> age;

        @Mapping(alias = "isMarried")
        private boolean married;

    }

    @Data
    public static class Bar {

        private Optional<String> name;

        private Integer age;

        @Mapping(alias = "married")
        private Optional<Boolean> isMarried;

    }

    @Test
    public void test() {
        Foo foo = new Foo();
        foo.setName(Optional.of("John"));
        foo.setAge(Optional.of(30));
        foo.setMarried(true);

        Mapper<Foo, Bar> mapper = mapperFactory.getMapperFor(Foo.class, Bar.class);
        Bar bar = mapper.map(foo);

        assertEquals(Optional.of("John"), bar.getName());
        assertEquals(30, bar.getAge());
        assertEquals(Optional.of(Boolean.TRUE), bar.getIsMarried());

        Mapper<Bar, Foo> mapper2 = mapperFactory.getMapperFor(Bar.class, Foo.class);
        Foo foo2 = mapper2.map(bar);

        assertEquals(Optional.of("John"), foo2.getName());
        assertEquals(Optional.of(30), foo2.getAge());
        assertEquals(true, foo2.isMarried());
    }

}
