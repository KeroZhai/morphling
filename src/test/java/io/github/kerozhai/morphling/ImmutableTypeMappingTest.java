package io.github.kerozhai.morphling;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.github.kerozhai.morphling.annotation.Mapping;
import io.github.kerozhai.morphling.codegenerator.ImmutableTypeConversionCodeGenerator;
import io.github.kerozhai.morphling.mapper.Mapper;
import io.github.kerozhai.morphling.mapper.MapperFactory;
import lombok.Data;

public class ImmutableTypeMappingTest {

    public static enum State {
        ON, OFF;
    }

    @Data
    public static class Source1 {
        private boolean booleanValue = true;
        private byte byteValue = 0;
        private char charValue = '1';
        private short shortValue = 2;
        private int intValue = 3;
        private long longValue = 4L;
        private float floatValue = 5.1f;
        private double doubleValue = 6.2d;
        private String stringValue = "name";
        private State enumValue = State.ON;
        private Date dateValue = new Date();
        private int emptyIntValue;
    }

    @Data
    public static class Target1 {
        private boolean booleanValue;
        private byte byteValue;
        private char charValue;
        private short shortValue;
        private int intValue;
        private long longValue;
        private float floatValue;
        private double doubleValue;
        private String stringValue;
        private State enumValue;
        private Date dateValue;
        private int emptyIntValue;
    }

    private static MapperFactory mapperFactory = new MapperFactory();

    @BeforeAll
    public static void beforeAll() {
        mapperFactory.addConversionCodeGenerator(new ImmutableTypeConversionCodeGenerator());
    }

    @Test
    public void testMapping1() {
        Source1 source = new Source1();
        Mapper<Source1, Target1> mapper = mapperFactory.getMapperFor(Source1.class, Target1.class);
        Target1 target = mapper.map(source);

        assertEquals(source.isBooleanValue(), target.isBooleanValue());
        assertEquals(source.getByteValue(), target.getByteValue());
        assertEquals(source.getCharValue(), target.getCharValue());
        assertEquals(source.getShortValue(), target.getShortValue());
        assertEquals(source.getIntValue(), target.getIntValue());
        assertEquals(source.getLongValue(), target.getLongValue());
        assertEquals(source.getFloatValue(), target.getFloatValue());
        assertEquals(source.getDoubleValue(), target.getDoubleValue());
        assertEquals(source.getStringValue(), target.getStringValue());
        assertEquals(source.getEnumValue(), target.getEnumValue());
        assertEquals(source.getDateValue(), target.getDateValue());
        assertEquals(source.getEmptyIntValue(), target.getEmptyIntValue());
    }

    @Data
    public static class Source2 {
        private Boolean booleanValue = true;
        private Byte byteValue = 0;
        private Character charValue = '1';
        private Short shortValue = 2;
        private Integer intValue = 3;
        private Long longValue = 4L;
        private Float floatValue = 5.1f;
        private Double doubleValue = 6.2d;
    }

    @Data
    public static class Target2 {
        private boolean booleanValue;
        private byte byteValue;
        private char charValue;
        private short shortValue;
        private int intValue;
        private long longValue;
        private float floatValue;
        private double doubleValue;
    }

    @Test
    public void testMapping2() {
        Source2 source = new Source2();
        Mapper<Source2, Target2> mapper = mapperFactory.getMapperFor(Source2.class, Target2.class);
        Target2 target = mapper.map(source);

        assertEquals(source.getBooleanValue(), target.isBooleanValue());
        assertEquals(source.getByteValue(), target.getByteValue());
        assertEquals(source.getCharValue(), target.getCharValue());
        assertEquals(source.getShortValue(), target.getShortValue());
        assertEquals(source.getIntValue(), target.getIntValue());
        assertEquals(source.getLongValue(), target.getLongValue());
        assertEquals(source.getFloatValue(), target.getFloatValue());
        assertEquals(source.getDoubleValue(), target.getDoubleValue());

        Mapper<Target2, Source2> reverseMapper = mapperFactory.getMapperFor(Target2.class, Source2.class);
        target.setBooleanValue(false);

        Source2 target2 = reverseMapper.map(target);

        assertEquals(target.isBooleanValue(), target2.getBooleanValue());
        assertEquals(target.getByteValue(), target2.getByteValue());
        assertEquals(target.getCharValue(), target2.getCharValue());
        assertEquals(target.getShortValue(), target2.getShortValue());
        assertEquals(target.getIntValue(), target2.getIntValue());
        assertEquals(target.getLongValue(), target2.getLongValue());
        assertEquals(target.getFloatValue(), target2.getFloatValue());
        assertEquals(target.getDoubleValue(), target2.getDoubleValue());
    }


    public static class Source3 {
        public boolean isBooleanValue() {
            return true;
        }

        public byte getByteValue() {
            return 0;
        }

        public char getCharValue() {
            return '1';
        }

        public short getShortValue() {
            return 2;
        }

        public int getIntValue() {
            return 3;
        }

        public long getLongValue() {
            return 4L;
        }

        public float getFloatValue() {
            return 5.1f;
        }

        public double getDoubleValue() {
            return 6.2d;
        }
    }

    @Data
    public static class Target3 {
        @Mapping(getterName = "isBooleanValue")
        private Boolean booleanValue;
        private Byte byteValue;
        private Character charValue;
        private Short shortValue;
        private Integer intValue;
        private Long longValue;
        private Float floatValue;
        private Double doubleValue;
    }

    @Test
    public void testMappingWithPureGetter() {
        Source3 source = new Source3();
        Mapper<Source3, Target3> mapper = mapperFactory.getMapperFor(Source3.class, Target3.class);
        Target3 target = mapper.map(source);

        assertEquals(source.isBooleanValue(), target.getBooleanValue());
        assertEquals(source.getByteValue(), target.getByteValue());
        assertEquals(source.getCharValue(), target.getCharValue());
        assertEquals(source.getShortValue(), target.getShortValue());
        assertEquals(source.getIntValue(), target.getIntValue());
        assertEquals(source.getLongValue(), target.getLongValue());
        assertEquals(source.getFloatValue(), target.getFloatValue());
        assertEquals(source.getDoubleValue(), target.getDoubleValue());
    }

}
