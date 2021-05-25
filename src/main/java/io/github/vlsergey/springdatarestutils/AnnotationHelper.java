package io.github.vlsergey.springdatarestutils;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import javax.annotation.Nullable;

import org.springframework.util.ReflectionUtils;

import lombok.NonNull;
import lombok.SneakyThrows;

public class AnnotationHelper {

    public static @Nullable Boolean getNullable(final @NonNull Class<?> owner, final @NonNull PropertyDescriptor pd) {
	final Method readMethod = pd.getReadMethod();
	Boolean result = null;

	if (readMethod != null) {
	    result = getNullable(readMethod.getAnnotations());
	}
	if (result == null) {
	    Field field = ReflectionUtils.findField(owner, pd.getName());
	    if (field != null) {
		result = getNullable(field.getAnnotations());
	    }
	}
	return result;
    }

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

	    if (name.contentEquals("javax.persistence.Column")) {
		if (Arrays.stream(annotations).noneMatch(
			a -> a.annotationType().getName().contentEquals("javax.persistence.GeneratedValue"))) {
		    final boolean nullable = (Boolean) ann.getClass().getMethod("nullable").invoke(ann);
		    if (!nullable) {
			return Boolean.FALSE;
		    }
		}

	    }
	}
	return null;
    }

}
