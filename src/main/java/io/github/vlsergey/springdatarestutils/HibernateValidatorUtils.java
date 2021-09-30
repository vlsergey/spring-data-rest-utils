package io.github.vlsergey.springdatarestutils;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;

import lombok.NonNull;

class HibernateValidatorUtils {

    static final Optional<Class<? extends Annotation>> CLASS_LENGTH = ReflectionUtils
	    .findClass("org.hibernate.validator.constraints.Length");

    private static final Optional<Method> METHOD_LENGTH_MAX = CLASS_LENGTH
	    .flatMap(cls -> ReflectionUtils.findMethod(cls, "max"));

    private static final Optional<Method> METHOD_LENGTH_MIN = CLASS_LENGTH
	    .flatMap(cls -> ReflectionUtils.findMethod(cls, "min"));

    static Optional<Integer> getLengthMax(final @NonNull PropertyDescriptor pd) {
	return ReflectionUtils.findAnnotationValue(CLASS_LENGTH, METHOD_LENGTH_MAX, pd, int.class)
		.map(value -> value.intValue() == Integer.MAX_VALUE ? null : value);
    }

    static Optional<Integer> getLengthMin(final @NonNull PropertyDescriptor pd) {
	return ReflectionUtils.findAnnotationValue(CLASS_LENGTH, METHOD_LENGTH_MIN, pd, int.class)
		.map(value -> value.intValue() <= 0 ? null : value);
    }

}
