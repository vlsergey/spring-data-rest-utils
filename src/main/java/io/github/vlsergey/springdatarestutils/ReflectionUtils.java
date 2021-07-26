package io.github.vlsergey.springdatarestutils;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import lombok.NonNull;

class ReflectionUtils {

    static <T extends Annotation> Optional<T> findAnnotationOnReadMethodOfField(final @NonNull Class<T> cls,
	    final @NonNull PropertyDescriptor pd) {
	if (pd.getReadMethod() != null) {
	    final T annotation = pd.getReadMethod().getAnnotation(cls);
	    if (annotation != null) {
		return Optional.of(annotation);
	    }
	}
	try {
	    final Field field = pd.getReadMethod().getDeclaringClass().getDeclaredField(pd.getName());
	    final T annotation = field.getAnnotation(cls);
	    if (annotation != null) {
		return Optional.of(annotation);
	    }
	} catch (Exception exc) {
	    // no field
	}
	return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    static <T> Optional<Class<T>> findClass(String className) {
	try {
	    return Optional.of((Class<T>) Class.forName(className));
	} catch (Exception exc) {
	    return Optional.empty();
	}
    }

    static Optional<Method> findMethod(Class<?> cls, String methodName, String... paramArgsClassNames) {
	return Arrays.stream(cls.getMethods()) //
		.filter(method -> Objects.equals(method.getName(), methodName))
		.filter(method -> method.getParameterCount() == paramArgsClassNames.length) //
		.filter(method -> {
		    Class<?>[] parameterTypes = method.getParameterTypes();
		    for (int i = 0; i < parameterTypes.length; i++) {
			Class<?> arg = parameterTypes[i];
			if (!Objects.equals(arg.getName(), paramArgsClassNames[i])) {
			    return false;
			}
		    }
		    return true;
		}) //
		.findAny();

    }

    static Optional<Class<?>> getCollectionGenericTypeArgument(PropertyDescriptor pd) {
	if (!Collection.class.isAssignableFrom(pd.getPropertyType()))
	    return Optional.empty();

	return getGenericType(pd).map(type -> {
	    try {
		if (type instanceof ParameterizedType) {
		    Type[] genericArguments = ((ParameterizedType) type).getActualTypeArguments();
		    return Class.forName(genericArguments[0].getTypeName());
		}
	    } catch (ClassNotFoundException exc) {
		// ignore
	    }
	    return null;
	});
    }

    static Optional<Type> getGenericType(PropertyDescriptor pd) {
	if (pd.getReadMethod() != null) {
	    final Type result = pd.getReadMethod().getGenericReturnType();
	    if (result != null) {
		return Optional.of(result);
	    }
	}
	try {
	    final Field field = pd.getReadMethod().getDeclaringClass().getDeclaredField(pd.getName());
	    final Type result = field.getGenericType();
	    if (result != null) {
		return Optional.of(result);
	    }
	} catch (Exception exc) {
	    // no field
	}
	return Optional.empty();
    }

    static <T extends Annotation> boolean hasAnnotationOnReadMethodOfField(
	    final @NonNull Optional<Class<T>> annotationClass, final @NonNull PropertyDescriptor pd) {
	return annotationClass.flatMap(cls -> findAnnotationOnReadMethodOfField(cls, pd)).isPresent();
    }

}
