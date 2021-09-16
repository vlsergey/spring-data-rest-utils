package io.github.vlsergey.springdatarestutils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;

import lombok.experimental.UtilityClass;

@UtilityClass
class SpringDataUtils {

    static final Optional<Class<?>> CLASS_PAGEABLE = ReflectionUtils
	    .findClass("org.springframework.data.domain.Pageable");

    static final Optional<Class<? extends Annotation>> CLASS_PROJECTION = ReflectionUtils
	    .findClass("org.springframework.data.rest.core.config.Projection");

    static final Optional<Class<?>> CLASS_QIERYDSL_PREDICATE = ReflectionUtils
	    .findClass("com.querydsl.core.types.Predicate");

    static final Optional<Class<?>> CLASS_QIERYDSL_PREDICATE_EXECUTOR = ReflectionUtils
	    .findClass("org.springframework.data.querydsl.QuerydslPredicateExecutor");

    static final Optional<Class<?>> CLASS_SIMPLE_JPA_REPOSITORY = ReflectionUtils
	    .findClass("org.springframework.data.jpa.repository.support.SimpleJpaRepository");

    private static final Optional<Method> METHOD_PROJECTION_TYPES = CLASS_PROJECTION
	    .flatMap(cls -> ReflectionUtils.findMethod(cls, "types"));
    private static final Optional<Method> METHOD_PROJECTION_NAME = CLASS_PROJECTION
	    .flatMap(cls -> ReflectionUtils.findMethod(cls, "name"));

    static Optional<String> getProjectionName(Class<?> projectionInterface) {
	return ReflectionUtils.getAnnotationAttributeValue(projectionInterface, CLASS_PROJECTION,
		METHOD_PROJECTION_NAME, String.class);
    }

    static Optional<Class[]> getProjectionTypes(Class<?> projectionInterface) {
	return ReflectionUtils.getAnnotationAttributeValue(projectionInterface, CLASS_PROJECTION,
		METHOD_PROJECTION_TYPES, Class[].class);
    }

}
