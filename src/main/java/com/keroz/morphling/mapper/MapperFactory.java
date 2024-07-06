package com.keroz.morphling.mapper;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import com.keroz.morphling.annotation.AliasFor;
import com.keroz.morphling.annotation.MapperIgnore;
import com.keroz.morphling.annotation.MapperIgnore.Excluded;
import com.keroz.morphling.annotation.MapperIgnore.Policy;
import com.keroz.morphling.annotation.MapperIgnore.Included;
import com.keroz.morphling.codegenerator.ArrayTypeConversionCodeGenerator;
import com.keroz.morphling.codegenerator.CollectionTypeConversionCodeGenerator;
import com.keroz.morphling.codegenerator.ConversionCodeGenerator;
import com.keroz.morphling.codegenerator.NestedTypeConversionCodeGenerator;
import com.keroz.morphling.codegenerator.SimpleTypeConversionCodeGenerator;
import com.keroz.morphling.converter.Converter;
import com.keroz.morphling.exception.MethodNotFoundException;
import com.keroz.morphling.util.JavassistUtils;
import com.keroz.morphling.util.ReflectionUtils;
import com.keroz.morphling.util.StringUtils;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.bytecode.SignatureAttribute.ClassSignature;
import javassist.bytecode.SignatureAttribute.ClassType;
import javassist.bytecode.SignatureAttribute.TypeArgument;

/**
 * 123
 */
public final class MapperFactory {

    private final String uniqueId = UUID.randomUUID().toString().substring(0, 8);
    private ClassPool POOL = ClassPool.getDefault();
    private CtClass mapperInterfaceCtClass = JavassistUtils.getCtClass(POOL, "com.keroz.morphling.mapper.Mapper");
    private CtClass objectCtClass = JavassistUtils.getCtClass(POOL, "java.lang.Object");
    private List<ConversionCodeGenerator> conversionCodeGenerators = new ArrayList<>();

    @SuppressWarnings("rawtypes")
    private HashMap<String, Mapper> generatedMapperMap = new HashMap<>();
    @SuppressWarnings("rawtypes")
    private HashMap<String, Converter> converterMap = new HashMap<>();

    public MapperFactory() {
        POOL.importPackage("com.keroz.morphling.mapper");
        POOL.importPackage("com.keroz.morphling.util");
    }

    public static MapperFactory defaultMapperFactory() {
        MapperFactory instance = new MapperFactory();

        instance.addConversionCodeGenerator(new SimpleTypeConversionCodeGenerator());
        instance.addConversionCodeGenerator(new ArrayTypeConversionCodeGenerator());
        instance.addConversionCodeGenerator(new CollectionTypeConversionCodeGenerator());
        instance.addConversionCodeGenerator(new NestedTypeConversionCodeGenerator());

        return instance;
    }

    @SuppressWarnings("unchecked")
    public <Source, Target> Mapper<Source, Target> getMapperFor(Class<Source> sourceClass, Class<Target> targetClass) {
        String mapperClassName = generateMapperClassNameFor(sourceClass, targetClass);

        Mapper<Source, Target> mapper = generatedMapperMap.get(mapperClassName);

        if (mapper == null) {
            try {
                mapper = (Mapper<Source, Target>) generateMapperClassFor(sourceClass, targetClass).newInstance();
                mapper.getClass().getDeclaredField("mapperFactory").set(mapper, this);
                generatedMapperMap.put(mapperClassName, mapper);
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | NoSuchFieldException
                    | SecurityException e) {
                e.printStackTrace();
            }
        }

        return mapper;
    }

    public Class<?> generateMapperClassFor(Class<?> sourceClass, Class<?> targetClass) {
        CtClass sourceCtClass = JavassistUtils.getCtClass(POOL, sourceClass.getName());
        CtClass targetCtClass = JavassistUtils.getCtClass(POOL, targetClass.getName());
        CtClass groupsCtClass = JavassistUtils.getCtClass(POOL, Class[].class.getName());

        try {
            CtClass mapperCtClass = POOL.makeClass(generateMapperClassNameFor(sourceClass, targetClass));
            CtField mapperFactoryField = new CtField(JavassistUtils.getCtClass(POOL, getClass().getName()),
                    "mapperFactory", mapperCtClass);

            mapperFactoryField.setModifiers(Modifier.PUBLIC);
            mapperCtClass.addField(mapperFactoryField);

            CtMethod mapMethod = new CtMethod(CtClass.voidType,
                    "map", new CtClass[] { sourceCtClass, targetCtClass, groupsCtClass }, mapperCtClass);
            String targetClassName = targetClass.getName();
            StringBuilder bodyBuilder = new StringBuilder("{\n");
            ClassSignature classSignature = new ClassSignature(null, null,
                    new ClassType[] { new ClassType(Mapper.class.getName(),
                            new TypeArgument[] { new TypeArgument(new ClassType(sourceClass.getName())),
                                    new TypeArgument(new ClassType(targetClass.getName())) }) });

            mapperCtClass.setInterfaces(new CtClass[] { mapperInterfaceCtClass });
            mapperCtClass.setGenericSignature(classSignature.encode());
            mapMethod.setModifiers(Modifier.PUBLIC);
            bodyBuilder.append(targetClassName).append(" target = $2;\n");

            for (Field targetField : ReflectionUtils.getDeclaredAndInheritedFields(targetClass)) {
                String targetFieldName = targetField.getName();
                String sourceFieldName = targetFieldName;
                Type targetFieldType = targetField.getGenericType();

                AliasFor aliasForAnnotation = targetField.getAnnotation(AliasFor.class);

                if (aliasForAnnotation != null) {
                    sourceFieldName = aliasForAnnotation.value();
                }

                Field sourceField = ReflectionUtils.findDeclaredOrInheritedField(sourceClass, sourceFieldName);

                // Check if sourceClass has the field with the same name
                if (sourceField != null) {
                    Type sourceFieldType = sourceField.getGenericType();
                    String getterPrefix = "boolean".equals(sourceFieldType.getTypeName()) ? "is" : "get";
                    String capitalizedSourceFieldName = StringUtils.capitalize(sourceFieldName);
                    String capitalizedTargetFieldName = StringUtils.capitalize(targetFieldName);
                    String getterName = getterPrefix + capitalizedSourceFieldName;
                    String setter = "target.set" + capitalizedTargetFieldName;
                    String sourceValue = "$1." + getterName + "()";
                    String sourceFieldNonGenericTypeName = getNonGenericTypeName(sourceFieldType);
                    String targetFieldNonGenericTypeName = getNonGenericTypeName(targetFieldType);

                    MapperIgnore mapperIgnoreAnnotation = targetField.getAnnotation(MapperIgnore.class);
                    Class<?>[] groups = null;
                    Policy ignorePolicy = null;
                    boolean hasGroups = false;

                    if (mapperIgnoreAnnotation != null) {
                        ignorePolicy = mapperIgnoreAnnotation.policy();
                        groups = mapperIgnoreAnnotation.groups();
                        hasGroups = groups.length > 0;

                        if (!hasGroups && ignorePolicy == Policy.DEFAULT) {
                            continue;
                        } else {
                            bodyBuilder.append("{boolean shouldIgnore = false;");

                            if (hasGroups) {
                                Class<?> lastGroup = groups[groups.length - 1];

                                bodyBuilder.append("if ($3 != null && $3.length > 0) {")
                                        .append("for (int i = 0; i < $3.length; i++) {")
                                        .append("if ($3[i] == ").append(lastGroup.getName()).append(".class) { ")
                                        .append("shouldIgnore = ").append(Excluded.class.isAssignableFrom(lastGroup))
                                        .append("; break; }}} else { shouldIgnore = ")
                                        .append(Included.class.isAssignableFrom(lastGroup)).append("; }");
                            }

                            bodyBuilder.append(sourceFieldNonGenericTypeName).append(" sv = ")
                                    .append(sourceValue).append(";");
                            switch (ignorePolicy) {
                                case IGNORE_NULL: {
                                    bodyBuilder.append("shouldIgnore = shouldIgnore || sv == null;");
                                    break;
                                }
                                case IGNORE_EMPTY: {
                                    bodyBuilder.append("shouldIgnore = shouldIgnore || ReflectionUtils.isEmpty(sv);");
                                    break;
                                }
                                default: {
                                    break;
                                }
                            }
                            bodyBuilder.append("if (!shouldIgnore) {");
                        }
                    } else {
                        bodyBuilder.append("{{").append(sourceFieldNonGenericTypeName).append(" sv = ")
                                .append(sourceValue).append(";");
                    }

                    // check if converter specified
                    com.keroz.morphling.annotation.Converter converterAnnotation = targetField
                            .getAnnotation(com.keroz.morphling.annotation.Converter.class);

                    if (converterAnnotation != null) {
                        Class<? extends Converter<?, ?>> converterClass = converterAnnotation.value();
                        String className = converterClass.getName();
                        Converter<?, ?> converter = converterMap.get(className);

                        if (converter == null) {
                            try {
                                converter = (Converter<?, ?>) converterClass.newInstance();
                                converterMap.put(className, converter);
                            } catch (InstantiationException | IllegalAccessException e) {
                                e.printStackTrace();
                            }

                            // use converter to convert value
                            bodyBuilder.append(setter).append("((").append(targetFieldNonGenericTypeName)
                                    .append(") mapperFactory.getConverter(\"")
                                    .append(className)
                                    .append("\").convert(");

                            // boxing for primitive types
                            if (ReflectionUtils.isPrimitiveType(sourceFieldType)) {
                                bodyBuilder
                                        .append(ReflectionUtils.toWrapper(sourceFieldType).getName())
                                        .append(".valueOf(")
                                        .append(sourceValue).append("), mapperFactory));");
                            } else {
                                bodyBuilder.append(sourceValue).append(", mapperFactory));");
                            }

                            bodyBuilder.append("}}");

                            continue;
                        }
                    }

                    for (ConversionCodeGenerator codeGenerator : conversionCodeGenerators) {
                        if (codeGenerator.isSupported(sourceFieldType, targetFieldType)) {
                            String code = codeGenerator.generate(sourceFieldType, targetFieldType);

                            if (code != null) {
                                bodyBuilder.append(targetFieldNonGenericTypeName)
                                        .append(" tv;").append(code)
                                        .append(setter).append("(tv);\n");
                            }

                            break;
                        }
                    }

                    bodyBuilder.append("}}");
                }
            }

            String body = bodyBuilder.append("}").toString();
            System.out.println(body);
            mapMethod.setBody(body);
            mapperCtClass.addMethod(mapMethod);

            CtMethod bridgeMethod = new CtMethod(CtClass.voidType,
                    "map", new CtClass[] { objectCtClass, objectCtClass, groupsCtClass }, mapperCtClass);

            bridgeMethod
                    .setBody("{ map((" + sourceClass.getName() + ") $1, (" + targetClass.getName() + ") $2, $3); }");
            mapperCtClass.addMethod(bridgeMethod);

            CtMethod mapMethod2 = new CtMethod(targetCtClass, "map", new CtClass[] { sourceCtClass }, mapperCtClass);
            mapMethod2.setBody("{ " + targetClassName + " target = new " + targetClassName
                    + "(); map($1, target, null); return target; }");
            mapperCtClass.addMethod(mapMethod2);

            CtMethod bridgeMethod2 = new CtMethod(objectCtClass,
                    "map", new CtClass[] { objectCtClass }, mapperCtClass);

            bridgeMethod2.setBody("{ return map((" + sourceClass.getName() + ") $1); }");
            mapperCtClass.addMethod(bridgeMethod2);

            CtMethod mapMethod3 = new CtMethod(targetCtClass, "map", new CtClass[] { sourceCtClass, groupsCtClass },
                    mapperCtClass);
            mapMethod3.setBody("{ " + targetClassName + " target = new " + targetClassName
                    + "(); map($1, target, $2); return target; }");
            mapperCtClass.addMethod(mapMethod3);

            CtMethod bridgeMethod3 = new CtMethod(objectCtClass,
                    "map", new CtClass[] { objectCtClass, groupsCtClass }, mapperCtClass);

            bridgeMethod3.setBody("{ return map((" + sourceClass.getName() + ") $1, $2); }");
            mapperCtClass.addMethod(bridgeMethod3);

            CtMethod mapMethod4 = new CtMethod(CtClass.voidType, "map", new CtClass[] { sourceCtClass, targetCtClass },
                    mapperCtClass);
            mapMethod4.setBody("{ map($1, $2, null); }");
            mapperCtClass.addMethod(mapMethod4);

            CtMethod bridgeMethod4 = new CtMethod(CtClass.voidType,
                    "map", new CtClass[] { objectCtClass, objectCtClass }, mapperCtClass);

            bridgeMethod4.setBody("{ map((" + sourceClass.getName() + ") $1, (" + targetClass.getName() + ") $2); }");
            mapperCtClass.addMethod(bridgeMethod4);

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
}
