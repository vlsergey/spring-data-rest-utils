package io.github.vlsergey.springdatarestutils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class JacksonUtilsTest {

    @Test
    void testIsChildFieldMayBeOmittedFromJson() {
	assertFalse(JacksonUtils.nullIncludedInJson(org.springframework.hateoas.Link.class).get());
    }

}
