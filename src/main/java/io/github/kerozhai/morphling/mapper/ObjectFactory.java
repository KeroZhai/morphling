package io.github.kerozhai.morphling.mapper;

public interface ObjectFactory<T> {

    T create(Object source);

}
