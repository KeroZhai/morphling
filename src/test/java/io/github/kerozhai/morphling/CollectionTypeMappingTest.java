package io.github.kerozhai.morphling;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import io.github.kerozhai.morphling.annotation.Mapping;
import io.github.kerozhai.morphling.codegenerator.ArrayTypeConversionCodeGenerator;
import io.github.kerozhai.morphling.codegenerator.CollectionTypeConversionCodeGenerator;
import io.github.kerozhai.morphling.codegenerator.ImmutableTypeConversionCodeGenerator;
import io.github.kerozhai.morphling.codegenerator.NestedTypeConversionCodeGenerator;
import io.github.kerozhai.morphling.codegenerator.OptionalTypeConversionCodeGenerator;
import io.github.kerozhai.morphling.mapper.Mapper;
import io.github.kerozhai.morphling.mapper.MapperFactory;
import lombok.Data;

public class CollectionTypeMappingTest {

    private static MapperFactory mapperFactory;

    static {
        mapperFactory = new MapperFactory();

        CollectionTypeConversionCodeGenerator collectionTypeConversionCodeGenerator = new CollectionTypeConversionCodeGenerator();

        collectionTypeConversionCodeGenerator.addExcludedCollectionType(List.class);

        mapperFactory.addConversionCodeGenerator(new ArrayTypeConversionCodeGenerator());
        mapperFactory.addConversionCodeGenerator(collectionTypeConversionCodeGenerator);
        mapperFactory.addConversionCodeGenerator(new NestedTypeConversionCodeGenerator());
        mapperFactory.addConversionCodeGenerator(new OptionalTypeConversionCodeGenerator());
        mapperFactory.addConversionCodeGenerator(new ImmutableTypeConversionCodeGenerator());

        mapperFactory.addFallbackObjectFactory(List.class, (source) -> new ArrayList<>());
    }

    @Data
    public static class Foo {
        private long id = 1;
        private String name = "foo";
    }

    @Data
    public static class Bar {
        private Long id;
        private String name;
    }

    @Data
    public static class Source1 {
        private List<Integer> stringArrayList = new ArrayList<>();
        private List<Foo> objectLinkedList = new LinkedList<>();
        private List<List<Foo>> nestedList = new LinkedList<>();

        public Source1() {
            Foo foo = new Foo();
            stringArrayList.add(1);
            objectLinkedList.add(foo);
            nestedList.add(Arrays.asList(foo));
        }
    }

    @Data
    public static class Target1 {
        private List<Integer> stringArrayList;
        private List<Bar> objectLinkedList;
        @Mapping(converter = ListConverter.class)
        private List<List<Bar>> nestedList;

    }

    public static class ListConverter
            implements io.github.kerozhai.morphling.converter.Converter<List<List<Foo>>, List<List<Bar>>> {

        @Override
        public List<List<Bar>> convert(List<List<Foo>> source, MapperFactory mapperFactory) {
            if (source == null) {
                return null;
            } else {
                Mapper<Foo, Bar> mapper = mapperFactory.getMapperFor(Foo.class, Bar.class);

                return source.stream().map((list) -> {
                    if (list == null) {
                        return null;
                    }

                    return list.stream().map(mapper::map).collect(Collectors.toList());
                }).collect(Collectors.toList());
            }
        }

    }

    @Test
    public void testInterface() {
        Source1 source = new Source1();
        Mapper<Source1, Target1> mapper = mapperFactory.getMapperFor(Source1.class, Target1.class);
        Target1 target = mapper.map(source);

        assertEquals(source.getStringArrayList(), target.getStringArrayList());
        assertEquals(source.getObjectLinkedList().get(0).getId(), target.getObjectLinkedList().get(0).getId());
        assertEquals(source.getObjectLinkedList().get(0).getName(), target.getObjectLinkedList().get(0).getName());
        assertEquals(source.getNestedList().get(0).get(0).getId(), target.getNestedList().get(0).get(0).getId());
        assertEquals(source.getNestedList().get(0).get(0).getName(), target.getNestedList().get(0).get(0).getName());
    }

    @Data
    public static class Source2 {
        private List<Integer> stringList = new LinkedList<>();
        private List<Foo> objectList = new ArrayList<>();

        public Source2() {
            stringList.add(1);
            objectList.add(new Foo());
        }
    }

    @Data
    public static class Target2 {
        private ArrayList<Integer> stringList;
        private LinkedList<Bar> objectList;
    }

    @Test
    public void testImplementation() {
        Source2 source = new Source2();
        Mapper<Source2, Target2> mapper = mapperFactory.getMapperFor(Source2.class, Target2.class);
        Target2 target = mapper.map(source);

        assertEquals(source.getStringList(), target.getStringList());
        assertEquals(source.getObjectList().get(0).getId(), target.getObjectList().get(0).getId());
        assertEquals(source.getObjectList().get(0).getName(), target.getObjectList().get(0).getName());
    }

    @Data
    public static class Source3 {
        private List<List<List<Integer>>> nestedList = new LinkedList<>();

        public Source3() {
            nestedList.add(Arrays.asList(Arrays.asList(1, 2, 3)));
        }
    }

    @Data
    public static class Target3 {
        private List<List<List<Integer>>> nestedList;
    }

    @Test
    public void testNested() {
        Source3 source = new Source3();
        Target3 target = mapperFactory.getMapperFor(Source3.class, Target3.class).map(source);

        assertEquals(source.getNestedList(), target.getNestedList());
    }

    @Test
    public void test() {
        try {
            Class<?> clazz = Class.forName("java.util.Arrays$ArrayList");
            System.out.println(clazz.getName());
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
