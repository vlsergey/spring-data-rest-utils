package io.github.vlsergey.springdatarestutils;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.util.Arrays;
import java.util.Optional;

import javax.persistence.Basic;
import javax.persistence.Column;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import lombok.Getter;

class NullableUtilsTest {

    private static Optional<Boolean> getNullable(final Class<?> beanClass, final String fieldName)
	    throws IntrospectionException {
	return NullableUtils.getNullable(Arrays.stream(Introspector.getBeanInfo(beanClass).getPropertyDescriptors())
		.filter(x -> x.getName().equals(fieldName)).findAny().get());
    }

    @Test
    void testGetNullable() throws IntrospectionException {
	assertEquals(Optional.empty(), getNullable(TestClass.class, "withoutAnnotations"));

	assertEquals(Optional.of(Boolean.TRUE), getNullable(TestClass.class, "withBasicOptional"));
	assertEquals(Optional.of(Boolean.TRUE), getNullable(TestClass.class, "withNullableColumn"));

	assertEquals(Optional.of(Boolean.FALSE), getNullable(TestClass.class, "withBasicRequired"));
	assertEquals(Optional.of(Boolean.FALSE), getNullable(TestClass.class, "withRequiredColumn"));
    }

    @Getter
    public static class TestClass {

	@Basic(optional = true)
	private String withBasicOptional;

	@Basic(optional = false)
	private String withBasicRequired;

	@Column(nullable = true)
	private String withNullableColumn;

	private String withoutAnnotations;

	@Column(nullable = false)
	private String withRequiredColumn;
    }

}
