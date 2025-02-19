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

    ValueStrategy valueStrategy() default ValueStrategy.DEFAULT;

    /**
     * The condition to apply the mapping.
     * <p>
     * Note: this attribute cannot be used with the {@link #unless()} attribute. If
     * both are present, this attribute will take precedence.
     */
    Class<?>[] when() default {};

    /**
     * The condition to not apply the mapping.
     * <p>
     * Note: this attribute cannot be used with the {@link #when()} attribute. If
     * both are present, {@link #when()} will take precedence.
     */
    Class<?>[] unless() default {};

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

    public static enum ValueStrategy {
        DEFAULT,
        IF_NOT_NULL,
        IF_NOT_EMPTY;
    }

}
