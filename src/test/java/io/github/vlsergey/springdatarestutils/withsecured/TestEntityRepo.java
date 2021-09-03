package io.github.vlsergey.springdatarestutils.withsecured;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.access.annotation.Secured;

@Secured({ "ROLE_FROM_CLASS" })
public interface TestEntityRepo extends JpaRepository<TestEntity, UUID> {

    @Override
    @Secured({ "ROLE_FROM_METHOD" })
    Page<TestEntity> findAll(Pageable pageable);

    @Secured({ "ROLE_FROM_METHOD" })
    List<TestEntity> findByValue(int value);

}
