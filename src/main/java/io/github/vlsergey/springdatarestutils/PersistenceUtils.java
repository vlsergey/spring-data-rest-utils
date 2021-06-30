package io.github.vlsergey.springdatarestutils;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

import lombok.SneakyThrows;

class PersistenceUtils {

    static final String CLASSNAME_COLUMN = "javax.persistence.Column";
    static final String CLASSNAME_DISCRIMINATOR_COLUMN = "javax.persistence.DiscriminatorColumn";
    static final String CLASSNAME_DISCRIMINATOR_VALUE = "javax.persistence.DiscriminatorValue";
    static final String CLASSNAME_INHERITANCE = "javax.persistence.Inheritance";

    static final Optional<Class<Annotation>> CLASS_COLUMN = ReflectionUtils.findClass(CLASSNAME_COLUMN);
    static final Optional<Class<Annotation>> CLASS_DISCRIMINATOR_COLUMN = ReflectionUtils
	    .findClass(CLASSNAME_DISCRIMINATOR_COLUMN);
    static final Optional<Class<Annotation>> CLASS_DISCRIMINATOR_VALUE = ReflectionUtils
	    .findClass(CLASSNAME_DISCRIMINATOR_VALUE);
    static final Optional<Class<Annotation>> CLASS_INHERITANCE = ReflectionUtils.findClass(CLASSNAME_INHERITANCE);

    static final Optional<Method> METHOD_COLUMN_NAME = CLASS_COLUMN
	    .flatMap(cls -> ReflectionUtils.findMethod(cls, "name"));

    static final Optional<Method> METHOD_DISCRIMINATOR_VALUE_VALUE = CLASS_DISCRIMINATOR_VALUE
	    .flatMap(cls -> ReflectionUtils.findMethod(cls, "value"));

    static final Optional<Method> METHOD_DISCRIMINATOR_COLUMN_NAME = CLASS_DISCRIMINATOR_COLUMN
	    .flatMap(cls -> ReflectionUtils.findMethod(cls, "name"));

    static String getColumnName(PropertyDescriptor pd) {
	// TODO: check annotation on field as well
	return CLASS_DISCRIMINATOR_VALUE.map(cls -> pd.getReadMethod().getAnnotation(cls))
		.flatMap(ann -> METHOD_DISCRIMINATOR_VALUE_VALUE.map(method -> getStringOrNull(method, ann)))
		.orElse(toColumnName(pd.getName()));
    }

    static Optional<String> getDiscriminatorColumnName(Class<?> cls) {
	return CLASS_DISCRIMINATOR_COLUMN.map(cls::getAnnotation)
		.flatMap(ann -> METHOD_DISCRIMINATOR_COLUMN_NAME.map(method -> getStringOrNull(method, ann)));
    }

    static Optional<String> getDiscriminatorValue(Class<?> cls) {
	return CLASS_DISCRIMINATOR_VALUE.map(cls::getAnnotation)
		.flatMap(ann -> METHOD_DISCRIMINATOR_VALUE_VALUE.map(method -> getStringOrNull(method, ann)));
    }

    @SneakyThrows
    private static String getStringOrNull(Method method, Annotation obj) {
	try {
	    return (String) method.invoke(obj);
	} catch (Exception exc) {
	    return null;
	}
    }

    private static boolean isUnderscoreRequired(char before, char current, char after) {
	return Character.isLowerCase(before) && Character.isUpperCase(current) && Character.isLowerCase(after);
    }

    /**
     * @see org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy
     */
    private static String toColumnName(String fieldName) {
	StringBuilder builder = new StringBuilder(fieldName);
	for (int i = 1; i < builder.length() - 1; i++) {
	    if (isUnderscoreRequired(builder.charAt(i - 1), builder.charAt(i), builder.charAt(i + 1))) {
		builder.insert(i++, '_');
	    }
	}
	return builder.toString();
    }

    public boolean isInheritanceRoot(Class<?> cls) {
	return Arrays.stream(cls.getAnnotations())
		.anyMatch(ann -> CLASSNAME_INHERITANCE.equals(ann.annotationType().getName()));
    }

}
