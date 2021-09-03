package io.github.vlsergey.springdatarestutils.withsecured;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.access.annotation.Secured;

public interface TestEntityRepo extends JpaRepository<TestEntity, UUID> {

    @Override
    @Secured({ "ROLE_USER", "ROLE_ADMIN" })
    Page<TestEntity> findAll(Pageable pageable);

    @Secured({ "ROLE_USER", "ROLE_ADMIN" })
    List<TestEntity> findByValue(int value);

}
