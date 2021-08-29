package io.github.vlsergey.springdatarestutils;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.util.Optional;

class HibernateUtils {

    static final String CLASSNAME_CREATION_TIMESTAMP = "org.hibernate.annotations.CreationTimestamp";
    static final String CLASSNAME_FORMULA = "org.hibernate.annotations.Formula";
    static final String CLASSNAME_UPDATE_TIMESTAMP = "org.hibernate.annotations.UpdateTimestamp";

    static final Optional<Class<? extends Annotation>> CLASS_CREATION_TIMESTAMP = ReflectionUtils
	    .findClass(CLASSNAME_CREATION_TIMESTAMP);
    static final Optional<Class<? extends Annotation>> CLASS_FORMULA = ReflectionUtils.findClass(CLASSNAME_FORMULA);
    static final Optional<Class<? extends Annotation>> CLASS_UPDATE_TIMESTAMP = ReflectionUtils
	    .findClass(CLASSNAME_UPDATE_TIMESTAMP);

    static boolean isCreationTimestamp(final PropertyDescriptor pd) {
	return ReflectionUtils.hasAnnotationOnReadMethodOfField(CLASS_CREATION_TIMESTAMP, pd);
    }

    static boolean isFormula(final PropertyDescriptor pd) {
	return ReflectionUtils.hasAnnotationOnReadMethodOfField(CLASS_FORMULA, pd);
    }

    static boolean isUpdateTimestamp(final PropertyDescriptor pd) {
	return ReflectionUtils.hasAnnotationOnReadMethodOfField(CLASS_UPDATE_TIMESTAMP, pd);
    }

}
