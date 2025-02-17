package com.keroz.morphling.codegenerator;

import java.lang.reflect.AnnotatedType;
import java.util.List;

import com.keroz.morphling.annotation.Mapping;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GenerationContext {

    private AnnotatedType sourceType;

    private AnnotatedType targetType;

    private List<ConversionCodeGenerator> generators;

    private int depth;

    private Mapping mapping;

    public String getSourceVariableName() {
        return addSuffix("sv");
    }

    public String getTargetVariableName() {
        return addSuffix("tv");
    }

    public String getInitialValueTypeVariableName() {
        return addSuffix("initialValueType");
    }

    public String addSuffix(String variableName) {
        return variableName + (depth == 0 ? "" : depth);
    }

    public String defineInitialValueType() {
        StringBuilder definition = new StringBuilder();
        definition.append("Class ").append(getInitialValueTypeVariableName()).append(" = null;");

        if (mapping != null) {
            Mapping.InitialValueTypeMapping[] typeMappings = mapping.initialValueTypeMappings();

            if (typeMappings.length > 0) {
                boolean isFirst = true;

                for (Mapping.InitialValueTypeMapping typeMapping : typeMappings) {
                    if (isFirst) {
                        definition.append("if (");
                        isFirst = false;
                    } else {
                        definition.append("else if (");
                    }

                    definition.append(getSourceVariableName()).append(" instanceof ")
                            .append(typeMapping.sourceType().getName()).append(") {")
                            .append(getInitialValueTypeVariableName()).append(" = ")
                            .append(typeMapping.targetType().getName()).append(".class;}");
                }
            } else {
                Class<?> initialValueType = mapping.initialValueType();

                if (initialValueType != null && initialValueType != void.class) {
                    definition.append(getInitialValueTypeVariableName()).append(" = ")
                            .append(initialValueType.getName()).append(".class;");
                }
            }
        } else {
            Mapping.Generic genericMapping = targetType.getAnnotation(Mapping.Generic.class);

            if (genericMapping != null) {
                Mapping.InitialValueTypeMapping[] typeMappings = genericMapping.initialValueTypeMappings();

                if (typeMappings.length > 0) {
                    boolean isFirst = true;

                    for (Mapping.InitialValueTypeMapping typeMapping : typeMappings) {
                        if (isFirst) {
                            definition.append("if (");
                            isFirst = false;
                        } else {
                            definition.append("else if (");
                        }

                        definition.append(getSourceVariableName()).append(" instanceof ")
                                .append(typeMapping.sourceType().getName()).append(") {")
                                .append(getInitialValueTypeVariableName()).append(" = ")
                                .append(typeMapping.targetType().getName()).append(".class;}");
                    }
                } else {
                    Class<?> initialValueType = genericMapping.initialValueType();

                    if (initialValueType != null && initialValueType != void.class) {
                        definition.append(getInitialValueTypeVariableName()).append(" = ")
                                .append(initialValueType.getName()).append(".class;");
                    }
                }
            }
        }

        return definition.toString();
    }

    public GenerationContext getSubContext(AnnotatedType sourceType, AnnotatedType targetType) {
        return GenerationContext.builder()
                .sourceType(sourceType)
                .targetType(targetType)
                .generators(generators)
                .depth(depth + 1)
                .build();
    }

}
