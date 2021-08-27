package io.github.vlsergey.springdatarestutils;

import org.junit.jupiter.api.Test;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy.RepositoryDetectionStrategies;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.vlsergey.springdatarestutils.CodebaseScannerFacade.ScanResult;
import io.github.vlsergey.springdatarestutils.projections.TestEntityDefaultProjection;
import io.github.vlsergey.springdatarestutils.test.TestEntity;
import io.github.vlsergey.springdatarestutils.test.TestEntityRepo;

class CodebaseScannerFacadeTest {

    private static final String MY_PACKAGE = "io.github.vlsergey.springdatarestutils";

    @Test
    void testScanProjections() {
	final ScanResult scanResult = new CodebaseScannerFacade(MY_PACKAGE + ".projections",
		RepositoryDetectionStrategies.ALL).scan(getClass().getClassLoader());
	assertEquals(1, scanResult.getProjections().size());

	final Class<?> projectionClass = scanResult.getProjections().iterator().next();
	assertEquals(TestEntityDefaultProjection.class, projectionClass);
    }

    @Test
    void testScanSimple() {
	final ScanResult scanResult = new CodebaseScannerFacade(MY_PACKAGE + ".test", RepositoryDetectionStrategies.ALL)
		.scan(getClass().getClassLoader());
	assertEquals(1, scanResult.getRepositories().size());

	final RepositoryMetadata member = scanResult.getRepositories().iterator().next();
	assertEquals(TestEntity.class, member.getDomainType());
	assertEquals(TestEntityRepo.class, member.getRepositoryInterface());
    }

}
