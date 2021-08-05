package io.github.vlsergey.springdatarestutils;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;

class ValidationUtils {

    static final Optional<Class<Annotation>> CLASS_MAX = ReflectionUtils.findClass("javax.validation.constraints.Max");
    static final Optional<Class<Annotation>> CLASS_MIN = ReflectionUtils.findClass("javax.validation.constraints.Min");

    private static final Optional<Method> METHOD_MAX_VALUE = CLASS_MAX
	    .flatMap(cls -> ReflectionUtils.findMethod(cls, "value"));

    private static final Optional<Method> METHOD_MIN_VALUE = CLASS_MIN
	    .flatMap(cls -> ReflectionUtils.findMethod(cls, "value"));

    static Optional<Long> getMaxValue(PropertyDescriptor pd) {
	return CLASS_MAX.flatMap(cls -> ReflectionUtils.findAnnotationOnReadMethodOfField(cls, pd))
		.flatMap(ann -> METHOD_MAX_VALUE.map(method -> ReflectionUtils.getOrNull(method, ann, Long.class)));
    }

    static Optional<Long> getMinValue(PropertyDescriptor pd) {
	return CLASS_MIN.flatMap(cls -> ReflectionUtils.findAnnotationOnReadMethodOfField(cls, pd))
		.flatMap(ann -> METHOD_MIN_VALUE.map(method -> ReflectionUtils.getOrNull(method, ann, Long.class)));
    }

}
