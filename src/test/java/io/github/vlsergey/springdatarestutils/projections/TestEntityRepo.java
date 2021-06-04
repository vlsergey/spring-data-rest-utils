package io.github.vlsergey.springdatarestutils.projections;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(excerptProjection = TestEntityDefaultProjection.class)
public interface TestEntityRepo extends JpaRepository<TestEntity, UUID> {

}
