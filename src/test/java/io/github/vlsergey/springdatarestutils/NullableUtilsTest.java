package io.github.vlsergey.springdatarestutils;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.hateoas.Link;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NullableUtilsTest {

    @Test
    void testGetNullable() throws IntrospectionException {
	PropertyDescriptor pdName = Arrays.stream(Introspector.getBeanInfo(Link.class).getPropertyDescriptors())
		.filter(x -> x.getName().equals("href")).findAny().get();
	final Optional<Boolean> nullable = NullableUtils.getNullable(Link.class, pdName);
	assertTrue(nullable.isEmpty());
    }

}
