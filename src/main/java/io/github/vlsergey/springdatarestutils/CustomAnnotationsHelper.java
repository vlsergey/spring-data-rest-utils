package io.github.vlsergey.springdatarestutils;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomAnnotationsHelper {

    private final @NonNull Map<String, Class<? extends Annotation>> customAnnotations;

    public CustomAnnotationsHelper(final @NonNull TaskProperties taskProperties) {
	this.customAnnotations = Optional.ofNullable(taskProperties.getAddXCustomAnnotations()).orElse(emptyList())
		.stream().map(clsName -> ReflectionUtils.<Annotation>findClass(clsName).orElse(null))
		.filter(Objects::nonNull)
		.collect(toMap(cls -> "x-" + CaseUtils.camelToKebab(cls.getSimpleName()), identity()));
    }

    private static <A extends Annotation> @NonNull Object getAnnotationValue(final @NonNull A annotation) {
	return ReflectionUtils.findMethod(annotation.getClass(), "value").map(valueMethod -> {
	    try {
		return valueMethod.invoke(annotation);
	    } catch (Exception exc) {
		log.error("Unable to migrate value of " + annotation.getClass().getName(), exc);
		return Boolean.TRUE;
	    }
	}).orElse(Boolean.TRUE);
    }

    public void populateMethod(final @NonNull Class<?> targetClass, final @NonNull Method method,
	    final @NonNull Operation operation) {
	customAnnotations.forEach(
		(extensionName, annClass) -> ReflectionUtils.findAnnotationMayBeOnClass(annClass, targetClass, method)
			.map(CustomAnnotationsHelper::getAnnotationValue)
			.ifPresent(value -> operation.addExtension(extensionName, value)));
    }

    public void populatePropertySchema(final @NonNull PropertyDescriptor pd, final @NonNull Schema<?> targetSchema) {
	customAnnotations.forEach((extensionName, annClass) -> ReflectionUtils
		.findAnnotationOnReadMethodOfField(annClass, pd).map(CustomAnnotationsHelper::getAnnotationValue)
		.ifPresent(value -> targetSchema.addExtension(extensionName, value)));
    }
}
