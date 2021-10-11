package io.github.vlsergey.springdatarestutils.disablesave;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RestResource;

public interface TestEntityRepo extends JpaRepository<TestEntity, UUID> {

    @Override
    @RestResource(exported = false)
    <S extends TestEntity> S save(S entity);

}
