package io.github.vlsergey.springdatarestutils;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

class PersistenceUtils {

    private static final Optional<Class<? extends Annotation>> CLASS_BASIC = ReflectionUtils
	    .findClass("javax.persistence.Basic");
    private static final Optional<Class<? extends Annotation>> CLASS_COLUMN = ReflectionUtils
	    .findClass("javax.persistence.Column");
    private static final Optional<Class<? extends Annotation>> CLASS_DISCRIMINATOR_COLUMN = ReflectionUtils
	    .findClass("javax.persistence.DiscriminatorColumn");
    private static final Optional<Class<? extends Annotation>> CLASS_DISCRIMINATOR_VALUE = ReflectionUtils
	    .findClass("javax.persistence.DiscriminatorValue");
    private static final Optional<Class<? extends Annotation>> CLASS_GENERATED_VALUE = ReflectionUtils
	    .findClass("javax.persistence.GeneratedValue");
    private static final Optional<Class<? extends Annotation>> CLASS_ID = ReflectionUtils
	    .findClass("javax.persistence.Id");
    static final Optional<Class<? extends Annotation>> CLASS_INHERITANCE = ReflectionUtils
	    .findClass("javax.persistence.Inheritance");
    private static final Optional<Class<? extends Annotation>> CLASS_JOIN_COLUMN = ReflectionUtils
	    .findClass("javax.persistence.JoinColumn");

    private static final Optional<Method> METHOD_BASIC_OPTIONAL = CLASS_BASIC
	    .flatMap(cls -> ReflectionUtils.findMethod(cls, "optional"));

    private static final Optional<Method> METHOD_COLUMN_INSERTABLE = CLASS_COLUMN
	    .flatMap(cls -> ReflectionUtils.findMethod(cls, "insertable"));
    private static final Optional<Method> METHOD_COLUMN_LENGTH = CLASS_COLUMN
	    .flatMap(cls -> ReflectionUtils.findMethod(cls, "length"));
    private static final Optional<Method> METHOD_COLUMN_NAME = CLASS_COLUMN
	    .flatMap(cls -> ReflectionUtils.findMethod(cls, "name"));
    private static final Optional<Method> METHOD_COLUMN_NULLABLE = CLASS_COLUMN
	    .flatMap(cls -> ReflectionUtils.findMethod(cls, "nullable"));
    private static final Optional<Method> METHOD_COLUMN_UPDATABLE = CLASS_COLUMN
	    .flatMap(cls -> ReflectionUtils.findMethod(cls, "updatable"));

    private static final Optional<Method> METHOD_DISCRIMINATOR_COLUMN_NAME = CLASS_DISCRIMINATOR_COLUMN
	    .flatMap(cls -> ReflectionUtils.findMethod(cls, "name"));

    private static final Optional<Method> METHOD_DISCRIMINATOR_VALUE_VALUE = CLASS_DISCRIMINATOR_VALUE
	    .flatMap(cls -> ReflectionUtils.findMethod(cls, "value"));

    private static final Optional<Method> METHOD_JOIN_COLUMN_INSERTABLE = CLASS_JOIN_COLUMN
	    .flatMap(cls -> ReflectionUtils.findMethod(cls, "insertable"));
    private static final Optional<Method> METHOD_JOIN_COLUMN_NULLABLE = CLASS_JOIN_COLUMN
	    .flatMap(cls -> ReflectionUtils.findMethod(cls, "nullable"));
    private static final Optional<Method> METHOD_JOIN_COLUMN_UPDATABLE = CLASS_JOIN_COLUMN
	    .flatMap(cls -> ReflectionUtils.findMethod(cls, "updatable"));

    static Optional<Boolean> getBasicOptional(final PropertyDescriptor pd) {
	return ReflectionUtils.findAnnotationValue(CLASS_BASIC, METHOD_BASIC_OPTIONAL, pd, boolean.class);
    }

    static Optional<Integer> getColumnLength(PropertyDescriptor pd) {
	return ReflectionUtils.findAnnotationValue(CLASS_COLUMN, METHOD_COLUMN_LENGTH, pd, int.class);
    }

    static String getColumnName(PropertyDescriptor pd) {
	return ReflectionUtils.findAnnotationValue(CLASS_COLUMN, METHOD_COLUMN_NAME, pd, String.class)
		.orElse(toColumnName(pd.getName()));
    }

    static Optional<Boolean> getColumnNullable(PropertyDescriptor pd) {
	return ReflectionUtils.findAnnotationValue(CLASS_COLUMN, METHOD_COLUMN_NULLABLE, pd, boolean.class);
    }

    static Optional<String> getDiscriminatorColumnName(Class<?> cls) {
	return CLASS_DISCRIMINATOR_COLUMN.map(cls::getAnnotation).flatMap(ann -> METHOD_DISCRIMINATOR_COLUMN_NAME
		.map(method -> ReflectionUtils.getOrNull(method, ann, String.class)));
    }

    static Optional<String> getDiscriminatorValue(Class<?> cls) {
	return CLASS_DISCRIMINATOR_VALUE.map(cls::getAnnotation).flatMap(ann -> METHOD_DISCRIMINATOR_VALUE_VALUE
		.map(method -> ReflectionUtils.getOrNull(method, ann, String.class)));
    }

    static Optional<Boolean> getJoinColumnNullable(PropertyDescriptor pd) {
	return ReflectionUtils.findAnnotationValue(CLASS_JOIN_COLUMN, METHOD_JOIN_COLUMN_NULLABLE, pd, boolean.class);
    }

    static boolean isColumnInsertable(PropertyDescriptor pd) {
	return ReflectionUtils.findAnnotationValue(CLASS_COLUMN, METHOD_COLUMN_INSERTABLE, pd, boolean.class)
		.orElse(true);
    }

    static boolean isColumnUpdatable(PropertyDescriptor pd) {
	return ReflectionUtils.findAnnotationValue(CLASS_COLUMN, METHOD_COLUMN_UPDATABLE, pd, boolean.class)
		.orElse(true);
    }

    static boolean isGeneratedValue(final PropertyDescriptor pd) {
	return ReflectionUtils.hasAnnotationOnReadMethodOfField(CLASS_GENERATED_VALUE, pd);
    }

    static boolean isId(final PropertyDescriptor pd) {
	return ReflectionUtils.hasAnnotationOnReadMethodOfField(CLASS_ID, pd);
    }

    static boolean isJoinColumnInsertable(PropertyDescriptor pd) {
	return ReflectionUtils.findAnnotationValue(CLASS_JOIN_COLUMN, METHOD_JOIN_COLUMN_INSERTABLE, pd, boolean.class)
		.orElse(true);
    }

    static boolean isJoinColumnUpdatable(PropertyDescriptor pd) {
	return ReflectionUtils.findAnnotationValue(CLASS_JOIN_COLUMN, METHOD_JOIN_COLUMN_UPDATABLE, pd, boolean.class)
		.orElse(true);
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
		.anyMatch(ann -> "javax.persistence.Inheritance".equals(ann.annotationType().getName()));
    }

}
