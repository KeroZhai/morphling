package com.keroz.morphling;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

import com.keroz.morphling.mapper.Mapper;
import com.keroz.morphling.mapper.MapperFactory;

import lombok.Data;

public class ArrayTypeMappingTest {

    private static MapperFactory mapperFactory = MapperFactory.defaultMapperFactory();

    @Data
    public static class Source1 {
        private int[] intArray = new int[] { 1, 2, 3 };
    }

    @Data
    public static class Target1 {
        private int[] intArray;
    }

    @Test
    public void test1() {
        Source1 source = new Source1();
        Mapper<Source1, Target1> mapper = mapperFactory.getMapperFor(Source1.class, Target1.class);
        Target1 target = mapper.map(source);

        assertArrayEquals(source.getIntArray(), target.getIntArray());
    }

    @Data
    public static class Foo {
        private String name = "foo";
    }

    @Data
    public static class Bar {
        private String name;
    }

    @Data
    public static class Source2 {
        private Foo[] objectArray = new Foo[] {
                new Foo()
        };
    }

    @Data
    public static class Target2 {
        private Bar[] objectArray;
    }

    @Test
    public void test2() {
        Source2 source = new Source2();
        Mapper<Source2, Target2> mapper = mapperFactory.getMapperFor(Source2.class, Target2.class);
        Target2 target = mapper.map(source);

        System.out.println(target);
    }

}
