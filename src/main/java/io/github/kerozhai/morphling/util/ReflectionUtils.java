package io.github.kerozhai.morphling.util;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Currency;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ReflectionUtils {

    public List<Field> getDeclaredAndInheritedFields(Class<?> declaringClass) {
        List<Field> fields = new ArrayList<>();
        Set<String> fieldNames = new HashSet<>();

        while (declaringClass != Object.class) {
            for (Field field : declaringClass.getDeclaredFields()) {
                int modifiers = field.getModifiers();

                if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) {
                    continue;
                }

                if (fieldNames.add(field.getName())) {
                    fields.add(field);
                }
            }

            declaringClass = declaringClass.getSuperclass();
        }

        return fields;
    }

    public List<Method> getDeclaredAndInheritedMethods(Class<?> declaringClass) {
        List<Method> methods = new ArrayList<>();

        while (declaringClass != Object.class) {
            // TODO remove duplicated
            methods.addAll(Arrays.asList(declaringClass.getDeclaredMethods()));

            declaringClass = declaringClass.getSuperclass();
        }

        return methods;
    }

    public Field findDeclaredField(Class<?> declaringClass, String fieldName) {
        Field field = null;
        try {
            field = declaringClass.getDeclaredField(fieldName);
        } catch (NoSuchFieldException | SecurityException ignored) {
        }

        return field;
    }

    public Field findDeclaredOrInheritedField(Class<?> declaringClass, String fieldName) {
        while (declaringClass != Object.class) {
            try {
                return declaringClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException | SecurityException ignored) {
                declaringClass = declaringClass.getSuperclass();
            }
        }

        return null;
    }

    public Method findDeclaredMethod(Class<?> declaringClass, String methodName, Class<?>... parameterTypes) {
        try {
            return declaringClass.getDeclaredMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException | SecurityException ignored) {
        }

        return null;
    }

    public Method findDeclaredOrInheritedMethod(Class<?> declaringClass, String methodName,
            Class<?>... parameterTypes) {
        while (declaringClass != Object.class) {
            try {
                return declaringClass.getDeclaredMethod(methodName, parameterTypes);
            } catch (NoSuchMethodException | SecurityException ignored) {
                declaringClass = declaringClass.getSuperclass();
            }
        }

        return null;
    }

    public Class<?> toClass(Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        }

        return toClass(((ParameterizedType) type).getRawType());
    }

    public boolean isPrimitiveType(Type type) {
        if (type instanceof Class) {
            return ((Class<?>) type).isPrimitive();
        }

        return false;
    }

    public boolean isPrimitiveOrWrapperType(Type type) {
        return toClass(type).isPrimitive() || Boolean.class.equals(type) || Byte.class.equals(type)
                || Character.class.equals(type) || Short.class.equals(type) || Integer.class.equals(type)
                || Long.class.equals(type) || Float.class.equals(type) || Double.class.equals(type);
    }

    public boolean isImmutableType(Type type) {
        if (type instanceof Class) {
            Class<?> classType = (Class<?>) type;
            return isPrimitiveOrWrapperType(classType)
                    || isNumberType(type)
                    || classType.isEnum()
                    || String.class.equals(classType)
                    || Date.class.equals(classType)
                    || UUID.class.equals(classType)
                    || URL.class.equals(classType)
                    || URI.class.equals(classType)
                    || Locale.class.equals(classType)
                    || File.class.equals(classType)
                    || Inet4Address.class.equals(classType)
                    || Inet6Address.class.equals(classType)
                    || InetSocketAddress.class.equals(classType)
                    || Currency.class.equals(classType)
                    || isJava8TImeType(classType);
        } else {
            return false;
        }
    }

    public boolean isCollectionType(AnnotatedType annotatedType) {
        return isCollectionType(annotatedType.getType());
    }

    public boolean isCollectionType(Type type) {
        return Collection.class.isAssignableFrom(toClass(type));
    }

    public boolean isMapType(Type type) {
        return Map.class.isAssignableFrom(toClass(type));
    }

    public boolean isNumberType(Type type) {
        return Number.class.isAssignableFrom(toClass(type));
    }

    public boolean isJava8TImeType(Type type) {
        return toClass(type).getName().startsWith("java.time.");
    }

    public boolean isOptionalType(Type type) {
        return toClass(type).equals(java.util.Optional.class);
    }

    public Class<?> toWrapper(Type primitiveType) {
        Class<?> clazz = toClass(primitiveType);

        if (!clazz.isPrimitive())
            return clazz;

        if (primitiveType == Integer.TYPE)
            return Integer.class;
        if (primitiveType == Long.TYPE)
            return Long.class;
        if (primitiveType == Boolean.TYPE)
            return Boolean.class;
        if (primitiveType == Byte.TYPE)
            return Byte.class;
        if (primitiveType == Character.TYPE)
            return Character.class;
        if (primitiveType == Float.TYPE)
            return Float.class;
        if (primitiveType == Double.TYPE)
            return Double.class;
        if (primitiveType == Short.TYPE)
            return Short.class;
        if (primitiveType == Void.TYPE)
            return Void.class;

        return clazz;
    }

    public boolean isEmpty(Object value) {
        if (value == null) {
            return true;
        } else if (value instanceof Number) {
            return ((Number) value).intValue() == 0;
        } else if (value instanceof CharSequence) {
            return ((CharSequence) value).length() == 0;
        } else if (value instanceof Collection) {
            return ((Collection<?>) value).isEmpty();
        } else if (value instanceof Map) {
            return ((Map<?, ?>) value).isEmpty();
        } else if (value.getClass().isArray()) {
            return Array.getLength(value) == 0;
        }

        return false;
    }

    public boolean isNotEmpty(Object value) {
        return !isEmpty(value);
    }

    public String getSimpleTypeName(Type type) {
        if (type instanceof Class) {
            return ((Class<?>) type).getName();
        } else if (type instanceof ParameterizedType) {
            return getSimpleTypeName(((ParameterizedType) type).getRawType());
        }

        return type.toString();
    }

    public <A extends Annotation> A getAnnotation(Type type, Class<A> annotationClass) {
        if (type instanceof Class) {
            return ((Class<?>) type).getAnnotation(annotationClass);
        } else if (type instanceof ParameterizedType) {
            return getAnnotation(((ParameterizedType) type).getRawType(), annotationClass);
        }

        return null;
    }

    public String getDefaultValueStringForPrimitiveType(Type type) {
        if (type == Boolean.TYPE)
            return "false";
        if (type == Byte.TYPE)
            return "0";
        if (type == Short.TYPE)
            return "0";
        if (type == Integer.TYPE)
            return "0";
        if (type == Long.TYPE)
            return "0";
        if (type == Character.TYPE)
            return "''";
        if (type == Float.TYPE)
            return "0.0";
        if (type == Double.TYPE)
            return "0.0";

        return "null";
    }
}
