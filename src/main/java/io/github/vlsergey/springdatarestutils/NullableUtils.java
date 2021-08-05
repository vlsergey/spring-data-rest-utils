package io.github.vlsergey.springdatarestutils;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

import javax.annotation.Nullable;

import org.springframework.util.ReflectionUtils;

import lombok.NonNull;
import lombok.SneakyThrows;

class NullableUtils {

    @SneakyThrows
    private static @Nullable Boolean getNullable(@NonNull Annotation[] annotations) {
	for (Annotation ann : annotations) {
	    final String name = ann.annotationType().getName();
	    final String lcName = name.toLowerCase();

	    if (lcName.endsWith(".notnull") || lcName.endsWith(".nonnull")) {
		return Boolean.FALSE;
	    }
	    if (lcName.endsWith(".nullable")) {
		return Boolean.TRUE;
	    }
	}
	return null;
    }

    static Optional<Boolean> getNullable(PropertyDescriptor pd) {
	if (pd.getPropertyType().isPrimitive()) {
	    return Optional.of(Boolean.FALSE);
	}

	// TODO: will be nice to check if results are compatible
	return OptionalUtils.coalesce( //
		PersistenceUtils.getBasicOptional(pd), //
		PersistenceUtils.getColumnNullable(pd), //
		getNullableAnnotationPresent(pd));
    }

    static Optional<Boolean> getNullableAnnotationPresent(final @NonNull PropertyDescriptor pd) {
	final Method readMethod = pd.getReadMethod();
	Boolean result = null;

	if (readMethod != null) {
	    result = getNullable(readMethod.getAnnotations());
	    if (result == null) {
		Field field = ReflectionUtils.findField(readMethod.getDeclaringClass(), pd.getName());
		if (field != null) {
		    result = getNullable(field.getAnnotations());
		}
	    }
	}
	return Optional.ofNullable(result);
    }

}
