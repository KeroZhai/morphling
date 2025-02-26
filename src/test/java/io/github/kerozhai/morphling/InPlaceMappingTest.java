package io.github.kerozhai.morphling;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import io.github.kerozhai.morphling.mapper.MapperFactory;
import lombok.Data;

public class InPlaceMappingTest {

    private static MapperFactory mapperFactory = MapperFactory.defaultMapperFactory();

    @Data
    public static class Foo {
        private Bar bar;
    }

    @Data
    public static class Bar {
        private String name;

        private Baz baz;
    }

    @Data
    public static class Baz {
        private String name;
    }

    @Data
    public static class FooDto {
        private BarDto bar;
    }

    @Data
    public static class BarDto {
        private String name;

        private BazDto baz;
    }

    @Data
    public static class BazDto {
        private String name;
    }

    @Test
    public void testInPlaceMapping() {
        Foo foo = new Foo();
        Bar bar = new Bar();
        Baz baz = new Baz();
        foo.setBar(bar);
        bar.setBaz(baz);

        FooDto fooDto = new FooDto();
        BarDto barDto = new BarDto();
        BazDto bazDto = new BazDto();
        barDto.setName("Bar");
        bazDto.setName("Baz");
        fooDto.setBar(barDto);
        barDto.setBaz(bazDto);

        mapperFactory.getMapperFor(Foo.class,
                FooDto.class).map(foo, fooDto);

        assertSame(fooDto.getBar(), barDto);
        assertSame(fooDto.getBar().getBaz(), bazDto);
        assertNull(barDto.getName());
        assertNull(bazDto.getName());
    }

}
