package com.keroz.morphling.mapper;

public interface ObjectFactory<T> {

    T create(Object source);

}
