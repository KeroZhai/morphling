package io.github.kerozhai.morphling.mapper;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
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
import javassist.CtField;
import javassist.CtMethod;

/**
 * MapperFactory is responsible for generating and caching Mappers.
 */
public final class MapperFactory {

    private final String uniqueId = UUID.randomUUID().toString().substring(0, 8);
    private ClassPool POOL = ClassPool.getDefault();
    private CtClass abstractMapperCtClass = JavassistUtils.getCtClass(POOL,
            "io.github.kerozhai.morphling.mapper.GeneratedMapper");
    private CtClass objectCtClass = JavassistUtils.getCtClass(POOL, "java.lang.Object");
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
                mapper = (Mapper<Source, Target>) generateMapperClassFor(sourceClass, targetClass).newInstance();
                mapper.getClass().getDeclaredField("mapperFactory").set(mapper, this);
                generatedMapperMap.put(mapperKey, mapper);
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | NoSuchFieldException
                    | SecurityException e) {
                e.printStackTrace();
            }
        }

        return mapper;
    }

    public Class<?> generateMapperClassFor(Class<?> sourceClass, Class<?> targetClass) {
        CtClass groupsCtClass = JavassistUtils.getCtClass(POOL, Class[].class.getName());
        CtClass contextCtClass = JavassistUtils.getCtClass(POOL, Context.class.getName());

        try {
            CtClass mapperCtClass = POOL.makeClass(generateMapperClassNameFor(sourceClass, targetClass));
            CtField mapperFactoryField = new CtField(JavassistUtils.getCtClass(POOL, getClass().getName()),
                    "mapperFactory", mapperCtClass);

            mapperFactoryField.setModifiers(Modifier.PUBLIC);
            mapperCtClass.addField(mapperFactoryField);

            StringBuilder bodyBuilder = new StringBuilder("{\n");

            mapperCtClass.setSuperclass(abstractMapperCtClass);

            String sourceClassName = sourceClass.getName();
            String targetClassName = targetClass.getName();
            CtMethod instantiateMethod = new CtMethod(objectCtClass, "instantiate", new CtClass[] { objectCtClass },
                    mapperCtClass);
            instantiateMethod.setModifiers(Modifier.PUBLIC);
            instantiateMethod.setBody(generateInstantiateMethodBody(sourceClass, targetClass));
            mapperCtClass.addMethod(instantiateMethod);

            CtMethod mapMethod = new CtMethod(CtClass.voidType, "map",
                    new CtClass[] { objectCtClass, objectCtClass, groupsCtClass, contextCtClass }, mapperCtClass);
            mapMethod.setModifiers(Modifier.PUBLIC);
            bodyBuilder.append(sourceClassName).append(" source = ").append("(").append(sourceClassName).append(") $1;")
                    .append(targetClassName)
                    .append(" target = ").append("(").append(targetClassName)
                    .append(") $2; Class[] ignoreGroups = $3; Context context = $4;");

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
                    String getterPrefix = "boolean".equals(sourceFieldType.getType().getTypeName()) ? "is" : "get";
                    String capitalizedSourceFieldName = StringUtils.capitalize(sourceFieldName);
                    String capitalizedTargetFieldName = StringUtils.capitalize(targetFieldName);
                    String getterName = getterPrefix + capitalizedSourceFieldName;
                    String setter = "target.set" + capitalizedTargetFieldName;
                    String sourceValue = "source." + getterName + "()";
                    String sourceFieldNonGenericTypeName = getNonGenericTypeName(sourceFieldType.getType());
                    String targetFieldNonGenericTypeName = getNonGenericTypeName(targetFieldType.getType());
                    boolean shouldCheckIgnore = false;

                    GenerationContext generationContext = GenerationContext.builder()
                            .sourceType(sourceFieldType)
                            .targetType(targetFieldType)
                            .generators(conversionCodeGenerators)
                            .mapping(mapping)
                            .build();

                    if (mapping != null) {
                        Mapping.ValueStrategy strategy = mapping.valueStrategy();
                        boolean shouldIgnore = false;
                        Class<?>[] groupsToMatch = null;

                        if (mapping.when().length > 0) {
                            shouldIgnore = true;
                            groupsToMatch = mapping.when();
                        } else if (mapping.unless().length > 0) {
                            groupsToMatch = mapping.unless();
                        }

                        if (strategy != Mapping.ValueStrategy.DEFAULT || groupsToMatch != null) {
                            shouldCheckIgnore = true;
                            bodyBuilder.append("{ boolean shouldIgnore = ").append(shouldIgnore).append(";");

                            if (groupsToMatch != null) {
                                bodyBuilder.append("if (ignoreGroups != null && ignoreGroups.length > 0) {")
                                        .append("for (int i = 0; i < ignoreGroups.length; i++) {");

                                for (Class<?> group : groupsToMatch) {
                                    bodyBuilder.append("if (ignoreGroups[i] == ").append(group.getName())
                                            .append(".class) { shouldIgnore = !shouldIgnore; break; }");
                                }

                                bodyBuilder.append("}}");
                            }

                            bodyBuilder.append(sourceFieldNonGenericTypeName).append(" ")
                                    .append(generationContext.getSourceVariableName()).append(" = ")
                                    .append(sourceValue).append(";");

                            switch (strategy) {
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

                            bodyBuilder.append("if (!shouldIgnore) {");
                        }
                    }

                    if (!shouldCheckIgnore) {
                        // Double curly braces to match cases with and without MapperIgnore annotation
                        bodyBuilder.append("{{").append(sourceFieldNonGenericTypeName).append(" ")
                                .append(generationContext.getSourceVariableName()).append(" = ")
                                .append(sourceValue).append(";");
                    }

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
                                bodyBuilder.append(setter).append("((").append(targetFieldNonGenericTypeName)
                                        .append(") mapperFactory.getConverter(\"")
                                        .append(className)
                                        .append("\").convert(");

                                // boxing for primitive types
                                if (ReflectionUtils.isPrimitiveType(sourceFieldType.getType())) {
                                    bodyBuilder
                                            .append(ReflectionUtils.toWrapper(sourceFieldType.getType()).getName())
                                            .append(".valueOf(")
                                            .append(sourceValue).append("), mapperFactory));");
                                } else {
                                    bodyBuilder.append(sourceValue).append(", mapperFactory));");
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
                                        .append(";").append(code)
                                        .append(setter).append("(").append(generationContext.getTargetVariableName())
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

        return StringUtils.classNameToPascalCase(sourceClass.getName()) + "To"
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
