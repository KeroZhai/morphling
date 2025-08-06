package io.github.kerozhai.morphling;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Test;

import io.github.kerozhai.morphling.mapper.Mapper;
import io.github.kerozhai.morphling.mapper.MapperFactory;
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

        private String description;

    }

    @Data
    public static class Dto {

        private String name;

        private String description;

    }

    @Test
    public void testSubTypeMapping() {
        EntityProxy entityProxy = new EntityProxy();
        entityProxy.setName("entityProxy");
        entityProxy.setDescription("description");

        Mapper<Entity, Dto> mapper = mapperFactory.getMapperFor(Entity.class, Dto.class);
        Dto dto = mapper.map(entityProxy);

        assertEquals(dto.getName(), entityProxy.getName());
        assertNull(dto.getDescription()); // No description field in Entity
    }

    @Test
    public void testSubTypeMapping2() {
        EntityProxy entityProxy = new EntityProxy();
        entityProxy.setName("entityProxy");
        entityProxy.setDescription("description");

        Entity entity = entityProxy;

        @SuppressWarnings("unchecked")
        Mapper<Entity, Dto> mapper = (Mapper<Entity, Dto>) mapperFactory.getMapperFor(entity.getClass(), Dto.class);
        Dto dto = mapper.map(entityProxy);

        assertEquals(dto.getName(), entityProxy.getName());
        assertEquals(dto.getDescription(), entityProxy.getDescription());
    }

}
