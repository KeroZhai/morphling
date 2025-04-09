package io.github.kerozhai.morphling.codegenerator;

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import io.github.kerozhai.morphling.util.ReflectionUtils;

@SuppressWarnings("rawtypes")
public class CollectionTypeConversionCodeGenerator implements ConversionCodeGenerator {

    /**
     * A list of excluded collection types that should not be used.
     * <p>
     * For example, Hibernates's {@code PersistentBag}.
     */
    private List<String> excludedCollectionTypes = new ArrayList<>(Arrays.asList(List.class.getName(), "java.util.Arrays$ArrayList"));

    public CollectionTypeConversionCodeGenerator() {
    }

    public CollectionTypeConversionCodeGenerator(List<Class<? extends Collection>> excludedCollectionTypes) {
        if (excludedCollectionTypes != null) {
            excludedCollectionTypes.forEach(
                    (excludedCollectionType) -> this.excludedCollectionTypes.add(excludedCollectionType.getName()));
        }
    }

    /**
     * Add an excluded collection type to the list of excluded collection types.
     *
     * @param excludedCollectionType the excluded collection type to be added.
     */
    public void addExcludedCollectionType(Class<? extends Collection> excludedCollectionType) {
        if (excludedCollectionType != null) {
            excludedCollectionTypes.add(excludedCollectionType.getName());
        }
    }

    /**
     * Add an excluded collection type to the list of excluded collection types by
     * its name.
     *
     * @param excludedCollectionTypeName the name of the excluded collection type to
     *                                   be added.
     * @throws IllegalArgumentException if the specified excluded collection type is
     *                                  not found or not a subtype of
     *                                  java.util.Collection.
     */
    public void addExcludedCollectionType(String excludedCollectionTypeName) {
        try {
            Class<?> excludedCollectionType = Class.forName(excludedCollectionTypeName);

            if (!Collection.class.isAssignableFrom(excludedCollectionType)) {
                throw new IllegalArgumentException(
                        "The specified excluded collection type is not a subtype of java.util.Collection.");
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("The specified excluded collection type is not found.");
        }

        excludedCollectionTypes.add(excludedCollectionTypeName);
    }

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

        builder.append("if (").append(context.getSourceVariableName()).append(" != null) {")
                .append("Class ").append(context.addSuffix("sourceCollectionType")).append(" = ")
                .append(context.getSourceVariableName()).append(".getClass();")
                .append(context.defineInitialValueType());

        GenerationContext subContext = context.getSubContext(sourceComponentType, targetComponentType);
        builder.append("if (").append(context.getInitialValueTypeVariableName()).append(" == null) { if (")
                .append(context.getSourceVariableName()).append(" instanceof ")
                .append(targetTypeClass.getName()).append(") {")
                .append(context.getInitialValueTypeVariableName()).append(" = sourceCollectionType;} else {")
                .append(context.getInitialValueTypeVariableName()).append(" = ").append(targetTypeClass.getName())
                .append(".class;}}");

        if (!excludedCollectionTypes.isEmpty()) {
            builder.append("if (")
                    .append(context.getInitialValueTypeVariableName()).append(" != null && ")
                    .append(String.join("||", excludedCollectionTypes.stream().map(
                            (className) -> context.getInitialValueTypeVariableName() + ".getName().equals(\"" + className + "\")")
                            .collect(Collectors.toList())))
                    .append(") {").append(context.getInitialValueTypeVariableName()).append(" = null; }");
        }

        builder.append("if (").append(context.getInitialValueTypeVariableName()).append(" != null) {")
                .append("try { ").append(context.getTargetVariableName()).append(" = (")
                .append(targetTypeClass.getName())
                .append(") ").append(context.getInitialValueTypeVariableName())
                .append(".newInstance();  } catch (InstantiationException ignored) {} catch (IllegalAccessException ignored) {}} if (")
                .append(context.getTargetVariableName()).append(" == null) { ").append(context.getTargetVariableName())
                .append(" = (").append(targetTypeClass.getName()).append(") mapperFactory.getFallbackObject(")
                .append(context.getSourceVariableName()).append(", ")
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
