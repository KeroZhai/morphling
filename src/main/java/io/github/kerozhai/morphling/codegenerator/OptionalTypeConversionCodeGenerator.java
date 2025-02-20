package io.github.kerozhai.morphling.codegenerator;

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;

import io.github.kerozhai.morphling.util.ReflectionUtils;

public class OptionalTypeConversionCodeGenerator implements ConversionCodeGenerator {

    @Override
    public boolean isSupported(GenerationContext context) {
        return ReflectionUtils.isOptionalType(context.getSourceType().getType())
                || ReflectionUtils.isOptionalType(context.getTargetType().getType());
    }

    @Override
    public String generate(GenerationContext context) {
        Type sourceType = context.getSourceType().getType();
        Type targetType = context.getTargetType().getType();
        boolean isSourceOptional = ReflectionUtils.isOptionalType(sourceType);
        boolean isTargetOptional = ReflectionUtils.isOptionalType(targetType);
        StringBuilder codeBuilder = new StringBuilder();

        if (isSourceOptional && isTargetOptional) {
            AnnotatedParameterizedType sourceParameterizedType = (AnnotatedParameterizedType) context.getSourceType();
            AnnotatedParameterizedType targetParameterizedType = (AnnotatedParameterizedType) context.getTargetType();
            AnnotatedType sourceTypeArgument = sourceParameterizedType.getAnnotatedActualTypeArguments()[0];
            AnnotatedType targetTypeArgument = targetParameterizedType.getAnnotatedActualTypeArguments()[0];
            GenerationContext subContext = context.getSubContext(sourceTypeArgument, targetTypeArgument);
            codeBuilder.append(
                    "if (").append(context.getSourceVariableName()).append(" == null) { ")
                    .append(context.getTargetVariableName()).append(" = null; } else if (!")
                    .append(context.getSourceVariableName())
                    .append(".isPresent()) { ").append(context.getTargetVariableName())
                    .append(" = java.util.Optional.empty(); } else { ")
                    .append(sourceTypeArgument.getType().getTypeName())
                    .append(" ").append(subContext.getSourceVariableName()).append(" = (")
                    .append(sourceTypeArgument.getType().getTypeName()).append(") ")
                    .append(context.getSourceVariableName())
                    .append(".get(); ")
                    .append(targetTypeArgument.getType().getTypeName()).append(" ")
                    .append(subContext.getTargetVariableName())
                    .append(" = null; ");

            for (ConversionCodeGenerator generator : context.getGenerators()) {
                if (generator.isSupported(subContext)) {
                    codeBuilder.append(generator.generate(subContext));
                    break;
                }
            }

            codeBuilder.append(context.getTargetVariableName()).append(" = java.util.Optional.ofNullable(")
                    .append(subContext.getTargetVariableName()).append("); }");
        } else if (isSourceOptional) {
            AnnotatedParameterizedType sourceParameterizedType = (AnnotatedParameterizedType) context.getSourceType();
            AnnotatedType sourceTypeArgument = sourceParameterizedType.getAnnotatedActualTypeArguments()[0];
            GenerationContext subContext = context.getSubContext(sourceTypeArgument, context.getTargetType());

            codeBuilder.append(sourceTypeArgument.getType().getTypeName()).append(" ")
                    .append(subContext.getSourceVariableName())
                    .append(" = (").append(context.getSourceVariableName()).append(" == null || !")
                    .append(context.getSourceVariableName()).append(".isPresent()) ? null : (")
                    .append(sourceTypeArgument.getType().getTypeName()).append(") ")
                    .append(context.getSourceVariableName())
                    .append(".get(); ")
                    .append(ReflectionUtils.getSimpleTypeName(targetType)).append(" ")
                    .append(subContext.getTargetVariableName()).append(";");

            for (ConversionCodeGenerator generator : context.getGenerators()) {
                if (generator.isSupported(subContext)) {
                    codeBuilder.append(generator.generate(subContext));
                    break;
                }
            }

            codeBuilder.append(context.getTargetVariableName()).append(" = ").append(subContext.getTargetVariableName())
                    .append(";");
        } else if (isTargetOptional) {
            AnnotatedParameterizedType targetParameterizedType = (AnnotatedParameterizedType) context.getTargetType();
            AnnotatedType targetTypeArgument = targetParameterizedType.getAnnotatedActualTypeArguments()[0];
            GenerationContext subContext = context.getSubContext(context.getSourceType(), targetTypeArgument);

            codeBuilder
                    .append(ReflectionUtils.getSimpleTypeName(sourceType)).append(" ")
                    .append(subContext.getSourceVariableName()).append(" = ").append(context.getSourceVariableName())
                    .append(";")
                    .append(targetTypeArgument.getType().getTypeName())
                    .append(" ").append(subContext.getTargetVariableName())
                    .append(" = null; ");

            for (ConversionCodeGenerator generator : context.getGenerators()) {
                if (generator.isSupported(subContext)) {
                    codeBuilder.append(generator.generate(subContext));
                    break;
                }
            }

            codeBuilder.append(context.getTargetVariableName()).append(" = java.util.Optional.ofNullable(")
                    .append(subContext.getTargetVariableName()).append(");");
        }

        return codeBuilder.toString();
    }

}
