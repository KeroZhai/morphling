package io.github.kerozhai.morphling.codegenerator;

import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedType;

import io.github.kerozhai.morphling.util.ReflectionUtils;

public class ArrayTypeConversionCodeGenerator implements ConversionCodeGenerator {

    @Override
    public boolean isSupported(GenerationContext context) {
        return context.getSourceType() instanceof AnnotatedArrayType
                && context.getTargetType() instanceof AnnotatedArrayType;
    }

    @Override
    public String generate(GenerationContext context) {
        AnnotatedType sourceComponentType = ((AnnotatedArrayType) context.getSourceType())
                .getAnnotatedGenericComponentType();
        AnnotatedType targetComponentType = ((AnnotatedArrayType) context.getTargetType())
                .getAnnotatedGenericComponentType();
        StringBuilder builder = new StringBuilder();

        String name = context.getTargetType().getType().getTypeName();
        int indexOfLeftBracket = name.indexOf("[");
        String simpleName = name.substring(0, indexOfLeftBracket);
        int dimensions = name.substring(indexOfLeftBracket).length() / 2;

        if (dimensions > 1) {
            // multi-dimension not supported yet
            return null;
        }

        GenerationContext subContext = context.getSubContext(sourceComponentType, targetComponentType);
        builder.append(context.getTargetVariableName())
                .append(" = null;") // important
                .append("if (").append(context.getSourceVariableName()).append(" != null) {")
                .append("int ").append(context.addSuffix("length")).append(" = ")
                .append(context.getSourceVariableName()).append(".length;")
                .append(context.getTargetVariableName()).append(" = new ").append(simpleName).append("[")
                .append(context.addSuffix("length")).append("];")
                .append("for (int ").append(context.addSuffix("i")).append(" = 0; ").append(context.addSuffix("i"))
                .append(" < ").append(context.addSuffix("length").toString()).append("; ")
                .append(context.addSuffix("i")).append("++) {")
                .append(ReflectionUtils.getSimpleTypeName(sourceComponentType.getType())).append(" ")
                .append(subContext.getSourceVariableName()).append(" = ").append(context.getSourceVariableName())
                .append("[").append(context.addSuffix("i")).append("];")
                .append(ReflectionUtils.getSimpleTypeName(targetComponentType.getType())).append(" ")
                .append(subContext.getTargetVariableName()).append(";");

        for (ConversionCodeGenerator generator : context.getGenerators()) {
            if (generator.isSupported(subContext)) {
                builder.append(generator.generate(subContext));
                break;
            }
        }

        builder
                .append(context.getTargetVariableName()).append("[").append(context.addSuffix("i")).append("] = ")
                .append(subContext.getTargetVariableName()).append(";")
                .append("}");
        builder.append("}");

        return builder.toString();
    }

}
