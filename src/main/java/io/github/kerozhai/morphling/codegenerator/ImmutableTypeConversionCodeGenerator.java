package io.github.kerozhai.morphling.codegenerator;

import java.lang.reflect.Type;

import io.github.kerozhai.morphling.exception.TypeMismatchException;
import io.github.kerozhai.morphling.util.ReflectionUtils;

/**
 * Generates conversion code between immutable types.
 */
public class ImmutableTypeConversionCodeGenerator implements ConversionCodeGenerator {

    @Override
    public boolean isSupported(GenerationContext context) {
        return ReflectionUtils.isImmutableType(context.getSourceType().getType())
                && ReflectionUtils.isImmutableType(context.getTargetType().getType());
    }

    @Override
    public String generate(GenerationContext context) {
        Type sourceType = context.getSourceType().getType();
        Type targetType = context.getTargetType().getType();
        boolean sourceTypePrimitive = ReflectionUtils.isPrimitiveType(sourceType);
        boolean targetTypePrimitive = ReflectionUtils.isPrimitiveType(targetType);

        if (sourceTypePrimitive && !targetTypePrimitive) {
            if (ReflectionUtils.toWrapper(sourceType).equals(targetType)) {
                return context.getTargetVariableName() + " = " + targetType.getTypeName() + ".valueOf("
                        + context.getSourceVariableName() + ");";
            } else {
                throw new TypeMismatchException("");
            }
        } else if (!sourceTypePrimitive && targetTypePrimitive) {
            if (ReflectionUtils.toWrapper(targetType).equals(sourceType)) {
                return "if (" + context.getSourceVariableName() + " != null) {" + context.getTargetVariableName()
                        + " = " + context.getSourceVariableName() + "."
                        + targetType.getTypeName() + "Value();} else { " + context.getTargetVariableName() + " = "
                        + ReflectionUtils.getDefaultValueStringForPrimitiveType(targetType) + "; }";
            } else {
                throw new TypeMismatchException("");
            }
        } else {
            return context.getTargetVariableName() + " = " + context.getSourceVariableName() + ";";
        }
    }


}
