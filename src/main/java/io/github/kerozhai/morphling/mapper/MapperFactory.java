package io.github.kerozhai.morphling.mapper;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.github.kerozhai.morphling.annotation.Mapping;
import io.github.kerozhai.morphling.annotation.MappingIgnore;
import io.github.kerozhai.morphling.codegenerator.ArrayTypeConversionCodeGenerator;
import io.github.kerozhai.morphling.codegenerator.CollectionTypeConversionCodeGenerator;
import io.github.kerozhai.morphling.codegenerator.ConversionCodeGenerator;
import io.github.kerozhai.morphling.codegenerator.GenerationContext;
import io.github.kerozhai.morphling.codegenerator.ImmutableTypeConversionCodeGenerator;
import io.github.kerozhai.morphling.codegenerator.NestedTypeConversionCodeGenerator;
import io.github.kerozhai.morphling.codegenerator.OptionalTypeConversionCodeGenerator;
import io.github.kerozhai.morphling.converter.Converter;
import io.github.kerozhai.morphling.exception.MethodNotFoundException;
import io.github.kerozhai.morphling.util.JavassistUtils;
import io.github.kerozhai.morphling.util.ReflectionUtils;
import io.github.kerozhai.morphling.util.StringUtils;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewConstructor;

/**
 * MapperFactory is responsible for generating and caching Mappers.
 */
public final class MapperFactory {

    private final String uniqueId = UUID.randomUUID().toString().substring(0, 8);
    private ClassPool POOL = ClassPool.getDefault();
    private CtClass abstractMapperCtClass = JavassistUtils.getCtClass(POOL,
            "io.github.kerozhai.morphling.mapper.GeneratedMapper");
    private CtClass objectCtClass = JavassistUtils.getCtClass(POOL, "java.lang.Object");
    private CtClass mapperFactoryCtClass = JavassistUtils.getCtClass(POOL, getClass().getName());
    private List<ConversionCodeGenerator> conversionCodeGenerators = new ArrayList<>();
    private ConcurrentHashMap<Class<?>, ObjectFactory<?>> fallbackObjecFactories = new ConcurrentHashMap<>();

    @SuppressWarnings("rawtypes")
    private ConcurrentHashMap<MapperKey, Mapper> generatedMapperMap = new ConcurrentHashMap<>();
    @SuppressWarnings("rawtypes")
    private ConcurrentHashMap<String, Converter> converterMap = new ConcurrentHashMap<>();

    public MapperFactory() {
        POOL.importPackage("io.github.kerozhai.morphling.mapper");
        POOL.importPackage("io.github.kerozhai.morphling.util");
    }

    public static MapperFactory defaultMapperFactory() {
        MapperFactory instance = new MapperFactory();

        instance.addConversionCodeGenerator(new ArrayTypeConversionCodeGenerator());
        instance.addConversionCodeGenerator(new CollectionTypeConversionCodeGenerator());
        instance.addConversionCodeGenerator(new NestedTypeConversionCodeGenerator());
        instance.addConversionCodeGenerator(new OptionalTypeConversionCodeGenerator());
        instance.addConversionCodeGenerator(new ImmutableTypeConversionCodeGenerator());

        instance.addFallbackObjectFactory(List.class, (source) -> new ArrayList<>());

        return instance;
    }

    @SuppressWarnings("unchecked")
    public <Source, Target> Mapper<Source, Target> getMapperFor(Class<Source> sourceClass, Class<Target> targetClass) {
        MapperKey mapperKey = MapperKey.of(sourceClass, targetClass);
        Mapper<Source, Target> mapper = generatedMapperMap.get(mapperKey);

        if (mapper == null) {
            try {
                mapper = (Mapper<Source, Target>) generateMapperClassFor(sourceClass, targetClass)
                        .getConstructor(MapperFactory.class).newInstance(this);
                generatedMapperMap.put(mapperKey, mapper);
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | SecurityException
                    | InvocationTargetException | NoSuchMethodException e) {
                e.printStackTrace();
            }
        }

        return mapper;
    }

    public Class<?> generateMapperClassFor(Class<?> sourceClass, Class<?> targetClass) {
        CtClass contextCtClass = JavassistUtils.getCtClass(POOL, Context.class.getName());

        try {
            CtClass mapperCtClass = POOL.makeClass(generateMapperClassNameFor(sourceClass, targetClass));
            mapperCtClass
                    .addConstructor(CtNewConstructor.make(new CtClass[] { mapperFactoryCtClass }, null, mapperCtClass));

            StringBuilder bodyBuilder = new StringBuilder("{\n");

            mapperCtClass.setSuperclass(abstractMapperCtClass);

            String sourceClassName = sourceClass.getName();
            String targetClassName = targetClass.getName();
            CtMethod instantiateMethod = new CtMethod(objectCtClass, "instantiate", new CtClass[] { objectCtClass },
                    mapperCtClass);
            instantiateMethod.setModifiers(Modifier.PROTECTED);
            instantiateMethod.setBody(generateInstantiateMethodBody(sourceClass, targetClass));
            mapperCtClass.addMethod(instantiateMethod);

            CtMethod mapMethod = new CtMethod(CtClass.voidType, "doMap",
                    new CtClass[] { objectCtClass, objectCtClass, contextCtClass }, mapperCtClass);
            mapMethod.setModifiers(Modifier.PROTECTED);
            bodyBuilder.append(sourceClassName).append(" source = ").append("(").append(sourceClassName).append(") $1;")
                    .append(targetClassName)
                    .append(" target = ").append("(").append(targetClassName)
                    .append(") $2; Context context = $3;");

            for (Field targetField : ReflectionUtils.getDeclaredAndInheritedFields(targetClass)) {
                String targetFieldName = targetField.getName();
                String sourceFieldName = targetFieldName;
                AnnotatedType targetFieldType = targetField.getAnnotatedType();

                MappingIgnore mappingIgnore = targetField.getAnnotation(MappingIgnore.class);

                if (mappingIgnore != null) {
                    continue;
                }

                Mapping mapping = targetField.getAnnotation(Mapping.class);

                if (mapping != null) {
                    String alias = mapping.alias();

                    if (!alias.isEmpty()) {
                        sourceFieldName = alias;
                    }
                }

                Field sourceField = ReflectionUtils.findDeclaredOrInheritedField(sourceClass, sourceFieldName);

                // Check if sourceClass has the field with the same name
                if (sourceField != null) {
                    AnnotatedType sourceFieldType = sourceField.getAnnotatedType();
                    String capitalizedSourceFieldName = StringUtils.capitalize(sourceFieldName);
                    String capitalizedTargetFieldName = StringUtils.capitalize(targetFieldName);
                    String sourceGetter = "source."
                            + ("boolean".equals(sourceFieldType.getType().getTypeName()) ? "is" : "get")
                            + capitalizedSourceFieldName + "()";
                    String targetGetter = "target."
                            + ("boolean".equals(targetFieldType.getType().getTypeName()) ? "is" : "get")
                            + capitalizedTargetFieldName + "()";
                    String targetSetter = "target.set" + capitalizedTargetFieldName;
                    String sourceFieldNonGenericTypeName = getNonGenericTypeName(sourceFieldType.getType());
                    String targetFieldNonGenericTypeName = getNonGenericTypeName(targetFieldType.getType());

                    GenerationContext generationContext = GenerationContext.builder()
                            .sourceType(sourceFieldType)
                            .targetType(targetFieldType)
                            .generators(conversionCodeGenerators)
                            .mapping(mapping)
                            .build();

                    bodyBuilder.append("{ boolean shouldIgnore = ");

                    Mapping.ValueStrategy localValueStrategy = null;

                    if (mapping != null) {
                        localValueStrategy = mapping.valueStrategy();
                        boolean shouldIgnore = false;
                        Class<?>[] groupsToMatch = null;

                        if (mapping.when().length > 0) {
                            shouldIgnore = true;
                            groupsToMatch = mapping.when();
                        } else if (mapping.unless().length > 0) {
                            groupsToMatch = mapping.unless();
                        }

                        if (groupsToMatch != null) {
                            bodyBuilder.append(shouldIgnore).append(";");

                            if (groupsToMatch != null) {
                                bodyBuilder.append(
                                        "if (context.getIgnoreGroups() != null && context.getIgnoreGroups().length > 0) {")
                                        .append("for (int i = 0; i < context.getIgnoreGroups().length; i++) {");

                                for (Class<?> group : groupsToMatch) {
                                    bodyBuilder.append("if (context.getIgnoreGroups()[i] == ").append(group.getName())
                                            .append(".class) { shouldIgnore = !shouldIgnore; break; }");
                                }

                                bodyBuilder.append("}}");
                            }
                        } else {
                            bodyBuilder.append("false;");
                        }
                    } else {
                        bodyBuilder.append("false;");
                    }

                    bodyBuilder.append(sourceFieldNonGenericTypeName).append(" ")
                            .append(generationContext.getSourceVariableName()).append(" = ")
                            .append(sourceGetter).append(";");

                    if (localValueStrategy != null && localValueStrategy != Mapping.ValueStrategy.DEFAULT) {
                        switch (localValueStrategy) {
                            case IF_NOT_NULL: {
                                bodyBuilder.append("shouldIgnore = shouldIgnore || ")
                                        .append(generationContext.getSourceVariableName()).append(" == null;");
                                break;
                            }
                            case IF_NOT_EMPTY: {
                                bodyBuilder.append("shouldIgnore = shouldIgnore || ReflectionUtils.isEmpty(")
                                        .append(generationContext.getSourceVariableName()).append(");");
                                break;
                            }
                            default: {
                                break;
                            }
                        }
                    } else {
                        boolean isSourceTypePrimitive = ReflectionUtils.isPrimitiveType(sourceFieldType.getType());

                        bodyBuilder.append("if (context.getValueStrategy() != null) {")
                                .append("switch (context.getValueStrategy().name()) {")
                                .append("case \"IF_NOT_NULL\": { shouldIgnore = shouldIgnore || ");

                        if (isSourceTypePrimitive) {
                            bodyBuilder.append("false;");
                        } else {
                            bodyBuilder.append(generationContext.getSourceVariableName()).append(" == null;");
                        }

                        bodyBuilder.append("break; }")
                                .append("case \"IF_NOT_EMPTY\": { shouldIgnore = shouldIgnore || ReflectionUtils.isEmpty(");

                        if (isSourceTypePrimitive) {
                            bodyBuilder.append(ReflectionUtils.toWrapper(sourceFieldType.getType()).getName())
                                    .append(".valueOf(")
                                    .append(sourceGetter).append("));");
                        } else {
                            bodyBuilder.append(generationContext.getSourceVariableName()).append(");");
                        }
                        bodyBuilder.append("break; }")
                                .append("default: break;")
                                .append("}}");
                    }

                    bodyBuilder.append("if (!shouldIgnore) {");

                    // check if converter specified
                    if (mapping != null) {
                        Class<? extends Converter> converterClass = mapping.converter();

                        if (converterClass != Converter.class) {
                            String className = converterClass.getName();
                            Converter converter = converterMap.get(className);

                            if (converter == null) {
                                try {
                                    converter = (Converter<?, ?>) converterClass.newInstance();
                                    converterMap.put(className, converter);
                                } catch (InstantiationException | IllegalAccessException e) {
                                    e.printStackTrace();
                                }
                            }

                            if (converter != null) {
                                // use converter to convert value
                                bodyBuilder.append(targetSetter).append("((").append(targetFieldNonGenericTypeName)
                                        .append(") mapperFactory.getConverter(\"")
                                        .append(className)
                                        .append("\").convert(");

                                // boxing for primitive types
                                if (ReflectionUtils.isPrimitiveType(sourceFieldType.getType())) {
                                    bodyBuilder
                                            .append(ReflectionUtils.toWrapper(sourceFieldType.getType()).getName())
                                            .append(".valueOf(")
                                            .append(sourceGetter).append("), mapperFactory));");
                                } else {
                                    bodyBuilder.append(sourceGetter).append(", mapperFactory));");
                                }

                                bodyBuilder.append("}}");
                            }

                            continue;
                        }
                    }

                    for (ConversionCodeGenerator codeGenerator : conversionCodeGenerators) {
                        if (codeGenerator.isSupported(generationContext)) {
                            String code = codeGenerator.generate(generationContext);

                            if (code != null) {
                                bodyBuilder.append(targetFieldNonGenericTypeName)
                                        .append(" ").append(generationContext.getTargetVariableName())
                                        .append(" =").append(targetGetter).append(";").append(code)
                                        .append(targetSetter).append("(")
                                        .append(generationContext.getTargetVariableName())
                                        .append(");\n");
                            }

                            break;
                        }
                    }

                    bodyBuilder.append("}}");
                }
            }

            String body = bodyBuilder.append("}").toString();
            mapMethod.setBody(body);
            mapperCtClass.addMethod(mapMethod);

            Class<?> mapperClass = mapperCtClass.toClass();
            mapperCtClass.detach();

            return mapperClass;
        } catch (CannotCompileException e) {
            e.printStackTrace();

            if (e.getMessage().contains("not found")) {
                throw new MethodNotFoundException(e.getMessage() + ". Did you forget to provide it?");
            }
        }

        return null;
    }

    public void addConversionCodeGenerator(ConversionCodeGenerator generator) {
        conversionCodeGenerators.add(generator);
    }

    public void addConverter(Converter<?, ?> converter) {
        converterMap.put(converter.getClass().getName(), converter);
    }

    public void addConverters(Converter<?, ?>... converters) {
        for (Converter<?, ?> converter : converters) {
            addConverter(converter);
        }
    }

    public void addConverters(Collection<Converter<?, ?>> converters) {
        converters.forEach(this::addConverter);
    }

    public Converter<?, ?> getConverter(String converterClassName) {
        return converterMap.get(converterClassName);
    }

    public <T> void addFallbackObjectFactory(Class<T> targetClass, ObjectFactory<T> objectFactory) {
        fallbackObjecFactories.put(targetClass, objectFactory);
    }

    public <T> T getFallbackObject(Object source, Class<T> targetClass) {
        @SuppressWarnings("unchecked")
        ObjectFactory<T> objectFactory = (ObjectFactory<T>) fallbackObjecFactories.get(targetClass);

        if (objectFactory != null) {
            return objectFactory.create(source);
        }

        return null;
    }

    private String generateMapperClassNameFor(Class<?> sourceClass, Class<?> targetClass) {
        return sourceClass.getPackage().getName() + "." + StringUtils.classNameToPascalCase(sourceClass.getName())
                + "To"
                + StringUtils.classNameToPascalCase(targetClass.getName()) + "Mapper$" + uniqueId;
    }

    private String getNonGenericTypeName(Type type) {
        String typeName = type.getTypeName();

        if (type instanceof ParameterizedType) {
            return typeName.substring(0, typeName.indexOf("<"));
        }

        return typeName;
    }

    private String generateInstantiateMethodBody(Class<?> sourceClass, Class<?> targetClass) {
        StringBuilder bodyBuilder = new StringBuilder("{ try {");

        if (targetClass.isInterface() || Modifier.isAbstract(targetClass.getModifiers())) {
            if (targetClass.isAssignableFrom(sourceClass)) {
                bodyBuilder.append("return $1.getClass().newInstance();");
            } else {
                bodyBuilder.append(
                        "throw new InstantiationException(\"Cannot instantiate interface or abstract class\");");
            }
        } else {
            bodyBuilder.append("return new ").append(targetClass.getName()).append("();");
        }

        bodyBuilder.append("} catch (Throwable e) { return mapperFactory.getFallbackObject($1, ")
                .append(targetClass.getName()).append(".class); } }");

        return bodyBuilder.toString();
    }

}
