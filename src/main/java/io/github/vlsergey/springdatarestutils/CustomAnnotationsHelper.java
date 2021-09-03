package io.github.vlsergey.springdatarestutils;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

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

    private <A extends Annotation> void extractAnnotationValue(A annotation, Consumer<Object> consumer) {
	final Optional<Method> valueMethodOp = ReflectionUtils.findMethod(annotation.getClass(), "value");
	if (!valueMethodOp.isPresent()) {
	    consumer.accept(Boolean.TRUE);
	    return;
	}

	valueMethodOp.ifPresent(valueMethod -> {
	    try {
		final Object value = valueMethod.invoke(annotation);
		consumer.accept(value);
	    } catch (Exception exc) {
		log.error("Unable to migrate value of " + annotation.getClass().getName(), exc);
		consumer.accept(Boolean.TRUE);
	    }
	});
    }

    public void populateMethod(Method method, final @NonNull Operation operation) {
	customAnnotations.forEach((extensionName, annClass) -> Optional.ofNullable(method.getAnnotation(annClass))
		.ifPresent(ann -> extractAnnotationValue(ann, value -> operation.addExtension(extensionName, value))));
    }

    public void populatePropertySchema(PropertyDescriptor pd, final @NonNull Schema<?> targetSchema) {
	customAnnotations.forEach(
		(extensionName, annClass) -> ReflectionUtils.findAnnotationOnReadMethodOfField(annClass, pd).ifPresent(
			ann -> extractAnnotationValue(ann, value -> targetSchema.addExtension(extensionName, value))));
    }
}
