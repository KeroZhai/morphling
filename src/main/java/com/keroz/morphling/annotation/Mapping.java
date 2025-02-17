package com.keroz.morphling.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@SuppressWarnings("rawtypes")
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface Mapping {

    String alias() default "";

    Class<? extends com.keroz.morphling.converter.Converter> converter() default com.keroz.morphling.converter.Converter.class;

    Class<?> initialValueType() default void.class;

    InitialValueTypeMapping[] initialValueTypeMappings() default {};

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE_USE })
    public static @interface Generic {

        // Class<? extends com.keroz.morphling.converter.Converter> converter() default
        // com.keroz.morphling.converter.Converter.class; // TODO add support

        Class<?> initialValueType() default void.class;

        InitialValueTypeMapping[] initialValueTypeMappings() default {};

    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.ANNOTATION_TYPE })
    public static @interface InitialValueTypeMapping {

        Class<?> sourceType();

        Class<?> targetType();

    }

}
