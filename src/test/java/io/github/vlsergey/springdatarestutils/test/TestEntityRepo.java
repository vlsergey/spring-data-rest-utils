package io.github.vlsergey.springdatarestutils.test;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TestEntityRepo extends JpaRepository<TestEntity, UUID> {

}
