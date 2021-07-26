package io.github.vlsergey.springdatarestutils;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
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

    static <T extends Annotation> boolean hasAnnotationOnReadMethodOfField(
	    final @NonNull Optional<Class<T>> annotationClass, final @NonNull PropertyDescriptor pd) {
	return annotationClass.flatMap(cls -> findAnnotationOnReadMethodOfField(cls, pd)).isPresent();
    }

}
