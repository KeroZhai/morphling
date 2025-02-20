package io.github.kerozhai.morphling.util;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;
import lombok.experimental.UtilityClass;

@UtilityClass
public class JavassistUtils {

    public CtClass getCtClass(ClassPool pool, String className) {
        try {
            return pool.get(className);
        } catch (NotFoundException ignored) {
        }

        return null;
    }

    public CtField getDeclaredField(CtClass ctClass, String fieldName) {
        try {
            return ctClass.getDeclaredField(fieldName);
        } catch (NotFoundException ignored) {
        }

        return null;
    }

    public CtMethod getDeclaredMethod(CtClass ctClass, String methodName, CtClass... parameters) {
        try {
            return ctClass.getDeclaredMethod(methodName, parameters);
        } catch (NotFoundException ignored) {
        }

        return null;
    }

    public boolean isAssignable(CtClass thisClass, String anotherClassName) {
        if (thisClass == null) {
            return false;
        }
        if (thisClass.getName().equals(anotherClassName)) {
            return true;
        }
        try {
            // Check if extends
            if (isAssignable(thisClass.getSuperclass(), anotherClassName)) {
                return true;
            }
            // Check if implements
            for (CtClass interfaceClass : thisClass.getInterfaces()) {
                if (isAssignable(interfaceClass, anotherClassName)) {
                    return true;
                }
            }
        } catch (NotFoundException e) {
            // keep going
        }

        return false;
    }

    public boolean isPrimitiveOrWrapperType(CtClass ctClass) {
        return ctClass.isPrimitive() || isAssignable(ctClass, "java.lang.Boolean")
                || isAssignable(ctClass, "java.lang.Byte")
                || isAssignable(ctClass, "java.lang.Character") || isAssignable(ctClass, "java.lang.Short")
                || isAssignable(ctClass, "java.lang.Integer") || isAssignable(ctClass, "java.lang.Long")
                || isAssignable(ctClass, "java.lang.Float") || isAssignable(ctClass, "java.lang.Double")
                || isAssignable(ctClass, "java.lang.Void");
    }

    public boolean isWrapper(CtClass ctClass) {
        return isAssignable(ctClass, "java.lang.Boolean")
                || isAssignable(ctClass, "java.lang.Byte")
                || isAssignable(ctClass, "java.lang.Character") || isAssignable(ctClass, "java.lang.Short")
                || isAssignable(ctClass, "java.lang.Integer") || isAssignable(ctClass, "java.lang.Long")
                || isAssignable(ctClass, "java.lang.Float") || isAssignable(ctClass, "java.lang.Double")
                || isAssignable(ctClass, "java.lang.Void");
    }

    public boolean isCollection(CtClass ctClass) {
        return isAssignable(ctClass, "java.util.Collection");
    }

    public boolean isMap(CtClass ctClass) {
        return isAssignable(ctClass, "java.util.Map");
    }

}
