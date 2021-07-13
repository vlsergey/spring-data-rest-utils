package io.github.vlsergey.springdatarestutils;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.util.Optional;

class HibernateUtils {

    static final String CLASSNAME_CREATION_TIMESTAMP = "org.hibernate.annotations.CreationTimestamp";
    static final String CLASSNAME_UPDATE_TIMESTAMP = "org.hibernate.annotations.UpdateTimestamp";

    static final Optional<Class<Annotation>> CLASS_CREATION_TIMESTAMP = ReflectionUtils
	    .findClass(CLASSNAME_CREATION_TIMESTAMP);
    static final Optional<Class<Annotation>> CLASS_UPDATE_TIMESTAMP = ReflectionUtils
	    .findClass(CLASSNAME_UPDATE_TIMESTAMP);

    static boolean isCreationTimestamp(final PropertyDescriptor pd) {
	return CLASS_CREATION_TIMESTAMP
		.map(cls -> ReflectionUtils.findAnnotationOnReadMethodOfField(cls, pd).isPresent()).orElse(false);
    }

    static boolean isUpdateTimestamp(final PropertyDescriptor pd) {
	return CLASS_UPDATE_TIMESTAMP.map(cls -> ReflectionUtils.findAnnotationOnReadMethodOfField(cls, pd).isPresent())
		.orElse(false);
    }

}
