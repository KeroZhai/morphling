package io.github.kerozhai.morphling.converter;

import io.github.kerozhai.morphling.mapper.MapperFactory;

public interface Converter<Source, Target> {

    Target convert(Source source, MapperFactory mapperFactory);

}
