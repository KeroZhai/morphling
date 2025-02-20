package com.keroz.morphling;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Test;

import com.keroz.morphling.mapper.Mapper;
import com.keroz.morphling.mapper.MapperFactory;

import lombok.Data;
import lombok.EqualsAndHashCode;

public class SubTypeMappingTest {

    private static MapperFactory mapperFactory = MapperFactory.defaultMapperFactory();

    @Data
    public static class Entity {

        private String name;

    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class EntityProxy extends Entity {

        private String name;

    }

    @Data
    public static class Dto {

        private String name;

    }

    @Test
    public void testSubTypeMapping() {
        EntityProxy entityProxy = new EntityProxy();
        entityProxy.setName("entityProxy");

        Mapper<Entity, Dto> mapper = mapperFactory.getMapperFor(Entity.class, Dto.class);
        Dto dto = mapper.map(entityProxy);

        assertEquals(dto.getName(), "entityProxy");
    }

}
