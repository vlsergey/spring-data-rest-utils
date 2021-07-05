package io.github.vlsergey.springdatarestutils;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Arrays;

import javax.persistence.Id;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vlsergey.springdatarestutils.example.Group;

class PersistenceUtilsTest {

    @Test
    void testClassId() {
	assertTrue(PersistenceUtils.CLASS_ID.isPresent());
	assertEquals(Id.class, PersistenceUtils.CLASS_ID.get());
    }

    @Test
    void testIsId() throws IntrospectionException {
	final BeanInfo groupBeanInfo = Introspector.getBeanInfo(Group.class);

	PropertyDescriptor groupIdPd = Arrays.stream(groupBeanInfo.getPropertyDescriptors())
		.filter(pd -> pd.getName().equals("groupId")).findAny().get();

	assertTrue(PersistenceUtils.isId(groupIdPd));
    }

}
