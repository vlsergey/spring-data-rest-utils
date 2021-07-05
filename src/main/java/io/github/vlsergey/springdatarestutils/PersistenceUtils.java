package io.github.vlsergey.springdatarestutils;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

import lombok.NonNull;
import lombok.SneakyThrows;

class PersistenceUtils {

    static final String CLASSNAME_BASIC = "javax.persistence.Basic";
    static final String CLASSNAME_COLUMN = "javax.persistence.Column";
    static final String CLASSNAME_DISCRIMINATOR_COLUMN = "javax.persistence.DiscriminatorColumn";
    static final String CLASSNAME_DISCRIMINATOR_VALUE = "javax.persistence.DiscriminatorValue";
    static final String CLASSNAME_GENERATED_VALUE = "javax.persistence.GeneratedValue";
    static final String CLASSNAME_ID = "javax.persistence.Id";
    static final String CLASSNAME_INHERITANCE = "javax.persistence.Inheritance";

    static final Optional<Class<Annotation>> CLASS_BASIC = ReflectionUtils.findClass(CLASSNAME_BASIC);
    static final Optional<Class<Annotation>> CLASS_COLUMN = ReflectionUtils.findClass(CLASSNAME_COLUMN);
    static final Optional<Class<Annotation>> CLASS_DISCRIMINATOR_COLUMN = ReflectionUtils
	    .findClass(CLASSNAME_DISCRIMINATOR_COLUMN);
    static final Optional<Class<Annotation>> CLASS_DISCRIMINATOR_VALUE = ReflectionUtils
	    .findClass(CLASSNAME_DISCRIMINATOR_VALUE);
    static final Optional<Class<Annotation>> CLASS_GENERATED_VALUE = ReflectionUtils
	    .findClass(CLASSNAME_GENERATED_VALUE);
    static final Optional<Class<Annotation>> CLASS_INHERITANCE = ReflectionUtils.findClass(CLASSNAME_INHERITANCE);
    static final Optional<Class<Annotation>> CLASS_ID = ReflectionUtils.findClass(CLASSNAME_ID);

    static final Optional<Method> METHOD_BASIC_OPTIONAL = CLASS_COLUMN
	    .flatMap(cls -> ReflectionUtils.findMethod(cls, "optional"));

    static final Optional<Method> METHOD_COLUMN_NAME = CLASS_COLUMN
	    .flatMap(cls -> ReflectionUtils.findMethod(cls, "name"));

    static final Optional<Method> METHOD_DISCRIMINATOR_VALUE_VALUE = CLASS_DISCRIMINATOR_VALUE
	    .flatMap(cls -> ReflectionUtils.findMethod(cls, "value"));

    static final Optional<Method> METHOD_DISCRIMINATOR_COLUMN_NAME = CLASS_DISCRIMINATOR_COLUMN
	    .flatMap(cls -> ReflectionUtils.findMethod(cls, "name"));

    private static <T extends Annotation> Optional<T> findAnnotationOnReadMethodOfField(final @NonNull Class<T> cls,
	    final @NonNull PropertyDescriptor pd) {
	if (pd.getReadMethod() != null) {
	    final T annotation = pd.getReadMethod().getAnnotation(cls);
	    if (annotation != null) {
		return Optional.of(annotation);
	    }
	}
	try {
	    final Field field = pd.getReadMethod().getDeclaringClass().getDeclaredField(pd.getName());
	    final T annotation = field.getAnnotation(cls);
	    if (annotation != null) {
		return Optional.of(annotation);
	    }
	} catch (Exception exc) {
	    // no field
	}
	return Optional.empty();
    }

    static Optional<Boolean> getBasicOptional(final PropertyDescriptor pd) {
	return CLASS_BASIC.flatMap(cls -> findAnnotationOnReadMethodOfField(cls, pd))
		.flatMap(ann -> METHOD_BASIC_OPTIONAL.map(method -> getOrNull(method, ann, Boolean.class)));
    }

    static String getColumnName(PropertyDescriptor pd) {
	return CLASS_DISCRIMINATOR_VALUE.flatMap(cls -> findAnnotationOnReadMethodOfField(cls, pd))
		.flatMap(ann -> METHOD_DISCRIMINATOR_VALUE_VALUE.map(method -> getOrNull(method, ann, String.class)))
		.orElse(toColumnName(pd.getName()));
    }

    static Optional<String> getDiscriminatorColumnName(Class<?> cls) {
	return CLASS_DISCRIMINATOR_COLUMN.map(cls::getAnnotation)
		.flatMap(ann -> METHOD_DISCRIMINATOR_COLUMN_NAME.map(method -> getOrNull(method, ann, String.class)));
    }

    static Optional<String> getDiscriminatorValue(Class<?> cls) {
	return CLASS_DISCRIMINATOR_VALUE.map(cls::getAnnotation)
		.flatMap(ann -> METHOD_DISCRIMINATOR_VALUE_VALUE.map(method -> getOrNull(method, ann, String.class)));
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    private static <T> T getOrNull(Method method, Annotation obj, Class<T> resultClass) {
	try {
	    return (T) method.invoke(obj);
	} catch (Exception exc) {
	    return null;
	}
    }

    static boolean isGeneratedValue(final PropertyDescriptor pd) {
	return CLASS_GENERATED_VALUE.map(cls -> findAnnotationOnReadMethodOfField(cls, pd).isPresent()).orElse(false);
    }

    static boolean isId(final PropertyDescriptor pd) {
	return CLASS_ID.map(cls -> findAnnotationOnReadMethodOfField(cls, pd).isPresent()).orElse(false);
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
