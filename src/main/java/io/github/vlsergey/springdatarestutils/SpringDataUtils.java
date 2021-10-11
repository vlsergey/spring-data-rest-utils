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

    static final Optional<Class<? extends Annotation>> CLASS_REST_RESOURCE = ReflectionUtils
	    .findClass("org.springframework.data.rest.core.annotation.RestResource");

    static final Optional<Class<?>> CLASS_SIMPLE_JPA_REPOSITORY = ReflectionUtils
	    .findClass("org.springframework.data.jpa.repository.support.SimpleJpaRepository");

    private static final Optional<Method> METHOD_PROJECTION_TYPES = CLASS_PROJECTION
	    .flatMap(cls -> ReflectionUtils.findMethod(cls, "types"));
    private static final Optional<Method> METHOD_PROJECTION_NAME = CLASS_PROJECTION
	    .flatMap(cls -> ReflectionUtils.findMethod(cls, "name"));

    private static final Optional<Method> METHOD_REST_RESOURCE_EXPORTED = CLASS_REST_RESOURCE
	    .flatMap(cls -> ReflectionUtils.findMethod(cls, "exported"));

    static boolean isRestResourceExported(Class<?> targetClass, Method method) {
	return CLASS_REST_RESOURCE
		.flatMap(annClass -> ReflectionUtils.findAnnotationMayBeOnClass(annClass, targetClass, method))
		.flatMap(ann -> ReflectionUtils.getOrEmpty(METHOD_REST_RESOURCE_EXPORTED, ann, boolean.class))
		.orElse(true);
    }

    static Optional<String> getProjectionName(Class<?> projectionInterface) {
	return ReflectionUtils.getAnnotationAttributeValue(projectionInterface, CLASS_PROJECTION,
		METHOD_PROJECTION_NAME, String.class);
    }

    static Optional<Class[]> getProjectionTypes(Class<?> projectionInterface) {
	return ReflectionUtils.getAnnotationAttributeValue(projectionInterface, CLASS_PROJECTION,
		METHOD_PROJECTION_TYPES, Class[].class);
    }

}
