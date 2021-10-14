package io.github.vlsergey.springdatarestutils;

import org.springframework.hateoas.Link;

import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import lombok.NonNull;

@FunctionalInterface
public interface ClassToRefResolver {

    /**
     * Recommended implementation of
     * {@link #getRefName(Class, ClassMappingMode, RequestType)}
     */
    static String generateName(final TaskProperties taskProperties, final @NonNull Class<?> cls,
	    final @NonNull ClassMappingMode classMappingMode, final @NonNull RequestType requestType) {
	if (!classMappingMode.isCompatibleWith(cls)) {
	    throw new IllegalArgumentException(
		    "Class mode " + classMappingMode + " is not compatible with " + cls.getName());
	}
	if (classMappingMode == ClassMappingMode.EXPOSED && requestType == RequestType.PARAMETER) {
	    throw new IllegalArgumentException("String shall be used to pass entity as parameter, not ref schema");
	}

	if (cls.isAssignableFrom(Link.class)) {
	    return taskProperties.getLinkTypeName();
	}
	if (cls.isEnum()) {
	    return cls.getSimpleName() + taskProperties.getEnumTypeSuffix();
	}

	final StringBuilder sb = new StringBuilder();
	sb.append(requestType.getPrefix().apply(taskProperties));
	sb.append(classMappingMode.getPrefix().apply(taskProperties));
	sb.append(cls.getSimpleName());
	if (cls.isEnum()) {
	    sb.append(taskProperties.getEnumTypeSuffix());
	}
	sb.append(classMappingMode.getSuffix().apply(taskProperties));
	sb.append(requestType.getSuffix().apply(taskProperties));
	return sb.toString();
    }

    default Content getRefContent(final @NonNull Class<?> cls, final @NonNull ClassMappingMode mode,
	    final @NonNull RequestType requestType) {
	final MediaType mediaType = new MediaType().schema(getRefSchema(cls, mode, requestType));
	return new Content().addMediaType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE, mediaType);
    }

    String getRefName(final @NonNull Class<?> cls, final @NonNull ClassMappingMode classMappingMode,
	    final @NonNull RequestType requestType);

    default Schema<Object> getRefSchema(final @NonNull Class<?> cls, final @NonNull ClassMappingMode mode,
	    final @NonNull RequestType requestType) {
	return new Schema<>().$ref(getRefUrl(cls, mode, requestType));
    }

    default String getRefUrl(final @NonNull Class<?> cls, final @NonNull ClassMappingMode mode,
	    final @NonNull RequestType requestType) {
	return "#/components/schemas/" + getRefName(cls, mode, requestType);
    }

    default RequestBody getRequestBody(final @NonNull Class<?> cls, final @NonNull ClassMappingMode classMappingMode,
	    final @NonNull RequestType requestType) {
	return new RequestBody().required(Boolean.TRUE).content(getRefContent(cls, classMappingMode, requestType));
    }

}
