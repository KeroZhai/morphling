package io.github.kerozhai.morphling;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import io.github.kerozhai.morphling.mapper.MapperFactory;
import lombok.Data;

public class CyclicReferenceMappingTest {

    private static MapperFactory mapperFactory = MapperFactory.defaultMapperFactory();

    @Data
    public static class Foo {

        private Foo foo;

        private List<Foo> foos;

        private List<List<Foo>> foos2;

        private Foo[] foos3;

    }

    @Data
    public static class FooDto {

        private FooDto foo;

        private List<FooDto> foos;

        private List<List<FooDto>> foos2;

        private FooDto[] foos3;

    }

    @Test
    public void test() {
        Foo foo = new Foo();
        foo.setFoo(foo);
        foo.setFoos(Arrays.asList(foo));
        foo.setFoos2(Arrays.asList(Arrays.asList(foo)));
        foo.setFoos3(new Foo[] { foo });

        FooDto fooDto = new FooDto();

        mapperFactory.getMapperFor(Foo.class, FooDto.class).map(foo, fooDto);

        assertTrue(fooDto == fooDto.getFoo());
        assertTrue(fooDto == fooDto.getFoos().get(0));
        assertTrue(fooDto == fooDto.getFoos2().get(0).get(0));
        assertTrue(fooDto == fooDto.getFoos3()[0]);
    }

}
