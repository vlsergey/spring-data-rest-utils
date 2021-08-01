package io.github.vlsergey.springdatarestutils;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;

import lombok.NonNull;

class JacksonUtils {

    private static final String CLASSNAME_JSON_IGNORE = "com.fasterxml.jackson.annotation.JsonIgnore";
    private static final String CLASSNAME_JSON_INCLUDE = "com.fasterxml.jackson.annotation.JsonInclude";
    private static final String CLASSNAME_JSON_INCLUDE_INCLUDE = "com.fasterxml.jackson.annotation.JsonInclude$Include";

    private static final Optional<Class<Annotation>> CLASS_JSON_IGNORE = ReflectionUtils
	    .findClass(CLASSNAME_JSON_IGNORE);
    private static final Optional<Class<Annotation>> CLASS_JSON_INCLUDE = ReflectionUtils
	    .findClass(CLASSNAME_JSON_INCLUDE);
    private static final Optional<Class<Enum<?>>> CLASS_JSON_INCLUDE_INCLUDE = ReflectionUtils
	    .findClass(CLASSNAME_JSON_INCLUDE_INCLUDE);

    private static final Optional<Method> METHOD_JSON_IGNORE_VALUE = CLASS_JSON_IGNORE
	    .flatMap(cls -> ReflectionUtils.findMethod(cls, "value"));
    private static final Optional<Method> METHOD_JSON_INCLUDE_VALUE = CLASS_JSON_INCLUDE
	    .flatMap(cls -> ReflectionUtils.findMethod(cls, "value"));

    private static final Optional<Boolean> OP_TRUE = Optional.of(Boolean.TRUE);
    private static final Optional<Boolean> OP_FALSE = Optional.of(Boolean.FALSE);

    private static Optional<String> getJsonIncludeName(final @NonNull Class<?> entityClass) {
	if (!CLASS_JSON_INCLUDE.isPresent() || !CLASS_JSON_INCLUDE_INCLUDE.isPresent()
		|| !METHOD_JSON_INCLUDE_VALUE.isPresent()) {
	    return Optional.empty();
	}

	final Class<? extends Annotation> annClass = CLASS_JSON_INCLUDE.get();
	final Class<? extends Enum<?>> valueClass = CLASS_JSON_INCLUDE_INCLUDE.get();

	final Annotation ann = entityClass.getAnnotation(annClass);
	if (ann == null) {
	    return Optional.empty();
	}

	return Optional.ofNullable(ReflectionUtils.getOrNull(METHOD_JSON_INCLUDE_VALUE.get(), ann, valueClass))
		.map(Enum::name);
    }

    static boolean isJsonIgnore(PropertyDescriptor pd) {
	return CLASS_JSON_IGNORE.flatMap(cls -> ReflectionUtils.findAnnotationOnReadMethodOfField(cls, pd)).flatMap(
		ann -> METHOD_JSON_IGNORE_VALUE.map(method -> ReflectionUtils.getOrNull(method, ann, boolean.class)))
		.orElse(false);
    }

    static Optional<Boolean> nullIncludedInJson(final @NonNull Class<?> entityClass) {
	Optional<String> op = getJsonIncludeName(entityClass);
	if (!op.isPresent()) {
	    return OP_TRUE; // ALWAYS is default
	}
	switch (op.get()) {
	case "ALWAYS":
	case "USE_DEFAULTS":
	    return OP_TRUE;
	case "NON_ABSENT":
	case "NON_EMPTY":
	case "NON_NULL":
	    return OP_FALSE;
	default:
	    return Optional.empty();
	}
    }
}
