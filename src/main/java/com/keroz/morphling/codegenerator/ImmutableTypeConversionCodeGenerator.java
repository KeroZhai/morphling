package com.keroz.morphling.codegenerator;

import java.lang.reflect.Type;

import com.keroz.morphling.exception.TypeMismatchException;
import com.keroz.morphling.util.ReflectionUtils;

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
                        + getDefaultValueForPrimitiveType(targetType) + "; }";
            } else {
                throw new TypeMismatchException("");
            }
        } else {
            return context.getTargetVariableName() + " = " + context.getSourceVariableName() + ";";
        }
    }

    private String getDefaultValueForPrimitiveType(Type type) {
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
