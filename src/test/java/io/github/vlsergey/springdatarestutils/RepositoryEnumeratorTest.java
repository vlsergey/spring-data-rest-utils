package io.github.vlsergey.springdatarestutils;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy.RepositoryDetectionStrategies;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.vlsergey.springdatarestutils.test.TestEntity;
import io.github.vlsergey.springdatarestutils.test.TestEntityRepo;

class RepositoryEnumeratorTest {

    @Test
    void testEnumerate() {
	final Set<RepositoryMetadata> actualSet = new RepositoryEnumerator(
		RepositoryEnumeratorTest.class.getPackageName() + ".test", RepositoryDetectionStrategies.ALL)
			.enumerate(getClass().getClassLoader());
	assertEquals(1, actualSet.size());

	final RepositoryMetadata member = actualSet.iterator().next();
	assertEquals(TestEntity.class, member.getDomainType());
	assertEquals(TestEntityRepo.class, member.getRepositoryInterface());
    }

}
