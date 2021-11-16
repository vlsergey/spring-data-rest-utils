package io.github.vlsergey.springdatarestutils.with_list_of_embeddables;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TestEntityRepo extends JpaRepository<TestEntity, UUID> {

}
