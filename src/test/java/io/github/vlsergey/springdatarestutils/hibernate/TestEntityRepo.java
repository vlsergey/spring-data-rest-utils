package io.github.vlsergey.springdatarestutils.hibernate;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TestEntityRepo extends JpaRepository<TestEntity, UUID> {

}
