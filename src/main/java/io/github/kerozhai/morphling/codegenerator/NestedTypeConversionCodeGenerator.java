package io.github.kerozhai.morphling.codegenerator;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;

import io.github.kerozhai.morphling.util.ReflectionUtils;

public class NestedTypeConversionCodeGenerator implements ConversionCodeGenerator {

    @Override
    public boolean isSupported(GenerationContext context) {
        Type sourceType = context.getSourceType().getType();
        Type targetType = context.getTargetType().getType();

        if (ReflectionUtils.isImmutableType(sourceType) || ReflectionUtils.isImmutableType(targetType)
                || ReflectionUtils.isCollectionType(sourceType) || ReflectionUtils.isCollectionType(targetType)
                || ReflectionUtils.isMapType(sourceType) || ReflectionUtils.isMapType(targetType)
                || ReflectionUtils.isOptionalType(sourceType) || ReflectionUtils.isOptionalType(targetType)) {
            return false;
        }

        return true;
    }

    @Override
    public String generate(GenerationContext context) {
        AnnotatedType targetType = context.getTargetType();
        String sourceTypeName = context.getSourceType().getType().getTypeName();
        String targetTypeName = targetType.getType().getTypeName();
        StringBuilder builder = new StringBuilder();

        builder.append(context.defineInitialValueType())
                .append("if (").append(context.getSourceVariableName()).append(" == null) {")
                .append(context.getTargetVariableName()).append(" = null;")
                .append("} else if (").append(context.addSuffix("initialValueType")).append("!= null) {")
                .append("GeneratedMapper " + context.addSuffix("mapper")
                        + " = (GeneratedMapper) mapperFactory.getMapperFor("
                        + context.getSourceVariableName() + ".getClass(), "
                        + context.addSuffix("initialValueType") + ");\n")
                .append(context.getTargetVariableName()).append(" = (").append(targetTypeName).append(")")
                .append(context.addSuffix("initialValueType"))
                .append(".newInstance();")
                .append(context.addSuffix("mapper")).append(".map(").append(context.getSourceVariableName())
                .append(", ").append(context.getTargetVariableName()).append(", context);}")
                .append("else {")
                .append("GeneratedMapper " + context.addSuffix("mapper")
                        + " = (GeneratedMapper) mapperFactory.getMapperFor("
                        + sourceTypeName + ".class, "
                        + targetTypeName + ".class);\n")
                .append("if (").append(context.getTargetVariableName()).append(" == null) {")
                .append(context.getTargetVariableName() + " = (" + targetTypeName
                        + ") " + context.addSuffix("mapper") + ".map(" + context.getSourceVariableName()
                        + ", context);} else  { ")
                .append(context.addSuffix("mapper")).append(".map(").append(context.getSourceVariableName())
                .append(", ").append(context.getTargetVariableName()).append(", context);}")
                .append("}");

        return builder.toString();
    }

}
