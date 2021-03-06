package io.github.vlsergey.springdatarestutils;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import org.springframework.core.annotation.AnnotationUtils;

import lombok.NonNull;
import lombok.SneakyThrows;

class ReflectionUtils {

    private static final Class<?>[] EMPTY_CLASSES = new Class[0];

    static <A extends Annotation> @NonNull Optional<A> findAnnotationMayBeOnClass(final @NonNull Class<A> annClass,
	    final @NonNull Class<?> targetClass, final @NonNull Method method) {

	A a = AnnotationUtils.findAnnotation(method, annClass);

	if (a == null) {
	    a = AnnotationUtils.findAnnotation(method.getDeclaringClass(), annClass);
	}

	if (a == null) {
	    a = AnnotationUtils.findAnnotation(targetClass, annClass);
	}

	return Optional.ofNullable(a);
    }

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

    static <R> Optional<R> findAnnotationValue(final @NonNull Optional<Class<? extends Annotation>> opCls,
	    final @NonNull Optional<Method> opMethod, final @NonNull PropertyDescriptor pd,
	    final @NonNull Class<R> resultClass) {
	return opCls.flatMap(cls -> findAnnotationOnReadMethodOfField(cls, pd))
		.flatMap(ann -> opMethod.map(method -> ReflectionUtils.getOrNull(method, ann, resultClass)));
    }

    @SuppressWarnings("unchecked")
    static <T> Optional<Class<? extends T>> findClass(String className) {
	try {
	    return Optional.of((Class<T>) Class.forName(className));
	} catch (Exception exc) {
	    return Optional.empty();
	}
    }

    static Optional<Method> findMethod(Class<?> cls, String methodName, Class<?>... paramArgsClasses) {
	return Arrays.stream(cls.getMethods()) //
		.filter(method -> Objects.equals(method.getName(), methodName))
		.filter(method -> method.getParameterCount() == paramArgsClasses.length) //
		.filter(method -> {
		    Class<?>[] parameterTypes = method.getParameterTypes();
		    for (int i = 0; i < parameterTypes.length; i++) {
			Class<?> arg = parameterTypes[i];
			if (!Objects.equals(arg.getName(), paramArgsClasses[i].getName())) {
			    return false;
			}
		    }
		    return true;
		}) //
		.findAny();
    }

    @SafeVarargs
    static Optional<Method> findMethod(Class<?> cls, String methodName, Optional<Class<?>>... paramArgsClasses) {
	if (Arrays.stream(paramArgsClasses).anyMatch(op -> !op.isPresent()))
	    return Optional.empty();

	final Class<?>[] paramClasses = Arrays.stream(paramArgsClasses).map(Optional::get).toArray(Class<?>[]::new);
	return findMethod(cls, methodName, paramClasses);
    }

    @Deprecated
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

    static <T> Optional<Method> findMethod(Class<T> cls, String methodName) {
	return findMethod(cls, methodName, EMPTY_CLASSES);
    }

    static <T> Optional<T> getAnnotationAttributeValue(AnnotatedElement annotated,
	    Optional<Class<? extends Annotation>> opAnnClass, Optional<Method> opMethod, Class<T> resultClass) {
	return opAnnClass.map(annotated::getAnnotation)
		.flatMap(ann -> opMethod.map(method -> ReflectionUtils.getOrNull(method, ann, resultClass)));
    }

    static @NonNull Optional<Class<?>> getCollectionGenericTypeArgument(final @NonNull PropertyDescriptor pd,
	    final int index) {
	return getGenericType(pd).map(type -> {
	    try {
		if (type instanceof ParameterizedType) {
		    Type[] genericArguments = ((ParameterizedType) type).getActualTypeArguments();
		    return Class.forName(genericArguments[index].getTypeName());
		}
	    } catch (ClassNotFoundException exc) {
		// ignore
	    }
	    return null;
	});
    }

    static @NonNull Optional<Type> getGenericType(final @NonNull PropertyDescriptor pd) {
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

    @SneakyThrows
    static <T> @NonNull Optional<T> getOrEmpty(final @NonNull Optional<Method> opMethod, final @NonNull Object obj,
	    final @NonNull Class<T> resultClass) {
	return opMethod.map(method -> getOrNull(method, obj, resultClass));
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    static <T> T getOrNull(Method method, Object obj, Class<T> resultClass) {
	try {
	    return (T) method.invoke(obj);
	} catch (Exception exc) {
	    return null;
	}
    }

    static boolean hasAnnotationOnReadMethodOfField(
	    final @NonNull Optional<Class<? extends Annotation>> annotationClass,
	    final @NonNull PropertyDescriptor pd) {
	return annotationClass.flatMap(cls -> findAnnotationOnReadMethodOfField(cls, pd)).isPresent();
    }

}
