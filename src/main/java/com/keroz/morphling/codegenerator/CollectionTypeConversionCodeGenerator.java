package com.keroz.morphling.codegenerator;

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import com.keroz.morphling.util.ReflectionUtils;

public class CollectionTypeConversionCodeGenerator implements ConversionCodeGenerator {

    @Override
    public boolean isSupported(GenerationContext context) {
        Type sourceType = context.getSourceType().getType();
        Type targetType = context.getTargetType().getType();

        return ReflectionUtils.isCollectionType(sourceType) && ReflectionUtils.isCollectionType(targetType);
    }

    @Override
    public String generate(GenerationContext context) {
        AnnotatedParameterizedType sourceParameterizedType = (AnnotatedParameterizedType) context.getSourceType();
        AnnotatedParameterizedType targetParameterizedType = (AnnotatedParameterizedType) context.getTargetType();
        Class<?> targetTypeClass = (Class<?>) ((ParameterizedType) targetParameterizedType.getType()).getRawType();
        AnnotatedType sourceComponentType = sourceParameterizedType.getAnnotatedActualTypeArguments()[0];
        AnnotatedType targetComponentType = targetParameterizedType.getAnnotatedActualTypeArguments()[0];
        StringBuilder builder = new StringBuilder();

        builder.append(context.getTargetVariableName()).append(" = null;")
                .append("if (").append(context.getSourceVariableName()).append(" != null) {")
                .append("Class ").append(context.addSuffix("sourceCollectionType")).append(" = ")
                .append(context.getSourceVariableName()).append(".getClass();")
                .append(context.defineInitialValueType());

        GenerationContext subContext = context.getSubContext(sourceComponentType, targetComponentType);
        builder.append("if (").append(context.getInitialValueTypeVariableName()).append(" == null) { if (")
                .append(context.getSourceVariableName()).append(" instanceof ")
                .append(targetTypeClass.getName()).append(") {")
                .append(context.getInitialValueTypeVariableName()).append(" = ").append(context.getSourceVariableName())
                .append(".getClass();} else {")
                .append(context.getInitialValueTypeVariableName()).append(" = ").append(targetTypeClass.getName())
                .append(".class;}}")
                .append("if (").append(context.getInitialValueTypeVariableName()).append(" != null) {")
                .append("try { ").append(context.getTargetVariableName()).append(" = (")
                .append(targetTypeClass.getName())
                .append(") ").append(context.getInitialValueTypeVariableName())
                .append(".newInstance();  } catch (InstantiationException ignored) {} catch (IllegalAccessException ignored) {}} if (")
                .append(context.getTargetVariableName()).append(" == null) { ").append(context.getTargetVariableName())
                .append(" = (").append(targetTypeClass.getName()).append(") mapperFactory.getFallbackObject(").append(context.getSourceVariableName()).append(", ")
                .append(targetTypeClass.getName()).append(".class); }")
                .append("if (!").append(context.getSourceVariableName()).append(".isEmpty()) { for (int ")
                .append(context.addSuffix("i")).append(" = 0; ").append(context.addSuffix("i")).append(" < ")
                .append(context.getSourceVariableName()).append(".size(); ").append(context.addSuffix("i"))
                .append("++) {")
                .append(ReflectionUtils.getSimpleTypeName(sourceComponentType.getType())).append(" ")
                .append(subContext.getSourceVariableName()).append(" = ")
                .append(context.getSourceVariableName()).append(".get(").append(context.addSuffix("i")).append(");")
                .append(ReflectionUtils.getSimpleTypeName(targetComponentType.getType()))
                .append(" ").append(subContext.getTargetVariableName()).append(" = null;");

        for (ConversionCodeGenerator generator : context.getGenerators()) {
            if (generator.isSupported(subContext)) {
                builder.append(generator.generate(subContext));
                break;
            }
        }

        builder.append(context.getTargetVariableName()).append(".add(").append(subContext.getTargetVariableName())
                .append("); }}}");

        return builder.toString();
    }

}
