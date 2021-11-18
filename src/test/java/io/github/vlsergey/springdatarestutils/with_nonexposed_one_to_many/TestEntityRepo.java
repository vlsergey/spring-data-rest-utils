package io.github.vlsergey.springdatarestutils.with_nonexposed_one_to_many;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TestEntityRepo extends JpaRepository<TestEntity, UUID> {

}
